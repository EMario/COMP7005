////////////////////////////////////////////////////
//Author: Mario Enriquez
//COMP 7005
//TCP Client
//Assignment 1
////////////////////////////////////////////////////

import java.net.Socket;
import java.io.*;
import java.util.Scanner;

public class tcp_client{
	
	//DefaultPort
	public static int default_tcp_port = 7005;
	//Default Size of the Buffer
	public static int buffer_length = 80;
	
	public static void main(String args[]) throws IOException{
		
		//Declaration of variables used within the code
		String host;
		String path=System.getProperty("user.dir") + "/Client_Files";
		String option,filename,line;
		int		port;
		int lastByteRead;
		long size;
		boolean done=false;
		boolean receivingData;
		byte fileContent[]; 		
		Socket socket=null;
		byte messageType; 		
		InputStream input = null;
		OutputStream output=null;
		DataOutputStream dataOutput = null;
		DataInputStream dataInput = null;
		FileInputStream inputStream = null;
		FileOutputStream fileOutput = null;
		Scanner scanner = new Scanner(System.in);
		File folder,file;
		File[] listOfFiles;
		
		//Switch to determine if the user manually input the port, if the user didn't specify the server.
		//It terminates the client
		switch(args.length){
			case 1:
				host = args[0];
				port = default_tcp_port;
				break;
			case 2:
				host =	args[0];
				port =	Integer.parseInt(args[1]);
				break;
			default:
				System.out.println("Error. Server isn't specified.");
				return;
		}
		System.out.println("Connecting to Host: " + host +" in the Port: " + port);
		
		try {
		
			//Initialize the connection with the client
			socket=new Socket(host,port);
			//Initialize the input and output streams
			output= socket.getOutputStream();
			dataOutput = new DataOutputStream(output);
			dataInput = new DataInputStream(socket.getInputStream());
			
			do{
				
				//Prompt User to input what he wants to do 
				//GET means the client wants to Download a file
				//SEND means the client wants to Upload a file
				//SHOW means the client wants to know what files are in the Server directory
				//DIR means the client wants to know what files are in the Client directory
				//END means the client wants to close the connection
				System.out.println("Please give an instruction, note that the instrutions are not case sensitive:\n" +
				"Get to download a file\n"+
				"Send to upload a file to the server\n"+
				"Show to show the current files in the server\n"+
				"Dir to show the current files in the directory\n"+
				"End to finish the current session");
				option=scanner.nextLine();
				option=option.toUpperCase();
				
				switch(option){
					
					case "GET":
						
						//User is asked to input the file name
						System.out.println("\nWrite the name of the file to download:");
						filename=scanner.nextLine();
						System.out.println("\n");
						//The Client sends a byte indicating that the user wants to download a file
						//And then sends a string with the file name attached
						dataOutput.writeByte(0);
						dataOutput.writeUTF(filename);
						receivingData=true;
						//The user waits for a response of the server
						messageType = dataInput.readByte();
						//If the server found the file the client will start it's output streams
						//to save the file while there are still bytes to download
						input=socket.getInputStream();
						if(messageType!=-1){
							fileOutput = new FileOutputStream(path + "/" + filename);
							while(dataInput.readByte()==4){
								String data=dataInput.readUTF();
								int bytes_received= Integer.parseInt(data);
								fileContent = new byte[bytes_received];
								input.read(fileContent,0,bytes_received);
								fileOutput.write(fileContent);
								// fileOutput.write((dataInput.readUTF()).getBytes());
							}
							System.out.println("\nDownload Complete!\n");
						} else {
							//In the case the file isn't in the Server, the server will return a
							//message indicating that the file wasn't found
							System.out.println(""+dataInput.readUTF());
						}
						
						break;
						
					case "SEND":
						
						//User is asked to input the file name
						System.out.println("\nWrite the name of the file to upload:");
						filename=scanner.nextLine();
						System.out.println("\n");
						//Since we are searching on the client's directory, we are will make
						//the search first in the Client's directory before making the Upload
						//Request to the Server
						folder = new File(path);
						listOfFiles = folder.listFiles();
						size=-1;
						for (int i = 0; i < listOfFiles.length; i++) {
							if (listOfFiles[i].isFile()) {
								if(listOfFiles[i].getName().equals(filename)){
									size=listOfFiles[i].length();
								}
							}
						}
						//If the file is found we initialize connection with the Server otherwise
						//we let the user know that the file was not found
						if(size<0){
							System.out.println("Error: File not found.\n");
							break;
						}else{
							file = new File(path + "/" + filename);
							inputStream = new FileInputStream(path + "/" + filename);
							lastByteRead=0;
							
							//The client write a 1 byte so that the server knows its an Upload request
							//the client sends the name and size of the file to upload
							dataOutput.writeByte(1);
							dataOutput.writeUTF(filename);
							dataOutput.writeUTF(""+size);
							dataOutput.flush();
							//The client reads the file in buffer_lenght sized chunks and sends them to the
							//server, when the file reaches EOF it ends the cycle
							output = socket.getOutputStream();
							while(lastByteRead<size){
								if((lastByteRead+buffer_length)>size){
									int newsize=buffer_length-((lastByteRead+buffer_length)-(int)size);
									fileContent = new byte[newsize];	
								} else{
									fileContent = new byte[buffer_length];
								}
								inputStream.read(fileContent);
								dataOutput.writeByte(4);
								dataOutput.writeUTF(""+fileContent.length);
								output.write(fileContent,0,fileContent.length);
								// dataOutput.writeUTF(new String(fileContent));
								lastByteRead+=buffer_length;
								dataOutput.flush();
							}
							System.out.println("\nUpload Complete!\n");
							inputStream.close();
							//We terminate the Uploading operation with the server
							dataOutput.writeByte(-1);
						}
						break;
					
					case "SHOW":
						
						//The client writes a byte with the number 2 to indicate the server that it wants to know
						//which files are in the server directory
						System.out.println("\n");
						dataOutput.writeByte(2);
						done=false;
						//If the Server contains files they will be sent with a 1 byte and then the name of the file
						//when there are no more files to show it sends -1 to end the operation
						while(!done) {
							messageType = dataInput.readByte();
							if(messageType==1)
							{
								System.out.println(""+dataInput.readUTF());
							} else {
								done = true;
							}

						}
						System.out.println("\n");
						break;
					
					case "DIR":
					
						//The client will get the files in the /Client_Files
						System.out.println("\nThese are the current files in the directory:\n");
						folder = new File(path);
						listOfFiles = folder.listFiles();
						//For cycle to show all the files in the /Client_Files directory of the client
						for (int i = 0; i < listOfFiles.length; i++) {
							if (listOfFiles[i].isFile()) {
								System.out.println("" + listOfFiles[i].getName());
							}
						}
						System.out.println("\n");
						dataOutput.flush();
						break;
					
					case "END":
						
						//Sends a -1 byte to the Server to clos the connection between the client and server.
						System.out.println("Ending Connection...");
						dataOutput.writeByte(-1);
						break;
					
					default:
						
						//User input a non-existant command
						System.out.println("Error, not valid command.");
						break;
						
				}
				
				dataOutput.flush();

			} while(!(option.equals("END")));
			
			//Closes data streams
			if (socket != null) socket.close();
			if (output != null) output.close();
			if (input != null) input.close();
			if (dataOutput != null) dataOutput.close();
			if (dataInput != null) dataInput.close();
			if (inputStream != null) inputStream.close();
			if (fileOutput != null) fileOutput.close();
		
		} catch(EOFException e){
			
			//Error message when a file is not found
			System.out.println("Error. Specified directory doesn't exists.");
		
		} catch(IOException e){
		
			//Error message when a connection error ocurrs
			System.out.println("Error. Server couldn't be reached.");
		
		} finally {
			
			//Closes any remaining instances
			if (socket != null) socket.close();
			if (output != null) output.close();
			if (input != null) input.close();
			if (dataOutput != null) dataOutput.close();
			if (dataInput != null) dataInput.close();
			if (inputStream != null) inputStream.close();
			if (fileOutput != null) fileOutput.close();
		
		} 

	}

}

