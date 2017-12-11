/*
 * 
 * 
 * 
 * 
 */

import java.io.*;
import java.net.*;
import java.lang.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;


public class Server {
    
    /*TODO
    * Figure out lastSeq
    * Write to file
    */
	
    static DatagramSocket serverSocket;

    private static final int PORT = 18987;
	
    public static void main(String [] args){
        
        boolean trace = true;
        
        InetAddress igAddr, hostAddr;
        int igPort, hostPort, lengthOfMessage;
        
        byte[] wholeMsgData = null;
        String wholeMsg = null;
        int index = 0;
        
        CRC32 checker = new CRC32();
        byte[] msgData, ackData;
        int connID, fileSize, seqNum, lastSeq;
        
        
        try {
            // Open a UDP datagram socket with a specified port number
            int portNumber = PORT;
            serverSocket = new DatagramSocket(portNumber);

            System.out.println("Server starts ...");

            // Create a buffer for receiving
            byte[] receivedData = new byte[2048];
            // Run forever
            while (true) {
                // Create a datagram
                DatagramPacket receivedDatagram =
                        new DatagramPacket(receivedData, receivedData.length);

                // Receive a datagram			
                serverSocket.receive(receivedDatagram);

                // Prepare for sending an echo message
                igAddr = receivedDatagram.getAddress();			
                igPort = receivedDatagram.getPort();
                lengthOfMessage = receivedDatagram.getLength();			
                String message = new String(receivedData, 0, receivedDatagram.getLength());

                if(trace) {
                    System.out.println("\nGateway IP address: " + igAddr.toString() +
                                       "\nGateway port number: " + igPort +
                                       "\nMessage: " + message);
                }

                //Establish which kind of message is being received
                if(lengthOfMessage == 8) {                                      //Open message
                    //Read header contents
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[0], receivedData[1]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Connecton ID: " + connID);
                    }

                    //Get Host IP address
                    byte[] hAddr = new byte[]{receivedData[2], receivedData[3], 
                                                receivedData[4], receivedData[5]};
                        hostAddr = InetAddress.getByAddress(hAddr);
                        
                    if(trace){
                        System.out.println("Host IP address: " + hostAddr.toString());
                    }
                    
                    //Get Host port number
                    byte[] b = new byte[]{receivedData[6], receivedData[7]};
                        low = b[0] >= 0 ? b[0] : 256 + b[0];
                        high = b[1] >= 0 ? b[1] : 256 + b[1];
                            hostPort = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Host port number: " + hostPort);
                    }
                    
                    //Send ACK
                    //Create ACK data
                    String temp = "ACK";
                    ackData = temp.getBytes();

                    // Create a buffer for sending
                    byte[] data = new byte[15];
                    
                    //Load Gateway IP address
                    byte[] addr = new byte[4];
                        addr = igAddr.getAddress();
                    for(int i = 0; i < 4; i++){
                        data[i] = addr[i];
                    }

                    if(trace) {
                        System.out.println("Gateway IP address loaded... " + 
                                            Arrays.toString(addr));
                    }
                    
                    //Load Connection ID
                    data[4] = (byte) (connID & 0xFF);
                    data[5] = (byte) ((connID >> 8) & 0xFF);

                    if(trace) {
                        System.out.println("ID loaded... " + connID);
                    }
                    
                    //Load reserved space
                    data[6] = (byte) 0;
                    data[7] = (byte) 0;

                    if(trace) {
                        System.out.println("Zeroes loaded... ");
                    }
                    
                    //Generate CRC value
                    checker.reset();
                    checker.update(ackData);
                    byte[] crcValue = new byte[4];
                        crcValue[0] = (byte) (checker.getValue() & 0xFF);
                        crcValue[1] = (byte) ((checker.getValue() >> 8) & 0xFF);
                        crcValue[2] = (byte) ((checker.getValue() >> 16) & 0xFF);
                        crcValue[3] = (byte) ((checker.getValue() >> 24));
                        
                    if(trace){
                        System.out.println("CRC value: " + checker.getValue());
                    }
                    
                    //Load CRC value
                    for(int i = 0; i < 4; i++){
                        data[i + 8] = crcValue[i];
                    }
                    
                    if(trace) {
                        System.out.println("CRC value loaded... " + Arrays.toString(crcValue));
                    }
                    
                    //Load ACK data
                    for(int i = 0; i < 3; i++){
                        data[i + 12] = ackData[i];
                    }
                    
                    if(trace) {
                        String tempMsg = new String(ackData, 0, ackData.length);
                        
                        System.out.println("Data loaded... " + tempMsg);
                    }
                    
                    // Create a datagram
                    DatagramPacket datagram = 
                            new DatagramPacket(data, data.length, igAddr, igPort);

                    if(trace) {
                        System.out.println("Message sent: " + Arrays.toString(data));
                    }

                    // Send a datagram carrying the connection ACK			
                    serverSocket.send(datagram);

                    System.out.println("ACK sent...");
                }
                
                else if(lengthOfMessage == 14){                                 //Close message
                    //Read header contents
                    //Get Gateway IP address
                    byte[] gAddr = new byte[]{receivedData[0], receivedData[1], 
                                                receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(gAddr);
                        
                    if(trace){
                        System.out.println("Gateway IP address: " + igAddr.toString());
                    }
                    
                    //Get Host IP address
                    byte[] hAddr = new byte[]{receivedData[4], receivedData[5], 
                                                receivedData[6], receivedData[7]};
                        hostAddr = InetAddress.getByAddress(hAddr);
                        
                    if(trace){
                        System.out.println("Host IP address: " + hostAddr.toString());
                    }
                    
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[12], receivedData[13]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Connecton ID: " + connID);
                    }
                    
                    //Send ACK
                    //Create ACK data
                    String temp = "END";
                    ackData = temp.getBytes();
                    
                    // Create a buffer for sending
                    byte[] data = new byte[15];
                    
                    //Load Gateway IP address
                    byte[] addr = new byte[4];
                        addr = igAddr.getAddress();
                    for(int i = 0; i < 4; i++){
                        data[i] = addr[i];
                    }

                    if(trace) {
                        System.out.println("Gateway IP address loaded... " + 
                                            Arrays.toString(addr));
                    }
                    
                    //Load Connection ID
                    data[4] = (byte) (connID & 0xFF);
                    data[5] = (byte) ((connID >> 8) & 0xFF);

                    if(trace) {
                        System.out.println("ID loaded... " + connID);
                    }
                    
                    //Load reserved space
                    data[6] = (byte) 0;
                    data[7] = (byte) 0;

                    if(trace) {
                        System.out.println("Zeroes loaded... ");
                    }
                    
                    //Generate CRC value
                    checker.update(ackData);
                    byte[] crcValue = new byte[4];
                        crcValue[0] = (byte) (checker.getValue() & 0xFF);
                        crcValue[1] = (byte) ((checker.getValue() >> 8) & 0xFF);
                        crcValue[2] = (byte) ((checker.getValue() >> 16) & 0xFF);
                        crcValue[3] = (byte) ((checker.getValue() >> 24));
                    
                    //Load CRC value
                    for(int i = 0; i < 4; i++){
                        data[i + 8] = crcValue[i];
                    }
                    
                    if(trace) {
                        System.out.println("CRC value loaded... " + Arrays.toString(crcValue));
                    }
                    
                    //Load ACK data
                    for(int i = 0; i < 3; i++){
                        data[i + 12] = ackData[i];
                    }
                    
                    if(trace) {
                        String tempMsg = new String(ackData, 0, ackData.length);
                        
                        System.out.println("Data loaded... " + tempMsg);
                    }
                    
                    // Create a datagram
                    DatagramPacket datagram = 
                            new DatagramPacket(data, data.length, igAddr, igPort);

                    if(trace) {
                        System.out.println("Message sent: " + Arrays.toString(data));
                    }

                    // Send a datagram carrying the closing ACK			
                    serverSocket.send(datagram);

                    System.out.println("ACK sent...");
                    
                    //Reset variables
                    hostAddr = null;
                    hostPort = -1;
                    connID = -1;
                    seqNum = -1;
                    lastSeq = -1;
                }
                
                else {                                                          //Data message
                    //Read header contents
                    //Get Gateway IP address
                    byte[] gAddr = new byte[]{receivedData[0], receivedData[1], 
                                                receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(gAddr);
                        
                    if(trace){
                        System.out.println("Gateway IP address: " + igAddr.toString());
                    }
                    
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[4], receivedData[5]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Connecton ID: " + connID);
                    }
                    
                    //Get type of file
                    int type = ((receivedData[6] & 0xc0) >> 6);
                    if(type == 1){//Bit file
                        if(trace) {
                            System.out.println("Bit file");
                        }
                    }
                    
                    else if(type == 2){//Txt file
                        if(trace) {
                            System.out.println("Txt file");
                        }
                    }
                    
                    else {
                        System.out.println("File type not supported");
                    }
                    
                    //Get size of file
                    byte[] c = new byte[]{(byte)(receivedData[6] & 0x3f), receivedData[7]};
                        low = c[0] >= 0 ? c[0] : 256 + c[0];
                        high = c[1] >= 0 ? c[1] : 256 + c[1];
                            fileSize = low | (high << 8);
                    
                    if(trace) {
                        System.out.println("Size of file: " + fileSize);
                    }
                    
                    //Get SEQ number
                    byte[] d = new byte[]{receivedData[8], receivedData[9]};
                        low = d[0] >= 0 ? d[0] : 256 + d[0];
                        high = d[1] >= 0 ? d[1] : 256 + d[1];
                            seqNum = low | (high << 8);
                            
                    if(trace) {
                        System.out.println("Sequence number: " + seqNum);
                    }
                            
                    //Get length of data
                    byte[] e = new byte[]{receivedData[10], receivedData[11]};
                        low = e[0] >= 0 ? e[0] : 256 + e[0];
                        high = e[1] >= 0 ? e[1] : 256 + e[1];
                            int dataLength = low | (high << 8);
                            
                    if(trace) {
                        System.out.println("Length of data: " + dataLength);
                    }
                    
                    //Read data
                    msgData = new byte[dataLength];
                    for(int i = 0; i < dataLength; i++){
                        msgData[i] = receivedData[i + 16];
                    }
                    
                    //Check for errors
                    checker.update(msgData);
                    byte[] checkArray = new byte[]{receivedData[12], receivedData[13],
                                                    receivedData[14], receivedData[15]};
                        ByteBuffer bb = ByteBuffer.wrap(checkArray);
                                long checkValue = bb.getInt();

                                long checkVal = 0;

                                if(checkValue < 0){
                                    checkVal = checkValue + 2147483647 + 2147483647 + 2;
                                }

                                else {
                                    checkVal = checkValue;
                                }


                        if(trace) {
                           System.out.println("CRC32 value: " + checkVal);
                        }

                        if(trace) {
                            System.out.println("Comparing " + checker.getValue() +
                                               " and " + checkVal);
                        }
                        if(checker.getValue() != checkVal) {//Error detected
                            //Do nothing
                            if(trace) {
                                System.out.println("Error detected...");
                            }
                        }

                        else {//No error detected
                        //Load into wholeMsgData
                        if(index == 0){
                            wholeMsgData = new byte[fileSize];
                        }
                        
                        for(int i = 0; i < msgData.length; i++){
                            wholeMsgData[index++] = msgData[i];
                        }
                        
                        if(trace) {
                            wholeMsg = new String(wholeMsgData, 0, wholeMsgData.length);
                            
                            System.out.println("Message: " + wholeMsg);
                        }
                    
                        //Send ACK
                        //Create ACK data
                        ackData = new byte[2];
                            ackData[0] = (byte) (seqNum & 0xFF);
                            ackData[1] = (byte) ((seqNum >> 8) & 0xFF);
                            
                        // Create a buffer for sending
                        byte[] data = new byte[14];

                        //Load Gateway IP address
                        byte[] addr = new byte[4];
                            addr = igAddr.getAddress();
                        for(int i = 0; i < 4; i++){
                            data[i] = addr[i];
                        }

                        if(trace) {
                            System.out.println("Gateway IP address loaded... " + 
                                                Arrays.toString(addr));
                        }

                        //Load Connection ID
                        data[4] = (byte) (connID & 0xFF);
                        data[5] = (byte) ((connID >> 8) & 0xFF);

                        if(trace) {
                            System.out.println("ID loaded... " + connID);
                        }

                        //Load reserved space
                        data[6] = (byte) 0;
                        data[7] = (byte) 0;

                        if(trace) {
                            System.out.println("Zeroes loaded... ");
                        }

                        //Generate CRC value
                        checker.update(ackData);
                        byte[] crcValue = new byte[4];
                            crcValue[0] = (byte) (checker.getValue() & 0xFF);
                            crcValue[1] = (byte) ((checker.getValue() >> 8) & 0xFF);
                            crcValue[2] = (byte) ((checker.getValue() >> 16) & 0xFF);
                            crcValue[3] = (byte) ((checker.getValue() >> 24));

                        //Load CRC value
                        for(int i = 0; i < 4; i++){
                            data[i + 8] = crcValue[i];
                        }

                        if(trace) {
                            System.out.println("CRC value loaded... " + Arrays.toString(crcValue));
                        }

                        //Load ACK data
                        for(int i = 0; i < 3; i++){
                            data[i + 12] = ackData[i];
                        }

                        if(trace) {
                            String tempMsg = new String(ackData, 0, ackData.length);

                            System.out.println("Data loaded... " + tempMsg);
                        }

                        // Create a datagram
                        DatagramPacket datagram = 
                                new DatagramPacket(data, data.length, igAddr, igPort);

                        if(trace) {
                            System.out.println("Message sent: " + Arrays.toString(data));
                        }

                        // Send a datagram carrying the data ACK			
                        serverSocket.send(datagram);

                        System.out.println("ACK sent...");
                    }
                }
            }
	} 
		
        catch (IOException ioEx) {
            ioEx.printStackTrace();
	}
        
	finally {
            // Close the socket 
            serverSocket.close();
	}            
    }
}
