package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;

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
		//do not need to consider this case
		if(!biChange.getIsAddedLine()) return false;
		String stmt = biChange.getLine();
		String initStmt=Utils.removeLineComments(stmt).trim();	// initial statement for removing false positive
		
		if(stmt.matches("^.*public\\s.*")){ //public -> protected,no midifier, private
			stmt=stmt.replaceAll("public\\s*", "");	// remove modifiers
			stmt=Utils.removeLineComments(stmt).trim(); // remove comments and space
			for( Edit edit:biChange.getEditListFromDiff()){
				for(int i=edit.getBeginB();i<edit.getEndB();i++){
					String fixStmt=wholeFixCode[i];
					String initFixStmt=fixStmt;
					initFixStmt=Utils.removeLineComments(initFixStmt).trim();
					fixStmt=fixStmt.replaceAll("(private|protected)\\s*", ""); // public removed, private|protected to public can be a real fix
					fixStmt=Utils.removeLineComments(fixStmt).trim();
					if(stmt.equals(fixStmt)&&!initStmt.equals(initFixStmt)) return true;					
				}								
			}
		}else if(stmt.matches("^.*protected\\s.*")){	//protected -> no midifier, private
			stmt=stmt.replaceAll("protected\\s*", "");// remove modifiers
			stmt=Utils.removeLineComments(stmt).trim(); // remove comments and space
			for( Edit edit:biChange.getEditListFromDiff()){
				for(int i=edit.getBeginB();i<edit.getEndB();i++){
					String fixStmt=wholeFixCode[i];
					String initFixStmt=fixStmt;
					initFixStmt=Utils.removeLineComments(initFixStmt).trim();
					fixStmt=fixStmt.replaceAll("private\\s*", ""); // public removed, private|protected to public can be a real fix
					fixStmt=Utils.removeLineComments(fixStmt).trim();
					if(stmt.equals(fixStmt)&&!initStmt.equals(initFixStmt)) return true;					
				}								
			}
		}else if(stmt.matches("^.*private\\s.*")){
			return false;
		}else{ //no modifier ->  private
			for( Edit edit:biChange.getEditListFromDiff()){
					for(int i=edit.getBeginB();i<edit.getEndB();i++){
						String fixStmt=wholeFixCode[i];
						if(fixStmt.matches("^.*private\\s.*")){
							fixStmt=fixStmt.replaceAll("private\\s*", ""); // public removed, private|protected to public can be a real fix
							fixStmt=Utils.removeLineComments(fixStmt).trim();
							if(initStmt.equals(fixStmt)) return true;	
						}	
					}
			}				
		}
		return false;
	}

//	// check whether the stament contains a modifier word, i.e., public, private, protected
//	public boolean containModifier(String stmt){
//		if(stmt.matches("^.*(public|private|protected)\\s.*"))
//			return true;
//		return false;
//	}
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
