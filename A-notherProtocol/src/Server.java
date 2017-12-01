/*
 * 
 * 
 * 
 * 
 */

import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class Server {
	
    static DatagramSocket serverSocket;

    private static final int PORT = 18987;
	
    public static void main(String [] args){
        try {
            // Open a UDP datagram socket with a specified port number
            int portNumber = PORT;
            //int portNumber = port;
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
                InetAddress destination = receivedDatagram.getAddress();			
                int clientPortNumber = receivedDatagram.getPort();
                int lengthOfMessage = receivedDatagram.getLength();			
                String message = new String(receivedData, 0, receivedDatagram.getLength());

                // Display received message and client address		 
                System.out.println("The received message is: " + message);
                System.out.println("The client address is: " + destination);
                System.out.println("The client port number is: " + clientPortNumber);

                // Create a buffer for sending
                byte[] data = new byte[lengthOfMessage];
                data = message.getBytes();

                // Create a datagram
                DatagramPacket datagram = 
                        new DatagramPacket(data, lengthOfMessage, destination, clientPortNumber);

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
