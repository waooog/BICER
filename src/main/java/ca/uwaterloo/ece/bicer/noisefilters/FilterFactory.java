package ca.uwaterloo.ece.bicer.noisefilters;

import org.eclipse.jgit.diff.EditList;

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
	
	public Filter createFilter(Filters filter,BIChange biChange, String[] wholeFixCode, EditList editListFromDiffOfPrev){
		if (filter == Filters.MODIFIER_CHANGE)
			return new ModifierChange(biChange,wholeFixCode,editListFromDiffOfPrev);
		return null;
	}
	
public Filter createFilter(Filters filter,BIChange biChange, JavaASTParser biWholeCodeAST, JavaASTParser fixWholeCodeAST){

		if(filter == Filters.NAME_CHANGE)
			return new NameChange(biChange,biWholeCodeAST,fixWholeCodeAST);
	
		if(filter == Filters.REMOVE_UN_METHOD)
			return new RemoveUnnecessaryMethod(biChange,biWholeCodeAST,fixWholeCodeAST);
		
		return null;
	}

}
