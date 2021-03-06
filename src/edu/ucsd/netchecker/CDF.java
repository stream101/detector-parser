package edu.ucsd.netchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

public class CDF {
	
	public static TreeMap<String, Double> plotDCDF(ArrayList<Double> list) {
		String[] xaxis = {"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0"};
		TreeMap<String,Integer> map = new TreeMap<String, Integer>();
		TreeMap<String,Double> cdf = new TreeMap<String, Double>();

		int total = 0;
		
		//initialize
		for (int i=0; i< 10; i++) {
			map.put(xaxis[i], 0);
			cdf.put(xaxis[i], 0.0);
		}
		
		for(double d : list) {
			if ( d <= 0.1) 
				map.put(xaxis[0], new Integer(map.get(xaxis[0]) + 1));
			else if ( d<= 0.2)
				map.put(xaxis[1], new Integer(map.get(xaxis[1]) + 1));
			else if (d <= 0.3)
				map.put(xaxis[2], new Integer(map.get(xaxis[2]) + 1));
			else if (d <= 0.4)
				map.put(xaxis[3], new Integer(map.get(xaxis[3]) + 1));
			else if (d <= 0.5)
				map.put(xaxis[4], new Integer(map.get(xaxis[4]) + 1));
			else if (d<=0.6)
				map.put(xaxis[5], new Integer(map.get(xaxis[5]) + 1));
			else if (d<=0.7)
				map.put(xaxis[6], new Integer(map.get(xaxis[6]) + 1));
			else if (d<=0.8)
				map.put(xaxis[7], new Integer(map.get(xaxis[7]) + 1));
			else if (d<=0.9)
				map.put(xaxis[8], new Integer(map.get(xaxis[8]) + 1));
			else if (d<=1)
				map.put(xaxis[9], new Integer(map.get(xaxis[9]) + 1));
			
			total += 1;
		}
		
		
		for (int i=1; i<xaxis.length; i++) {
			int origin = map.get(xaxis[i]);
			map.put(xaxis[i], new Integer(map.get(xaxis[i-1]) + origin));
		}
	
		for (int i=1; i<xaxis.length; i++) {
			cdf.put(xaxis[i], new Double((double)map.get(xaxis[i])/total));
		}
			
		return cdf;
	}
	
	public static TreeMap<Integer, Double> plotICDF(ArrayList<Integer> list) {
		int[] xaxis = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
		TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Double> cdf = new TreeMap<Integer, Double>();
		double total = 0;
		//initialize
		for (int i=0; i< 10; i++) {
			map.put(xaxis[i], 0);
			cdf.put(xaxis[i], 0.0);
		}
				
		for(int d : list) {
			if ( d <= xaxis[0]) 
				map.put(xaxis[0], new Integer(map.get(xaxis[0]) + 1));
			else if ( d<= xaxis[1])
				map.put(xaxis[1], new Integer(map.get(xaxis[1]) + 1));
			else if (d <= xaxis[2])
				map.put(xaxis[2], new Integer(map.get(xaxis[2]) + 1));
			else if (d <= xaxis[3])
				map.put(xaxis[3], new Integer(map.get(xaxis[3]) + 1));
			else if (d <= xaxis[4])
				map.put(xaxis[4], new Integer(map.get(xaxis[4]) + 1));
			else if (d <= xaxis[5])
				map.put(xaxis[5], new Integer(map.get(xaxis[5]) + 1));
			else if (d<=xaxis[6])
				map.put(xaxis[6], new Integer(map.get(xaxis[6]) + 1));
			else if (d<=xaxis[7])
				map.put(xaxis[7], new Integer(map.get(xaxis[7]) + 1));
			else if (d<=xaxis[8])
				map.put(xaxis[8], new Integer(map.get(xaxis[8]) + 1));
			else 
				map.put(xaxis[9], new Integer(map.get(xaxis[9]) + 1));
			
			total += 1;
		}
		
		for (int i=1; i<xaxis.length; i++) {
			int origin = map.get(xaxis[i]);
			map.put(xaxis[i], new Integer(map.get(xaxis[i-1]) + origin));
		}
		
		for (int i=1; i<xaxis.length; i++) {
			cdf.put(xaxis[i], new Double((double)map.get(xaxis[i])/total));
		}
		
		return cdf;
	}
}
