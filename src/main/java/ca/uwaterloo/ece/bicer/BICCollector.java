package ca.uwaterloo.ece.bicer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class BICCollector {

	public static void main(String[] args) {
		new BICCollector().run(args);
	}

	private String gitURI;
	private String pathToBIChangeData;
	private boolean help;
	private boolean verbose;

	private void run(String[] args) {
		
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

		options.addOption(Option.builder("b").longOpt("bugs")
				.desc("A file path for bug report IDs")
				.hasArg()
				.argName("file")
				.required()
				.build());
		
		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting bug-introducing changes")
				.required()
				.build());
		
		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes")
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
		String header = "Execute BICER to collect bug-introducing changes. On Windows, use BICER.bat instead of ./BICER";
		String footer ="\nPlease report issues at https://github.com/lifove/BICER/issues";
		formatter.printHelp( "./BICER", header, options, footer, true);
	}
}
