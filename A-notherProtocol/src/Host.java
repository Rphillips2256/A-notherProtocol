/*
 * Host.java builds and sends the files to the server and receives acknowledgments.
 * 
 * 
 * 
 */

import java.io.*;
import java.net.*;
import java.net.DatagramPacket;
import java.lang.*;
import java.util.*;
import java.util.zip.*;
import java.nio.*;


public class Host {
	
	static DatagramSocket clientSocket;
	
	public static void main(String [] args){
		
		String filename;
		byte [] servAddress, checkArray, portArray;
		byte [] addr;
		byte [] openMessage = new byte[1024];
		int desPri, count;
		long check, gatewayPort, serverPort, hostPort; 
		CRC32 checksum = new CRC32();
		boolean open = false;
		
		
		
		
		
		
		try {
			
			// Open a UDP datagram socket
			clientSocket = new DatagramSocket();
			
			//Get the IG ip address and port number
			//modify this to make it correct.
			byte[] b = new byte[] {(byte) 146,(byte) 57,(byte) 194,(byte) 238};
			InetAddress address = null;
			try {
	        		address = InetAddress.getByAddress(b);
			} catch (UnknownHostException impossible) {
				System.out.println("Unable to determine the host by address!");
			}
			InetAddress destination = address;
			
			
			// Determine server port number
			gatewayPort = 58989;
			count = 0;
			desPri = 1;
			
			
			//Build the header and data of the open connection packet
			
			addr = clientSocket.getInetAddress().getAddress() ;
			
			for(int i = 0; i < addr.length; i++){
				openMessage[i] = addr[i];
				count++;
			}
			hostPort = clientSocket.getLocalPort();
			
			portArray = longToBytes(hostPort);
			int temp = 0;
			for(int i = 0; i < portArray.length; i++){
				openMessage[i + count] = portArray[i];
				temp++;
			}
			
			count += temp;
			
			portArray = new byte[2];
			
			portArray = longToBytes(gatewayPort);
			
			temp = 0;
			
			for(int i = 0; i < portArray.length; i++){
				openMessage[i + count] = portArray[i];
				temp++;
			}
			
			count += temp;
			
			DatagramPacket nData = new DatagramPacket(openMessage, (int) gatewayPort);
			clientSocket.send(nData);
			//receive ack from the IG for the connection being open
			
			while(open != false){
				

				//build and send data packets with packetMessage and other arrays


				// Message and its length		
				String message = "Hello World!";
				int lengthOfMessage = message.length(); 
				byte[] data = new byte[lengthOfMessage];
				data = message.getBytes();

				// Create a datagram
				DatagramPacket datagram = 
						new DatagramPacket(data, lengthOfMessage, destination, (int) gatewayPort);

				// Send a datagram carrying the message
				clientSocket.send(datagram);

				// Print out the message sent
				System.out.println("Message sent is:   [" + message + "]");

				// Prepare for receiving
				//should not have to change anything from here.
				// Create a buffer for receiving
				byte[] receivedData = new byte[2048];

				// Create a datagram
				DatagramPacket receivedDatagram = 
						new DatagramPacket(receivedData, receivedData.length);

				// Receive a datagram
				clientSocket.receive(receivedDatagram);

				// Display the message in the datagram
				String echoMessage = new String(receivedData, 0, receivedDatagram.getLength());
				System.out.println("Message echoed is: [" + echoMessage + "]");	
			}
		} 
		catch (IOException ioEx) {
			ioEx.printStackTrace();
		} 
		finally {
			// Close the socket 
			clientSocket.close();
		}
	
	}
	
	public static byte[] longToBytes(long value) {
	    ByteBuffer buffer = ByteBuffer.allocate(8);
	    buffer.putLong(value);
	    return buffer.array();
	}
}
