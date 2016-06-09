package ca.uwaterloo.ece.bicer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
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
		
		Git git;
		try {
			git = Git.open( new File( System.getProperty("user.home") + "/git/BICER") );
			Utils.getEditListFromDiff(git, "6a25685e6a4dcf379718057b2465d998242f6ff6~1", "6a25685e6a4dcf379718057b2465d998242f6ff6", "src/main/java/ca/uwaterloo/ece/bicer/noisefilters/NameChange.java");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
