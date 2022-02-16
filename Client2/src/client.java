import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class client {
	
	private static boolean listening = true;
	private static boolean threadActive = false;
	static final int DEFAULT_PORT = 5000;
	static final String DEFAULT_IP = "127.0.0.1";

	public static void main(String[] args) throws Exception {
		Socket socket = null;
		String server;
		InetAddress serverAddress;
		int servPort;
		Scanner in = new Scanner(System.in);
		
		List<String> files = new CopyOnWriteArrayList<String>();
		downloadUpload upload = new downloadUpload(files);
		Thread t = new Thread(upload);

		server = DEFAULT_IP;
		if (args.length > 0)
			server = args[0];

		servPort = DEFAULT_PORT;
		if (args.length > 1)
			servPort = Integer.parseInt(args[1]);

		serverAddress = InetAddress.getByName(server);

		try { // check valid port num
			if (servPort <= 0 || servPort > 65535)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			System.out.println("Illegal port number 0, " + servPort);
			in.close();
			return;
		}
		
		// data to server, start handshake
		socket = new Socket(serverAddress, servPort);  
		
		DataObject data = new DataObject();

		// Get the HELO!!
		data = fromServer(socket);
		
		while(listening) {
			data.printMessage();
			System.out.println("(1)\tRegister a file");
			System.out.println("(2)\tRequest a file");
			System.out.println("(3)\tExit\n");
			int response = Integer.parseInt(in.nextLine());

			if (response == 1) {
				data.setRegistering();
				data.port = downloadUpload.DEFAULT_PORT;
				toServer(socket, data);
				data = fromServer(socket);
				String fname = "";
				while (!fname.toLowerCase().equals("back") && !data.success) {		// Server clears file name
					data.printMessage();
					fname = in.nextLine();
					while (!fname.toLowerCase().equals("back") && !fileExists(fname)) {
						System.out.println("Sorry, that file name/path doesn't exist.");
						data.printMessage();
						fname = in.nextLine();
					}
					data.setFileName(fname);
					if (fname.toLowerCase().equals("back"))
						data.exit = true;
					toServer(socket, data);		// Tell server the filename to register
					data = fromServer(socket);
				}
				if (fname.toLowerCase().equals("back")) {
					data.exit = true;
					continue;
				}

				files = Arrays.asList(data.fileNames);
				if (!threadActive) {
					// Start service thread
					t = new Thread(upload);
					t.start();
					threadActive = true;
				}
				continue;
			} else if (response == 2) {
				requestAFile(socket, in);
			} else {
				// Exit
				data.exit = true;
				toServer(socket, data);
				break;
			}
			toServer(socket, data);	// Received port ty
			data = fromServer(socket);
		}

		// Kill our thread
		if (t.isAlive()) {
			System.out.println("Closing threads...");
			t.join(3000);
		}

		socket.close();
		System.out.println("Thanks for using this service!");
		System.exit(0);
	}

	private static void requestAFile(Socket socket, Scanner in) {
		try {
			DataObject data = new DataObject();
			data.setRequesting();
			toServer(socket, data);
			data = fromServer(socket);
			String[] files = {};
			String ip = "127.0.0.1";
			int port = 0;
			// Server queries ip/port names until user gives valid ip/port
			while (files.length == 0) {
				data.printMessage();
				String repl = in.nextLine();
				if (repl.toLowerCase().equals("exit") || repl.toLowerCase().equals("back")) {
					return;
				}
				String[] components = repl.split(":");
				ip = components[0];
				port = Integer.parseInt(components[1]);
				data.setIPPort(ip, port);
				toServer(socket, data);
				data = fromServer(socket);	// Server responds with files at this ip
				files = data.fileNames;
			}
			
			data.printMessage();
			System.out.println("Here are the files at this address/port:");
			for (int i = 0; i < files.length; i++) {
				if (i%2 ==0) {
					System.out.println(i+" "+files[i]);
				} else {
					System.out.print(i+" "+files[i]+"\t");
				}
			}
			System.out.println("\nPlease provide an index with the file you would like to download");
			int index = Integer.parseInt(in.nextLine());
			data.fileName = data.fileNames[index];
			toServer(socket, data);		// Tell server what file we want

			// Server gives us the port associated with this client's file(s) to download
			// Different than the port the client used to connect to server
			data = fromServer(socket);
			port = data.getPort();

			if (port == -1) {
				System.out.println("*** Error finding port. Exiting");
				return;
			}
			// start handshake with other
			socket = new Socket(InetAddress.getByName(ip), port);
			data.fileName = files[index];
			toServer(socket, data);		// Tell client the file we want
			data = fromServer(socket);	// Client responds with filesize
			int filesize = data.filesize;
			downloadFile(socket, data.fileName, filesize);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean fileExists(String fname) {
		return (new File(fname)).exists();
	}

	public static void downloadFile(Socket socket, String fileName, int fileSize) {
		byte [] mybytearray  = new byte [fileSize];
		InputStream is;
		try {
			is = socket.getInputStream();
			FileOutputStream fos = new FileOutputStream(fileName);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			int bytesRead = is.read(mybytearray,0,mybytearray.length);
			int current = bytesRead;
	
			do {
				bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
				if(bytesRead >= 0) current += bytesRead;
			} while(bytesRead > 0);
	
			bos.write(mybytearray, 0 , current);
			bos.flush();
			bos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void toServer(Socket clntSock, DataObject toSend) throws IOException {
		try {
			OutputStream os = clntSock.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(toSend);

		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in Send EOFException: goodbye client");
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in Send IOException: goodbye client at");
			clntSock.close(); // this requires the throws IOException
		}
	}

	public static DataObject fromServer(Socket clntSock) throws IOException {
		
		// client object
		DataObject fromServer = null;

		try {
			InputStream is = clntSock.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			fromServer = (DataObject) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (EOFException e) { // needed to catch when client is done
			System.out.println("in receive EOF: goodbye  ");
			clntSock.close(); // Close the socket. We are done with this client!
		} catch (IOException e) {
			System.out.println("in receive IO: goodbye ");
			clntSock.close(); // this requires the throws IOException
		}
		return fromServer;
	}

}

// while (!data.playMove) {	// Wait until we get the green light to play a move (waiting on other player)
// 	System.out.print("Waiting for opponent");

// 	// Will get an IllegalThreadStateException if we try to run the same thread multiple times at once
// 	if (!loading.isAlive())
// 		loading.start();
	
// 	data = fromServer(socket);
// 	System.out.println();
// 	data.printMessage();
// }
// loading.interrupt();

// // A thread just for a little custom loading animation. I spent more time on this than I should have.
// Thread loading = new Thread() {
// 	public void run() {
// 		while (true) {
// 			String turn = "Waiting for opponent";

// 			int NUM_DOTS = 8;
// 			// Loading dots
// 			try {
// 				Thread.sleep(500);
// 				for (int i=0; i<NUM_DOTS; i++) {
// 					System.out.print(".");
// 					Thread.sleep(500);
// 				}
// 			} catch (InterruptedException e) {
// 				// We don't care if it's interrupted, we will just stop it.
// 				return;
// 			}
			
// 			// Delete and write blank over it
// 			String delete = "", clear = "";
// 			for (int c=0; c<turn.length()+NUM_DOTS; c++) {
// 				delete+="\b";
// 				clear+=" ";
// 			}
// 			System.out.print(delete);
// 			System.out.print(clear);
// 			System.out.print(delete);
// 			System.out.print(turn);
// 		}
// 	}
// }; 