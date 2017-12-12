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
    */
	
    static DatagramSocket serverSocket;

    private static final int PORT = 18987;
	
    public static void main(String [] args) throws IOException{
        
        boolean trace = false;
        boolean log = false;
        
        Scanner console = new Scanner(System.in);
        FileWriter logOut = new FileWriter("log.txt");
        FileWriter fileOut;
        
        InetAddress igAddr, hostAddr;
        int igPort, hostPort, lengthOfMessage;
        String fileName = null;
        
        byte[] wholeMsgData = null;
        String wholeMsg = null;
        int index = 0;
        
        CRC32 checker = new CRC32();
        int choice = -1;
        byte[] msgData, ackData;
        int connID, fileSize = 0, seqNum, currSeq = -1;
        
        
        //User options
        while(choice == -1){
            System.out.println("Would you like to use the trace function?\n" +
                             "1 ---------------- Yes\n" +
                             "2 ---------------- No");
                choice = console.nextInt();
                if(choice != 1 && choice != 2){
                    choice = -1;
                    System.out.println("Invalid choice...");
                }
                
                else{
                    if(choice == 1){
                        trace = true;
                        
                        choice = -1;
                        while(choice == -1){
                            System.out.println("Would you like to write it to a log file?\n" +
                                               "1 ---------------- Yes\n" +
                                               "2 ---------------- No");
                                choice = console.nextInt();
                                if(choice != 1 && choice != 2){
                                    choice = -1;
                                    System.out.println("Invalid choice...");
                                }
                                
                                else{
                                    if(choice == 1){
                                        log = true;
                                    }
                                }
                        }
                    }
                }
        }
        
        try {
            // Open a UDP datagram socket with a specified port number
            int portNumber = PORT;
            serverSocket = new DatagramSocket(portNumber);

            System.out.println("Server starts ...");

            // Create a buffer for receiving
            byte[] receivedData = new byte[1516];
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
                if(log) {
                    logOut.write("\nGateway IP address: " + igAddr.toString() +
                                 "\nGateway port number: " + igPort +
                                 "\nMessage: " + message);
                }

                //Establish which kind of message is being received
                if(lengthOfMessage > 14 && receivedData[6] != 0 && receivedData[7] != 0) {//Open message
                    System.out.println("\nOpen message received...");
                    if(log){
                        logOut.write("\n\nOpen message received...");
                    }

                    //Read header contents
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[0], receivedData[1]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("\nConnecton ID: " + connID);
                    }
                    if(log){
                        logOut.write("\n\nConnecton ID: " + connID);
                    }

                    //Get Host IP address
                    byte[] hAddr = new byte[]{receivedData[2], receivedData[3], 
                                                receivedData[4], receivedData[5]};
                        hostAddr = InetAddress.getByAddress(hAddr);
                        
                    if(trace){
                        System.out.println("Host IP address: " + hostAddr.toString());
                    }
                    if(log){
                        logOut.write("\nHost IP address: " + hostAddr.toString());
                    }
                    
                    //Get Host port number
                    byte[] b = new byte[]{receivedData[6], receivedData[7]};
                        low = b[0] >= 0 ? b[0] : 256 + b[0];
                        high = b[1] >= 0 ? b[1] : 256 + b[1];
                            hostPort = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Host port number: " + hostPort);
                    }
                    if(log){
                        logOut.write("\nHost port number: " + hostPort);
                    }
                    
                    //Get file size
                    byte[] c = new byte[]{receivedData[8], receivedData[9],
                                            receivedData[10], receivedData[11]};
                        ByteBuffer size = ByteBuffer.wrap(c);
                        fileSize = size.getInt();
                        
                    if(trace){
                        System.out.println("File size: " + fileSize);
                    }
                    if(log){
                        logOut.write("\nFile size: " + fileSize);
                    }
                    
                    //Get file name
                    byte[] d = new byte[lengthOfMessage - 12];
                    for(int i = 12; i < lengthOfMessage; i++){
                        d[i - 12] = receivedData[i];
                    }
                    fileName = new String(d, 0, d.length);
                    
                    if(trace){
                        System.out.println("File name: " + fileName);
                    }
                    if(log){
                        logOut.write("\nFile name: " + fileName);
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
                    if(log) {
                        logOut.write("\n\nGateway IP address loaded... " + 
                                            Arrays.toString(addr));
                    }
                    
                    //Load Connection ID
                    data[4] = (byte) (connID & 0xFF);
                    data[5] = (byte) ((connID >> 8) & 0xFF);

                    if(trace) {
                        System.out.println("ID loaded... " + connID);
                    }
                    if(log) {
                        logOut.write("\nID loaded... " + connID);
                    }
                    
                    //Load reserved space
                    data[6] = (byte) 0;
                    data[7] = (byte) 0;

                    if(trace) {
                        System.out.println("Zeroes loaded... ");
                    }
                    if(log) {
                        logOut.write("\nZeroes loaded... ");
                    }
                    
                    //Generate CRC value
                    checker.reset();
                    checker.update(ackData);
                   
                    ByteBuffer sum = ByteBuffer.allocate(4);
			sum.putInt((int) checker.getValue());
			byte [] crcValue = new byte[4];
			for(int i = 0; i < crcValue.length; i++) {
				crcValue[i] = sum.get(i);
			}
                   
                    if(trace){
                        System.out.println("CRC value: " + checker.getValue());
                    }
                    if(log){
                        logOut.write("\nCRC value: " + checker.getValue());
                    }
                    
                    //Load CRC value
                    for(int i = 0; i < 4; i++){
                        data[i + 8] = crcValue[i];
                    }
                    
                    if(trace) {
                        System.out.println("CRC value loaded... " + Arrays.toString(crcValue));
                    }
                    if(log) {
                        logOut.write("\nCRC value loaded... " + Arrays.toString(crcValue));
                    }
                    
                    //Load ACK data
                    for(int i = 0; i < 3; i++){
                        data[i + 12] = ackData[i];
                    }
                    
                    if(trace) {
                        String tempMsg = new String(ackData, 0, ackData.length);
                        
                        System.out.println("Data loaded... " + tempMsg);
                        if(log){
                            logOut.write("\nData loaded... " + tempMsg);
                        }
                    }
                    
                    
                    // Create a datagram
                    DatagramPacket datagram = 
                            new DatagramPacket(data, data.length, igAddr, igPort);

                    if(trace) {
                        System.out.println("Message sent: " + Arrays.toString(data));
                    }
                    if(log) {
                        logOut.write("\nMessage sent: " + Arrays.toString(data));
                    }

                    // Send a datagram carrying the connection ACK			
                    serverSocket.send(datagram);

                    System.out.println("\nACK sent...");
                    if(log){
                        logOut.write("\n\nACK sent...");
                    }
                }
                
                else if(lengthOfMessage == 14){                                 //Close message
                    System.out.println("\nClose message received...");
                    if(log){
                        logOut.write("\n\nClose message received...");
                    }
                    
                    //Read header contents
                    //Get Gateway IP address
                    byte[] gAddr = new byte[]{receivedData[0], receivedData[1], 
                                                receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(gAddr);
                        
                    if(trace){
                        System.out.println("\nGateway IP address: " + igAddr.toString());
                    }
                    if(log){
                        logOut.write("\n\nGateway IP address: " + igAddr.toString());
                    }
                    
                    //Get Host IP address
                    byte[] hAddr = new byte[]{receivedData[4], receivedData[5], 
                                                receivedData[6], receivedData[7]};
                        hostAddr = InetAddress.getByAddress(hAddr);
                        
                    if(trace){
                        System.out.println("Host IP address: " + hostAddr.toString());
                    }
                    if(log){
                        logOut.write("\nHost IP address: " + hostAddr.toString());
                    }
                    
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[12], receivedData[13]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Connecton ID: " + connID);
                    }
                    if(log){
                        logOut.write("\nConnecton ID: " + connID);
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
                        System.out.println("\nGateway IP address loaded... " + 
                                            Arrays.toString(addr));
                    }
                    if(log) {
                        logOut.write("\n\nGateway IP address loaded... " + 
                                            Arrays.toString(addr));
                    }
                    
                    //Load Connection ID
                    data[4] = (byte) (connID & 0xFF);
                    data[5] = (byte) ((connID >> 8) & 0xFF);

                    if(trace) {
                        System.out.println("ID loaded... " + connID);
                    }
                    if(log) {
                        logOut.write("\nID loaded... " + connID);
                    }
                    
                    //Load reserved space
                    data[6] = (byte) 0;
                    data[7] = (byte) 0;

                    if(trace) {
                        System.out.println("Zeroes loaded... ");
                    }
                    if(log) {
                        logOut.write("\nZeroes loaded... ");
                    }
                    
                    //Generate CRC value
                    checker.reset();
                    checker.update(ackData);
                    byte[] crcValue = new byte[4];
                        long check = checker.getValue();
			ByteBuffer sum = ByteBuffer.allocate(4);
			sum.putInt((int) check);
			for(int i = 0; i < crcValue.length; i++) {
				crcValue[i] = sum.get(i);
			}
                    
                    //Load CRC value
                    for(int i = 0; i < 4; i++){
                        data[i + 8] = crcValue[i];
                    }
                    
                    if(trace) {
                        System.out.println("CRC value loaded... " + Arrays.toString(crcValue));
                    }
                    if(log) {
                        logOut.write("\nCRC value loaded... " + Arrays.toString(crcValue));
                    }
                    
                    //Load ACK data
                    for(int i = 0; i < 3; i++){
                        data[i + 12] = ackData[i];
                    }
                    
                    if(trace) {
                        String tempMsg = new String(ackData, 0, ackData.length);
                        
                        System.out.println("Data loaded... " + tempMsg);
                        if(log){
                            logOut.write("\nData loaded... " + tempMsg);
                        }
                    }
                    
                    // Create a datagram
                    DatagramPacket datagram = 
                            new DatagramPacket(data, data.length, igAddr, igPort);

                    if(trace) {
                        System.out.println("Message sent: " + Arrays.toString(data));
                    }
                    if(log) {
                        logOut.write("\nMessage sent: " + Arrays.toString(data));
                    }

                    // Send a datagram carrying the closing ACK			
                    serverSocket.send(datagram);

                    System.out.println("\nACK sent...");
                    if(log){
                        logOut.write("\n\nACK sent...");
                    }
                    
                    //Convert file bytes to String
                    wholeMsg = new String(wholeMsgData);

                    //Copy file to file
                    fileOut = new FileWriter(fileName);
                        fileOut.write(wholeMsg);
                        fileOut.close();
                    
                    //Reset variables
                    hostAddr = null;
                    hostPort = -1;
                    connID = -1;
                    seqNum = -1;
                    currSeq = -1;
                    
                    //Close log file
                    if(log){
                        logOut.close();
                    }
                }
                
                else {                                                          //Data message
                    System.out.println("\nData message received...");
                    if(log){
                        logOut.write("\n\nData message received...");
                    }
                    
                    //Read header contents
                    //Get Gateway IP address
                    byte[] gAddr = new byte[]{receivedData[0], receivedData[1], 
                                                receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(gAddr);
                        
                    if(trace){
                        System.out.println("\nGateway IP address: " + igAddr.toString());
                    }
                    if(log){
                        logOut.write("\n\nGateway IP address: " + igAddr.toString());
                    }
                    
                    //Get Connection ID
                    byte[] a = new byte[]{receivedData[4], receivedData[5]};
                        int low = a[0] >= 0 ? a[0] : 256 + a[0];
                        int high = a[1] >= 0 ? a[1] : 256 + a[1];
                            connID = low | (high << 8);
                            
                    if(trace){
                        System.out.println("Connecton ID: " + connID);
                    }
                    if(log){
                        logOut.write("\nConnecton ID: " + connID);
                    }
                    
                    //Get SEQ number
                    byte[] d = new byte[]{receivedData[8], receivedData[9]};
                        low = d[0] >= 0 ? d[0] : 256 + d[0];
                        high = d[1] >= 0 ? d[1] : 256 + d[1];
                            seqNum = low | (high << 8);
                            
                    if(trace) {
                        System.out.println("Sequence number: " + seqNum);
                    }
                    if(log) {
                        logOut.write("\nSequence number: " + seqNum);
                    }
                            
                    //Get length of data
                    byte[] e = new byte[]{receivedData[10], receivedData[11]};
                        low = e[0] >= 0 ? e[0] : 256 + e[0];
                        high = e[1] >= 0 ? e[1] : 256 + e[1];
                            int dataLength = low | (high << 8);
                            
                    if(trace) {
                        System.out.println("Length of data: " + dataLength);
                    }
                    if(log) {
                        logOut.write("\nLength of data: " + dataLength);
                    }
                    
                    //Read data
                    msgData = new byte[dataLength];
                    for(int i = 0; i < dataLength; i++){
                        msgData[i] = receivedData[i + 16];
                    }
                    
                    //Check for errors
                    checker.reset();
                    checker.update(msgData);
                    if(trace){
                        System.out.println("Calculated CRC: " + checker.getValue());
                    }
                    if(log){
                        logOut.write("\nCalculated CRC: " + checker.getValue());
                    }
                    
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
                        if(log) {
                           logOut.write("\nCRC32 value: " + checkVal);
                        }

                        if(trace) {
                            System.out.println("Comparing " + checker.getValue() +
                                               " and " + checkVal);
                        }
                        if(log) {
                            logOut.write("\nComparing " + checker.getValue() +
                                               " and " + checkVal);
                        }
                        
                        if(checker.getValue() != checkVal) {//Error detected
                            //Do nothing
                            System.out.println("Error detected...");
                            
                            if(log) {
                                logOut.write("\nError detected...");
                            }
                        }

                        else {//No error detected
                        if(seqNum != currSeq){
                            //Load into wholeMsgData
                            if(index == 0){
                                wholeMsgData = new byte[fileSize];
                            }

                            for(int i = 0; i < msgData.length; i++){
                                wholeMsgData[index++] = msgData[i];
                            }

                            currSeq = seqNum;
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
                            System.out.println("\nGateway IP address loaded... " + 
                                                Arrays.toString(addr));
                        }
                        if(log) {
                            logOut.write("\n\nGateway IP address loaded... " + 
                                                Arrays.toString(addr));
                        }

                        //Load Connection ID
                        data[4] = (byte) (connID & 0xFF);
                        data[5] = (byte) ((connID >> 8) & 0xFF);

                        if(trace) {
                            System.out.println("ID loaded... " + connID);
                        }
                        if(log) {
                            logOut.write("\nID loaded... " + connID);
                        }

                        //Load reserved space
                        data[6] = (byte) 0;
                        data[7] = (byte) 0;

                        if(trace) {
                            System.out.println("Zeroes loaded... ");
                        }
                        if(log) {
                            logOut.write("\nZeroes loaded... ");
                        }

                        //Generate CRC value
                        checker.reset();
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
                        if(log) {
                            logOut.write("\nCRC value loaded... " + Arrays.toString(crcValue));
                        }

                        //Load ACK data
                        for(int i = 0; i < 2; i++){
                            data[i + 12] = ackData[i];
                        }

                        if(trace) {
                            String tempMsg = new String(ackData, 0, ackData.length);

                            System.out.println("Data loaded... " + tempMsg);
                            if(log){
                                logOut.write("Data loaded... " + tempMsg);
                            }
                        }

                        // Create a datagram
                        DatagramPacket datagram = 
                                new DatagramPacket(data, data.length, igAddr, igPort);

                        if(trace) {
                            System.out.println("Message sent: " + Arrays.toString(data));
                        }
                        if(log) {
                            logOut.write("\nMessage sent: " + Arrays.toString(data));
                        }

                        // Send a datagram carrying the data ACK			
                        serverSocket.send(datagram);

                        System.out.println("\nACK sent...");
                        if(log){
                            logOut.write("\n\nACK sent...");
                        }
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
