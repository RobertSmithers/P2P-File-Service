/**
 * @author RJ Smithers
 *
 * Registers clients by IP:PORT#:Filename and provides this information to a client.
 * If a client requests a file that does not exist,
 * broker informs requesting peer that file does not exist.
 *
 */
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
	static final int DEFAULT_PORT = 5000;
	public static void main(String[] args) throws Exception {
        // Load clients from saved file
        // TODO if time
		int servPort = DEFAULT_PORT;
		if (args.length > 0)
            servPort = Integer.parseInt(args[0]);
		
        ServerSocket listener = null;
        //check to see if listener can be made
        try {
			listener = new ServerSocket(servPort);
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+servPort);
			System.exit(-1);
		}

		List<ClientObject> list = new CopyOnWriteArrayList<>();
        System.out.println("Server online...");
        try {
            // Wait for clients
            while (true) {
                Socket clntSocket = listener.accept();
                ClientObject client = new ClientObject(clntSocket);
                handleClientThread register = new handleClientThread(client, list);
                Thread t = new Thread(register); 
                t.start();
            }
        } finally {
            listener.close();
        }
        
	}
}
