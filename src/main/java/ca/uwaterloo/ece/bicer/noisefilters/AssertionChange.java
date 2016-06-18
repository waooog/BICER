package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;

public class AssertionChange implements Filter{
	final String name="Assertion change";
	BIChange biChange;
	boolean isNoise=false;
	
	public AssertionChange (BIChange biChange){
		this.biChange=biChange;
		this.isNoise=filterOut();
	}
	
	@Override
	public boolean filterOut() {
		if(biChange.getLine().matches("^\\s*assert\\s.*")) return true;
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
