package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.diff.Edit;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class NameChange implements Filter {
	
	final String name="Change a name";
	BIChange biChange;
	JavaASTParser preFixWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	
	public NameChange(BIChange biChange, JavaASTParser preFixWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		this.biChange = biChange;
		this.preFixWholeCodeAST = preFixWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		String biSource = preFixWholeCodeAST.getStringCode();
		int startPositionOfBILine = Utils.getStartPosition(biSource,biChange.getLineNumInPrevFixRev());
		
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
		
		int biLineNum = biChange.getLineNumInPrevFixRev();
		int fixLineNum = biChange.getEdit().getBeginB()+1;
		
		ArrayList<SingleVariableDeclaration> lstBISinggleVariableDeclaration = preFixWholeCodeAST.getSingleVariableDeclarations();
		ArrayList<SingleVariableDeclaration> lstFixSingleVariableDeclaration = fixedWholeCodeAST.getSingleVariableDeclarations();
		
		String originalName = "";
		String changedName = "";
		
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		
		for(SingleVariableDeclaration varDecFragNode:lstBISinggleVariableDeclaration){
			
			if(biLineNum==preFixWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				biLineNodes.add(varDecFragNode.getModifiers());
				biLineNodes.add(varDecFragNode.getType());	
	
				Expression exp = varDecFragNode.getInitializer();
				if(exp!=null)
					biLineNodes.add(exp);
				originalName = varDecFragNode.getName().toString();
				break;
			}
		}
		
		for(SingleVariableDeclaration varDecFragNode:lstFixSingleVariableDeclaration){
			if(fixLineNum==fixedWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				fixLineNodes.add(varDecFragNode.getModifiers());
				fixLineNodes.add(varDecFragNode.getType());	
	
				Expression exp = varDecFragNode.getInitializer();
				if(exp!=null)
					fixLineNodes.add(exp);
				changedName = varDecFragNode.getName().toString();
				break;
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
		
		int biLineNum = biChange.getLineNumInPrevFixRev();
		int fixLineNum = biChange.getEdit().getBeginB()+1;
		
		ArrayList<MethodDeclaration> lstBIMethodDeclaration = preFixWholeCodeAST.getMethodDeclarations();
		ArrayList<MethodDeclaration> lstFixMethodDeclaration = fixedWholeCodeAST.getMethodDeclarations();
		
		String originalName = "";
		String changedName = "";
		
		ArrayList<Object> biLineNodes = new ArrayList<Object>(), fixLineNodes = new ArrayList<Object>();
		
		for(MethodDeclaration methodDecNode:lstBIMethodDeclaration){
			if(biLineNum>=preFixWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition())
					&& biLineNum<preFixWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength())
					){
				originalName = methodDecNode.getName().toString();
				biLineNodes.add(methodDecNode.getModifiers());
				if(methodDecNode.getReturnType2() != null)
					biLineNodes.add(methodDecNode.getReturnType2());
				biLineNodes.add(methodDecNode.parameters());
				break;
			}
		}
		
		for(MethodDeclaration methodDecNode:lstFixMethodDeclaration){
			if(fixLineNum>=fixedWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition())
					&& fixLineNum<fixedWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength())
					){
				changedName = methodDecNode.getName().toString();
				fixLineNodes.add(methodDecNode.getModifiers());
				if(methodDecNode.getReturnType2() != null)
					fixLineNodes.add(methodDecNode.getReturnType2());
				fixLineNodes.add(methodDecNode.parameters());
				break;
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
