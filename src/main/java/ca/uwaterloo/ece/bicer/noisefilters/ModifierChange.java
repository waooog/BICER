package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class ModifierChange implements Filter{
	final String name="Change the modifier";
	BIChange biChange;
	boolean isNoise=false;
	String[] wholeFixCode;
	
	public ModifierChange (BIChange biChange, String[] wholeFixCode){
		this.biChange=biChange;
		this.wholeFixCode=wholeFixCode;
		this.isNoise=filterOut();	
	}
	@Override
	public boolean filterOut() {
		// TODO Auto-generated method stub
		
		//do not need to consider this case
		if(!biChange.getIsAddedLine()) return false;
		String stmt = biChange.getLine();
		String initStmt=stmt;
		if(containModifier(stmt)){
			initStmt=Utils.removeLineComments(stmt).trim();	// initial statement for removing false positive
			stmt=stmt.replaceAll("(public|private|protected)\\s*", "");// remove modifiers
			stmt=Utils.removeLineComments(stmt).trim(); // remove comments and space
			// if stmt contains a modifier, test each statements in the wholeFixCode
			for(String fixStmt:wholeFixCode){
				String initFixStmt=fixStmt;
				initFixStmt=Utils.removeLineComments(initFixStmt).trim();
				fixStmt=fixStmt.replaceAll("(public|private|protected)\\s*", "");
				fixStmt=Utils.removeLineComments(fixStmt).trim();
				if(stmt.equals(fixStmt)&&!initStmt.equals(initFixStmt)) return true;
			}
		}else{
			stmt=Utils.removeLineComments(stmt).trim(); // remove comments and space
	        /*if stmt does not contain a modifer, 
		    we only test statements in the wholeFixCode that has a modifers  */
			for(String fixStmt:wholeFixCode){
				if(containModifier(fixStmt)){
					fixStmt=fixStmt.replaceAll("(public|private|protected)\\s*", "");
					fixStmt=Utils.removeLineComments(fixStmt).trim();
					if(stmt.equals(fixStmt)) return true;					
				}
			}
		}
		
		return false;
	}
	
	// check whether the stament contains a modifier word, i.e., public, private, protected
	public boolean containModifier(String stmt){
		if(stmt.matches("^.*(public|private|protected)\\s.*"))
			return true;
		return false;
	}
	@Override
	public boolean isNoise() {
		// TODO Auto-generated method stub
		return isNoise;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

}
