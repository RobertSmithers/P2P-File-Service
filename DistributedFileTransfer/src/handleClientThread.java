import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class handleClientThread implements Runnable {

	List<ClientObject> regClnts = new CopyOnWriteArrayList<>();

	ClientObject account;
	Socket socket;

	// clients should not include client. That is for this thread to decide
	public handleClientThread(ClientObject client, List<ClientObject> clients) throws IOException {
		this.account = client;
		this.socket = client.getSocket();
		this.regClnts = clients;
	}

	@Override
	public void run() {
		// find out who the client is.
		SocketAddress clientAddress = account.getSocket().getRemoteSocketAddress();
		System.out.printf("Client (%s) is being handled\n", clientAddress);

		DataObject data;

		Boolean firstRegister = true;
		try {
			// Introduction
			data = new DataObject();
			data.setMessage("Welcome to the Public File Exchange!\nWould you like to register a file or make a request?");
			data.setRegistering();
			toClient(socket, data);

			data = fromClient(socket);

			// Client wants to register/request
			while (true) {
				if (data.exit)
				 	break;

				while (data.isRegistering() && !data.exit) {
					if (firstRegister) {
						account.setPort(data.getPort());	// Gets port of uploadDownload server
						account = registerAccount(account, regClnts);
						firstRegister = false;
					}

					boolean success = registerFile(socket, account);
					if (success) {
						String msg = "Your file has been successfully registered at " + socket.getInetAddress() + ".\n\nWould you like to register another file or make a request?";
						data.setMessage(msg);
						data.fileNames = getFileListFrom(socket, regClnts);
						data.success = true;
					} else {
						data.setMessage("\nWould you like to register a file or make a request?");
					}
					toClient(socket, data);
					data = fromClient(socket);
				}

				while (data.isRequesting() && !data.exit) {
					requestFile(socket, account);
					data = fromClient(socket);
					data.setMessage("Your file is being downloaded.\n\nWould you like to register a file or make another request?");
					toClient(socket, data);
					data = fromClient(socket);
				}
			}
			System.out.println("Closing connection with " + socket.getInetAddress() + ":" + socket.getPort() + " and removing them from client list");
			regClnts.remove(account);
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This is made with the intention of scaling the file lists/client lists to some database
	private ClientObject registerAccount(ClientObject account, List<ClientObject> regClients) {
		// Check if this account already exists
		// If it is, account = regClnts with that info (so as to keep array of
		// fileNames)
		for (ClientObject clnt : regClients) {
			if (clnt.getIp() == account.getIp() && clnt.getPort() == account.getPort())
				return clnt;
		}

		// Add to list of registered clients
		regClnts.add(account);
		return account;
	}

	private boolean registerFile(Socket socket, ClientObject client) {
		DataObject data = new DataObject();
		data.setMessage("Okay! What is the path to your file? (Ex: \"./file.txt\")");
		data.setRegistering();
		try {
			toClient(socket, data);
			boolean alreadyExists = true, found = false;
			String file = "";
			while (alreadyExists && !data.exit) {
				data = fromClient(socket);
				file = data.getFileName();

				for (String fname : client.getFiles()) {
					if (fname.equals(file)){
						data.setMessage("That file is already linked with that ip/port. Please try again, or type back to go back.\nWhat is the path to your file? (Ex: \"./file.txt\")");
						data.success = false;
						toClient(socket, data);
						found = true;
					}
					
				}
				alreadyExists = found;
				found = false;
			}
			
			if (data.exit)
				return false;

			client.addFile(file);
			System.out.printf("Client (%s) has registered file %s at port %s\n", socket.getInetAddress(), file, client.getPort());			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void requestFile(Socket socket, ClientObject client) {
		DataObject data = new DataObject();
		data.setMessage("Okay! What is the ip:port? (Ex: \"127.0.0.1:1234\")");
		data.setRequesting();
		try {
			toClient(socket, data);

			data = fromClient(socket);
			String ip = data.ip;
			int port = data.port;

			// Check if client has this file registered
			String[] files = getFileListFrom(ip, port, regClnts);
			while (files.length == 0) {
				String msg = "There are no files at "+ip+":"+port+". Please try again or type back";
				data.setMessage(msg+"What is the ip:port? (Ex: \"127.0.0.1:1234\")");
				data.setRequesting();
				toClient(socket, data);
				files = getFileListFrom(ip, port, regClnts);
			}
			
			data.setMessage("Client "+ip+":"+port+" is a valid client.");
			data.fileNames = files;
			toClient(socket, data);		// Tell client that is a valid ip/port
			data = fromClient(socket);	// Client gives us filename they want
			String file = data.getFileName();
			int peerPort = findPortFor(file, ip, regClnts);
			data.setIPPort(data.getIp(), peerPort);
			toClient(socket, data);

			System.out.printf("Client (%s:%s) is receiving file %s from %s:%s\n", socket.getInetAddress(), socket.getPort(), file, ip, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int findPortFor(String fname, String ip, List<ClientObject> regClients) {
		for (ClientObject client : regClients)
			try {
				if (client.getIp().equals(InetAddress.getByName(ip))) {
					String[] flist = getFileListFrom(ip, client.getPort(), regClients);
					for (String file : flist)
						if (file.equals(fname))
							return client.getPort();
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		return -1;
	}

	private static String[] getFileListFrom(String ip, int port, List<ClientObject> regClients) {
		for (ClientObject client : regClients)
			try {
				if (client.getIp().equals(InetAddress.getByName(ip)) && client.getPort() == port)
					return client.getFiles();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		String[] empty = {};
		return empty;
	}

	private static String[] getFileListFrom(Socket socket, List<ClientObject> regClients) {
		for (ClientObject client : regClients)
			if (client.getIp().equals(socket.getInetAddress()))
				return client.getFiles();
		String[] empty = {};
		return empty;
	}

	public static void toClient(Socket clntSock, DataObject toClient) throws IOException {
		try {
			OutputStream os = clntSock.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(toClient);

		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in Send EOFException: goodbye client at " + clntSock.getRemoteSocketAddress()
					+ " with port# " + clntSock.getPort());
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in Send IOException: goodbye client at " + clntSock.getRemoteSocketAddress()
					+ " with port# " + clntSock.getPort());
			clntSock.close(); // this requires the throws IOException
		}
	}

	public static DataObject fromClient(Socket clntSock) throws IOException {
		// client transport and network info
		SocketAddress clientAddress = clntSock.getRemoteSocketAddress();
		int port = clntSock.getPort();
		DataObject fromClient = null;
		try {
			InputStream is = clntSock.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			fromClient = (DataObject) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in receive EOF: goodbye client at " + clientAddress + " with port# " + port);
			clntSock.close(); // Close the socket. We are done with this client!
			// now terminate the thread
		} catch (IOException e) {
			System.out.println("in receive IO: goodbye client at " + clientAddress + " with port# " + port);
			clntSock.close(); // this requires the throws IOException
		}
		return fromClient;
	}

}
