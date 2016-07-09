package ca.uwaterloo.ece.bicer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import ca.uwaterloo.ece.bicer.utils.Utils;

public class BICCollector {

	public static void main(String[] args) {
		new BICCollector().run(args);
	}

	private String gitURI;
	private String pathToBuggyIDs;
	private boolean help;
	private boolean verbose;
	private Date startDate;
	private Date endDate;
	private ArrayList<String> bugIDs;
	
	private Git git;
	private Repository repo;

	public void run(String[] args) {
		Options options = createOptions();

		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			// load BugsIDs
			bugIDs = Utils.getLines(pathToBuggyIDs, false);
			
			String bugIDPreFix = bugIDs.get(0).split("-")[0] + "-";
			
			// get commits between the start and end dates
			ArrayList<RevCommit> commits = getRevCommits();
			
			repo = git.getRepository();
			
			for(RevCommit rev:commits){
				String message = rev.getFullMessage();
				// Create matcher on file
			    Pattern pattern = Pattern.compile(bugIDPreFix + "\\d+");
			    Matcher matcher = pattern.matcher(message);

			    while(matcher.find()){
			    	if(bugIDs.contains(matcher.group(0))){
			    		
			    		System.out.println(rev.toString());
			    		
			    		// get a list of files in the commit
			    		RevCommit parent = rev.getParent(0);
			    		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			    		df.setRepository(repo);
			    		df.setDiffComparator(RawTextComparator.DEFAULT);
			    		df.setDetectRenames(true);
			    		List<DiffEntry> diffs;
						try {
							// its bug-fixing commit
				    		
				    		// do diff and get only added lines
							diffs = df.scan(parent.getTree(), rev.getTree());
							for (DiffEntry diff : diffs) {
								String oldPath = diff.getOldPath();
								String newPath = diff.getNewPath();
								
								String id =  rev.name() + "";
								
								String prevfileSource=Utils.removeLineComments(Utils.fetchBlob(repo, id +  "~1", oldPath));
								String fileSource=Utils.removeLineComments(Utils.fetchBlob(repo, id, newPath));	
								
								Utils.getEditListFromDiff(prevfileSource, fileSource);
								
								
								/*df.format(diff);
								FileHeader fileHeader = df.toFileHeader( diff );
								
								EditList editList = fileHeader.toEditList();
								
								for(Edit edit:editList){
									
								}*/
				    		    //System.out.println(MessageFormat.format("({0} {1} {2}", diff.getChangeType().name(), diff.getNewMode().getBits(), diff.getNewPath()));
				    		}
				    		
				    		
				    		// get the previous commit and do blame for deleted liens in the prev commit
				    		
				    		// 
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    	}
			    }
			}
			
		}
	}

	private ArrayList<RevCommit> getRevCommits() {
		ArrayList<RevCommit> commits = new ArrayList<RevCommit>();
		
		try {
			
			git = Git.open( new File(gitURI) );
			
			Iterable<RevCommit> logs = git.log()
		            .call();

		    //SimpleDateFormat ft =  new SimpleDateFormat ("E yyyy.MM.dd 'at' HH:mm:ss zzz");
		    for (RevCommit rev : logs) {
		    	Date commitDate = new Date(rev.getCommitTime()* 1000L);
		    	
		    	if(startDate.compareTo(commitDate)<=0 && commitDate.compareTo(endDate)<=0)
		    		commits.add(rev);
		    }             

		} catch (IOException | GitAPIException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}
		
		return commits;
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
				.build());
		
		options.addOption(Option.builder("s").longOpt("startdate")
				.desc("Start date for collecting bug-introducing changes")
				.hasArg()
				.argName("Start date")
				.required()
				.build());
		
		options.addOption(Option.builder("e").longOpt("enddate")
				.desc("End date for collecting bug-introducing changes")
				.hasArg()
				.argName("End date")
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
			pathToBuggyIDs = cmd.getOptionValue("b");

			help = cmd.hasOption("h");
			verbose = cmd.hasOption("v");
			
			startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cmd.getOptionValue("s"));
			endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(cmd.getOptionValue("e"));

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
