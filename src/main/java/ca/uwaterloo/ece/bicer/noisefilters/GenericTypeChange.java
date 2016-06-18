package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class GenericTypeChange implements Filter {
	final String name="Change the generic type";
	BIChange biChange;
	boolean isNoise=false;
	String[] wholeFixCode;
	
	public GenericTypeChange (BIChange biChange, String[] wholeFixCode){
		this.biChange=biChange;
		this.wholeFixCode=wholeFixCode;
		this.isNoise=filterOut();
	}

	@Override
	public boolean filterOut() {
		if(!biChange.getIsAddedLine()) return false;
		String stmt= biChange.getLine();
		String initStmt=Utils.removeLineComments(stmt).trim();;
		Edit edit=biChange.getEdit();
		if(edit!=null){ 
			if(stmt.matches(".*<.*>.*")){
				stmt=stmt.replaceAll("<.*>", "");
				stmt=Utils.removeLineComments(stmt).trim();
				for(int i=edit.getBeginB();i<edit.getEndB();i++){
					String fixStmt=Utils.removeLineComments(wholeFixCode[i]).trim();
					if(!fixStmt.matches(".*<.*>.*")&&fixStmt.equals(stmt)) return true;
				}			
			}else{
				for(int i=edit.getBeginB();i<edit.getEndB();i++){
					String fixStmt=wholeFixCode[i];
					if(fixStmt.matches(".*<.*>.*")){
						fixStmt=fixStmt.replaceAll("<.*>", "");
						fixStmt=Utils.removeLineComments(fixStmt).trim();
						if(fixStmt.equals(initStmt)) return true;					
					}
				}				
			}
		}
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
