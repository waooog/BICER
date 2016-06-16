package ca.uwaterloo.ece.bicer.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.bicer.data.BIChange;

public class Sanitizer {

	ArrayList<BIChange> biChanges;
	Git git;
	Repository repo;
	
	public void sanitizer(String pathToBIChangeData,String gitURI){
		try {
			git = Git.open( new File( gitURI) );
			repo = git.getRepository();

		} catch (IOException e) {
			System.err.println("Repository does not exist: " + gitURI);
		}
		
		loadBIChanges(pathToBIChangeData);
		
		sanitizer();		
	}
	
	private void loadBIChanges(String pathToBIChangeData) {
		ArrayList<String> BIChangeInfo = Utils.getLines(pathToBIChangeData, true);
		biChanges = new ArrayList<BIChange>();
		for(String info: BIChangeInfo){
			biChanges.add(new BIChange(info));
		}
	}

	private void sanitizer() {
		for(BIChange biChange:biChanges){
			biChange = Utils.getBIChangeWithCorrectBISha1(git,biChange);
		}
	}

}
