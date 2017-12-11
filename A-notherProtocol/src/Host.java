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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Host {

	static DatagramSocket clientSocket;
	static List<byte []> packet = new ArrayList();

	public static void main(String [] args){

		//used variable declaration
		byte [] servAddress, checkArray;
		byte [] addr;
		byte [] gatewayAddr = new byte[4];
		byte [] message;
		byte [] dataBuffer;
		byte [] header;
		byte [] mainDataMessage;
		int desPri = 0, gatewayPort, serverPort, hostPort, conID;
		int packetCount = 0, resent = 0, seq = 0, len = 0;
		long check;
		CRC32 checksum = new CRC32();
		boolean open = false;
		//boolean flag = true;
		String name = "//Users/rs5644nr/Desktop/CS413/alice.txt";

		//TODO: make a trace function.


		try {

			// Open a UDP datagram socket
			clientSocket = new DatagramSocket();

			//Get the IG ip address and port number
			//modify this to make it correct.
			gatewayAddr = new byte[] {(byte) 192,(byte) 168,(byte) 1,(byte) 2};



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


			addr = new byte[] {(byte) 192,(byte) 168,(byte) 1,(byte) 14};
			servAddress = new byte[] {(byte) 192,(byte) 168,(byte) 1,(byte) 2};

			header = openHead(addr, gatewayAddr, gatewayPort, hostPort);

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
			int count1 = header.length - 4;
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

			//split data from header

			byte [] receivedHeader = new byte[12];
			byte[] recData = new byte[receivedDatagram.getLength() - 12];
			int out = 0;
			for(int i = 0; i < receivedHeader.length; i++) {
				receivedHeader[i] = receivedData[i];
			}
			byte [] cSum = new byte [4];

			for(int i = 0; i < cSum.length; i++) {
				cSum[i] = receivedHeader[i + 8];
			}

			out += receivedHeader.length;

			for(int i = 0; i < recData.length; i++) {
				recData[i] = receivedData[i + out];
			}

			long sumCheck = toLong(cSum);

			checksum.update(recData);
			long dataCheck = checksum.getValue();
			byte [] gID = new byte[2];
			for(int i = 0; i < gID.length; i++) {
				gID[i] = receivedHeader[i];
			}
			
			conID = getInt(gID);
			
			if(dataCheck == sumCheck) {
				
				
				
				String openMessage = new String(data(recData));
				System.out.println(openMessage);

				if(openMessage.toUpperCase() == "ACK") {
					open = true;
				}
			} else {

				clientSocket.send(nData);

			}

			while(open){
				//prepare the packets for transmission,
				createPackets(name);
				
				mainDataMessage = new byte[1516];
				
				byte [] m = packet.get(packetCount);
				long dataSum = calculateChecksum(checksum, m);
				len = (Files.readAllBytes(Paths.get(name)).length);
				byte [] h = new byte[16];
				int currentLength = m.length;
				h = mainHeader(gatewayAddr, conID, seq, currentLength, len, dataSum);
				
				int newCount = 0;
				for(int i = 0; i < h.length; i++) {
					
					mainDataMessage[i] = h[i];
					newCount++;
				}
				
				for(int i = 0; i < m.length; i++) {
					mainDataMessage[i + newCount] = m[i];
				}
				
				DatagramPacket mainData = new DatagramPacket(mainDataMessage, mainDataMessage.length, destination, gatewayPort);
				clientSocket.send(mainData);
				clientSocket.setSoTimeout(1000);
				
				try {
					clientSocket.receive(receivedDatagram);
					receivedHeader = new byte[6];
					recData = new byte[3];
					out = 0;
					for(int i = 0; i < receivedHeader.length; i++) {
						receivedHeader[i] = receivedData[i];
					}
					cSum = new byte [4];

					for(int i = 0; i < cSum.length; i++) {
						cSum[i] = receivedHeader[i + 4];
					}

					out += receivedHeader.length;

					for(int i = 0; i < recData.length; i++) {
						recData[i] = receivedData[i + out];
					}
					
					int curr = getInt(recData);
					
					if(curr == conID) {
						packetCount++;
						seq++;
					} else {
						clientSocket.send(mainData);
						resent++;
					}
				} catch (SocketTimeoutException e) {
					System.out.println("Timeout reached: " + e);
					
				}
				
				if(packetCount == packet.size()) {
					
					byte [] closeHeader = new byte [12];
					byte [] closeMess;
					long closeCheck = 0;
					String end = "END";
					
					closeMess = end.getBytes();
					
					closeCheck = calculateChecksum(checksum, closeMess);
					
					closeHeader = closeHead(gatewayAddr, addr, closeCheck);
					
					byte [] close = new byte[15];
					
					count1 = 0;
					
					for(int i = 0; i < closeHeader.length; i ++) {
						close[i] = closeHeader[i];
						count1++;
					}
					
					for(int i = 0; i < closeMess.length; i++) {
						close[i + count1] = closeMess[i];
					}
					
					DatagramPacket letClose = new DatagramPacket(close, close.length, destination, gatewayPort);
					
					clientSocket.send(letClose);
					
					open = false;
					
				}
				
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
                
                byte[] hostp = new byte[2];
                
                hostp[0] = (byte) (hPort & 0xFF);
                hostp[1] = (byte) ((hPort >> 8) & 0xFF);
                
		for(int i = 0; i < portArray.length; i++) {
			head[i + count] = hostp[i];
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
		
                servp[0] = (byte) (sPort & 0xFF);
                servp[1] = (byte) ((sPort >> 8) & 0xFF);
		
		for(int i = 0; i < servAdd.length; i++) {
			mess[i + 1] = servAdd[i];
			newtemp++;
		}

		for(int i = 0; i < servp.length; i++) {
			mess[i + newtemp] = servp[i];
		}
		
		
		return mess;
	}
	
	public static byte [] mainHeader(byte [] gateAdd, int id, int seq, int dataLen, int totalSize, long check) {
		byte [] head = new byte[16];
		int count = 0;
		int temp = 0;
		
		
		for(int i = 0; i < gateAdd.length; i++) {
			head[i] = gateAdd[i];
			count++;
		}
		
		ByteBuffer destId = ByteBuffer.allocate(2);
		destId.putShort((short) id);
		
		for(int i = 0; i < 2; i++) {
			head[i] = destId.get(i);
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		ByteBuffer size = ByteBuffer.allocate(2);
		size.putShort((short) totalSize);
		for(int i = 0; i < 2; i++) {
			head[i + count] = size.get(i);
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		ByteBuffer sequence = ByteBuffer.allocate(2);
		sequence.putShort((short) seq);
		for(int i = 0; i < 2; i++) {
			head[i + count] = sequence.get(i);
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		ByteBuffer dLen = ByteBuffer.allocate(2);
		dLen.putShort((short) dataLen);
		for(int i = 0; i < 2; i++) {
			head[i + count] = dLen.get(i);
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		ByteBuffer cs = ByteBuffer.allocate(4);
		cs.putInt((int) check);
		for(int i = 0; i < 4; i++) {
			head[i+count] = cs.get(i);
		}
		
		return head;
	}
	
	public static long calculateChecksum(CRC32 c,byte [] d ) {
		long s = 0;
		
		c.update(d);
		s = c.getValue();
		
		return s;
	}
	
	public static List createPackets(String file) throws IOException{
	    
	     //read the file into array
	        Path path = Paths.get(file);
	        byte[] data = Files.readAllBytes(path);
	        final int length = data.length;

	        int position = 0;
	        int messageSent = data.length;
	        
	        //create an array for the message split into 16 bytes
	        while(data.length > position)
	        {
	            int byteSize = 1500;
	            byte[] bFile;
	            
	            if(messageSent < byteSize)
	            {
	                    bFile = new byte[messageSent];
	                    System.arraycopy(data, position, bFile, 0, messageSent);
	            }
	            else
	            {
	                    bFile = new byte[byteSize];
	                    System.arraycopy(data, position, bFile, 0, byteSize);
	            }

	        packet.add(bFile);
	        
	        //increase the position by max data size bytes
	        position = position + 1500;
	        //decrease the position by 16 bytes
	        messageSent = messageSent - 1500;

	        
	        }//end
	   
	        return packet; 
	} 
	
	public static int getInt(byte [] h) {
		int id = 0;
		
		for(int i = 0; i < 2; i++) {
			id = ( (id << 8) + ( h[i] & 0xff ) );
		}
		
		return id;
	}
	
	public static byte [] closeHead(byte [] gatewayIP, byte [] hostIP, long checksum) {
		byte [] head = new byte [12];
		
		int count = 0;
		int temp = 0;
		
		for(int i = 0; i < gatewayIP.length; i++) {
			head[i] = gatewayIP[i];
			count++;
		}
		
		for(int i = 0; i < hostIP.length; i++) {
			head[i + count] = hostIP[i];
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt( (int) (checksum) );
		for(int i = 0; i < 4; i++) {
			head[i + count] = b.get(i);
		}
		
		return head;
	}
}
