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
import java.util.*;


public class MyGateway {
    
    //TODO: create the basic variables needed to perform functions
		
    //TODO: Create a MAP for keys and values to map each connection (connection map/table)
    // Consider forwarding of keys to respective destinations.

    //Start gateway to receive messages and be ready to forward those messages.


    //TODO:  Figure out how to build the table and what to use for priority
    //set up the breakdown of the received packet into a header and data
    // also get it set for the header in this part.
	
    static DatagramSocket serverSocket;

    private static final int PORT = 58989;
	
    public static void main(String [] args){
        
        boolean trace = true;
        
        int tableSize = 10;
        int tableCount = 0;
        Connection[] connectionTable = new Connection[tableSize];
        
        InetAddress igAddr, senderAddr, receiverAddr;
        int senderPort, receiverPort;
        
        try {
            // Open a UDP datagram socket with a specified port number
            int portNumber = PORT;
            //int portNumber = port;
            serverSocket = new DatagramSocket(portNumber);

            System.out.println("Gateway starts ...");

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
                senderAddr = receivedDatagram.getAddress();			
                int senderPortNumber = receivedDatagram.getPort();
                int lengthOfMessage = receivedDatagram.getLength();			
                String message = new String(receivedData, 0, receivedDatagram.getLength());
                
                //Establish which kind of message is being received
                if(lengthOfMessage < 16) {  //ACK or close message
                    if(receivedData[10] == 0 && receivedData[11] == 0) {//ACK
                        
                    }
                    
                    else {                                                      //Close
                        
                    }
                }
                
                else if(receivedData[10] == 0 && receivedData[11] == 0) {//Open
                   if(trace) {
                       System.out.println("Open connection message received");
                   }
                   
                   //Read header contents
                   //Get IG's IP address
                    byte[] a = new byte[]{receivedData[0], receivedData[1],
                                              receivedData[2], receivedData[3]};
                    igAddr = InetAddress.getByAddress(a);
                    
                    //Get sender's IP address
                    byte[] b = new byte[]{receivedData[4], receivedData[5],
                                              receivedData[6], receivedData[7]};
                    senderAddr = InetAddress.getByAddress(b);
                    
                    //Get sender's port number                                  //////////////////////////////
                    byte[] c = new byte[]{receivedData[8], receivedData[9]};
                    senderAddr = InetAddress.getByAddress(b);
                    
                    //Check if connectionTable is full
                    if(tableCount == tableSize) {
                        tableSize = tableSize * 2;
                        Connection[] tempTable = new Connection[tableSize];
                        //Copy table contents
                        for(int i = 0; i < tableCount; i++) {
                            tempTable[i] = connectionTable[i];
                        }
                        connectionTable = tempTable;
                    }
                    
                    else {
                        
                    }
                }
                
                else {                                                          //Data
                    
                }

                // Display received message and client address		 
                System.out.println("The received message is: " + message);
                System.out.println("The sender address is: " + senderAddr);
                System.out.println("The sender port number is: " + senderPortNumber);

                // Create a buffer for sending
                byte[] data = new byte[lengthOfMessage];
                data = message.getBytes();

                // Create a datagram
                DatagramPacket datagram = 
                        new DatagramPacket(data, lengthOfMessage, receiverAddr, receiverPortNumber);

                // Send a datagram carrying the echo message			
                serverSocket.send(datagram);
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
