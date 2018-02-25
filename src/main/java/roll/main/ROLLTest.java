package roll.main;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ROLLTest {
	public static void main(String[] args) throws IOException {
		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in = new PipedInputStream();
		ROLL roll = new ROLL(null, in, out); 
	
	}	
}
	
