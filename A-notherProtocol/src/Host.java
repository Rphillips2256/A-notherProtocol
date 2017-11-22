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
		
		//used variable declaration
		
		String filename;
		byte [] servAddress, checkArray, portArray;
		byte [] addr;
		byte [] message = new byte[1024];
		byte [] header = new byte[20];
		int desPri, count, gatewayPort, serverPort, hostPort;
		long check;
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
			
			
			//Build the header and data of the open connection packet
			
			addr = new byte[] {(byte)146, (byte) 57, (byte) 194, (byte) 32};
			
			for(int i = 0; i < addr.length; i++){
				message[i] = addr[i];
				count++;
			}
			System.out.println(count);
			hostPort = clientSocket.getLocalPort();
			
			ByteBuffer portBuf = ByteBuffer.allocate(2);
			portBuf.putShort((short) hostPort);
			portArray = new byte[2];
			
			for(int i = 0; i<portArray.length; i++) {
				portArray[i] = portBuf.get(i);
			}
			int temp = 0;
			for(int i = 0; i < portArray.length; i++){
				message[i + count] = portArray[i];
				temp++;
			}
			
			count += temp;
			System.out.println(count);
			
			desPri = 1;
			
			message[count + 1] = (byte) desPri;
			count += 1;
			System.out.println(count);
			
			servAddress = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
			temp = 0;
			for(int i = 0; i < servAddress.length; i++){
				message[i + count] = servAddress[i];
				temp++;
			}
			
			count += temp;
			
			System.out.println(count);
			
			checksum.update(message);
			
			check = checksum.getValue();
			
			System.out.println(check);
			
			ByteBuffer checkSumBuf = ByteBuffer.allocate(4);
			checkSumBuf.putInt((int) check);
			checkArray = new byte[4];
			
			for(int i = 0; i < 4; i++) {
				checkArray[i] = checkSumBuf.get(i);
			}
			
			temp = 0;
			for(int i = 0; i < checkArray.length; i++){
				message[i + count] = checkArray[i];
				temp++;
			}
			
			count += temp;
			System.out.println(count);
			
			DatagramPacket nData = new DatagramPacket(message, message.length);
			clientSocket.send(nData);
			//receive ack from the IG for the connection being open
			
			while(open != false){
				

				//build and send data packets with message array


				// Message and its length		
				//String message = "Hello World!";
				//int lengthOfMessage = message.length(); 
				//byte[] data = new byte[lengthOfMessage];
				//data = message.getBytes();

				// Create a datagram
				//DatagramPacket datagram = 
						//new DatagramPacket(data, lengthOfMessage, destination, (int) gatewayPort);

				// Send a datagram carrying the message
				//clientSocket.send(datagram);

				// Print out the message sent
				//System.out.println("Message sent is:   [" + message + "]");

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
}
