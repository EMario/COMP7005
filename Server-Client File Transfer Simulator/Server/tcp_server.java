////////////////////////////////////////////////////
//Author: Mario Enriquez
//COMP 7005
//TCP Server
//Assignment 1
////////////////////////////////////////////////////

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class tcp_server {

	//DefaultPort
	public static int default_tcp_port = 7005;
	//Default Size of the Buffer
	public static int buffer_length=80;

    public static void main(String[] args) throws IOException{
		
		//Declaration of variables used within the code
		int		port;
		ServerSocket server_socket=null;
		Socket socket=null;
		FileOutputStream fileOutput = null;
		DataInputStream dataInput = null;
		DataOutputStream dataOutput = null;
		InputStream input = null;
		OutputStream output=null;
		boolean isConnected=true;
		boolean done=false;
		String filename;
		String path =System.getProperty("user.dir") + "/Server_Files";
		long size;
		int read=0;
		int bytes=0;
		int lastByteRead;
		byte fileContent[];
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		
		//Switch to determine if the user manually input the port
		switch(args.length){
			
			default:
				port = default_tcp_port;
				break;
			
			case 1:
				port =	Integer.parseInt(args[0]);
				break;
		
		}
		
		try{
			
			//Initialize the Server Socket
			server_socket=new ServerSocket(port);
			//Cycle to maintain the server connection
		
			while (isConnected) {
				System.out.println("Waiting for connection...");
				try{
					
					//Initialize the connection with the client
					socket=server_socket.accept();
					System.out.println("Accepted connection : " + socket);
					//Initialize the Input and Output Streams
					dataInput = new DataInputStream(socket.getInputStream());
					dataOutput = new DataOutputStream(socket.getOutputStream());
					done=false;
					//While cycle that is maintained until user finishes connection
					while(!done) {
						
						//Server receives data about the operation that the client wants to do
						byte messageType = dataInput.readByte();
						
						//Switch depending what the client wants to do
						//0 means the client wants to Download a file
						//1 means the client wants to Upload a file
						//2 means the client wants to know what files are in the directory
						//-1 means the client wants to close the connection
						switch(messageType){
							
							case 0: // User wants to Download a file
								
								//The server receives the file name that the client wants to download
								filename = dataInput.readUTF();
								listOfFiles = folder.listFiles();
								size=-1;
								//The server searches in his Server_Files directory for the file
								for (int i = 0; i < listOfFiles.length; i++) {
									if (listOfFiles[i].isFile()) {
										if(listOfFiles[i].getName().equals(filename)){
											size=listOfFiles[i].length();
										}
									}
								}
								lastByteRead=0;
								//If the server found the file, the server will send it to the client
								//if not the server will send notice that the specified file doesn't exists
								if(size!=-1){
									System.out.println("Sending "+ filename +"...");
									dataOutput.writeByte(4);
									dataOutput.flush();
									input = new FileInputStream(path + "/" + filename);
									//The server reads the file in buffer_lenght sized chunks and sends them to the
									//client, when the file reaches EOF it ends the cycle
									output = socket.getOutputStream();
									while(lastByteRead<size){
										if((lastByteRead+buffer_length)>size){
											int newsize=(lastByteRead+buffer_length)-(int)size;
											fileContent = new byte[newsize];	
										} else{
											fileContent = new byte[buffer_length];
										}
										input.read(fileContent);
										dataOutput.writeByte(4);
										dataOutput.writeUTF(""+fileContent.length);
										output.write(fileContent,0,fileContent.length);
										// dataOutput.writeUTF(new String(fileContent));
										lastByteRead+=buffer_length;
										dataOutput.flush();
									}
									//The Server sends a -1 to the client, so that the client knows when the server
									//will stop sending him data
									dataOutput.writeByte(-1);
									System.out.println("Download complete!");
									dataOutput.flush();
								} else{
									//The Server sends a -1 to the client, so that the client knows that the specified
									//file doesn't exists
									dataOutput.writeByte(-1);
									dataOutput.writeUTF("Error: "+ filename +" File not found.");
									dataOutput.flush();
								}
								break;
							
							case 1: //User wants to upload a file
								
								//Server receives client filename and size
								filename = dataInput.readUTF();
								size = Long.parseLong(dataInput.readUTF());
								System.out.println("Receiving " + filename + " Size: " +size +"...");
								fileOutput = new FileOutputStream(path+"/"+filename);
								//Server receives the client's file data to create the new file
								input=socket.getInputStream();
								fileContent = null;
								while(dataInput.readByte()==4){
									String data=dataInput.readUTF();
									int bytes_received= Integer.parseInt(data);
									fileContent = new byte[bytes_received];
									input.read(fileContent,0,bytes_received);
									fileOutput.write(fileContent);
								}
								fileOutput.close();
								break;
							
							case 2: // User wants to know which files are in the server

								System.out.println("Showing Current Files in the directory...");
								//For cycle to show all the files in the /Server_Files directory of the server
								//For each file it finds, the file name is sent to the client
								for (int i = 0; i < listOfFiles.length; i++) {
									if (listOfFiles[i].isFile()) {
										dataOutput.writeByte(1);
										dataOutput.writeUTF(listOfFiles[i].getName());
										dataOutput.flush();
									}
								}
								//The Server sends a -1 to the client, so that the client knows when the server
								//will stop sending him data
								dataOutput.writeByte(-1);
								dataOutput.flush();
								break;
							
							default: // User wants to end the connection, done flag changes to true to it closes connection with the client
								
								done = true;
								break;
						
						}
					
					}
					// isConnected=false;
				
				} catch(IOException e){
			
					//Error Message when a connection problem occurs
					System.out.println("Error. Connection Problem with the client.");
					done=true;
		
				}finally{
					
					//Closes the connection with the current client
					if (socket != null) socket.close();
				
				}
				if (input != null) input.close();
				if (socket != null) socket.close();
				if (dataInput != null) dataInput.close();
				if (dataOutput != null) dataOutput.close();
				if (fileOutput != null) fileOutput.close();
				if (output != null) output.close();

			}
		
		} catch(EOFException e){
		
			//Error Message
			System.out.println("Error. Specified directory doesn't exists.");
		
		} catch(IOException e){
			
			//Error Message when a connection problem occurs
			System.out.println("Error. Connection Problem.");
		
		}finally{
			
			//Closes all instances that may be open
			if (server_socket != null) server_socket.close();
			if (input != null) input.close();
			if (output != null) output.close();
			if (socket != null) socket.close();
			if (dataInput != null) dataInput.close();
			if (dataOutput != null) dataOutput.close();
			if (fileOutput != null) fileOutput.close();
			
		} 		
	}

}