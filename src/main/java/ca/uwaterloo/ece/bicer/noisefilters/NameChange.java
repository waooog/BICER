package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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
		if(isMethodNameChanged())
			return true;
		
		// (2) check if variable (including member) name changed
		if(isVariableNameChanged())
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

	private boolean isVariableNameChanged() {
		
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
		
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		
		for(ASTNode varDecFragNode:lstBIVariableDeclarationFragment){
			if(biLineNum==biWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				ASTNode parent = varDecFragNode.getParent();
				
				if(parent instanceof VariableDeclarationStatement){
					biLineNodes.add(((VariableDeclarationStatement)parent).getModifiers());
					biLineNodes.add(((VariableDeclarationStatement)parent).getType());
				}
				if(parent instanceof FieldDeclaration){
					biLineNodes.add(((FieldDeclaration)parent).getModifiers());
					biLineNodes.add(((FieldDeclaration)parent).getType());
				}

				Expression exp = ((VariableDeclarationFragment)varDecFragNode).getInitializer();
				if(exp!=null)
					biLineNodes.add(exp);
				originalName = ((VariableDeclarationFragment)varDecFragNode).getName().toString();
			}
		}
		
		for(ASTNode varDecFragNode:lstFixVariableDeclarationFragment){
			if(fixLineNum==fixedWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				ASTNode parent = varDecFragNode.getParent();
				if(parent instanceof VariableDeclarationStatement){
					fixLineNodes.add(((VariableDeclarationStatement)parent).getModifiers());
					fixLineNodes.add(((VariableDeclarationStatement)parent).getType());
				}
				if(parent instanceof FieldDeclaration){
					fixLineNodes.add(((FieldDeclaration)parent).getModifiers());
					fixLineNodes.add(((FieldDeclaration)parent).getType());
				}

				Expression exp = ((VariableDeclarationFragment)varDecFragNode).getInitializer();
				if(exp!=null)
					fixLineNodes.add(exp);
				changedName = ((VariableDeclarationFragment)varDecFragNode).getName().toString();
			}
		}
		
		if(biLineNodes.size()==fixLineNodes.size()){
			for(int i=0;i<biLineNodes.size();i++){
				if(!biLineNodes.get(i).toString().equals(fixLineNodes.get(i).toString()))
					return false;
			}
			
			if(!originalName.equals("") && !originalName.equals(changedName))
				return true;
			
		}else{
			return false;
		}
		
		return false;
	}

	private boolean isMethodNameChanged() {
		
		// check if the edit is an one-line replace
		Edit edit = biChange.getEdit();

		if(edit==null) // sometimes edit can be null (e.g., position change)
			return false;

		if(edit.getType()!=Edit.Type.REPLACE || (edit.getEndA()-edit.getBeginA())!=1 || (edit.getEndB()-edit.getBeginB())!=1){
			return false;
		}
		
		int biLineNum = biChange.getLineNum();
		int fixLineNum = biChange.getEdit().getBeginB()+1;
		
		ArrayList<MethodDeclaration> lstBIMethodDeclaration = biWholeCodeAST.getMethodDeclarations();
		ArrayList<MethodDeclaration> lstFixMethodDeclaration = fixedWholeCodeAST.getMethodDeclarations();
		
		String originalName = "";
		String changedName = "";
		
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		
		for(MethodDeclaration methodDecNode:lstBIMethodDeclaration){
			if(biLineNum==biWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition())){
				originalName = methodDecNode.getName().toString();
				biLineNodes.add(methodDecNode.getModifiers());
				biLineNodes.add(methodDecNode.getReturnType2());
				biLineNodes.add(methodDecNode.parameters());
			}
		}
		
		for(MethodDeclaration methodDecNode:lstFixMethodDeclaration){
			if(fixLineNum==fixedWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition())){
				changedName = methodDecNode.getName().toString();
				fixLineNodes.add(methodDecNode.getModifiers());
				fixLineNodes.add(methodDecNode.getReturnType2());
				fixLineNodes.add(methodDecNode.parameters());
			}
		}
		
		if(biLineNodes.size()==fixLineNodes.size()){
			for(int i=0;i<biLineNodes.size();i++){
				if(!biLineNodes.get(i).toString().equals(fixLineNodes.get(i).toString()))
					return false;
			}
			
			if(!originalName.equals("") && !originalName.equals(changedName))
				return true;

		}else{
			return false;
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
