////////////////////////////////
//
//	Author: Mario Enriquez
//
//	ID: A00909441
//
//	Network Emulator
//
//	COMP 7005
//
////////////////////////////////

import java.io.*; 
import java.net.*;
import java.util.Random;
 
class UDPNetwork {    
	
	public String UDPServerAdress;
	public String UDPClientAdress;
	public RandomAccessFile logFile;
	public int UDPServerPort;
	public int UDPClientPort;
	public int windowSize;
	public int bufferLength;
	public int errorRate;
	public int packetRate;
	public String config_file= "config.txt";
	public String log_file="log.txt";	
	public String path;

	public String getConfig(){ // function to get the configuration parameters
		String config = "";
		try{
			path =System.getProperty("user.dir");
			BufferedReader br = new BufferedReader(new FileReader(  path + "/" + config_file));
			String line = new String();
			while((line = br.readLine()) != null ){
				config+=line;
			} 
		}catch(FileNotFoundException f){
			System.out.println("FileNotFoundException: " + f);
		}catch(IOException io){
			System.out.println("IOException: " + io);
		}
		
		return config;
	}
	
	public void set_network_params(String config){ //function to set the configuration parameters related to the client
		String[] config_params = config.split(";");
		String temp = "";
		String newValue;
		
		for(int counter=0; counter<config_params.length;counter++){
			if(config_params[counter].startsWith("clientIP")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				UDPClientAdress = newValue;
			} else if(config_params[counter].startsWith("serverIP")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				UDPServerAdress = newValue;
			} else if(config_params[counter].startsWith("windowSize")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				windowSize = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("bufferLength")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				bufferLength = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("port_server")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				UDPServerPort = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("errorRate")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				errorRate = Integer.parseInt(newValue);
			} else if(config_params[counter].startsWith("packetRate")){
				temp = config_params[counter].substring(config_params[counter].indexOf("=")+1);
				newValue=temp.replaceAll("\\s+","");
				packetRate = Integer.parseInt(newValue);
			} 
		}
	}
	
	public static void main(String args[]) throws Exception       {       
		boolean cont=true;
		UDPNetwork udpNetwork =  new UDPNetwork();
		String config=udpNetwork.getConfig();
		udpNetwork.set_network_params(config);
		DatagramSocket networkSocket = new DatagramSocket(udpNetwork.UDPServerPort);             
		byte[] receiveData = new byte[udpNetwork.bufferLength+15];             
		byte[] sendData = new byte[udpNetwork.bufferLength+15];             
		InetAddress clientIPAddress = InetAddress.getByName(udpNetwork.UDPClientAdress);                   
		InetAddress serverIPAddress = InetAddress.getByName(udpNetwork.UDPServerAdress);                   
		Random r = new Random();
		int result = r.nextInt(udpNetwork.packetRate-1) + 1;
		File logs= new File((udpNetwork.path + "/network_files/" + udpNetwork.log_file));
		logs.delete();
		logs.createNewFile();			
		udpNetwork.logFile = new RandomAccessFile((udpNetwork.path + "/network_files/" + udpNetwork.log_file),"rw");
		while(cont){                   
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);                   
			networkSocket.receive(receivePacket);                   
			String sentence = new String( receivePacket.getData());  
			InetAddress IPAddress = receivePacket.getAddress();
			sendData = sentence.getBytes();
			r = new Random();
			result = r.nextInt(udpNetwork.packetRate-0) + 0;
			String aux = ((new String(sendData)).substring(0,1));
			int receivedType = Integer.parseInt(aux);
			aux = (new String(sendData)).substring(1,11).replaceAll("\\s+","");
			int receivedSeq = Integer.parseInt(aux);
			aux = (new String(sendData)).substring(11,15).replaceAll("\\s+","");
			int receivedDataSize = Integer.parseInt(aux);
			udpNetwork.logFile.write(("Received message from: " + IPAddress.getHostAddress() + " Type: " + receivedType + " Sequence: " + receivedSeq + " Size: " + receivedDataSize + "\n").getBytes());
			
			if(result>udpNetwork.errorRate){
				System.out.println("RECEIVED MESSAGE from "+ IPAddress.getHostAddress() +":, SENDING...");
				if((IPAddress.getHostAddress()).equals(udpNetwork.UDPServerAdress)){
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, udpNetwork.UDPClientPort);                   
					udpNetwork.logFile.write(("Sending to : " + udpNetwork.UDPClientAdress + "\n").getBytes());
					System.out.println("...to Client : " + udpNetwork.UDPClientAdress);
					networkSocket.send(sendPacket);                
				} else {
					udpNetwork.UDPClientPort = receivePacket.getPort();      
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIPAddress, udpNetwork.UDPServerPort);                   
					udpNetwork.logFile.write(("Sending to : " + udpNetwork.UDPServerAdress + "\n").getBytes());
					System.out.println("...to Server : " + udpNetwork.UDPServerAdress);
					networkSocket.send(sendPacket);                
				}
			} else {
				System.out.println("...Dropping MESSAGE");
					udpNetwork.logFile.write(("Dropping Message\n").getBytes());
			}
		}       
		udpNetwork.logFile.close();
	} 
}