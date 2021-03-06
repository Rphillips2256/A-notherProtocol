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
	 * Log function
	 */

	static DatagramSocket clientSocket;
	static List<byte []> packet = new ArrayList();
	static Scanner in = new Scanner(System.in);
	static PrintWriter write; 

	public static void main(String [] args) throws IOException{
		FileWriter toLog = new FileWriter("log.txt");
		//used variable declaration
		byte [] servAddress, checkArray;
		byte [] addr;
		byte [] gatewayAddr = new byte[4];
		byte [] message;
		byte [] dataBuffer;
		byte [] header;
		byte [] mainDataMessage;
		int desPri = 0, gatewayPort, serverPort, hostPort, conID = 0;
		int appCount = 0;
		int choice = 0;
		int timeout = 0;
		boolean trace = false;
		int packetCount = 0, resent = 0, seq = 0, len = 0;
		long check;
		CRC32 checksum = new CRC32();
		boolean open = false;
		boolean flag = true;
		boolean log = false;
		String name = "";
		String name1 = "/Users/rs5634nr/git/A-notherProtocol/A-notherProtocol/src/alice.txt";
		String name2 = "C:/Users/Adam/Desktop/test1.txt";
		String name3 = "C:/Users/Adam/Desktop/alice.txt";
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

			System.out.print("Did you want to write this to a log file.\n" +
					"1 -------------- Yes\n" +
					"2 -------------- No\n");
			choice = in.nextInt();

			switch(choice) {
			case 1:
				log = true;
				break;
			case 2:
				log = false;
				break;
			default:
				System.out.println("Invalid Choice");
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
				if(trace) {
					System.out.println("Building the header for the open connection.");
					if(log)
						toLog.write("Building the header for the open connection.\n");
				}
				dataBuffer = new byte[11 + fileName.length()];
				byte [] total = (Files.readAllBytes(Paths.get(name)));
				int lenTotal = total.length;
				if(trace) {
					System.out.println("Size of file: " + lenTotal);
					if(log)
						toLog.write("Size of file: " + lenTotal + "\n");
				}

				dataBuffer = openMess(servAddress, serverPort, desPri, lenTotal, fileName);
				if(trace) {
					System.out.println("Building the data to be sent.");
					if(log)
						toLog.write("Building the data to be sent.\n");
				}

				//do the checksum should be done last after data has been built
				checksum.update(dataBuffer);
				check = checksum.getValue();
				if(trace) {
					System.out.println("Running checksum on the data ... checksum is: " + check);

					if(log) {
						toLog.write("Running checksum on the data ... checksum is: " + check + "\n");
					}
				}

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
				//build the openMessage
				message = new byte[header.length + dataBuffer.length];
				for(int i = 0; i < header.length; i++) {
					message[i] = header[i];
				}

				for(int i = 0; i < dataBuffer.length; i++) {
					message[i + header.length] = dataBuffer[i];
				}


				//make a packet
				DatagramPacket nData = new DatagramPacket(message, message.length, destination, gatewayPort);
				if(trace) {
					System.out.println("New Datagram built.");
					if(log)
						toLog.write("New Datagram built.\n");
				}
				//send the packet
				clientSocket.send(nData);
				appCount++;
				if(trace) {
					System.out.println("Datagram sent to IG to open the connection.");
					
					if(log)
						toLog.write("Datagram sent to IG to open the connection.\n");
				}

                                clientSocket.setSoTimeout(1000);
                                
                                
                                        //receive something from the IG for the connection being open

                                        byte[] receivedData = new byte[2048];

                                        // Create a datagram
                                        DatagramPacket receivedDatagram = 
                                                        new DatagramPacket(receivedData, receivedData.length);
                                
                                while(!open){
                                    try{
                                        // Receive a datagram
                                        clientSocket.receive(receivedDatagram);

                                        if(trace) {
                                                System.out.println("Datagram Receieved");

                                                if(log)
                                                        toLog.write("Datagram Receieved\n");
                                        }

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
                                                            if(trace) {
                                                                    System.out.println("Connection now open preparing to send data.");

                                                                    if(log)
                                                                            toLog.write("Connection now open preparing to send data.\n");
                                                            }
                                                    }
                                            } else {

                                                    clientSocket.send(nData);
                                                    appCount++;
                                                    if(trace) {
                                                            System.out.println("The connection is not open resending request.");

                                                            if(log)
                                                                    toLog.write("The connection is not open resending request.\n");
                                                    }
                                            }
                                    }
                                
                                catch (SocketTimeoutException e) {
						System.out.println("Timeout reached: " + e);
						resent++;
						timeout++;
						if(trace) {
							System.out.println("Reached timeout resending file.");
							
							if(log)
								toLog.write("Reached timeout resending file.\n");
						}

					}
                                }

				//prepare the packets for transmission,
				createPackets(name);
				if(trace) {
					System.out.println("File loaded into the buffer to be sent.");
					
					if(log)
						toLog.write("File loaded into the buffer to be sent.\n");
				}

				while(open){




					byte [] m = packet.get(packetCount);

					mainDataMessage = new byte[m.length + 16];

					if(trace) {
						System.out.println("Data packet loaded into the frame.");
					}

					long dataSum = calculateChecksum(checksum, m);

					if(trace) {
						System.out.println("Checksum calculated: " + dataSum);
					}

					len = m.length;

					if(trace) {
						System.out.println("Total length of data to be sent is: " + len);
					}

					byte [] h = new byte[16];

					h = mainHeader(gatewayAddr, conID, seq, len, dataSum);

					if(trace) {
						System.out.println("Header is loaded.");
					}

					int newCount = 0;
					for(int i = 0; i < h.length; i++) {

						mainDataMessage[i] = h[i];
						newCount++;
					}

					for(int i = 0; i < m.length; i++) {
						mainDataMessage[i + newCount] = m[i];
					}

					DatagramPacket mainData = new DatagramPacket(mainDataMessage, mainDataMessage.length, destination, gatewayPort);
					if(trace) {
						System.out.println("The main data message is built.");
						
						if(log)
							toLog.write("The main data message is built.\n");
					}
					clientSocket.send(mainData);
					appCount++;
					if(trace) {
						System.out.println("The message is sent waiting for acknowledgement.");
						
						if(log)
							toLog.write("The message is sent waiting for acknowledgement.\n");
					}
					clientSocket.setSoTimeout(1000);

					try {
						
                                            clientSocket.receive(receivedDatagram);
						if(trace) {
							System.out.println("Received a datagram.");
							
							if(log)
								toLog.write("Received a datagram.\n");
						}
                                            byte[] receivedHeader = new byte[12];
                                            byte[] recData = new byte[receivedDatagram.getLength() - 12];
                                            int out = 0;
						for(int i = 0; i < receivedHeader.length; i++) {
							receivedHeader[i] = receivedData[i];
						}
                                            byte[] cSum = new byte [4];

						for(int i = 0; i < cSum.length; i++) {
							cSum[i] = receivedHeader[i + 4];
						}

						out += receivedHeader.length;

						for(int i = 0; i < recData.length; i++) {
							recData[i] = receivedData[i + out];
						}

                                            int low = recData[0] >= 0 ? recData[0] : 256 + recData[0];
                                            int high = recData[1] >= 0 ? recData[1] : 256 + recData[1];
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
						resent++;
						timeout++;
						if(trace) {
							System.out.println("Reached timeout resending file.");
							
							if(log)
								toLog.write("Reached timeout resending file.\n");
						}

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
						appCount++;
						if(trace) {
							System.out.println("Closing packet sent.");
							
							System.out.println("Application packets sent: " + appCount + "\n"
								+"Packets that were resent: " + resent + "\n"
								+"Packets resent through timeout: " + timeout + "\n");
							
							if(log) {
								toLog.write("Closing packet sent.\n");
								
								toLog.write("Application packets sent: " + appCount + "\n"
								+"Packets that were resent: " + resent + "\n"
								+"Packets resent through timeout: " + timeout + "\n");
							}
						}
						open = false;
						
						//this is a comment

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
		//System.out.println(count);

		// put the host address in
		for(int i = 0; i < hostAdd.length; i++) {
			head[i + count] = hostAdd[i];
			temp++;
		}

		count += temp;

		//System.out.println(count);//only for debugging will be commented out

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
		count++;

		//server port number
		byte [] servp = new byte [2];
		servp[0] = (byte) (sPort & 0xFF);
		servp[1] = (byte) ((sPort >> 8) & 0xFF);

		for(int i = 0; i < servAdd.length; i++) {
			mess[count++] = servAdd[i];
		}

		for(int i = 0; i < servp.length; i++) {
			mess[count++] = servp[i];
		}

		byte [] size = new byte[4];
		size[3] = (byte) (totalLength & 0xFF);
		size[2] = (byte) ((totalLength >> 8) & 0xFF);
		size[1] = (byte) ((totalLength >> 16) & 0xFF);
		size[0] = (byte) ((totalLength >> 24) & 0xFF);

		ByteBuffer s = ByteBuffer.wrap(size);
		System.out.println(s.getInt());


		for(int i = 0; i < size.length; i++) {
			mess[count++] = size[i];
		}

		byte[] name = filename.getBytes();
		for(int i = count; i < mess.length; i++) {
			mess[i] = name[i - count];
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

		//Load zeroes
		head[count++] = (byte) 0;//padding
		head[count++] = (byte) 0;//padding

		//Load Seq number
		head[count++] = (byte) (seq & 0xFF);
		head[count++] = (byte) ((seq >> 8) & 0xFF);

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
