package ca.uwaterloo.ece.bicer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class NoiseFilterRunner {
	
	String gitURI;
	String pathToBIChangeData;
	boolean help;
	boolean verbose;

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
