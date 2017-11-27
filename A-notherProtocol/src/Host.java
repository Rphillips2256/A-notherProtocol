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
		byte [] servAddress, checkArray, portArray, gatePort;
		byte [] addr;
		byte [] gatewayAddr = new byte[4];
		byte [] message;
		byte [] dataBuffer;
		byte [] header = new byte[16];
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
			
			
			// Determine server port number, gateway and host.
			gatewayPort = 58989;
			serverPort = 18987;
			hostPort = clientSocket.getLocalPort();
			
			//Prep the open connection message starting with prepping data for header and actual message.
			//gateway ip address
			for(int i = 0; i < b.length; i++) {
				gatewayAddr[i] = b[i];
			}
			
			addr = new byte[] {(byte)146, (byte) 57, (byte) 194, (byte) 32};
			servAddress = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
			
			//host port number
			ByteBuffer portBuf = ByteBuffer.allocate(2);
			portBuf.putShort((short) hostPort);
			portArray = new byte[2];
			for(int i = 0; i<portArray.length; i++) {
				portArray[i] = portBuf.get(i);
			}
			
			//gateway port number
			ByteBuffer gateBuf = ByteBuffer.allocate(2);
			gateBuf.putShort((short) gatewayPort);
			gatePort = new byte [2];
			for(int i = 0; i < gatePort.length; i++) {
				gatePort[i] = gateBuf.get(i);
			}
			
			//server port number
			ByteBuffer servPort = ByteBuffer.allocate(2);
			servPort.putShort((short) serverPort);
			byte [] servp = new byte [2];
			for(int i = 0; i < servp.length; i++) {
				servp[i] = servPort.get(i);
			}

			//Start to build the header.
			//open header has fields for host ip, gateway ip, host port, gateway port, and checksum
			//count is used to keep the data organized in the header.
			count = 0;
			//temp is to update count after first iteration.
			int temp = 0;
			// start with gateway address
			for(int i = 0; i < gatewayAddr.length; i++) {
				header[i] = gatewayAddr[i];
				count++;
			}
			//debugging use
			System.out.println(count);
			//insert the port into the header.
			for(int i = 0; i < gatePort.length; i++) {
				header[i + count] = gatePort[i];
				temp++;
			}
			
			count += temp; // add the temp to the count
			
			System.out.println(count);//more debugging use.
			
			temp = 0; // reset temp
			// put the host address in
			for(int i = 0; i < addr.length; i++) {
				header[i + count] = addr[i];
				temp++;
			}
			
			count += temp;
			
			System.out.println(count);
			
			temp = 0;
			
			//add in host port
			for(int i = 0; i < portArray.length; i++) {
				header[i + count] = portArray[i];
				temp++;
			}
			count += temp;
			System.out.println(count);
			
			temp = 0;
			 // build the data portion of the message
			
			dataBuffer = new byte [7];
			desPri = 1;
			dataBuffer[0] = (byte) desPri;
			
			int newtemp = 0; 
			
			for(int i = 1; i < servAddress.length; i++) {
				dataBuffer[i] = servAddress[i];
				newtemp++;
			}
			
			for(int i = 0; i < servp.length; i++) {
				dataBuffer[i + newtemp] = servp[i];
			}
			
			//checksum time
			checksum.update(dataBuffer);
			check = checksum.getValue();
			System.out.println(check);
			ByteBuffer sum = ByteBuffer.allocate(4);
			sum.putInt((int) check);
			checkArray = new byte[4];
			for(int i = 0; i < checkArray.length; i++) {
				checkArray[i] = sum.get(i);
			}
			
			//build the rest of the the header and build entire packet.
			
			temp = 0;
			
			for(int i = 0; i < checkArray.length; i++) {
				header[i + count] = checkArray[i];
				temp++;
			}
			
			count += temp;
			
			System.out.println(count);
			
			message = new byte[23];
			for(int i = 0; i < header.length; i++) {
				message[i] = header[i];
			}
			
			for(int i = 0; i < dataBuffer.length; i++) {
				message[i + header.length] = dataBuffer[i];
			}
			//System.out.println(header.length);
			//System.out.println(dataBuffer.length);
			//System.out.println(message.length);
			
			//DatagramPacket nData = new DatagramPacket(message, message.length);
			//clientSocket.send(nData);
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
