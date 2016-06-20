package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
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
		this.wholeFixCode = preFixWholeCodeAST.getStringCode().split("\n");
		this.preFixWholeCodeAST = preFixWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;

		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {

		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;

		String stmt = biChange.getLine();

		// (1) Check the line is a declarative statement such as
		// import, ClassName variable = ..., ClassName variable;, a whole method where the line lives.
		if(areDeclarativeStmts(stmt)){

			// (2) check if there is the same line in a different position.
			if(Utils.doesSameLineExist(stmt, wholeFixCode, true, true))
				return true;
		}

		return false;
	}

	private boolean areDeclarativeStmts(String stmt) {

		// java import?
		if (stmt.matches("^\\s*import\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;

		// c include?
		if (stmt.matches("^\\s*#include\\s\\s*[\"<]\\s*[\\w\\-. ]+\\s*[\">].*"))
			return true;

		// java members
		if(isPositionChangeOfFields())
			return true;

		// TODO java methods

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

	@Override
	public boolean isNoise() {
		return isNoise;
	}

	@Override
	public String getName() {
		return name;
	}
}
