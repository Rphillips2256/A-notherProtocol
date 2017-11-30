/*
 * Host.java builds and sends the files to the server through the gateway and receives acknowledgments.
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
		byte [] servAddress, checkArray;
		byte [] addr;
		byte [] gatewayAddr = new byte[4];
		byte [] message;
		byte [] dataBuffer;
		byte [] header = new byte[16];
		byte [] mainDataMessage = new byte [2048];
		int desPri = 0, gatewayPort, serverPort, hostPort;
		long check;
		CRC32 checksum = new CRC32();
		boolean open = false;
		boolean flag = true;
		String name = "//Users/rs5644nr/Desktop/CS413/alice.txt";
		File file = new File(name);
		FileInputStream fis;
		//BufferedReader buf;

		//TODO: make a trace function.
		while(flag) {

			try {

				// Open a UDP datagram socket
				clientSocket = new DatagramSocket();

				//Get the IG ip address and port number
				//modify this to make it correct.
				gatewayAddr = new byte[] {(byte) 146,(byte) 57,(byte) 194,(byte) 238};
				
				
				
				InetAddress address = null;
				try {
					address = InetAddress.getByAddress(gatewayAddr);
				} catch (UnknownHostException impossible) {
					System.out.println("Unable to determine the host by address!");
				}
				InetAddress destination = address;
				
				
				
				// Determine server port number, gateway and host.
				//subject to change to an input type of method
				gatewayPort = 58989;
				serverPort = 18987;
				hostPort = clientSocket.getLocalPort();

				//Prep the open connection message starting with prepping data for header and actual message.
				//gateway ip address
				

				addr = new byte[] {(byte)146, (byte) 57, (byte) 194, (byte) 32};
				servAddress = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
				
				header = openHead(addr, gatewayAddr, hostPort, gatewayPort);
				
				dataBuffer = new byte[7];
				dataBuffer = openMess(servAddress, serverPort, desPri);

				//do the checksum should be done last after data has been built
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

				int temp = 0;
				int count1 = header.length;
				System.out.println(count1);

				for(int i = 0; i < checkArray.length; i++) {
					header[i + count1] = checkArray[i];
					temp++;
				}

				count1 += temp;

				System.out.println(count1);
				//buid the openMessage
				message = new byte[23];
				for(int i = 0; i < header.length; i++) {
					message[i] = header[i];
				}

				for(int i = 0; i < dataBuffer.length; i++) {
					message[i + header.length] = dataBuffer[i];
				}

				
				//make a packet
				DatagramPacket nData = new DatagramPacket(message, message.length, destination, gatewayPort);
				//send the packet
				clientSocket.send(nData);
				
				//receive something from the IG for the connection being open

				byte[] receivedData = new byte[2048];

				// Create a datagram
				DatagramPacket receivedDatagram = 
						new DatagramPacket(receivedData, receivedData.length);

				// Receive a datagram
				clientSocket.receive(receivedDatagram);

				//Figure out where data message begins and header ends

				byte [] receivedHeader = new byte[20];
				byte[] recData = new byte[14];
				int out = 0;
				for(int i = 0; i < receivedHeader.length; i++) {
					receivedHeader[i] = receivedData[i];
				}
				
				out += receivedHeader.length;
				
				for(int i = 0; i < recData.length; i++) {
					recData[i] = receivedData[i + out];
				}
				
				String openMessage = new String(data(recData));
				System.out.println(openMessage);

				if(openMessage.toUpperCase() == "ACK") {
					open = true;
				}
				
				while(open){
					
					//prepare the packets for transmission,
					
					//TODO: helper methods????
					
					
					
					//set variables for the loop. and other needed parts.
					


					/*
					 *  TODO: Prepare for receiving
					 *  should not have to change anything from here.
					 *  split the packet into header and data also.
					 *  Create a buffer for receiving
					 */
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

	public static StringBuilder data(byte [] a) {

		if( a == null) 
			return null;

		StringBuilder ret = new StringBuilder();
		int i = 0;
		while(a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;


	}
	
	public static long toLong(byte [] by) {
		long c = 0;
		
		for(int i = 0; i < by.length; i++) {
			c = ((c << 8) + (by[i] & 0xff)); 
		}
		
		return c;
	}
	//TODO: Add helper methods so that main is not cluttered
	
	//open message header method
	public static byte [] openHead(byte[] hostAdd, byte [] gateAdd, int gPort, int hPort) {
		byte [] head = new byte[16];
		byte [] portArray, gatePort;
		
		//host port number
		ByteBuffer portBuf = ByteBuffer.allocate(2);
		portBuf.putShort((short) hPort);
		portArray = new byte[2];
		for(int i = 0; i<portArray.length; i++) {
			portArray[i] = portBuf.get(i);
		}

		//gateway port number
		ByteBuffer gateBuf = ByteBuffer.allocate(2);
		gateBuf.putShort((short) gPort);
		gatePort = new byte [2];
		for(int i = 0; i < gatePort.length; i++) {
			gatePort[i] = gateBuf.get(i);
		}

		//Start to build the header.
		//open header has fields for host ip, gateway ip, host port, gateway port, and checksum
		//count is used to keep the data organized in the header.
		int count = 0;
		//temp is to update count after first iteration.
		int temp = 0;
		// start with gateway address
		for(int i = 0; i < gateAdd.length; i++) {
			head[i] = gateAdd[i];
			count++;
		}
		//debugging use
		System.out.println(count);

		// put the host address in
		for(int i = 0; i < hostAdd.length; i++) {
			head[i + count] = hostAdd[i];
			temp++;
		}

		count += temp;

		System.out.println(count);//only for debugging will be commented out

		temp = 0;

		//add in host port
		for(int i = 0; i < portArray.length; i++) {
			head[i + count] = portArray[i];
			temp++;
		}
		count += temp;
		System.out.println(count);
		
		return head;
	}
	
	public static byte [] openMess(byte [] servAdd, int sPort, int priority) {
		byte [] mess = new byte[7];
		
		
		mess[0] = (byte) priority;
		int newtemp = 1;
		
		//server port number
		ByteBuffer servPort = ByteBuffer.allocate(2);
		servPort.putShort((short) sPort);
		byte [] servp = new byte [2];
		for(int i = 0; i < servp.length; i++) {
			servp[i] = servPort.get(i);
		}
		
		

		

		for(int i = 1; i < servAdd.length; i++) {
			mess[i] = servAdd[i];
			newtemp++;
		}

		for(int i = 0; i < servp.length; i++) {
			mess[i + newtemp] = servp[i];
		}
		
		
		return mess;
	}
	
	public static byte [] mainHeader() {
		byte [] head = new byte[20];
		
		return head;
	}
	
	public static byte[] mainMessage() {
		byte [] message = new byte[2048];
		
		
		return message;
	}
	
	//TODO: trace implentation menu screen????????
	//TODO: other menus as needed??!?!?!?!?
}
