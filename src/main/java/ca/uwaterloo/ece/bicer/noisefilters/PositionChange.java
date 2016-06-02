package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

/**
 * Implement a filter for Position change of a declaration statements
 * 
 * @author JC
 *
 */
public class PositionChange implements Filter {

	BIChange biChange;
	String[] wholeFixCode;
	
	public PositionChange(BIChange biChange, String[] wholeFixCode) {
		this.biChange = biChange;
		this.wholeFixCode = wholeFixCode;
		
		filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		String stmt = biChange.getLine();
		
		// (1) Check the line is a declarative statement such as
		// import, ClassName variable = ..., ClassName variable;, a whole method where the line lives.
		if(areDeclarativeStmts(stmt)){
			
			// (2) check if there is the same line in a different position.
			if(Utils.doesSameLineExist(stmt, wholeFixCode, true, true))
				return true;
		}
		
		return false;
	}

	private boolean areDeclarativeStmts(String stmt) {
		
		// java import?
		if (stmt.matches("^\\s*import\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		// c include?
		if (stmt.matches("^\\s*#include\\s\\s*[\"<]\\s*[\\w\\-. ]+\\s*[\">].*"))
			return true;
		
		return false;		
	}
}
