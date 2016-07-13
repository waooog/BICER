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
				System.out.println(biChange.getRecord());
			else if (biChange.isNoise()){
				noisyBIChanges.add(biChange);
			}
		}

		// List Noisy BI changes
		System.out.println("\n\nNoisy BI changes\nBI_SHA1\tPATH\tBIDATE\tFixDATE\t DUE_TO");
		for(BIChange biChange:noisyBIChanges){
			System.out.println(biChange.getRecord() + "\t" +biChange.getFilteredDueTo());
		}
	}

	private void filterOutNoises() {
		try {
			git = Git.open( new File( gitURI) );
			repo = git.getRepository();

		} catch (IOException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}

		String[] wholePreFixCode = null;
		String[] wholeFixCode=null;
		EditList editListFromDiff = null;
		biChangesNotExist = new ArrayList<BIChange>();
		for(BIChange biChange:biChanges){

			String preFixSha1 = biChange.getFixSha1() + "~1";
			String fixSha1 = biChange.getFixSha1();
			String biPath = biChange.getBIPath();
			String fixPath = biChange.getPath();
			
			// load whole fix code
			try {
				String code = Utils.fetchBlob(repo, preFixSha1, fixPath);
				if(code.equals("")){
					System.err.println("WARNING pre fix revision path does not exist: " + fixSha1 + ":" + fixPath);
					System.err.println("Try to get code from biPath " + biPath);
					code = Utils.fetchBlob(repo, biChange.getBISha1(), biPath);
					if(code.equals("")){
						System.err.println("WARNING even bi path does not exist: " + biPath);
						System.exit(0);
					}
				}
				
				wholePreFixCode = code.split("\n");
				
				wholeFixCode = Utils.fetchBlob(repo, fixSha1, fixPath).split("\n");

				editListFromDiff = Utils.getEditListFromDiff(Utils.getStringFromStringArray(wholePreFixCode),Utils.getStringFromStringArray(wholeFixCode));
				//editListFromDiff = Utils.getEditListFromDiff(git, preFixSha1, fixSha1, fixPath);//Utils.getEditListFromDiff(Utils.getStringFromStringArray(wholePreFixCode),Utils.getStringFromStringArray(wholeFixCode));

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

			updateBIChangeWithEditList(biChange,wholePreFixCode,wholeFixCode,editListFromDiff);
			
			// ignore line with only one character such as {,}
			if(biChange.getLine().trim().length()<2){
				biChange.setFilteredDueTo("One character line");
				Edit edit = biChange.getEdit();
				// check edit is null, if null skip
				if(biChange.getEdit()==null){
					System.err.println("WARNING not filtered: Diff results are different between jGit and git diff");
					System.err.println(biChange.toString());
					biChange.setIsNoise(false);
					continue;
				}
				int beginA = edit.getBeginA();
				int endA = edit.getEndA();
				
				if(edit.getType()==Edit.Type.DELETE && (endA-beginA)==1)
					biChange.setIsNoise(false); // single line delete change should not be noise
				else
					biChange.setIsNoise(true);
				continue;
			}

			biChange.setIsNoise(isNoise(biChange,wholePreFixCode,wholeFixCode,editListFromDiff));
		}
	}

	private boolean isNoise(BIChange biChange,String[] wholePreFixCode, String[] wholeFixCode, EditList editListFromDiff){

		FilterFactory factory = new FilterFactory();

		JavaASTParser preFixWholeCodeAST = new JavaASTParser(Utils.getStringFromStringArray(wholePreFixCode));
		JavaASTParser fixedWholeCodeAST = new JavaASTParser(Utils.getStringFromStringArray(wholeFixCode));

		// TODO Implement filtering

		ArrayList<Filter> filters = new ArrayList<Filter>();

		// Filter 01: Position change of declaration statements
		Filter postisionChangeFilter = factory.createFilter(Filters.POSITION_CHANGE, biChange,preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(postisionChangeFilter);

		// Filter 02: Remove unnecessary import (java) and include (c)
		Filter removeUnnImportFilter = factory.createFilter(Filters.REMOVE_UN_IMPORT, biChange, wholeFixCode);
		filters.add(removeUnnImportFilter);

		// Filter 03: Cosmetic change
		Filter cosmeticChangeFilter = factory.createFilter(Filters.COSMETIC_CHANGE, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(cosmeticChangeFilter);

		// Filter 04: Change a name
		Filter nameChange = factory.createFilter(Filters.NAME_CHANGE, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(nameChange);

		// Filter 05: Remove unnecessary method
		Filter removeUnnecessaryMethod = factory.createFilter(Filters.REMOVE_UN_METHOD, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(removeUnnecessaryMethod);
		
		// Filter 06: Change a Modifer
		Filter changeMidifier = factory.createFilter(Filters.MODIFIER_CHANGE, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(changeMidifier);
		
		// Filter 07: Change a assertion
		Filter assertionChange = factory.createFilter(Filters.ASSERTION_CHANGE, biChange, wholeFixCode);
		filters.add(assertionChange);

		// Filter 08: Add or detele a genetic type
		Filter geneticTypeChange = factory.createFilter(Filters.GENERIC_CHANGE, biChange, wholeFixCode);
		filters.add(geneticTypeChange);
		
		// Filter 09: Rebuild def-use chain
		Filter rebuildDefUseChain = factory.createFilter(Filters.REBUILD_DEF_USE_CHAIN, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(rebuildDefUseChain);
		
		// Filter 10: Remove def-use chain
		Filter removeDefUseChain = factory.createFilter(Filters.REMOVE_DEF_USE_CHAIN, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(removeDefUseChain);
		
		//Filter 11: String value change
		Filter stringValueChange = factory.createFilter(Filters.STRING_VALUE_CHANGE, biChange, preFixWholeCodeAST, fixedWholeCodeAST);
		filters.add(stringValueChange);
		
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

	private void updateBIChangeWithEditList(BIChange biChange,String[] wholePreFixCode,String[] wholeFixedCode, EditList editListFromDiff) {
		
		for(Edit edit:editListFromDiff){
			int beginA = edit.getBeginA();
			int endA = edit.getEndA();
			
			if(edit.getType().equals(Edit.Type.INSERT) && beginA <= biChange.getLineNumInPrevFixRev() && biChange.getLineNumInPrevFixRev()<=endA+1){
				biChange.setEdit(edit);
				break;
			}
			
			if(beginA < biChange.getLineNumInPrevFixRev() && biChange.getLineNumInPrevFixRev()<=endA){
				biChange.setEdit(edit);
				break;
			}
		}

		biChange.setEditList(editListFromDiff);
	}

	private void loadBIChanges() {
		ArrayList<String> BIChangeInfo = Utils.getLines(pathToBIChangeData, true);
		biChanges = new ArrayList<BIChange>();
		for(String info: BIChangeInfo){
			biChanges.add(new BIChange(info,runSanitizer));
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
				.desc("Run Sanitizer that regenerates new data by correcting a wrong line num")
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
