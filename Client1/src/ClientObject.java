import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientObject {
    private Socket socket;
    private int port;   // Peer port for other clients to download stuff
    private InetAddress ip;
    private List<String> files = new CopyOnWriteArrayList<>();
    private String name;

    public ClientObject(Socket socket, int port) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
        this.port = port;
    }

    public ClientObject(Socket socket) {
        this.socket = socket;
        this.ip = socket.getInetAddress();
    }

    public void addFile(String fName) {
        files.add(fName);
    }

    public String[] getFiles() {
        String[] fileArr = new String[files.size()];
        int i=0;
        for (String file : files)
            fileArr[i++] = file;
        return fileArr;
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setPort(int p) {
        this.port = p;
    }

    public int getPort() {
        return port;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public void kill() throws IOException {
        this.socket.close();
    }
}