package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class RemoveUnnecessaryMethod implements Filter {
	
	final String name="Remove Unnecessary Method";
	BIChange biChange;
	JavaASTParser biWholeCodeAST;
	JavaASTParser fixWholeCodeAST;
	boolean isNoise=false;
	
	public RemoveUnnecessaryMethod(BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixWholeCodeAST = fixWholeCodeAST;
		
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		// Exceptional case for @Override since git diff and jgit diff are different
		if(biChange.getLine().trim().equals("@Override") && (biChange.getEdit().getEndA()-biChange.getEdit().getBeginA())!=1){
			biChange.setFilteredDueTo("Ignore @Override|" + biChange.getFilteredDueTo());
			return true;
		}
		
		ArrayList<MethodDeclaration> lstMethodDeclaration = biWholeCodeAST.getMethodDeclarations();
		
		// (1) get method that contains a BI line that is not a line for method declaration.
		MethodDeclaration biMethodDecNodeHavingBILine=null;
		int potentialBeginALineNum = -1; // deleted line range start line num
		int potentialEndALineNum = -1; // deleted line range end line num
		
		for(MethodDeclaration methodDecNode:lstMethodDeclaration){
			int beginLineNum = biWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition());
			int endLineNum = biWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength());
			if(biChange.getLineNumInPrevFixRev()>=beginLineNum && biChange.getLineNumInPrevFixRev()<= endLineNum){
				biMethodDecNodeHavingBILine = methodDecNode;
				potentialBeginALineNum = beginLineNum;
				potentialEndALineNum = endLineNum;
				break;
			}
		}
		

		if(biMethodDecNodeHavingBILine==null) // biLine is not a line in a method.
			return false;
		
		if(biChange.getEdit()==null)
			return false;
		
		// (2) check all lines for the method are removed.
		if(!((potentialBeginALineNum >= biChange.getEdit().getBeginA()) && (potentialEndALineNum-1 <= biChange.getEdit().getEndA()))){ // the closing } can be ignored
			return false;
		}
		
		// (3) check if the method and BI line does not exists in fixed source code. No existence, method removed.
		lstMethodDeclaration = fixWholeCodeAST.getMethodDeclarations();
		return notExistMethodAndBILine(lstMethodDeclaration,biMethodDecNodeHavingBILine);
	}

	@SuppressWarnings("unchecked")
	private boolean notExistMethodAndBILine(ArrayList<MethodDeclaration> lstMethodDeclaration, MethodDeclaration methodHavingBILine) {
		
		for(MethodDeclaration methodDecl:lstMethodDeclaration){
			
			//System.out.println(methodDecl.getName().toString());
			//System.out.println(methodDecl.parameters().toString());
	
			
			// (1) check if a method with the same name and parameters exists. if it exists, not a noise.
			if(methodDecl.getName().toString().equals(methodHavingBILine.getName().toString())
					&& Utils.compareMethodParametersFromAST(methodHavingBILine.parameters(),methodDecl.parameters()))
				return false;
			
			// (2) check if an exactly same method exists >> it implies Method position changed
			if(methodDecl.toString().equals(methodHavingBILine.toString()))
				return false;
			
			// (3) check if an exactly same body exists >> it implies a method name change.
			if(methodHavingBILine.getBody()==null){ // to avoid NPE
				if(methodHavingBILine.getBody()==null) // if empty bodies in both, consider the same body even though it is null
					return false;
			}else{
				if(methodDecl.getBody()!=null){
					if(methodDecl.getBody().toString().equals(methodHavingBILine.getBody().toString())
							&& !methodDecl.getBody().toString().replaceAll("\\s","").equals("{}"))
						return false;
				}
			}
		}
		
		return true;
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
