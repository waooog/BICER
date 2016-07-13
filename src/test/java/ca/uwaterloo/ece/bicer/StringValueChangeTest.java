package ca.uwaterloo.ece.bicer;

import org.junit.Test;

public class StringValueChangeTest {
	@Test
	public void test() {
		// test modifier change
		
    	NoiseFilterRunner runner = new NoiseFilterRunner();
    	
    	//String [] args ={"-d","data/exampleBIChanges.txt", "-g","/Users/jiangchenyang/Documents/uwaterloo/BICER"};
    	String [] args ={"-d","C:\\Users\\song\\Desktop\\Waterloo\\PCC-DL\\lucene\\biChangesSanitized_StringValue_Change.txt", "-g", "C:\\Users\\song\\Desktop\\Waterloo\\PCC-DL\\lucene\\git"};
    	runner.run(args);
	}
}
