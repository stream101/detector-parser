package edu.ucsd.netchecker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import soot.jimple.infoflow.android.analysis.result.AnalysisResults;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIStats;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIType;

import com.google.gson.Gson;


public class Parser {
	static boolean totalSummary = false;
	static String showApp="";
	static boolean showLib = false; 
	
	private static void printUsage() {
		 System.out.println("Usage: [0] = parsed-file");
		 System.out.println("--all: Show total stats");
		 System.out.println("--app: Show one app stats");
		 System.out.println("--libs: Show lib usage distribution");
	}
	
	static void parseAdditionalOptions(String[] args) {
		int i = 1;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("--all")) {
				totalSummary = true;
				i += 1;
			}
			else if (args[i].equalsIgnoreCase("--app")) {
				showApp = args[i+1];
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--libs")) {
				showLib = true;
				i += 1;
			}
		}
	}
	
	public static void main(final String[] args) {
		
		if (args.length < 2) {
			printUsage();
			return;
		}
		
		parseAdditionalOptions(args);
		
		
	
	   
	    if (totalSummary ) {
	    	GetTotal result = new GetTotal(args[0]);
	    	result.show();
	    }
	    if (showLib) {
	    	GetLibs result = new GetLibs(args[0]);
	    	result.show();
	    }
	    if (showApp != "") {
	    	GetSingle result = new GetSingle(args[0], showApp);
	    	result.show();
	    }
		
	}
}
