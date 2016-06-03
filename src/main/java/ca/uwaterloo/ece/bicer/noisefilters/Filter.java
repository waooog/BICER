package ca.uwaterloo.ece.bicer.noisefilters;

public interface Filter {
	
	boolean filterOut();
	boolean isNoise();
	String getName();
}
