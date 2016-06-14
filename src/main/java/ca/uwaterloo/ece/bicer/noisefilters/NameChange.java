package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
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
		int startPositionOfBILine = getStartPosition(biSource,biChange.getLineNum());
		
		if(startPositionOfBILine <0){
			System.err.println("Warning: line does not exist in BI source code " + ": " + biChange.getLine());
			//System.err.println(biSource);
			//System.exit(0);
		}

		// (1) check if method name changed
		if(isMethodNameChanged(startPositionOfBILine))
			return true;
		
		// (2) check if variable (including member) name changed
		if(isVariableNameChanged(startPositionOfBILine))
			return true;
		
		return false;
	}

	private int getStartPosition(String biSource, int lineNum) {
		
		int currentPosition = 0;
		String[] lines = biSource.split("\n");
		
		for(int i=0; i < lines.length; i++){
			if(i==lineNum-1)
				return currentPosition;
			
			currentPosition+=lines[i].length() + 1; // + 1 is for \n
		}
		
		return -1;
	}

	private boolean isVariableNameChanged(int startPositionOfBILine) {
		
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
		/*int startPosition = biWholeCodeAST.getCompilationUnit().getPosition(biChange.getLineNum(), 0); // line num starts from 1
		int lineNumFromStartPosition = biWholeCodeAST.getCompilationUnit().getLineNumber(startPosition);
		ASTNode node = NodeFinder.perform(biWholeCodeAST.getCompilationUnit(), startPosition,2);
		int a = node.getNodeType();*/
		
		int biLineNum = biChange.getLineNum();
		int fixLineNum = biChange.getEdit().getBeginB()+1;
		
		ArrayList<VariableDeclarationFragment> lstBIVariableDeclarationFragment = biWholeCodeAST.getVariableDeclarationFragments();
		ArrayList<VariableDeclarationFragment> lstFixVariableDeclarationFragment = fixedWholeCodeAST.getVariableDeclarationFragments();
		
		String originalName = "";
		String changedName = "";
		
		for(ASTNode varDecFragNode:lstBIVariableDeclarationFragment){
			if(biLineNum==biWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				originalName = varDecFragNode.toString();
			}
		}
		
		for(ASTNode varDecFragNode:lstFixVariableDeclarationFragment){
			if(fixLineNum==fixedWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				changedName = varDecFragNode.toString();
			}
		}
		
		if(!originalName.equals("") && !originalName.equals(changedName))
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
			
			// ignore empty body methods
			if(methodDecl.getBody()==null || methodHavingBILine.getBody()==null)
				return false;
			
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
