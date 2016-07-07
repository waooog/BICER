package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;

public class StringValueChange  implements Filter{
	final String name="String value change";
	BIChange biChange;
	JavaASTParser preFixWholeCodeAST;
	JavaASTParser fixedWholeCodeAST;
	boolean isNoise=false;
	
	
	public StringValueChange(BIChange biChange, JavaASTParser preFixWholeCodeAST, JavaASTParser fixedWholeCodeAST) {
		super();
		this.biChange = biChange;
		this.preFixWholeCodeAST = preFixWholeCodeAST;
		this.fixedWholeCodeAST = fixedWholeCodeAST;
	}

	@Override
	public boolean filterOut() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNoise() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
