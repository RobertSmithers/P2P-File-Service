import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataObject implements Serializable{
	
	private static final long serialVersionUID = 1L;

	String message,name,fileName;
	String[] fileNames = {};
	String ip;
	int port, filesize;
	boolean exit = false, success = false;
	private Boolean isRegistering;
	
	public DataObject(){
		isRegistering = false;
	}

	
	public void setFileName(String f) {
		fileName = f;
	}

	public String getFileName() {
		return fileName;
	}

	public void setIPPort(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}

	public void setName(String n){
		this.name = n;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void setMessage(String message){
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}

	public Boolean isRegistering() {
		return isRegistering;
	}

	public Boolean isRequesting() {
		return !isRegistering;
	}

	public void setRegistering() {
        isRegistering = true;
    }

    public void setRequesting() {
        isRegistering = false;
    }
	
	public void printMessage(){
		System.out.println(this.message);
	}
}
