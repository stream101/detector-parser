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
	static boolean totalSummary = true;
	static String showApp="";
	
	private static void printUsage() {
		 System.out.println("Incorrect arguments: [0] = parsed-file");
	}
	
	static void parseAdditionalOptions(String[] args) {
		int i = 1;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("--all")) {
				totalSummary = true;
				i += 1;
			}
			else if (args[i].equalsIgnoreCase("--app")) {
				totalSummary = false;
				showApp = args[i+1];
			}
		}
	}
	
	public static void main(final String[] args) {
		
		if (args.length < 1) {
			printUsage();
			return;
		}
		
		parseAdditionalOptions(args);
		
		GetResult result = new GetResult(args[0]);
		result.setShowSummary(totalSummary);
		result.setTargetApp(showApp);
	    result.show();
		
	}
}
