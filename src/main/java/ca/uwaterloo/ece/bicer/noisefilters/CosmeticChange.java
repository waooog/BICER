package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.Edit;

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
		
		// check edit is null, if null skip
		if(biChange.getEdit()==null){
			System.err.println("WARNING: Diff results are different between jGit and git diff");
			System.err.println(biChange.toString());
			return false;
		}
		
		// only consider REPLACE case
		if(biChange.getEdit().getType()!=Edit.Type.REPLACE)
			return false;

		// get changed code range
		int startLineInFixCode  = biChange.getEdit().getBeginB();
		int endLineInFixCode  = biChange.getEdit().getEndB();
		
		String stmtWithoutWhiteSpaces = stmt.replaceAll("\\s", "");
		
		if (stmtWithoutWhiteSpaces.length()<4)
			System.err.println("WARNING(" + name + "): a too short line: " + stmt);
		
		String affectedFixCode = "";
		
		for(int i=startLineInFixCode; i<=endLineInFixCode;i++){
			affectedFixCode += Utils.removeLineComments(fixCode[i]).replaceAll("\\s", "");
			// if a change happens in the front, do not filter. That can be adding a modifier or similar changes.
			int indexOf = fixCode[i].trim().indexOf(biChange.getLine().trim());
			if(indexOf>0)
				return false;
		}
		
		int indexOf;
		if((indexOf = affectedFixCode.indexOf(stmtWithoutWhiteSpaces))!= -1){
			if(indexOf != affectedFixCode.lastIndexOf(stmtWithoutWhiteSpaces)){
				System.err.println("WARNING(" + name + "): not a single occurence for: " + stmt);
				return false; // let this case just buggy
			}
			return true;
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
