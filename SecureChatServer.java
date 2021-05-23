import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SecureChatServer {

	protected int serverPort = 1234;
	protected List<SSLSocket> clients = new ArrayList<SSLSocket>(); // list of clients

	protected HashMap<String, SSLSocket> databaseHashMap = new HashMap<String, SSLSocket>();


	public static void main(String[] args) throws Exception {
		new SecureChatServer();
	}

	public SecureChatServer() {
		SSLServerSocket serverSocket = null;

		// create socket
		try {
			String passphrase = "serverpwd";

			// preberi datoteko z odjemalskimi certifikati
			KeyStore clientKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje odjemalčevih javnih
																	// ključev (certifikatov)
			clientKeyStore.load(new FileInputStream("keys/client.public"), "public".toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim ključem
			KeyStore serverKeyStore = KeyStore.getInstance("JKS"); // KeyStore za shranjevanje strežnikovega tajnega in
																	// javnega ključa
			serverKeyStore.load(new FileInputStream("keys/server.private"), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kakšni so moji tajni ključi in
			// certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(clientKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(serverKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), (new SecureRandom()));

			// kreiramo socket
			SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
			 serverSocket = (SSLServerSocket) factory.createServerSocket(this.serverPort);
			serverSocket.setNeedClientAuth(true); // tudi odjemalec se MORA predstaviti s certifikatom
			serverSocket.setEnabledCipherSuites(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256" });
			//serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				//Socket newClientSocket = serverSocket.accept(); // wait for a new client connection
				SSLSocket newClientSocket = (SSLSocket) serverSocket.accept(); // vzpostavljena povezava
				newClientSocket.startHandshake(); // eksplicitno sprozi SSL Handshake
				String username = newClientSocket.getSession().getPeerPrincipal().getName().substring(3);
				System.out.println("Established SSL connection with: " + username);

				
				synchronized(this) {
					clients.add(newClientSocket); // add client to the list of clients
				}
				SecureChatServerConnector conn = new SecureChatServerConnector(this, newClientSocket); // create a new thread for communication with the new client
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String ime,String message,String date) throws Exception {
		Iterator<SSLSocket> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			SSLSocket socket = (SSLSocket) i.next(); // get the socket for communicating with this client
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client

				JSONObject obj = new JSONObject();

					obj.put("tip", "public");
					obj.put("from", ime);
					obj.put("date", date);
					obj.put("text", message);

				out.writeUTF(obj.toJSONString()); // send message to the client
			} catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			}
		}
	}

	public void sendToClient(String tip,String ime,String prejemnik,String message,String date) throws Exception {

		System.out.println(databaseHashMap.keySet());
		SSLSocket socket = databaseHashMap.get(prejemnik);

		if(socket==null){
			throw new NullPointerException();
		}
			try {
				DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // create output stream for sending messages to the client

				JSONObject obj = new JSONObject();

					obj.put("tip", tip);
					obj.put("from", ime);
					obj.put("to", prejemnik);
					obj.put("date", date);
					obj.put("text", message);

				out.writeUTF(obj.toJSONString()); // send message to the client
			}

			catch (NullPointerException e){

				System.err.println("[system] could not send message to a client - client not found");
				
			}
			
			catch (Exception e) {
				System.err.println("[system] could not send message to a client");
				e.printStackTrace(System.err);
			} 
		
	}

	

	public void removeClient(SSLSocket socket){
		synchronized(this) {
			clients.remove(socket);
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean dodajIme(String ime,SSLSocket socket) {

		boolean ret = true;

		synchronized(this) {

			if(databaseHashMap.containsKey(ime)){
				ret = false; 
			}else{
				databaseHashMap.put(ime, socket);
			}
			
		}

		return ret;
	}
}

class SecureChatServerConnector extends Thread {
	private SecureChatServer server;
	private SSLSocket socket;

	public SecureChatServerConnector(SecureChatServer server, SSLSocket socket) {
		this.server = server;
		this.socket = socket;
	}

	public void run() {
		System.out.println("[system] connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());

		DataInputStream in;
		try {
			in = new DataInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		try {
			String username = this.socket.getSession().getPeerPrincipal().getName().substring(3).toLowerCase();

			if (!this.server.dodajIme(username, socket)) {// dodaj ime in preveri ce je ze vpisan
				System.out.println("Uporabnik " + username + " je ze vpisan!");
				this.server.removeClient(this.socket);

				return;
				
			}
		} catch (SSLPeerUnverifiedException e1) {
			e1.printStackTrace();
		}

		JSONParser parser = new JSONParser();

		ZANKA : while (true) { // infinite loop in which this thread waits for incoming messages and processes them
			String msg_received;
			try {
				msg_received = in.readUTF(); // read the message from the client

				if (msg_received.length() == 0){
					continue ZANKA;

				} // invalid message

				

			JSONObject jsonObject = (JSONObject) parser.parse(msg_received);
            

            String TIP = (String) jsonObject.get("tip");
			String ime = (String) jsonObject.get("name");

			switch(TIP){

				/*

				case "auth":{

					System.out.println("Nov uporabnik se je povezal z imenom "+ ime);
					if(!this.server.dodajIme(ime,socket)){//dodaj ime in preveri ce je ze vpisan
						System.out.println("Uporabnik " + ime+" je ze vpisan!");
						
						//this.server.sendToClient("error","Server",ime,"Uporabnik je ze vpisan",formattedDate);
						this.server.removeClient(this.socket);
						break ZANKA;
						
					}
				}
				break;
				*/

				case "public":{
					String message = (String) jsonObject.get("text");
					String date = (String) jsonObject.get("date");

					
						

					try {
						this.server.sendToAllClients(ime,message,date); // send message to all clients
					} catch (Exception e) {
						System.err.println("[system] there was a problem while sending the message to all clients");
						e.printStackTrace(System.err);
						continue ZANKA;
					}

				}
				break;

				case "private":{
					String message = (String) jsonObject.get("text");
					String prejemnik = (String) jsonObject.get("to");
					String date = (String) jsonObject.get("date");

					try {
						this.server.sendToClient("private",ime,prejemnik,message,date); // send message to all clients
					}

					catch(NullPointerException e){
						this.server.sendToClient("error","Server",ime,"uporabnik ne obstaja zal mi je :(",date);
					}
					catch (Exception e) {
						System.err.println("[system] there was a problem while sending the message to all clients");
						e.printStackTrace(System.err);
						continue ZANKA;
					}

				}

				System.out.println("[RKchat] [" + this.socket.getPort() + "] : " + msg_received); // print the incoming message in the console
				




			}

			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port " + this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

			

			

			
		}
		
	}
}
