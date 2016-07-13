package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.StringTokenizer;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class StringValueChange  implements Filter{
	final String name="String value change (in a throw statement)";
	BIChange biChange;
	String[] wholeFixCode;
	JavaASTParser preFixWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	
	
	public StringValueChange(BIChange biChange, JavaASTParser preFixWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		super();
		this.biChange = biChange;
		this.preFixWholeCodeAST = preFixWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
		
		this.wholeFixCode = fixedWholeCodeAST.getStringCode().split("\n");
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		// TODO Auto-generated method stub
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;

		String stmt = biChange.getLine().trim();

		// (1) Check the line is a declarative statement such as
		// import
		StringTokenizer stokenize;
		if(isThrowStmt(stmt)){
			stokenize = new StringTokenizer(stmt, "(");
			String throw_st_before = stokenize.nextToken();
			
			if(Utils.doesContainLine(throw_st_before, wholeFixCode, true, true))
				return true;
		}
		
		return false;
	}

	private boolean isThrowStmt(String stmt) {

		// Tthrow statements
		if (stmt.matches("^\\s*throw\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;

		if (stmt.matches("^\\s*message\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		if (stmt.matches("^\\s*log.info\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		if (stmt.matches("^\\s*log.debug\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		if (stmt.matches("^\\s*log.fatal\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		if (stmt.matches("^\\s*log.warn\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))
			return true;
		
		if (stmt.matches("^\\s*log.error\\s\\s*[a-zA-Z_$][a-zA-Z_$0-9.]*\\s*;.*"))	
			return true;
		
		return false;		
	}
	
	@Override
	public boolean isNoise() {
		// TODO Auto-generated method stub
		return this.isNoise;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
