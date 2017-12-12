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
    
    /*TODO
    * Set up D_LEN to be size of data in app packet
    * Add file type to mainHeader
    */

	static DatagramSocket clientSocket;
	static List<byte []> packet = new ArrayList();
	static Scanner in = new Scanner(System.in);
	static PrintWriter write; 

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
		int appCount = 0;
		int choice = 0;
		int timeout = 0;
		boolean trace = false;
		int packetCount = 0, resent = 0, seq = 0, len = 0;
		long check;
		CRC32 checksum = new CRC32();
		boolean open = false;
		boolean flag = true;
		String name = "";
		String name1 = "/Users/rs5634nr/git/A-notherProtocol/A-notherProtocol/src/alice.txt";
		String name2 = "C:/Users/Adam/Desktop/test1.txt";
		String name3 = "";
                String fileName = "";

		while(flag) {
			
			System.out.print(menu());
			choice = in.nextInt();
			
			switch(choice) {
			case 1: 
				trace = true;
				break;
			case 2:
				trace = false;
				break;
			default:
				System.out.println("invalid choice");
				break;
			}
			
			System.out.print(nextMenu());
			choice = in.nextInt();
			
			
			switch(choice) {
			case 1:
				name = name1;
				break;
			case 2:
				name = name2;
				break;
			case 3:
				name = name3;
				break;
			default:
				System.out.println("Invalid Choice try again.");
				break;
			}

                        //Get file name
                        String[] tempName = name.split("/");
                            fileName = tempName[tempName.length - 1];

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


				addr = new byte[] {(byte) 192,(byte) 168,(byte) 1,(byte) 14};//146.57.194.31
				servAddress = new byte[] {(byte) 192,(byte) 168,(byte) 1,(byte) 2};

				header = openHead(addr, gatewayAddr, gatewayPort, hostPort);
				if(trace)
					System.out.println("Building the header for the open connection.");
				dataBuffer = new byte[11 + fileName.length()];
				byte [] total = (Files.readAllBytes(Paths.get(name)));
				int lenTotal = total.length;
				dataBuffer = openMess(servAddress, serverPort, desPri, lenTotal, fileName);
				if(trace)
					System.out.println("Building the data to be sent.");

				//do the checksum should be done last after data has been built
				checksum.update(dataBuffer);
				check = checksum.getValue();
				if(trace)
					System.out.println("Running checksum on the data ... checksum is: " + check);
				
				ByteBuffer sum = ByteBuffer.allocate(4);
				sum.putInt((int) check);
				checkArray = new byte[4];
				for(int i = 0; i < checkArray.length; i++) {
					checkArray[i] = sum.get(i);
				}

				//build the rest of the the header and build entire packet.

				int temp = 0;
				int count1 = header.length - 4;
				//System.out.println(count1);

				for(int i = 0; i < checkArray.length; i++) {
					header[i + count1] = checkArray[i];
					temp++;
				}

				count1 += temp;

				//System.out.println(count1);
				//buid the openMessage
				message = new byte[header.length + dataBuffer.length];
				for(int i = 0; i < header.length; i++) {
					message[i] = header[i];
				}

				for(int i = 0; i < dataBuffer.length; i++) {
					message[i + header.length] = dataBuffer[i];
				}


				//make a packet
				DatagramPacket nData = new DatagramPacket(message, message.length, destination, gatewayPort);
				if(trace)
					System.out.println("New Datagram built.");
				//send the packet
				clientSocket.send(nData);
				if(trace) 
					System.out.println("Datagram sent to IG to open the connection.");

				//receive something from the IG for the connection being open

				byte[] receivedData = new byte[2048];

				// Create a datagram
				DatagramPacket receivedDatagram = 
						new DatagramPacket(receivedData, receivedData.length);

				// Receive a datagram
				clientSocket.receive(receivedDatagram);
				
				if(trace)
					System.out.println("Datagram Receieved");
				
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

				//long sumCheck = toLong(cSum);

				ByteBuffer bb = ByteBuffer.wrap(cSum);
				long checkValue = bb.getInt();

				long checkVal = 0;

				if(checkValue < 0){
					checkVal = checkValue + 2147483647 + 2147483647 + 2;
				}

				else {
					checkVal = checkValue;
				}

				checksum.reset();
				checksum.update(recData);
				long dataCheck = checksum.getValue();
				
                                byte [] gID = new byte[2];
				for(int i = 0; i < gID.length; i++) {
					gID[i] = receivedHeader[i + 4];
				}

				int low = gID[0] >= 0 ? gID[0] : 256 + gID[0];
                                int high = gID[1] >= 0 ? gID[1] : 256 + gID[1];
                                conID = low | (high << 8);

				if(dataCheck == checkVal) {



					String openMessage = new String(recData, 0, recData.length);
					System.out.println(openMessage);

					if(openMessage.equals("ACK")) {
						open = true;
						if(trace)
							System.out.println("Connection now open preparing to send data.");
					}
				} else {

					clientSocket.send(nData);
					if(trace)
						System.out.println("The connection is not open resending request.");
				}

				while(open){
					//prepare the packets for transmission,
					createPackets(name);
					if(trace)
						System.out.println("File loaded into the buffer to be sent.");

					

					byte [] m = packet.get(packetCount);
                                        
                                        mainDataMessage = new byte[m.length + 16];
					
					if(trace)
						System.out.println("Data packet loaded into the frame.");
					
					long dataSum = calculateChecksum(checksum, m);
					
					if(trace)
						System.out.println("Checksum calculated: " + dataSum);
					
					len = m.length;
					
					if(trace)
						System.out.println("Total length of data to be sent is: " + len);
					
					byte [] h = new byte[16];
					
					h = mainHeader(gatewayAddr, conID, seq, len, dataSum);
					
					if(trace)
						System.out.println("Header is loaded.");

					int newCount = 0;
					for(int i = 0; i < h.length; i++) {

						mainDataMessage[i] = h[i];
						newCount++;
					}

					for(int i = 0; i < m.length; i++) {
						mainDataMessage[i + newCount] = m[i];
					}

					DatagramPacket mainData = new DatagramPacket(mainDataMessage, mainDataMessage.length, destination, gatewayPort);
					if(trace)
						System.out.println("The main data message is built.");
					clientSocket.send(mainData);
					if(trace)
						System.out.println("The message is sent waiting for acknowledgement.");
					clientSocket.setSoTimeout(10000);

					try {
						clientSocket.receive(receivedDatagram);
						receivedHeader = new byte[12];
						recData = new byte[receivedDatagram.getLength() - 12];
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

						low = recData[0] >= 0 ? recData[0] : 256 + recData[0];
                                                high = recData[1] >= 0 ? recData[1] : 256 + recData[1];
                                                    int curr = low | (high << 8);

						if(curr == seq) {
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
						byte [] closeMess = new byte[2];
						long closeCheck = 0;
                                                
                                                closeMess[0] = (byte) (conID & 0xFF);
                                                closeMess[1] = (byte) ((conID >> 8) & 0xFF);

						closeCheck = calculateChecksum(checksum, closeMess);

						closeHeader = closeHead(gatewayAddr, addr, closeCheck);

						byte [] close = new byte[14];

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
				flag = false;
			}
		}
	}

	public static long toLong(byte [] by) {
		long c = 0;
		
		for(int i = 0; i < by.length; i++) {
			c = ((c << 8) + (by[i] & 0xff)); 
		}
		
		return c;
	}
	
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
	
	public static byte [] openMess(byte [] servAdd, int sPort, int priority,int totalLength, String filename) {
		byte [] mess = new byte[11 + filename.length()];
		
		
		int count = 0;
		System.out.println(filename.length());
		
		mess[0] = (byte) priority;
		int newtemp = 1;
		
		//server port number
		ByteBuffer servPort = ByteBuffer.allocate(2);
		servPort.putShort((short) sPort);
		byte [] servp = new byte [2];
		
                servp[0] = (byte) (sPort & 0xFF);
                servp[1] = (byte) ((sPort >> 8) & 0xFF);
                count += 2;
		
		for(int i = 0; i < servAdd.length; i++) {
			mess[i + 1] = servAdd[i];
			newtemp++;
		}
		count += newtemp;
		for(int i = 0; i < servp.length; i++) {
			mess[i + newtemp] = servp[i];
			count++;
		}
		
		byte [] size = new byte[4];
		size[0] = (byte) (totalLength & 0xFF);
		size[1] = (byte) ((totalLength >> 8) & 0xFF);
		size[2] = (byte) ((totalLength >> 16) & 0xFF);
		size[3] = (byte) ((totalLength >> 24) & 0xFF);
		
		
		for(int i = 0; i < size.length; i++) {
			mess[i + count] = size[i];
		}
		
		newtemp = 11;
		
                byte[] name = filename.getBytes();
		for(int i = 11; i < mess.length; i++) {
			mess[i] = name[i - 11];
		}

		return mess;
	}
	
	public static byte [] mainHeader(byte [] gateAdd, int id, int seq, int dataLen, long check) {
		byte [] head = new byte[16];
		int count = 0;
		int temp = 0;
		
		
		for(int i = 0; i < gateAdd.length; i++) {
			head[i] = gateAdd[i];
			count++;
		}
		
                //Load ID
		head[count++] = (byte) (id & 0xFF);
        head[count++] = (byte) ((id >> 8) & 0xFF);
		
        head[count++] = (byte) 0;//padding
        head[count++] = (byte) 0;//padding
		
		
		ByteBuffer sequence = ByteBuffer.allocate(2);
		sequence.putShort((short) seq);
		for(int i = 0; i < 2; i++) {
			head[i + count] = sequence.get(i);
			temp++;
		}
		
		count += temp;
		temp = 0;
		
		head[count++] = (byte) (dataLen & 0xFF);
        head[count++] = (byte) ((dataLen >> 8) & 0xFF);
		
		ByteBuffer cs = ByteBuffer.allocate(4);
		cs.putInt((int) check);
		for(int i = 0; i < 4; i++) {
			head[i+count] = cs.get(i);
		}
		
		return head;
	}
	
	public static long calculateChecksum(CRC32 c,byte [] d ) {
		long s = 0;
		
		c.reset();
                c.update(d);
		s = c.getValue();
		
		return s;
	}
	
	public static List createPackets(String file) throws IOException{
	    
	     //read the file into array
	        Path path = Paths.get(file);
	        byte[] data = Files.readAllBytes(path);

	        int position = 0;
	        int messageSent = data.length;
	        
	        //create an array for the message split into 1500 bytes
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
	        //decrease the position by 1500 bytes
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
	public static String menu() {
		
		return "Welcome to the File Transfer\n" +
		"Please Select from the following: \n" + 
		"1 --------------- To turn on the trace.\n" +
		"2 --------------- To run without the trace.\n";
		
	}
	
	public static String nextMenu() {
		return "Please select from the following\n" +
			   "1 -------------- To send Alice.txt\n" +
			   "2 -------------- To send this file\n" +
			   "3 -------------- To send this file\n";
	}
}
