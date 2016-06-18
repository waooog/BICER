package ca.uwaterloo.ece.bicer;
import static org.junit.Assert.*;
import org.junit.Test;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.noisefilters.ModifierChange;
import ca.uwaterloo.ece.bicer.utils.Utils;


public class ModifierChangeTest {
	@Test
	public void test() {
		// test modifier change
		
    	NoiseFilterRunner runner = new NoiseFilterRunner();
    	
    	//String [] args ={"-d","data/exampleBIChanges.txt", "-g","/Users/jiangchenyang/Documents/uwaterloo/BICER"};
    	String [] args ={"-d","/Users/jiangchenyang/Documents/uwaterloo/noisybi/sanBiData/jackrabbit/biChangesSanitized.txt", "-g", "/Users/jiangchenyang/Documents/uwaterloo/noisybi/git/jackrabbit/git"};
    	runner.run(args);
	}

}
