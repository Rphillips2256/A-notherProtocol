/*
 * The gateway is in charge of forwarding packets back and forth from the client to server.
 * Also has error generation to simulate packets being dropped or has multiple errors
 * maintains a connection table for the forwarding of packets
 * makes the connection to the server and maintains priority order.
 * 
 */

import java.io.*;
import java.net.*;
import java.lang.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.CRC32;


public class MyGateway {

    /*TODO
    * Log to file
    */
	
    static DatagramSocket serverSocket;
    

    private static final int PORT = 58989;
	
    public static void main(String [] args){
        
        boolean trace = true;
        boolean log = false;
        boolean lost = false;
        int errorRate = 0, oneRate = 0, twoRate = 0;
        
        Random random = new Random();
        Scanner console = new Scanner(System.in);
        int input = -1;
        
        int tableSize = 10;
        int tableCount = 0;
        Connection[] connectionTable = new Connection[tableSize];
        Connection newConn = new Connection();
        int id = random.nextInt(32767);
        int index = -1;
        
        
        InetAddress igAddr, senderAddr, receiverAddr;
        int senderPort, receiverPort, priority, lengthOfMessage;
        
        CRC32 checker = new CRC32();
        byte[] messageData;
        int connID, seqNum;
        String fileName = null;
        Connection currConn = new Connection();
        
        //Stat variables
        int fileSize = 0;
        long endTime;
        String last = "";
        int appCount = 0, udpCount = 0;
        int rtMax = 0,rtCount = 0, resent = 0;
        
        
        //User options
        while(input == -1){
            System.out.print("Enter desired error rate: ");
                input = console.nextInt();
                
                if(input < 0 || input > 100){
                    input = -1;
                }
                
                else {
                    errorRate = input;
                }
        }
            input = -1;
        
        while(input == -1){
            System.out.print("Enter desired one-bit error rate: ");
                input = console.nextInt();
                
                if(input < 0 || input > 100){
                    input = -1;
                }
                
                else {
                    oneRate = input;
                }
        }
            input = -1;
            
        while(input == -1){
            System.out.print("Enter desired two-bit error rate: ");
                input = console.nextInt();
                
                if(input < 0 || input > 100 || (input + oneRate) > 100){
                    input = -1;
                }
                
                else {
                    twoRate = input;
                }
        }
            input = -1;
        
        
        try {
            // Open a UDP datagram socket with a specified port number
            int portNumber = PORT;
            serverSocket = new DatagramSocket(portNumber);

            System.out.println("Gateway starts...");

            // Create a buffer for receiving
            byte[] receivedData = new byte[1516];
            // Run forever
            while (true) {
                System.out.println("\nWaiting for message...");
                
                // Create a datagram
                DatagramPacket receivedDatagram =
                        new DatagramPacket(receivedData, receivedData.length);

                // Receive a datagram			
                serverSocket.receive(receivedDatagram);

                System.out.println("\nMessage received...");
                
                //Increment UDP count
                udpCount++;

                //Open received DatagramPacket
                senderAddr = receivedDatagram.getAddress();			
                senderPort = receivedDatagram.getPort();
                lengthOfMessage = receivedDatagram.getLength();			
                String message = new String(receivedData, 0, receivedDatagram.getLength());
                
                //Check for duplicate
                if(last.equals(message)){
                    resent++;
                    rtCount++;
                }
                else {
                    appCount++;
                    
                    if(rtCount > rtMax){
                        rtMax = rtCount;
                    }
                    rtCount = 0;
                }
                
                if(trace) {
                       System.out.println("\nSender IP address: " + senderAddr.toString() +
                                          "\nSender port number: " + senderPort +
                                          "\nMessage: " + message + "\n");
                }
                
                //Reset lost
                lost = false;
                
                //Establish which kind of message is being received
                if(lengthOfMessage < 16) {  //ACK or close message
                    if(receivedData[6] == 0 && receivedData[7] == 0) {          //ACK message
                        if(trace) {
                            System.out.println("\nACK message received");
                        }
                        //Read header contents
                        //Get IG's IP address
                        byte[] a = new byte[]{receivedData[0], receivedData[1],
                                                  receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(a);

                        if(trace) {
                           System.out.println("IG IP address: " + igAddr.toString());
                        }
                        
                        //Get connection ID
                        byte[] b = new byte[]{receivedData[4], receivedData[5]};
                            int low = b[0] >= 0 ? b[0] : 256 + b[0];
                            int high = b[1] >= 0 ? b[1] : 256 + b[1];
                                connID = low | (high << 8);
                        
                        if(trace) {
                           System.out.println("Connection ID: " + connID);
                        }
                        
                        //Read data
                        messageData = new byte[lengthOfMessage - 12];
                        for(int i = 12; i < lengthOfMessage; i++) {
                            messageData[i - 12] = receivedData[i];
                        }

                        if(trace) {
                            String tempMsg = new String(messageData, 0, messageData.length);
                            
                            System.out.println("Message: " + tempMsg);
                        }
                        
                        //Generate errors
                        if(random.nextInt(100) < errorRate){//Error occurred
                            int error = random.nextInt(100);
                            
                            if(error < oneRate){//One bit error
                                int pos = random.nextInt(messageData.length - 1);
                                int bit = random.nextInt(7);
                                
                                messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));
                                
                                if(trace){
                                    System.out.println("One bit error...");
                                }
                                    
                            }
                            
                            else if(error < (twoRate + oneRate)){//Two bit error
                                int pos = random.nextInt(messageData.length - 1);
                                int bit = random.nextInt(7);
                                
                                messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));
                                
                                pos = random.nextInt(messageData.length - 1);
                                bit = random.nextInt(7);
                                
                                messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));
                                
                                if(trace){
                                    System.out.println("Two bit error...");
                                }
                            }
                            
                            else{//Lost packet
                                lost = true;
                            }
                        }
                        
