package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class CosmeticChange implements Filter {
	
	final String name="Cosmetic change";
	BIChange biChange;
	String[] wholeFixCode;
	boolean isNoise=false;
	
	public CosmeticChange(BIChange biChange, String[] wholeFixCode) {
		this.biChange = biChange;
		this.wholeFixCode = wholeFixCode;
		
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		String stmt = Utils.removeLineComments(biChange.getLine());
		
		if(doesAFixCosmeticChange(stmt,wholeFixCode))
			return true;
		
		return false;
	}

	private boolean doesAFixCosmeticChange(String stmt, String[] fixCode) {
		
		String stmtWithoutSpaces = stmt.replaceAll("\\s", "");
		
		if (stmtWithoutSpaces.length()<4)
			System.err.println("WARNING(" + name + "): a too short line: " + stmt);
		
		String wholeCodeWithoutSpace = "";
		
		for(String s:fixCode){
			wholeCodeWithoutSpace += Utils.removeLineComments(s).replaceAll("\\s", "");
		}
		
		if(wholeCodeWithoutSpace.indexOf(stmtWithoutSpaces)!= -1)
			return true;
		
		return false;
	}

	@Override
	public boolean isNoise() {
		return isNoise;
	}

	@Override
	public String getName() {
		return name;
	}
}
