package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;

public class FilterFactory {
	
	public static enum Filters {
		POSITION_CHANGE,
		REMOVE_UN_IMPORT,
		COSMETIC_CHANGE
	}
	
	public Filter createFilter(Filters filter,BIChange biChange, String[] wholeFixCode){
		
		if (filter == Filters.POSITION_CHANGE)
			return new PositionChange(biChange,wholeFixCode);
		
		if(filter == Filters.REMOVE_UN_IMPORT)
			return new RemoveUnnImport(biChange,wholeFixCode);
		
		if(filter == Filters.COSMETIC_CHANGE)
			return new CosmeticChange(biChange,wholeFixCode);
		
		return null;
	}

}