                        //Forward message
                        //Find Connection in connectionTable
                        boolean match = false;
                        for(int i = 0; i < tableCount; i++){
                            if(connID == connectionTable[i].getId()){
                                match = true;
                                currConn = connectionTable[i];
                                break;
                            }
                            else;
                        }
                        
                        if(!match){//Not a valid Connection
                            System.out.println("Invalid connection ID...");
                        }
                        
                        else{
                            if(trace) {
                                System.out.println("Connection found...");
                            }
                            
                            //Determine which address is sender/receiver
                            if(senderAddr.equals(currConn.getAddr1())){
                                receiverAddr = currConn.getAddr2();
                                receiverPort = currConn.getPort2();
                            }
                            
                            else{
                                receiverAddr = currConn.getAddr1();
                                receiverPort = currConn.getPort1();
                            }
                            
                            if(trace) {
                                System.out.println("\nSender IP address: " + senderAddr.toString() +
                                                   "\nSender port number: " + senderPort +
                                                   "\nReceiver IP address: " + receiverAddr.toString() +
                                                   "\nReceiver port number: " + receiverPort);
                            }
                            
                            // Create a buffer for sending
                            byte[] data = new byte[lengthOfMessage];
                            
                            //Copy data
                            for(int i = 0; i < 12; i++){
                                data[i] = receivedData[i];
                            }
                            for(int i = 0; i < messageData.length; i++){
                                data[i + 12] = messageData[i];
                            }
                            
                            // Create a datagram
                            DatagramPacket datagram = 
                                    new DatagramPacket(data, data.length, receiverAddr, receiverPort);

                            if(trace) {
                                System.out.println("Message sent: " + Arrays.toString(data));
                            }

                            //Send datagram			
                            if(!lost){
                                serverSocket.send(datagram);
                                
                                System.out.println("Message forwarded...");
                            }

                            if(lost){
                                System.out.println("Packet lost...");
                            }
                            
                            //Handle Closing ACK
                            String ackMessage = new String(messageData, 0, messageData.length);
                            if(ackMessage.equals("END")){
                                //Calculate stats
                                endTime = (System.currentTimeMillis() - currConn.getStartTime());
                                double expRT = (errorRate / (100 - errorRate));
                                double perRT = ((resent / udpCount) * 100);

                                //Display Stats
                                System.out.println( "        Transfer Statistics\n" +
                                                    "Size of file: " + fileSize + "\n" +
                                                    "Transfer time: "+ endTime + "\n" +
                                                    "Application messages: " + appCount + "\n" +
                                                    "UDP datagrams: " + udpCount + "\n" +
                                                    "Retransmissions: " + resent + "\n" +
                                                    "Expected retransmissions: " + expRT + "\n" +
                                                    "Max retransmissions: " + rtMax + "\n" +
                                                    "Retransmission percentage: " + perRT);
                                

                                //Find index of Connection
                                for(int i = 0; i < tableCount; i++){
                                    if(currConn.getId() == connectionTable[i].getId()){
                                        index = i;
                                    }

                                    else;
                                }

                                if(trace) {
                                    System.out.println("\n\nConnection index: " + index);
                                }

                                //Update connectionTable
                                for(int i = (index + 1); i < tableCount; i++){
                                    connectionTable[i - 1] = connectionTable[i];
                                }
                                connectionTable[--tableCount] = null;

                                if(trace) {
                                    System.out.println("Connection deleted..." +
                                                       "New table count: " + tableCount);
                                }
                            }
                        }
                    }
                    
                    else {                                                      //Close message
                        last = message;
                        if(trace) {
                            System.out.println("\nClose connection message received");
                        }
                        //Read header contents
                        //Get IG's IP address
                        byte[] a = new byte[]{receivedData[0], receivedData[1],
                                                  receivedData[2], receivedData[3]};
                        igAddr = InetAddress.getByAddress(a);

                        if(trace) {
                           System.out.println("IG IP address: " + igAddr.toString());
                        }
                        
                        //Get sender's IP address
                        byte[] b = new byte[]{receivedData[4], receivedData[5],
                                                  receivedData[6], receivedData[7]};
                        senderAddr = InetAddress.getByAddress(b);

                        if(trace) {
                           System.out.println("Sender IP address: " + senderAddr.toString());
                        }
                    
                        //Read data
                        messageData = new byte[lengthOfMessage - 12];
                        for(int i = 12; i < lengthOfMessage; i++) {
                            messageData[i - 12] = receivedData[i];
                        }

                        if(trace) {
                            String tempMsg = new String(messageData, 0, messageData.length);
                            
                            System.out.println("Message: " + tempMsg);
                        }
                        
                        //Check for errors
                        checker.reset();
                        checker.update(messageData);
                        byte[] checkArray = new byte[]{receivedData[8], receivedData[9],
                                                        receivedData[10], receivedData[11]};
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
                            //Get connection ID
                            byte[] c = new byte[]{messageData[0], messageData[1]};
                                int low = c[0] >= 0 ? c[0] : 256 + c[0];
                                int high = c[1] >= 0 ? c[1] : 256 + c[1];
                                    connID = low | (high << 8);

                            if(trace) {
                               System.out.println("Connection ID: " + connID);
                            }
                            
                            //Forward message
                            //Find Connection in connectionTable
                            boolean match = false;
                            for(int i = 0; i < tableCount; i++){
                                if(connID == connectionTable[i].getId()){
                                    match = true;
                                    currConn = connectionTable[i];
                                    break;
                                }
                                else;
                            }

                            if(!match){//Not a valid Connection
                                System.out.println("Invalid connection ID...");
                            }

                            else{
                                if(trace) {
                                    System.out.println("Connection found...");
                                }

                                //Determine which address is sender/receiver
                                if(senderAddr.equals(currConn.getAddr1())){
                                    receiverAddr = currConn.getAddr2();
                                    receiverPort = currConn.getPort2();
                                }

                                else{
                                    receiverAddr = currConn.getAddr1();
                                    receiverPort = currConn.getPort1();
                                }

                                if(trace) {
                                    System.out.println("\nSender IP address: " + senderAddr.toString() +
                                                       "\nSender port number: " + senderPort +
                                                       "\nReceiver IP address: " + receiverAddr.toString() +
                                                       "\nReceiver port number: " + receiverPort);
                                }

                                // Create a buffer for sending
                                byte[] data = new byte[lengthOfMessage];

                                //Copy data
                                for(int i = 0; i < lengthOfMessage; i++){
                                    data[i] = receivedData[i];
                                }

                                // Create a datagram
                                DatagramPacket datagram = 
                                        new DatagramPacket(data, data.length, receiverAddr, receiverPort);

                                if(trace) {
                                    System.out.println("Message sent: " + Arrays.toString(data));
                                }

                                //Send datagram to Server			
                                serverSocket.send(datagram);

                                System.out.println("Message forwarded...");
                            }
                        }
                    }
                }
                
                else if(receivedData[10] == 0 && receivedData[11] == 0) {       //Open message
                    last = message;
                    if(trace) {
                       System.out.println("\nOpen connection message received");
                    }
                   
                    //Read header contents
                    //Get IG's IP address
                    byte[] a = new byte[]{receivedData[0], receivedData[1],
                                              receivedData[2], receivedData[3]};
                    igAddr = InetAddress.getByAddress(a);
                    
                    if(trace) {
                       System.out.println("IG IP address: " + igAddr.toString());
                    }
                    
                    //Get sender's IP address
                    byte[] b = new byte[]{receivedData[4], receivedData[5],
                                              receivedData[6], receivedData[7]};
                    senderAddr = InetAddress.getByAddress(b);
                    
                    if(trace) {
                       System.out.println("Sender IP address: " + senderAddr.toString());
                    }
                    
                    //Get sender's port number                                  
                    byte[] c = new byte[]{receivedData[8], receivedData[9]};
                        int low = c[0] >= 0 ? c[0] : 256 + c[0];
                        int high = c[1] >= 0 ? c[1] : 256 + c[1];
                    senderPort = low | (high << 8);
                    
                    if(trace) {
                       System.out.println("Sender port number: " + senderPort);
                    }
                    
                    //Get data
                    messageData = new byte[lengthOfMessage - 16];
                    for(int i = 16; i < lengthOfMessage; i++) {
                        messageData[i - 16] = receivedData[i];
                    }
                    
                    if(trace) {
                        String tempMsg = new String(messageData, 0, messageData.length);
                            
                        System.out.println("Message: " + tempMsg);
                    }
                    
                    //Check for errors
                    checker.reset();
                    checker.update(messageData);
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
                        if(trace) {
                            System.out.println("No errors...");
                        }
                        
                        //Read message and assign variables
                        priority = (int)messageData[0];
                        byte[] rAddr = new byte[]{messageData[1], messageData[2], 
                                                    messageData[3], messageData[4]};
                            receiverAddr = InetAddress.getByAddress(rAddr);
                        byte[] rPort = new byte[]{messageData[5], messageData[6]};
                            low = rPort[0] >= 0 ? rPort[0] : 256 + rPort[0];
                            high = rPort[1] >= 0 ? rPort[1] : 256 + rPort[1];
                                receiverPort = low | (high << 8);
                        byte[] size = new byte[]{messageData[7], messageData[8],
                                                messageData[9], messageData[10]};
                            ByteBuffer s = ByteBuffer.wrap(size);
                            fileSize = s.getInt();
                        byte[] name = new byte[messageData.length - 11];
                            for(int i = 11; i < messageData.length; i++){
                                name[i - 11] = messageData[i];
                            }
                            fileName = new String(name, 0, name.length);
                                
                        if(trace) {
                            System.out.println("\nPriority: " + priority +
                                               "\nServer IP address: " + receiverAddr.toString() +
                                               "\nServer port number: " + receiverPort +
                                               "\nFile size: " + fileSize +
                                               "\nFileName: " + fileName);
                        }
                        
                        //Check if connectionTable is full
                        if(tableCount == tableSize) {//Full table
                            if(trace) {
                                System.out.println("Connection table is full...");
                            }
                            
                            tableSize = tableSize * 2;
                            Connection[] tempTable = new Connection[tableSize];
                            //Copy table contents
                            for(int i = 0; i < tableCount; i++) {
                                tempTable[i] = connectionTable[i];
                            }
                            connectionTable = tempTable;
                            
                            if(trace) {
                                System.out.println("Table size is now " + tableSize);
                            }
                        }

                        //Make sure id is valid
                        if(id == 32768) {
                            id = 0;
                        }

                        //Create entry in connectionTable
                        newConn.setId(id);
                        newConn.setPriority(priority);
                        newConn.setAddr1(senderAddr);
                        newConn.setPort1(senderPort);
                        newConn.setAddr2(receiverAddr);
                        newConn.setPort2(receiverPort);
                        newConn.setStartTime(System.currentTimeMillis());
                            connectionTable[tableCount++] = newConn;
                        
                        if(trace) {
                            System.out.println("\nConnection " + (tableCount - 1) +
                                               "\n" + newConn.toString());
                        }
                        
                        //Priority given to more important connection
                        if(newConn.getPriority() > currConn.getPriority()){
                            currConn = newConn;
                        }
                            
                        // Create a buffer for sending
                        byte[] data = new byte[12 + name.length];
                        
                        //Load connection ID
                        data[0] = (byte) (newConn.getId() & 0xFF);
                        data[1] = (byte) ((newConn.getId() >> 8) & 0xFF);
                            id++;
                        
                        if(trace) {
                            System.out.println("ID loaded... " + newConn.getId());
                        }
                        
                        //Load Host's IP address
                        byte[] addr = new byte[4];
                            addr = newConn.getAddr1().getAddress();
                        for(int i = 2; i < 6; i++){
                            data[i] = addr[i - 2];
                        }
                        
                        if(trace) {
                            System.out.println("Host IP address loaded... " + 
                                                newConn.getAddr1());
                        }
                        
                        //Load Host's port number
                        data[6] = (byte) (newConn.getPort1() & 0xFF);
                        data[7] = (byte) ((newConn.getPort1() >> 8) & 0xFF);
                        
                        if(trace) {
                            System.out.println("Host port number loaded... " + 
                                                newConn.getPort1());
                        }
                        
                        //Load file size
                        for(int i = 0; i < 4; i++){
                            data[i + 8] = size[i];
                        }
                        
                        if(trace) {
                            System.out.println("File size loaded... " + 
                                                fileSize);
                        }
                        
                        //Load file name
                        for(int i = 0; i < name.length; i++){
                            data[i + 12] = name[i];
                        }
                        
                        if(trace) {
                            System.out.println("File name loaded... " + 
                                                fileName);
                        }

                        // Create a datagram
                        DatagramPacket datagram = 
                                new DatagramPacket(data, data.length, receiverAddr, receiverPort);
                        
                        if(trace) {
                            System.out.println("Message sent: " + Arrays.toString(data));
                        }

                        // Send a datagram carrying the connection message			
                        serverSocket.send(datagram);
                        
                        System.out.println("Message forwarded...");
                    }
                }
                
                else {                                                          //Data message
                    last = message;
                    if(trace) {
                        System.out.println("Data message received");
                    }

                    //Read header contents
                    //Get IG's IP address
                    byte[] a = new byte[]{receivedData[0], receivedData[1],
                                              receivedData[2], receivedData[3]};
                    igAddr = InetAddress.getByAddress(a);
                    
                    if(trace) {
                       System.out.println("IG IP address: " + igAddr.toString());
                    }
                    
                    //Get connection ID
                    byte[] b = new byte[]{receivedData[4], receivedData[5]};
                        int low = b[0] >= 0 ? b[0] : 256 + b[0];
                        int high = b[1] >= 0 ? b[1] : 256 + b[1];
                            connID = low | (high << 8);

                    if(trace) {
                       System.out.println("Connection ID: " + connID);
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
                    messageData = new byte[dataLength];
                    for(int i = 0; i < dataLength; i++){
                        messageData[i] = receivedData[i + 16];
                    }
                    
                    //Generate errors
                    if(random.nextInt(100) < errorRate){//Error occurred
                        int error = random.nextInt(100);

                        if(error < oneRate){//One bit error
                            int pos = random.nextInt(messageData.length - 1);
                            int bit = random.nextInt(7);

                            messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));
                            
                            if(trace){
                                System.out.println("One bit error...");
                            }
                        }

                        else if(error < (twoRate + oneRate)){//Two bit error
                            int pos = random.nextInt(messageData.length - 1);
                            int bit = random.nextInt(7);

                            messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));

                            pos = random.nextInt(messageData.length - 1);
                            bit = random.nextInt(7);

                            messageData[pos] = (byte) (messageData[pos] ^ (1 << bit));
                            
                            if(trace){
                                System.out.println("Two bit error...");
                            }
                        }

                        else{//Lost packet
                            lost = true;
                        }
                    }
                    
                    
                    //Forward message
                    //Find Connection in connectionTable
                    boolean match = false;
                    for(int i = 0; i < tableCount; i++){
                        if(connID == connectionTable[i].getId()){
                            match = true;
                            currConn = connectionTable[i];
                            break;
                        }
                        else;
                    }

                    if(!match){//Not a valid Connection
                        System.out.println("Invalid connection ID...");
                    }

                    else{
                        if(trace) {
                            System.out.println("Connection found...");
                        }

                        //Determine which address is sender/receiver
                        if(senderAddr.equals(currConn.getAddr1())){
                            receiverAddr = currConn.getAddr2();
                            receiverPort = currConn.getPort2();
                        }

                        else{
                            receiverAddr = currConn.getAddr1();
                            receiverPort = currConn.getPort1();
                        }

                        if(trace) {
                            System.out.println("\nSender IP address: " + senderAddr.toString() +
                                               "\nSender port number: " + senderPort +
                                               "\nReceiver IP address: " + receiverAddr.toString() +
                                               "\nReceiver port number: " + receiverPort);
                        }

                        // Create a buffer for sending
                        byte[] data = new byte[lengthOfMessage];

                        //Copy data
                        for(int i = 0; i < 16; i++){
                            data[i] = receivedData[i];
                        }
                        for(int i = 0; i < messageData.length; i++){
                            data[i + 16] = messageData[i];
                        }

                        // Create a datagram
                        DatagramPacket datagram = 
                                new DatagramPacket(data, data.length, receiverAddr, receiverPort);

                        if(trace) {
                            System.out.println("Message sent: " + Arrays.toString(data));
                        }

                        //Send datagram			
                        if(!lost){
                            serverSocket.send(datagram);
                            
                            System.out.println("Message forwarded...");
                        }

                        if(lost){
                            System.out.println("Packet lost...");
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
