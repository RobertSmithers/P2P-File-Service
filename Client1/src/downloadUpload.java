import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class downloadUpload implements Runnable {

    static final int DEFAULT_PORT = 5001;

    List<String> fileList = new CopyOnWriteArrayList<>();
    Socket socket;

    public downloadUpload(List<String> fileList) {
        this.fileList = fileList;
    }

    @Override
	public void run() {
		int servPort;

		servPort = DEFAULT_PORT;	
		
        ServerSocket listener = null;
        //check to see if listener can be made
        try {
			listener = new ServerSocket(servPort);
		} catch (IOException e) {
			System.err.println("Could not listen on port: "+servPort);
			System.exit(-1);
		}

        // System.out.println("Peer-2-peer server online...");

        try {
            // Wait for other clients
            while (true) {
                Socket socket = listener.accept();
                DataObject data = fromClient(socket);
                System.out.printf("%s now is downloading %s from us\n", socket.getInetAddress(), data.fileName);
                uploadDownload(data.fileName, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                listener.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadDownload(String fileName, Socket socket) {
        try {
            File file = new File(fileName);  //open file to for reading

            DataObject data = new DataObject();
            data.filesize = (int) file.length();
            data.fileName = fileName;
            toClient(socket, data);

            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte [] mybytearray  = new byte [(int) file.length()];
            bis.read(mybytearray, 0, mybytearray.length);
            OutputStream os = socket.getOutputStream();
            os.write(mybytearray, 0, mybytearray.length);
            os.flush();
            bis.close();
            os.flush();
            os.close();				//definitely need to close the write file
            fis.close();
        } catch(FileNotFoundException ex) {
            System.out.println("Exception occurred: the input file does not exist");
            DataObject data = new DataObject();
            data.filesize = -1;
            try {
                toClient(socket, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }catch(IOException ex) {
            System.out.println("Exception occurred:");
            ex.printStackTrace();
        }
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
