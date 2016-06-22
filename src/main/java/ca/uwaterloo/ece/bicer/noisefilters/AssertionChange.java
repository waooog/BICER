package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class AssertionChange implements Filter{
	final String name="Assertion change";
	BIChange biChange;
	boolean isNoise=false;
	String[] wholeFixCode;
	
	public AssertionChange (BIChange biChange, String[] wholeFixCode){
		this.biChange=biChange;
		this.wholeFixCode=wholeFixCode;
		this.isNoise=filterOut();
	}
	
	@Override
	public boolean filterOut() {
		if(biChange.getLine().matches("^\\s*assert\\s.*")) return true;
		Edit edit=biChange.getEdit();
		String stmt=Utils.removeLineComments(biChange.getLine()).trim();
		stmt=stmt.replaceAll(";$", "");
		if(edit!=null){
			for(int i=edit.getBeginB();i<edit.getEndB();i++){
				String fixStmt=wholeFixCode[i];
				if(fixStmt.matches("^\\s*assert\\s+.*")){
					//fixStmt=fixStmt.replaceAll("^\\s*assert\\s+\\(*\\s*", "");
					if(fixStmt.indexOf(stmt)!=-1) return true;
				}
			}
		}

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
