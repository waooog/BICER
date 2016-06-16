package ca.uwaterloo.ece.bicer;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.noisefilters.Filter;
import ca.uwaterloo.ece.bicer.noisefilters.FilterFactory;
import ca.uwaterloo.ece.bicer.noisefilters.FilterFactory.Filters;
import ca.uwaterloo.ece.bicer.utils.JavaASTParser;
import ca.uwaterloo.ece.bicer.utils.Sanitizer;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class NoiseFilterRunner {

	String gitURI;
	String pathToBIChangeData;
	boolean runSanitizer;
	boolean help;
	boolean verbose;
	ArrayList<BIChange> biChanges;
	ArrayList<BIChange> biChangesNotExist;

	Git git;
	Repository repo;

	public static void main(String[] args) {
		new NoiseFilterRunner().run(args);
	}

	void run(String[] args) {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}

			if(runSanitizer){
				(new Sanitizer()).sanitizer(pathToBIChangeData, gitURI);
			}else{
				loadBIChanges();
				filterOutNoises();
				printCleanBIChanges();
			}
		}
	}

	public void printCleanBIChanges(){

		ArrayList<BIChange> noisyBIChanges = new ArrayList<BIChange>();

		// List cleaned BI changes
		System.out.println("BI_SHA1\tPATH\tBIDATE\tFixDATE");
		for(BIChange biChange:biChanges){
			if(!biChange.isNoise() && !biChangesNotExist.contains(biChange))
				System.out.println(biChange.getBISha1() + "\t" + biChange.getPath() +
						"\t" + biChange.getBIDate() + "\t" + biChange.getFixDate());
			else if (biChange.isNoise()){
				noisyBIChanges.add(biChange);
			}
		}

		// List Noisy BI changes
		System.out.println("\n\nNoisy BI changes\nBI_SHA1\tPATH\tBIDATE\tFixDATE\t DUE_TO");
		for(BIChange biChange:noisyBIChanges){
			System.out.println(biChange.getBISha1() + "\t" + biChange.getPath() +
					"\t" + biChange.getBIDate() + "\t" + biChange.getFixDate() +
					"\t" + biChange.getFilteredDueTo() + "\t" +  biChange.getIsAddedLine() + 
					"\t" + biChange.getLine());
		}
	}

	private void filterOutNoises() {
		try {
			git = Git.open( new File( gitURI) );
			repo = git.getRepository();

		} catch (IOException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}

		String[] wholeBICode=null;
		String[] wholeFixCode=null;
		EditList editListFromDiff = null;
		biChangesNotExist = new ArrayList<BIChange>();
		for(BIChange biChange:biChanges){

			String biSha1 = biChange.getBISha1();
			String fixSha1 = biChange.getFixSha1();
			String biPath = biChange.getBIPath();
			String fixPath = biChange.getPath();

			// load whole BI code
			try {
				// TODO when path does not exist in BI change, biPath should be identified.
				wholeBICode = Utils.fetchBlob(repo, biSha1, biPath).split("\n");

				if (wholeBICode.equals("")){
					System.err.println("Ignore (no code): " + biSha1 + "\t" + fixSha1 + "\t" + biPath);
					continue;
				}

			} catch (MissingObjectException e) {
				System.err.println("The sha1 does not exist: " + biSha1 + ":" + biPath);
				biChangesNotExist.add(biChange);
				continue;
			} catch (IncorrectObjectTypeException e) {
				e.printStackTrace();
				biChangesNotExist.add(biChange);
				continue;
			} catch (IOException e) {
				System.err.println("The file path does not exist: " + biSha1 + ":" + biPath);
				biChangesNotExist.add(biChange);
				continue;
			}

			// load whole fix code
			try {
				wholeFixCode = Utils.fetchBlob(repo, fixSha1, fixPath).split("\n");

				editListFromDiff = Utils.getEditListFromDiff(Utils.getStringFromStringArray(wholeBICode),Utils.getStringFromStringArray(wholeFixCode));

			} catch (MissingObjectException e) {
				System.err.println("The sha1 does not exist: " + fixSha1 + ":" + fixPath);
				biChangesNotExist.add(biChange);
				continue;
			} catch (IncorrectObjectTypeException e) {
				e.printStackTrace();
				biChangesNotExist.add(biChange);
				continue;
			} catch (IOException e) {
				System.err.println("The file path does not exist: " + fixSha1 + ":" + fixPath);
				biChangesNotExist.add(biChange);
				continue;
			}

			// ignore line with only one character such as {,}
			if(biChange.getLine().trim().length()<2){
				biChange.setFilteredDueTo("One character line");
				biChange.setIsNoise(true);
				continue;
			}

			biChange.setIsNoise(isNoise(biChange,wholeBICode,wholeFixCode,editListFromDiff));
		}
	}

	private boolean isNoise(BIChange biChange,String[] wholeBICode, String[] wholeFixCode, EditList editListFromDiff){

		FilterFactory factory = new FilterFactory();

		updateBIChangeWithEditList(biChange,wholeBICode,wholeFixCode,editListFromDiff);

		JavaASTParser biWholeCodeAST = new JavaASTParser(Utils.getStringFromStringArray(wholeBICode));
		JavaASTParser fixedWholeCodeAST = new JavaASTParser(Utils.getStringFromStringArray(wholeFixCode));

		// TODO Implement filtering

		ArrayList<Filter> filters = new ArrayList<Filter>();

		// Filter 01: Position change of declaration statements
		Filter postisionChangeFilter = factory.createFilter(Filters.POSITION_CHANGE, biChange, wholeFixCode);
		filters.add(postisionChangeFilter);

		// Filter 02: Remove unnecessary import (java) and include (c)
		Filter removeUnnImportFilter = factory.createFilter(Filters.REMOVE_UN_IMPORT, biChange, wholeFixCode);
		filters.add(removeUnnImportFilter);

		// Filter 03: Cosmetic change
		Filter cosmeticChangeFilter = factory.createFilter(Filters.COSMETIC_CHANGE, biChange, wholeFixCode);
		filters.add(cosmeticChangeFilter);

		// Filter 04: Change a name
		Filter nameChange = factory.createFilter(Filters.NAME_CHANGE, biChange, biWholeCodeAST, fixedWholeCodeAST);
		filters.add(nameChange);

		// Filter 05: Remove unnecessary method
		Filter removeUnnecessaryMethod = factory.createFilter(Filters.REMOVE_UN_METHOD, biChange, biWholeCodeAST, fixedWholeCodeAST);
		filters.add(removeUnnecessaryMethod);
		
		// Filter 06: Change a Modifer
		Filter changeMidifier = factory.createFilter(Filters.MODIFIER_CHANGE, biChange, wholeFixCode);
		filters.add(changeMidifier);

		boolean isNoise = false;
		for(Filter filter:filters){
			if(filter.isNoise()){
				biChange.setIsNoise(filter.isNoise());
				biChange.setFilteredDueTo(filter.getName() + "|" + biChange.getFilteredDueTo());
				isNoise = true;
			}
		}

		return isNoise;
	}

	private void updateBIChangeWithEditList(BIChange biChange,String[] wholeBICode,String[] wholeFixedCode, EditList editListFromDiff) {
		String biLine = biChange.getLine().trim();

		ArrayList<Integer> candidateLineNums = new ArrayList<Integer>();
		ArrayList<Edit> candidateEdits = new ArrayList<Edit>();
		for(Edit edit:editListFromDiff){
			int beginA = edit.getBeginA();
			int endA = edit.getEndA();

			for(int i=beginA;i<=endA;i++){
				try{	
					if(biLine.equals(wholeBICode[i].trim())){
						candidateLineNums.add(i);
						candidateEdits.add(edit);
					}
				} catch(ArrayIndexOutOfBoundsException e){
					System.err.println(wholeBICode.length + "\t" +biChange.getBISha1() + "\t" + biChange.getFixSha1() + "\t" + biChange.getBIPath());
				}
			}
		}
		
		if(candidateLineNums.size()==0 && biChange.getIsAddedLine()){
			// no matched lines exist
			System.err.println("WARNING: the following line change is not exist in bi file\n" +
					biChange.getBISha1()+"," + biChange.getBIPath() + "," + 
					biChange.getFixSha1()+"," + biChange.getPath() + "\t" + biChange.getLineNum() + "\t" + biLine);
		}

		// adjust actual line num and set edit that is related to a bi line
		int rawLineNum = biChange.getLineNum();
		if(candidateLineNums.size()==1){
			biChange.setLineNum(candidateLineNums.get(0)+1);
			biChange.setEdit(candidateEdits.get(0));
		}
		else{
			// heuristic to get the best matching line
			for(int i=0;i<candidateLineNums.size();i++){
				int lineIdx = candidateLineNums.get(i);
				
				if(lineIdx <=(rawLineNum-1)){
					biChange.setLineNum(lineIdx+1);
					biChange.setEdit(candidateEdits.get(i));
				}
			}
		}

		biChange.setEditList(editListFromDiff);
	}

	private void loadBIChanges() {
		ArrayList<String> BIChangeInfo = Utils.getLines(pathToBIChangeData, true);
		biChanges = new ArrayList<BIChange>();
		for(String info: BIChangeInfo){
			biChanges.add(new BIChange(info));
		}
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("g").longOpt("git")
				.desc("Git URI")
				.hasArg()
				.argName("URI")
				.required()
				.build());

		options.addOption(Option.builder("d").longOpt("data")
				.desc("A file path for bug-introducing change data")
				.hasArg()
				.argName("file")
				.required()
				.build());
		
		options.addOption(Option.builder("s").longOpt("sanitize")
				.desc("Run Sanitizer that regenerate data by correcting a wrong line num")
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());


		options.addOption(Option.builder("v").longOpt("verbose")
				.desc("Verbose")
				.build());

		return options;
	}

	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);

			gitURI = cmd.getOptionValue("g");
			pathToBIChangeData = cmd.getOptionValue("d");

			runSanitizer = cmd.hasOption("s");
			help = cmd.hasOption("h");
			verbose = cmd.hasOption("v");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Execute noise filter for bug-introducing changes. On Windows, use filter.bat instead of ./filter";
		String footer ="\nPlease report issues at https://github.com/lifove/BICER/issues";
		formatter.printHelp( "./filter", header, options, footer, true);
	}

}
