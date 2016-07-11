package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

/**
 * Implement a filter for Position change of a declaration statements
 * 
 * @author JC
 *
 */
public class PositionChange implements Filter {

	final String name="Position change of declarative statements";
	BIChange biChange;
	String[] wholeFixCode;
	JavaASTParser preFixWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;

	public PositionChange(BIChange biChange,JavaASTParser preFixWholeCodeAST,JavaASTParser fixedWholeCodeAST) {

		this.biChange = biChange;
		this.wholeFixCode = fixedWholeCodeAST.getStringCode().split("\n");
		this.preFixWholeCodeAST = preFixWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;

		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {

		// No need to consider a deleted line in a BI change, but need to consider when it is import statement even in a deleted line in a BI change.
		if(!biChange.getIsAddedLine() && !biChange.getLine().startsWith("import"))
			return false;

		String stmt = biChange.getLine();

		// (1) Check the line is a declarative statement such as
		// import
		if(isImportStmt(stmt)){

			// (2) check if there is the same line in a different position.
			if(Utils.doesSameLineExist(stmt, wholeFixCode, true, true))
				return true;
		}
		
		// java members
		if(isPositionChangeOfFields())
			return true;

		// TODO java methods
		if(isPositionChangeOfMethod())
			return true;

		return false;
	}

	private boolean isImportStmt(String stmt) {

		// java import?
		if (stmt.matches("^\\s*import\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;

		// c include?
		if (stmt.matches("^\\s*#include\\s\\s*[\"<]\\s*[\\w\\-. ]+\\s*[\">].*"))
			return true;
		
		// TODO c constants / variables

		// TODO c functions / structs

		return false;		
	}

	private boolean isPositionChangeOfFields() {

		int biLineNum = biChange.getLineNumInPrevFixRev();

		ArrayList<FieldDeclaration> lstBIFieldDeclaration = preFixWholeCodeAST.getFieldDeclarations();
		ArrayList<FieldDeclaration> lstFixFieldDeclaration = fixedWholeCodeAST.getFieldDeclarations();

		FieldDeclaration biFieldDeclaration = null;
		for(ASTNode varDecFragNode:lstBIFieldDeclaration){
			if(biLineNum==preFixWholeCodeAST.getCompilationUnit().getLineNumber(varDecFragNode.getStartPosition())){
				biFieldDeclaration = (FieldDeclaration)varDecFragNode;
				break;
			}
		}

		for(ASTNode varDecFragNode:lstFixFieldDeclaration){
			if(biFieldDeclaration!=null
					&& biFieldDeclaration.toString().equals(varDecFragNode.toString())
					&& ((TypeDeclaration)biFieldDeclaration.getParent()).getName().toString().equals
					(
							((TypeDeclaration)varDecFragNode.getParent()).getName().toString()
							)
			)
				return true;
		}

		return false;
	}

	private boolean isPositionChangeOfMethod() {

		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;

		ArrayList<MethodDeclaration> lstMethodDeclaration = preFixWholeCodeAST.getMethodDeclarations();
		ArrayList<MethodDeclaration> lstFixMethodDeclaration = fixedWholeCodeAST.getMethodDeclarations();

		// (1) get method that contains a BI line that is not a line for method declaration.
		MethodDeclaration preFixMethodDecNodeHavingBILine=null;
		int potentialBeginALineNum = -1; // deleted line range start line num
		int potentialEndALineNum = -1; // deleted line range end line num

		for(MethodDeclaration methodDecNode:lstMethodDeclaration){
			int beginLineNum = preFixWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition());
			int endLineNum = preFixWholeCodeAST.getCompilationUnit().getLineNumber(methodDecNode.getStartPosition()+methodDecNode.getLength());
			if(biChange.getLineNumInPrevFixRev()>=beginLineNum && biChange.getLineNumInPrevFixRev()<= endLineNum){
				preFixMethodDecNodeHavingBILine = methodDecNode;
				potentialBeginALineNum = beginLineNum;
				potentialEndALineNum = endLineNum;
				break;
			}
		}

		if(preFixMethodDecNodeHavingBILine==null) // biLine is not a line in a method.
			return false;

		if(biChange.getEdit()==null)
			return false;

		// (2) check all lines for the method are removed.
		if(!((potentialBeginALineNum > biChange.getEdit().getBeginA()) && (potentialEndALineNum <= biChange.getEdit().getEndA()))){
			return false;
		}

		// (3) check if the same method exists
		for(MethodDeclaration methodDec:lstFixMethodDeclaration){
			if(preFixMethodDecNodeHavingBILine.toString().equals(methodDec.toString())
					&& ((TypeDeclaration)preFixMethodDecNodeHavingBILine.getParent()).getName().toString().equals
							(
									((TypeDeclaration)methodDec.getParent()).getName().toString()
							)
			)
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
