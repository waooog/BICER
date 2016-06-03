package ca.uwaterloo.ece.bicer.data;

public class BIChange {
	String BISha1;
	String path;
	String FixSha1;
	String BIDate;
	String FixDate;
	int lineNum;
	boolean isAddedLine;
	String line;
	boolean isNoise;
	
	public BIChange(String changeInfo){
		String[] splitString = changeInfo.split("\t");
		
		BISha1 = splitString[0];
		path = splitString[1];
		FixSha1 = splitString[2];
		BIDate = splitString[3];
		FixDate = splitString[4];
		lineNum = Integer.parseInt(splitString[6]);
		isAddedLine = splitString[6].equals("t")?true:false;
		line = splitString[7];
	}
	
	public void setIsNoise(boolean isNoise){
		this.isNoise = isNoise;
	}
	
	public boolean isNoise() {
		return isNoise;
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
	
	public String getBIDate() {
		return BIDate;
	}
	
	public String getFixDate() {
		return FixDate;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public String getLine() {
		return line;
	}
	
	public boolean getIsAddedLine() {
		return isAddedLine;
	}
}
