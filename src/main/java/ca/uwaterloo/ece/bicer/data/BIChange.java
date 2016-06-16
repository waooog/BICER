package ca.uwaterloo.ece.bicer.data;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;

public class BIChange {
	String BISha1;
	String biPath;
	String path;
	String FixSha1;
	String BIDate;
	String FixDate;
	int lineNum;
	int lineNumInPrevFixRev;
	boolean isAddedLine;
	String line;
	boolean isNoise;
	Edit edit;
	EditList editList;
	
	String filteredDueTo;

	public BIChange(String changeInfo){
		String[] splitString = changeInfo.split("\t");
		
		BISha1 = splitString[0];
		biPath = splitString[1];
		path = splitString[2];
		FixSha1 = splitString[3];
		BIDate = splitString[4];
		FixDate = splitString[5];
		lineNum = Integer.parseInt(splitString[6]); // if applying Sanitizer, this will be line num in BI code.
		lineNumInPrevFixRev = Integer.parseInt(splitString[7]); // lineNum in the prv. of fix revision.
		isAddedLine = splitString[8].equals("t")||splitString[7].equals("true")?true:false;
		line = splitString[9];
		filteredDueTo = "";
	}
	
	public void setIsNoise(boolean isNoise){
		this.isNoise = isNoise;
	}
	
	public void setBIPath(String biPath){
		this.biPath = biPath;
	}
	
	public void setFilteredDueTo(String filterName) {
		filteredDueTo = filterName;
	}
	
	public boolean isNoise() {
		return isNoise;
	}
	
	public String getBISha1() {
		return BISha1;
	}
	
	public String getBIPath() {
		return biPath;
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
	
	public String getFilteredDueTo() {
		return filteredDueTo;
	}

	public void setLineNum(Integer lineNum) {
		this.lineNum = lineNum;
	}

	public void setEdit(Edit edit) {
		this.edit = edit;
	}

	public void setEditList(EditList editListFromDiff) {
		this.editList = editListFromDiff;
	}

	public Edit getEdit() {
		return edit;
	}

	public void setBISha1(String biSha1) {
		BISha1=biSha1;
	}
	
	public String toString(){
		return getBISha1() + "\t" +
				getBIPath() + "\t" +
				getPath() + "\t" + 
				getFixSha1() + "\t" +
				getIsAddedLine() + "\t" +
				getLineNum() + "\t" +
				getLine();
	}
	
	public String getRecord(){
		return getBISha1() + "\t" +
				getBIPath() + "\t" +
				getPath() + "\t" + 
				getFixSha1() + "\t" +
				getBIDate() + "\t" +
				getFixDate() + "\t" +
				getLineNum() + "\t" +
				getLineNumInPrevFixRev() + "\t" +
				getIsAddedLine() + "\t" +
				getLine();
	}

	public int getLineNumInPrevFixRev() {
		return lineNumInPrevFixRev;
	}

	public void setLineNumInPrevFixRev(int lineNum) {
		lineNumInPrevFixRev = lineNum;
	}
}
