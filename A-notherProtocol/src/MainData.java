import java.io.*;

/*
 * The object to compress and create the data portion of the datagram.
 * 
 * 
 * 
 * 
 */
public class MainData {
	private File file;
	public MainData(){
		
	}
	
	public MainData(File file){
		
		this.setFile(file);
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public byte prepareForTransmit(File file){
		
		return (byte)0;
	}
	
	public File openFile(byte data){
		
		return getFile();
	}

}
