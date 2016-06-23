package ca.uwaterloo.ece.bicer.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import ca.uwaterloo.ece.bicer.data.BIChange;
import weka.core.Instance;
import weka.core.Instances;

public class Utils {
	static public ArrayList<String> getLines(String file,boolean removeHeader){
		ArrayList<String> lines = new ArrayList<String>();
		String thisLine="";
		//Open the file for reading
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((thisLine = br.readLine()) != null) { // while loop begins here
				lines.add(thisLine);
			} // end while 
			br.close();
		} // end try
		catch (IOException e) {
			System.err.println("Error: " + e);
			//System.exit(0);
		}

		if(removeHeader)
			lines.remove(0);

		return lines;
	}
	
	static public EditList getEditListFromDiff(String file1, String file2) {
        RawText rt1 = new RawText(file1.getBytes());
        RawText rt2 = new RawText(file2.getBytes());
        EditList diffList = new EditList();
        diffList.addAll(new HistogramDiff().diff(RawTextComparator.WS_IGNORE_ALL, rt1, rt2));
        return diffList;
	}
	
	static public Object[] getASTNodeChildren(ASTNode node) {
	    List list= node.structuralPropertiesForType();
	    for (int i= 0; i < list.size(); i++) {
	        StructuralPropertyDescriptor curr= (StructuralPropertyDescriptor) list.get(i);
	            Object child= node.getStructuralProperty(curr);
	        if (child instanceof List) {
	                return ((List) child).toArray();
	        } else if (child instanceof ASTNode) {
	            return new Object[] { child };
	            }
	        return new Object[0];
	    }
		return null;
	}
	
	public static BIChange getBIChangeWithCorrectBISha1(Git git, BIChange biChange) {
		
		// skip biLine is a deleted line
		if(!biChange.getIsAddedLine())
			return biChange;
		
		Repository repository = git.getRepository();
		BlameCommand blamer = new BlameCommand(repository);
        ObjectId commitID;
		try {
			commitID = repository.resolve(biChange.getFixSha1() + "~1");
			blamer.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
			blamer.setFollowFileRenames(true);
			blamer.setStartCommit(commitID);
	        blamer.setFilePath(biChange.getPath());
	        BlameResult blame = blamer.call();
	        
	        // biPath might be different from fixPath
	        boolean renamed = false;
	        if(blame==null){
	        	renamed = true;
	        	commitID = repository.resolve(biChange.getBISha1());
				blamer.setTextComparator(RawTextComparator.WS_IGNORE_ALL);
				blamer.setFollowFileRenames(false);
				blamer.setStartCommit(commitID);
		        blamer.setFilePath(biChange.getBIPath());
		        blame = blamer.call();
	        }
	        
	        // get code
	        String fixCode = fetchBlob(repository,biChange.getFixSha1(),biChange.getPath());
	        
	        String preFixCode = !renamed?fetchBlob(repository,biChange.getFixSha1() + "~1",biChange.getPath())
	        						:fetchBlob(repository,biChange.getBISha1(),biChange.getBIPath());
	        
	        String[] splitlines = preFixCode.split("\n");
	        
	        EditList editList = getEditListFromDiff(preFixCode,fixCode);
	        
	        // find a biLine among changed lines
	        ArrayList<Integer> candidateLineNums = new ArrayList<Integer>(); // line num starts from 0
	        for(Edit edit:editList){
	        	if(edit.getType()==Edit.Type.DELETE || edit.getType()==Edit.Type.REPLACE){
		        	for(int i = edit.getBeginA();i<edit.getBeginA()+edit.getLengthA();i++){
		        		if(biChange.getLine().equals(splitlines[i].trim()))
		        			candidateLineNums.add(i);
		        	}
	        	}
	        }
	        
	        // get the best lineNum
	        int originalLineNum = biChange.getLineNum();
	        if(candidateLineNums.size()==1){
	        	biChange.setLineNum(candidateLineNums.get(0)+1);
	        	if(biChange.getLineNum()!=candidateLineNums.get(0)+1){
		        	System.err.println("WARNING: LinNum updated: " + originalLineNum +  "==>" + biChange.getLineNum() + "\n" + biChange.toString());
		        }
			}
			else{
				// heuristic to get the best matching line
				for(int i=0;i<candidateLineNums.size();i++){
					int lineIdx = candidateLineNums.get(i);
					if(lineIdx<=(originalLineNum-1)){
						biChange.setLineNum(lineIdx+1);
					}
				}
				if(originalLineNum!=biChange.getLineNum())
					System.err.println("WARNING: LineNum updated(2): " + originalLineNum +  "==>" + biChange.getLineNum() + "\n" + biChange.toString());
			}
	        
	        RevCommit commit = blame.getSourceCommit(biChange.getLineNum()-1);
	        if(!commit.name().equals(biChange.getBISha1())){
	        	System.err.println("WARNING: Wrong BI Sha1 - correct BI Sha1 = " + commit.name() + " lineNum=" + biChange.getLineNum() + "\n" + biChange.toString());
	        	System.err.println("git blame -w -C " + biChange.getFixSha1() + "~1 -- " + biChange.getPath());
	        	biChange.setBISha1(commit.name());
	        }
	        biChange.setBISha1(commit.name());
	        biChange.setLineNumInPrevFixRev(biChange.getLineNum()); // set lineNum in the code of previous revisions of the fix revision
	        biChange.setLineNum(blame.getSourceLine(biChange.getLineNum()-1)+1); // set lineNum in BI code
            /*for (int i = 0; i < splitlines.length; i++) {
                RevCommit commit = blame.getSourceCommit(i);
                System.out.println("Line: " + (i+1) + ": " + commit.name() + splitlines[i]);
            }*/  
		} catch (RevisionSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AmbiguousObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		return biChange;
	}
	
	static public EditList getEditListFromDiff(Git git,String oldSha1, String newSha1, String path){

		Repository repo = git.getRepository();

		ObjectId oldId;
		try {
			oldId = repo.resolve(oldSha1 + "^{tree}:");
			ObjectId newId = repo.resolve(newSha1 + "^{tree}");


			ObjectReader reader = repo.newObjectReader();
			
			// setting for renamed or copied path
			Config config = new Config();
			config.setBoolean("diff", null, "renames", true);
			DiffConfig diffConfig = config.get(DiffConfig.KEY);

			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldId);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, newId);

			List<DiffEntry> diffs= git.diff()
					.setPathFilter(FollowFilter.create(path, diffConfig))
					.setNewTree(newTreeIter)
					.setOldTree(oldTreeIter)
					.call();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DiffFormatter df = new DiffFormatter(out);
			df.setRepository(repo);

			for(DiffEntry entry:diffs){
		
				df.format(entry);
				FileHeader fileHeader = df.toFileHeader( entry );
				if(!fileHeader.getNewPath().equals(path))
					continue;
				
				return fileHeader.toEditList();
			}
			
			df.close();

		} catch (IndexOutOfBoundsException e){
					
		}
		catch (RevisionSyntaxException | IOException | GitAPIException e) {
			e.printStackTrace();
		}

		return null;
	}

	static public String fetchBlob(Repository repo, String revSpec, String path) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException{

		// Resolve the revision specification
		final ObjectId id = repo.resolve(revSpec);

		// Makes it simpler to release the allocated resources in one go
		ObjectReader reader = repo.newObjectReader();

		// Get the commit object for that revision
		RevWalk walk = new RevWalk(reader);
		RevCommit commit = walk.parseCommit(id);
		walk.close();

		// Get the revision's file tree
		RevTree tree = commit.getTree();
		// .. and narrow it down to the single file's path
		TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

		if (treewalk != null) {
			// use the blob id to read the file's data
			byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
			reader.close();
			return new String(data, "utf-8");
		} else {
			return "";
		}

	}

	static public boolean doesSameLineExist(String line,String[] lines,boolean trim,boolean ignoreLineComments){

		line = ignoreLineComments?removeLineComments(line):line;

		for(String lineCompare:lines){
			lineCompare = ignoreLineComments?removeLineComments(lineCompare):lineCompare;
			if(trim){
				if(line.trim().equals(lineCompare.trim()))
					return true;
			}
			else{
				if(line.equals(lineCompare))
					return true;
			}
		}

		return false;
	}

	public static String removeLineComments(String line) {
		// http://stackoverflow.com/questions/2613432/remove-source-file-comments-using-intellij
		return line.replaceAll("(/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*+/|[ \\t]*//.*)", "");
	}

	public static String getStringFromStringArray(String[] wholeFixCode) {

		String string="";

		for(String line:wholeFixCode)
			string += line +"\n";

		return string;
	}

	public static boolean compareMethodParametersFromAST(List<SingleVariableDeclaration> parameters1, List<SingleVariableDeclaration> parameters2) {
		
		if(parameters1.size()!=parameters2.size())
			return false;
		
		for(int i=0;i<parameters1.size();i++){
			String type1 = parameters1.get(i).getType().toString();
			String type2 = parameters2.get(i).getType().toString();
			if(!type1.equals(type2.toString()))
				return false;
		}
		
		return true;
	}
	
	public static int getStartPosition(String biSource, int lineNum) {
		
		int currentPosition = 0;
		String[] lines = biSource.split("\n");
		
		for(int i=0; i < lines.length; i++){
			if(i==lineNum-1)
				return currentPosition;
			
			currentPosition+=lines[i].length() + 1; // + 1 is for \n
		}
		
		return -1;
	}
	
	static public ArrayList<BIChange> loadBIChanges(String pathToBIChangeData,boolean isNonSanitized) {
		ArrayList<String> BIChangeInfo = getLines(pathToBIChangeData, true);
		ArrayList<BIChange> biChanges = new ArrayList<BIChange>();
		for(String info: BIChangeInfo){
			biChanges.add(new BIChange(info,isNonSanitized));
		}
		return biChanges;
	}
	
	public static void writeAFile(String lines, String targetFileName){
		try {
			File file= new File(targetFileName);
			FileOutputStream fos = new FileOutputStream(file);
			DataOutputStream dos=new DataOutputStream(fos);
			
			dos.writeBytes(lines);
				
			dos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}
