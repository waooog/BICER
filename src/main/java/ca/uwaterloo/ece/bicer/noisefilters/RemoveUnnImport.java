package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class RemoveUnnImport implements Filter {
	
	final String name="Remove unnecessary import";
	BIChange biChange;
	String[] wholeFixCode;
	boolean isNoise=false;
	
	public RemoveUnnImport(BIChange biChange, String[] wholeFixCode) {
		this.biChange = biChange;
		this.wholeFixCode = wholeFixCode;
		
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		String stmt = biChange.getLine();
		
		// (1) Check the line is a import / include statement
		if(areImportStmts(stmt)){
			
			// (2) check if there is the same line in a different position.
			//     If the same line does not exist, it is a noise.
			if(!Utils.doesSameLineExist(stmt, wholeFixCode, true, true,biChange.getIsAddedLine()))
				return true;
		}
		
		return false;
	}

	private boolean areImportStmts(String stmt) {
		// java import?
		if (stmt.matches("^\\s*import\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
				
		// c include?
		if (stmt.matches("^\\s*#include\\s\\s*[\"<]\\s*[\\w\\-. ]+\\s*[\">].*"))
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
