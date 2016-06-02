package ca.uwaterloo.ece.bicer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.bicer.data.BIChange;
import ca.uwaterloo.ece.bicer.utils.Utils;

public class NoiseFilterRunner {
	
	String gitURI;
	String pathToBIChangeData;
	boolean help;
	boolean verbose;
	ArrayList<BIChange> biChanges;
	
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
			
			loadBIChanges();
			
			filterOutNoises();
		}
	}

	private void filterOutNoises() {
		try {
			git = Git.open( new File( gitURI) );
			repo = git.getRepository();
			
		} catch (IOException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}
		
		String currentFixSha1="",currentPath="";
		String[] currentLines;
		for(BIChange biChange:biChanges){
			
			if(!currentFixSha1.equals(biChange.getFixSha1()) || currentPath.equals(biChange.getPath())){
				currentFixSha1 = biChange.getFixSha1();
				currentPath = biChange.getPath();
				try {
					currentLines = Utils.fetchBlob(repo, currentFixSha1, currentPath).split("\n");
				} catch (MissingObjectException e) {
					System.err.println("The sha1 does not exist: " + currentFixSha1);
				} catch (IncorrectObjectTypeException e) {
					e.printStackTrace();
				} catch (IOException e) {
					System.err.println("The file path does not exist: " + currentPath);
				}
			}
			
			// TODO Implement filtering
		}
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
