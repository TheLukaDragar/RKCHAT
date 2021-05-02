import java.io.*;
import java.net.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ChatClient extends Thread
{
	protected int serverPort = 1234;
	

	public static void main(String[] args) throws Exception {
		new ChatClient();
	}

	public ChatClient() throws Exception {
		Socket socket = null;
		DataInputStream in = null;
		DataOutputStream out = null;
		String name = "";

		String s = """


           Welcome to chat 3000
		   1. Enter your name 
		   2. send public messages by thyping in the console
		   3. send private messages with //username message

           """;
		System.out.println(s);


		System.out.println("Vnesi svoje ime");
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));

		
			do{	
			name = std_in.readLine();
			}
			while(name.equals(""));
			

		// connect to the chat server
		try {
			

			System.out.println("[system] connecting to chat server ...");
			socket = new Socket("4.tcp.ngrok.io", 15642); // create socket connection
			in = new DataInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
			out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages
			System.out.println("[system] connected");

			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread for listening to messages from the chat server
			message_receiver.start(); // run the new thread
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		sendAuth(name, out);//posiljanje imena na server

		
		// read from STDIN and send messages to the chat server
		
		String userInput;
		while ((userInput = std_in.readLine()) != null) { // read a line from the console

			if(userInput.length() > 4 &&   userInput.substring(0, 2).equals("//")){
				if( userInput.split(" ").length<=1){
					System.out.println("Wrong format!");
					continue;

				}
				this.sendPrivateMessage(name,userInput.split(" ")[0].substring(2), userInput.split(" ")[1], out);
				System.out.println("Sending private message");

			}else{
				this.sendPublicMessage(name,userInput, out); // send the message to the chat server
			}
			
		}

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendPublicMessage(String ime,String message, DataOutputStream out) {
		try {
			
			
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a");
			String formattedDate = sdf.format(date);
			JSONObject obj = new JSONObject();

        	obj.put("tip", "public");
        	obj.put("name", ime);
			obj.put("date", formattedDate);
			obj.put("text", message);

			out.writeUTF(obj.toJSONString()); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}
	private void sendPrivateMessage(String ime,String naslovnik,String message, DataOutputStream out) {
		try {
			
			
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM:dd:yyyy h:mm:ss a");
			String formattedDate = sdf.format(date);
			JSONObject obj = new JSONObject();

        	obj.put("tip", "private");
        	obj.put("name", ime);
			obj.put("to", naslovnik);
			obj.put("date", formattedDate);
			obj.put("text", message);
			
			out.writeUTF(obj.toJSONString()); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}

	private void sendAuth(String name, DataOutputStream out) {
		try {

			JSONObject obj = new JSONObject();
        	obj.put("tip", "auth");
        	obj.put("name", name);


			out.writeUTF(obj.toJSONString()); // send the message to the chat server
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send AUTH");
			e.printStackTrace(System.err);
		}
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private DataInputStream in;

	public ChatClientMessageReceiver(DataInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			String message;
			while ((message = this.in.readUTF()) != null) { // read new message
				JSONParser parser = new JSONParser();
				JSONObject jsonObject = (JSONObject) parser.parse(message);
					String TIP = (String) jsonObject.get("tip");

					

					switch(TIP){
						case"public":{
							String from = (String) jsonObject.get("from");
							String text = (String) jsonObject.get("text");

							System.out.printf("Public [%s]: %s\n",from, text);

						}
						break;

						case "private":{
							String from = (String) jsonObject.get("from");
							String text = (String) jsonObject.get("text");

							System.out.printf("Private [%s]: %s\n",from, text);

						}
						break;

						case "error":{

							String from = (String) jsonObject.get("from");
							String text = (String) jsonObject.get("text");

							System.out.printf("[%s] ERROR %s\n",from, text);

						}
						
					}


			}
		} catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
