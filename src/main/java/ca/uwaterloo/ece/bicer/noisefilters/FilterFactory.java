package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;

public class FilterFactory {
	
	public static enum Filters {
		POSITION_CHANGE,
		REMOVE_UN_IMPORT,
		COSMETIC_CHANGE
	}
	
	public Filter createFlter(Filters filter,BIChange biChange, String[] wholeFixCode){
		if (filter == Filters.POSITION_CHANGE)
			return new PositionChange(biChange,wholeFixCode);
		
		return null;
	}

}
