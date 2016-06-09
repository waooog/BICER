package ca.uwaterloo.ece.bicer.noisefilters;

import java.util.ArrayList;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.CustomASTParser;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class RemoveUnnecessaryMethod implements Filter {
	
	final String name="Remove Unnecessary Method";
	BIChange biChange;
	String[] wholeBICode;
	String[] wholeFixCode;
	boolean isNoise=false;
	
	public RemoveUnnecessaryMethod(BIChange biChange, String[] wholeBICode, String[] wholeFixCode) {
		this.biChange = biChange;
		this.wholeBICode = wholeBICode;
		this.wholeFixCode = wholeFixCode;
		
		isNoise = filterOut();
	}

	@Override
	public boolean filterOut() {
		
		// No need to consider a deleted line in a BI change
		if(!biChange.getIsAddedLine())
			return false;
		
		String biSource = Utils.getStringFromStringArray(wholeBICode);
		int startPositionOfBILine = biSource.indexOf(biChange.getLine());
		int startPositionOfBILineFromEnd = biSource.lastIndexOf(biChange.getLine());
		
		if(startPositionOfBILine!=startPositionOfBILineFromEnd){
			System.err.println("Redundant BI line exists in source code: " + biChange.getLine());
			System.exit(0);
		}

		ArrayList<String> ast = CustomASTParser.praseJavaFile(biSource);
		
		// (1) get method that contains a BI line.
		String methodHavingBILine = getMethodHavingBILine(getListOfMethods(ast),startPositionOfBILine);
		if (methodHavingBILine==null)
			return false;
		
		// (2) check if the method and BI line does not exists in fixed source code. No existence, method removed.
		String fixedSource = Utils.getStringFromStringArray(wholeFixCode);
		return notExistMethodAndBILine(getListOfMethods(CustomASTParser.praseJavaFile(fixedSource)),fixedSource,methodHavingBILine,biChange.getLine());
	}

	private boolean notExistMethodAndBILine(ArrayList<String> listOfMethods, String fixedSource, String methodHavingBILine, String line) {
		
		// if method and BI lines do not exist, it is noise.
		if(!listOfMethods.contains(methodHavingBILine) && fixedSource.indexOf(line)<0)
			return true;
		
		return false;
	}

	private String getMethodHavingBILine(ArrayList<String> listOfMethods, int startPositionOfBILine) {
		
		for(String element:listOfMethods){
			String[] methodInfo = element.split(":");
			String method = methodInfo[1];
			int startPosition = Integer.parseInt(methodInfo[2]);
			int length = Integer.parseInt(methodInfo[3]);
			
			if(startPosition < startPositionOfBILine && startPositionOfBILine <startPosition+length)
				return method;
			
		}
		
		return null;
	}

	private ArrayList<String> getListOfMethods(ArrayList<String> ast) {
		
		ArrayList<String> list = new ArrayList<String>();
		
		for(String element:ast){
			if(element.startsWith("METHOD:")){
				list.add(element);
			}
		}
		
		return list;
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
