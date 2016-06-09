package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;

public class NameChange implements Filter {
	
	final String name="Change a name";
	BIChange biChange;
	JavaASTParser biWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	
	public NameChange(BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.biWholeCodeAST = biWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		
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

		if(isMethodNameChanged(startPositionOfBILine))
			return true;
		
		return false;
	}

	private boolean isMethodNameChanged(int startPositionOfBILine) {
		ArrayList<MethodDeclaration> lstMethodDeclaration = biWholeCodeAST.getMethodDeclarations();
		
		// (1) get method that contains a BI line.
		MethodDeclaration methodHavingBILine = getMethodHavingBILine(lstMethodDeclaration,startPositionOfBILine);
		if (methodHavingBILine==null) // not exist? then skip
			return false;
		
		// (2) find a same method except for name from a fixed source code file
		lstMethodDeclaration =  fixedWholeCodeAST.getMethodDeclarations();
		
		for(MethodDeclaration methodDecl:lstMethodDeclaration){
			
			if(!methodDecl.getName().equals(methodHavingBILine.getName())
					&& methodDecl.parameters().toString().equals(methodHavingBILine.parameters().toString())
					&& methodDecl.getBody().toString().equals(methodHavingBILine.getBody().toString())){
				return true;
			}
		}
		
		return false;
	}

	private MethodDeclaration getMethodHavingBILine(ArrayList<MethodDeclaration> lstMethodDeclaration, int startPositionOfBILine) {
		
		for(MethodDeclaration methodDecl:lstMethodDeclaration){
			int startPosition = methodDecl.getStartPosition();
			int length = methodDecl.getLength();
			
			if(startPosition <= startPositionOfBILine && startPositionOfBILine <startPosition+length)
				return methodDecl;
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
