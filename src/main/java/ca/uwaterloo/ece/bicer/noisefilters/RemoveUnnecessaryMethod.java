package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;

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
		
		String biSource = biWholeCodeAST.getStringCode();
		int startPositionOfBILine = biSource.indexOf(biChange.getLine());
		int startPositionOfBILineFromEnd = biSource.lastIndexOf(biChange.getLine());
		
		if(startPositionOfBILine!=startPositionOfBILineFromEnd){
			System.err.println("Redundant BI line exists in source code: " + biChange.getLine());
			System.exit(0);
		}

		ArrayList<MethodDeclaration> lstMethodDeclaration = biWholeCodeAST.getMethodDeclarations();
		
		// (1) get method that contains a BI line.
		String methodHavingBILine = getMethodHavingBILine(lstMethodDeclaration,startPositionOfBILine);
		if (methodHavingBILine==null)
			return false;
		
		// (2) check if the method and BI line does not exists in fixed source code. No existence, method removed.
		String fixedSource = fixWholeCodeAST.getStringCode();
		lstMethodDeclaration = fixWholeCodeAST.getMethodDeclarations();
		return notExistMethodAndBILine(lstMethodDeclaration,fixedSource,methodHavingBILine,biChange.getLine());
	}

	private boolean notExistMethodAndBILine(ArrayList<MethodDeclaration> lstMethodDeclaration, String fixedSource, String methodHavingBILine, String line) {
		
		// if method and BI lines do not exist, it is noise.
		boolean doesMethodExist = false;
		for(MethodDeclaration methodDecl:lstMethodDeclaration){
			String method = methodDecl.getName() + methodDecl.parameters().toString();
			if(method.equals(methodHavingBILine)){
				doesMethodExist = true;
				continue;
			}
		}

		if(!doesMethodExist && fixedSource.indexOf(line)<0)
			return true;
		
		return false;
	}

	private String getMethodHavingBILine(ArrayList<MethodDeclaration> lstMethodDeclaration, int startPositionOfBILine) {
		
		for(MethodDeclaration methodDecl:lstMethodDeclaration){
			String method = methodDecl.getName() + methodDecl.parameters().toString();
			int startPosition = methodDecl.getStartPosition();
			int length = methodDecl.getLength();
			
			if(startPosition <= startPositionOfBILine && startPositionOfBILine <startPosition+length)
				return method;
			
		}
		
		return null;
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
