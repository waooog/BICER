package ca.uwaterloo.ece.bicer;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uwaterloo.ece.bicer.utils.Utils;

public class UtilsTest {

	@Test
	public void test() {
		String line = "public /*  dfgh */ static String  removeLineComments(String line) { /*  dfadsf \n asdfasdf */";
		Utils.removeLineComments(line);
		System.out.println(line);
		line = Utils.removeLineComments(line);
		System.out.println(line);
		assertEquals("public  static String  removeLineComments(String line) { ",line);	
	}

}
