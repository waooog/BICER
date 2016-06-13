package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jgit.diff.Edit;

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

		// (1) check if method name changed
		if(isMethodNameChanged(startPositionOfBILine))
			return true;
		
		// (2) check if member name changed
		if(isMemberNameChanged(startPositionOfBILine))
			return true;
		
		return false;
	}

	private boolean isMemberNameChanged(int startPositionOfBILine) {
		
		// TODO
		// BI line can be either declaration or its use. Most cases are one-line replace. So only consider the one-line replace.
		
		// check if the edit is an one-line replace
		Edit edit = biChange.getEdit();
		
		if(edit==null) // sometimes edit can be null (e.g., position change)
			return false;
		
		if(edit.getType()!=Edit.Type.REPLACE || (edit.getEndA()-edit.getBeginA())!=1 || (edit.getEndB()-edit.getBeginB())!=1){
			return false;
		}
		
		// find AST node for a bi line and its fix line
		int startPosition = biWholeCodeAST.getCompilationUnit().getPosition(2, 0); // line num starts from 1
		ASTNode node = NodeFinder.perform(biWholeCodeAST.getCompilationUnit(), startPosition,1);
		int a = node.getNodeType();
		
		ArrayList<FieldDeclaration> lstBIFieldDeclaration = biWholeCodeAST.getFieldDeclarations();
		ArrayList<FieldDeclaration> lstFixedFieldDeclaration = fixedWholeCodeAST.getFieldDeclarations();
		
		ArrayList<FieldAccess> lstBIFieldAccess = biWholeCodeAST.getFieldAccesses();
		ArrayList<FieldAccess> lstFixedFieldAccess = fixedWholeCodeAST.getFieldAccesses();
		
		
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
			
			// ignore abstract method
			if(methodDecl.getBody()==null)
				return false;
			
			try{
			if(!methodDecl.getName().equals(methodHavingBILine.getName())
					&& methodDecl.parameters().toString().equals(methodHavingBILine.parameters().toString())
					&& methodDecl.getBody().toString().equals(methodHavingBILine.getBody().toString())){
				return true;
			}
			}catch (NullPointerException e){
				System.out.println(methodDecl.toString());
				System.exit(0);
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
