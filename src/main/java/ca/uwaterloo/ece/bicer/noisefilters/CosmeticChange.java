package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class CosmeticChange implements Filter {
	
	final String name="Cosmetic change";
	BIChange biChange;
	String[] wholePreFixCode;
	String[] wholeFixCode;
	boolean isNoise=false;
	
	public CosmeticChange(BIChange biChange, JavaASTParser preFixWholeCodeAST, JavaASTParser fixWholeCodeAST) {
		this.biChange = biChange;
		wholePreFixCode = preFixWholeCodeAST.getStringCode().split("\n");
		wholeFixCode = fixWholeCodeAST.getStringCode().split("\n");
		
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
		
		// TODO consider all possible cosmetic change cases
		
		// check edit is null, if null skip
		if(biChange.getEdit()==null){
			System.err.println("WARNING: Diff results are different between jGit and git diff");
			System.err.println(biChange.toString());
			return false;
		}
		
		// only consider REPLACE case
		if(biChange.getEdit().getType()!=Edit.Type.REPLACE)
			return false;
		
		// get all prefix lines in edit
		int startLineInPreFixCode  = biChange.getEdit().getBeginA();
		int endLineInPreFixCode  = biChange.getEdit().getEndA();

		// get changed code range
		int startLineInFixCode  = biChange.getEdit().getBeginB();
		int endLineInFixCode  = biChange.getEdit().getEndB();
		
		String stmtWithoutWhiteSpaces = stmt.replaceAll("\\s", "");
		
		// (1) check all deleted lines and added lines are same
		// get deleted lines without spaces
		String deletedLinesWithotSpaces = "";
		for(int i=startLineInPreFixCode;i<endLineInPreFixCode;i++){
			deletedLinesWithotSpaces += wholePreFixCode[i];
		}
		// get addedLines
		String addedLinesWithoutSpaces = "";
		for(int i=startLineInFixCode;i<endLineInFixCode;i++){
			addedLinesWithoutSpaces += wholeFixCode[i];
		}
		if(addedLinesWithoutSpaces.replaceAll("\\s", "").indexOf(deletedLinesWithotSpaces.replaceAll("\\s", ""))>=0)
			return true;
		
		if (stmtWithoutWhiteSpaces.length()<4)
			System.err.println("WARNING(" + name + "): a too short line: " + stmt);
		
		String affectedFixCode = "";
		
		/*
		 *  This is a unique case to deal with
		 *  -            qManager = new QueryManagerImpl(session,
		 *  -                    session.getNamePathResolver(), session.getItemManager(),
		 *  -                    session.getHierarchyManager(), wspManager);
		 *  +            qManager = new QueryManagerImpl(session, session,
		 *  +                    session.getItemManager(), wspManager);
		 */
		 if(biChange.getEdit().getBeginA()+1 == biChange.getLineNumInPrevFixRev()){
			 boolean existInFirstFixStmt = false;
			 boolean existInBiLine = false;
			 int beginA = biChange.getEdit().getBeginA();
			 int endA = biChange.getEdit().getEndA();
			 int beginB = biChange.getEdit().getBeginB();
			 int endB = biChange.getEdit().getEndB();
			 if(endA-beginA>=2 &&endB-beginB>=2){
				 String biStmtWOSpace = biChange.getLine().replaceAll("\\s", "");
				 String fixstmtsWOSpace = (fixCode[beginB] + fixCode[beginB+1]).replaceAll("\\s", "");
				 if(fixstmtsWOSpace.indexOf(biStmtWOSpace)>=0)
					 existInFirstFixStmt = true;
				 
				 String fixstmtWOSpace = (fixCode[beginB]).replaceAll("\\s", "");
				 String biStmtsWOSpace = (biChange.getLine() + wholePreFixCode[biChange.getLineNumInPrevFixRev()]).replaceAll("\\s", ""); // biChange.getLineNumInPrevFixRev() is the same as the index of the next line of bi line.
				 if(biStmtsWOSpace.indexOf(fixstmtWOSpace)>=0)
					 existInBiLine = true;
				 
				 if(existInFirstFixStmt && existInBiLine)
					 return true;
				 
				 if(existInFirstFixStmt && !existInBiLine)
					 return false;
			 }
		 }

		
		for(int i=startLineInFixCode; i<endLineInFixCode;i++){
			affectedFixCode += Utils.removeLineComments(fixCode[i]).replaceAll("\\s", "");
			// if a change happens in the front, do not filter. That can be adding a modifier or similar changes.
			// B ==> A B
			int indexOf = fixCode[i].trim().indexOf(biChange.getLine().trim());
			if(indexOf>0)
				return false;
			
			// the following also can happen. this should not be noise.
			// B C ==> A B\nC
			// (1) Merge the first and second fix line
			String mergeFirstAndSecondFixLine="";
			String biLineWithoutSpace = biChange.getLine().replaceAll("\\s", "");
			if(i+1<endLineInFixCode)
				mergeFirstAndSecondFixLine = Utils.removeLineComments((fixCode[i] + fixCode[i+1]).replaceAll("\\s", ""));
			indexOf = mergeFirstAndSecondFixLine.indexOf(biLineWithoutSpace);
			if(indexOf>0)
				return false;
		}
		
		// B C ==> B\nC or B\nC==> B C
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
