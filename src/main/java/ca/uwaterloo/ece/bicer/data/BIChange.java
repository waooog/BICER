package ca.uwaterloo.ece.bicer.data;

public class BIChange {
	String BISha1;
	String path;
	String FixSha1;
	int lineNum;
	String line;
	
	BIChange(String changeInfo){
		String[] splitString = changeInfo.split("\t");
		
		BISha1 = splitString[0];
		path = splitString[1];
		FixSha1 = splitString[2];
		lineNum = Integer.parseInt(splitString[3]);
		line = splitString[4];
	}
	
	public String getBISha1() {
		return BISha1;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getFixSha1() {
		return FixSha1;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public String getLine() {
		return line;
	}
}
