package ca.uwaterloo.ece.bicer.noisefilters;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;

public class FilterFactory {
	
	public static enum Filters {
		POSITION_CHANGE,
		REMOVE_UN_IMPORT,
		COSMETIC_CHANGE,
		NAME_CHANGE,
		REMOVE_UN_METHOD,
		MODIFIER_CHANGE,
		ASSERTION_CHANGE,
		GENERIC_CHANGE,
		REBUILD_DEF_USE_CHAIN,
		REMOVE_DEF_USE_CHAIN,
		STRING_VALUE_CHANGE,
	}
	
	public Filter createFilter(Filters filter,BIChange biChange, String[] wholeFixCode){
		
		if(filter == Filters.REMOVE_UN_IMPORT)
			return new RemoveUnnImport(biChange,wholeFixCode);
		
		if (filter == Filters.ASSERTION_CHANGE)
			return new AssertionChange(biChange,wholeFixCode);
		
		if (filter == Filters.GENERIC_CHANGE)
			return new GenericTypeChange(biChange,wholeFixCode);
		
		return null;
	}
	
	
	public Filter createFilter(Filters filter,BIChange biChange, JavaASTParser preFixWholeCodeAST, JavaASTParser fixWholeCodeAST){

		if (filter == Filters.POSITION_CHANGE)
			return new PositionChange(biChange,preFixWholeCodeAST, fixWholeCodeAST);
		
		if(filter == Filters.COSMETIC_CHANGE)
			return new CosmeticChange(biChange,preFixWholeCodeAST, fixWholeCodeAST);
		
		if(filter == Filters.NAME_CHANGE)
			return new NameChange(biChange,preFixWholeCodeAST,fixWholeCodeAST);
	
		if(filter == Filters.REMOVE_UN_METHOD)
			return new RemoveUnnecessaryMethod(biChange,preFixWholeCodeAST,fixWholeCodeAST);
		
		if (filter == Filters.MODIFIER_CHANGE)
		return new ModifierChange(biChange,preFixWholeCodeAST,fixWholeCodeAST);
		
		if (filter == Filters.REBUILD_DEF_USE_CHAIN)
		return new ReBuildDefUseChain(biChange,preFixWholeCodeAST,fixWholeCodeAST);
		
		if (filter == Filters.REMOVE_DEF_USE_CHAIN)
		return new RemoveDefUseChain(biChange,preFixWholeCodeAST,fixWholeCodeAST);
		

		if (filter == Filters.STRING_VALUE_CHANGE)
			return new StringValueChange(biChange,preFixWholeCodeAST, fixWholeCodeAST);
		return null;
	}

}
