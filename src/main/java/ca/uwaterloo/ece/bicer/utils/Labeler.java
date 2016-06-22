package ca.uwaterloo.ece.bicer.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ca.uwaterloo.ece.bicer.data.BIChange;
import weka.core.Instance;
import weka.core.Instances;

public class Labeler {
	
	static public void relabelArff(String pathToArff,String classAttributeName,String positiveLabel,
									String pathToChangeIDSha1Pair,
									String pathToBIChanges,String pathToNewArff,
									String startDate,String endDate,String lastDateForFixCollection){
		
		// load arff
		Instances instances = loadArff(pathToArff, classAttributeName);
		
		// load changd_id and sha1 pair
		HashMap<String,String> sha1sbyChangeIDs = getSha1sByChangeIDs(pathToChangeIDSha1Pair);
		
		// load BIChanges
		ArrayList<BIChange> biChanges = Utils.loadBIChanges(pathToBIChanges);
		HashMap<String,ArrayList<BIChange>> biChangesByKey = getHashMapForBIChangesByKey(biChanges); // key: biSha1+biPath
		
		// relabel
		for(Instance instance:instances){
			String changeID = instance.stringValue(instances.attribute("change_id"));
			String sha1 = sha1sbyChangeIDs.get(changeID);
			
			String key = changeID + sha1;
			
			String newLabel = getNewLabel(key,startDate,endDate,lastDateForFixCollection,biChangesByKey);
		}
		
	}
	
	private static String getNewLabel(String key, String startDate, String endDate, String lastDateForFixCollection,
			HashMap<String, ArrayList<BIChange>> biChangesByKey) {
		
		String newLabel = "0"; // 0: clean 1: buggy
		
		for(BIChange biChange:biChangesByKey.get(key)){
			if(biChange.getFixDate().compareTo(lastDateForFixCollection)<0)
				continue;
			if(!(startDate.compareTo(biChange.getBIDate()) <= 0 && 0 <= biChange.getBISha1().compareTo(endDate)))
				continue;
			
			// biChange is now valid for a buggy label
			
				
		}
		
		return newLabel;
	}

	private static HashMap<String, String> getSha1sByChangeIDs(String pathToChangeIDSha1Pair) {
		
		HashMap<String, String> sha1sByChangeIDs = new HashMap<String, String>();
		
		// load a file
		ArrayList<String> lines = Utils.getLines(pathToChangeIDSha1Pair, false);
		
		for(String line:lines){
			String[] splitLine = line.split(","); // 0: change_id 1: sha1
			sha1sByChangeIDs.put(splitLine[0], splitLine[1]);
		}
		
		return sha1sByChangeIDs;
	}

	private static HashMap<String,ArrayList<BIChange>> getHashMapForBIChangesByKey(ArrayList<BIChange> biChanges) {
		
		HashMap<String,ArrayList<BIChange>> biChangesByKey = new HashMap<String,ArrayList<BIChange>>(); // key: biSha1+biPath
		
		for(BIChange biChange: biChanges){
			String key = biChange.getBISha1() + biChange.getBIPath();
			
			if(biChangesByKey.containsKey(key)){
				biChangesByKey.get(key).add(biChange);
			} else{
				biChangesByKey.put(key, new ArrayList<BIChange>());
				biChangesByKey.get(key).add(biChange);
			}
			
		}
		
		return biChangesByKey;
	}

	/**
	 * Load Instances from arff file. Last attribute will be set as class attribute
	 * @param path arff file path
	 * @return Instances
	 */
	public static Instances loadArff(String path,String classAttributeName){
		Instances instances=null;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(path));
			instances = new Instances(reader);
			reader.close();
			instances.setClassIndex(instances.attribute(classAttributeName).index());
		} catch (NullPointerException e) {
			System.err.println("Class label name, " + classAttributeName + ", does not exist! Please, check if the label name is correct.");
			instances = null;
		} catch (FileNotFoundException e) {
			System.err.println("Data file, " +path + ", does not exist. Please, check the path again!");
		} catch (IOException e) {
			System.err.println("I/O error! Please, try again!");
		}

		return instances;
	}

}
