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
		BIChange bic=new BIChange("7c1178615911675ce00d2b91e237cdd69555cb8d	"
				+ "src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	9f2ceaea2b0e36a485ca7a3f6ff75191df9134c6"
				+ "	2016-06-06 10:18:49-04"
				+ "	2016-06-07 10:18:49-04	37	t"
				+ "	public String test(int x){");
		String[] strs=new String[]{" String test(int x){ "," int test(int x){","int x=y;"};
		ModifierChange mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);	
		
		strs=new String[]{" private String test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{" protected String test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{"  String test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{" public String test(int y){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),false);
		
		strs=new String[]{" private String test(int x){ // change a modifer"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{" public static String test(int x){ // change a modifer"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),false);	
		
		//test add a modifier
		bic=new BIChange("7c1178615911675ce00d2b91e237cdd69555cb8d	"
				+ "src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	9f2ceaea2b0e36a485ca7a3f6ff75191df9134c6"
				+ "	2016-06-06 10:18:49-04"
				+ "	2016-06-07 10:18:49-04	37	t"
				+ "	 String test(int x){");
		strs=new String[]{" private String test(int x){ "," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{" protected String test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{"  int test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),false);
		
		strs=new String[]{"  String test(int x){"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),false);
		
		//test changing modifer of a class component
		bic=new BIChange("7c1178615911675ce00d2b91e237cdd69555cb8d	"
				+ "src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	src/main/java/ca/uwaterloo/ece/bicer/noisefilters/CosmeticChange.java"
				+ "	9f2ceaea2b0e36a485ca7a3f6ff75191df9134c6"
				+ "	2016-06-06 10:18:49-04"
				+ "	2016-06-07 10:18:49-04	37	t"
				+ "	 public BIChange bic;");
		
		strs=new String[]{" private BIChange bic;"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{"  BIChange bic;"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),true);
		
		strs=new String[]{" public BIChange boc;"," int test(int x){","int x=y;"};
		mc=new ModifierChange(bic,strs);
		assertEquals(mc.isNoise(),false);
		
    	NoiseFilterRunner runner = new NoiseFilterRunner();
    	
    	//String [] args ={"-d","data/exampleBIChanges.txt", "-g", System.getProperty("user.home") + "/git/BICER"};
    	String [] args ={"-d","data/exampleJackRabbitBIChanges", "-g", "/Users/jiangchenyang/Documents/uwaterloo/noisybi/git/jackrabbit/git"};
    	runner.run(args);
	}

}
