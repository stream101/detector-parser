package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.gson.Gson;

import edu.ucsd.netchecker.AnalysisResults.APIStats;
import edu.ucsd.netchecker.AnalysisResults.APIType;

public class GetSingle {
	String file;
	String target;
	public GetSingle(String inputFile, String app) {
		this.file = inputFile;
		this.target = app;
	}

void showAppDetail(AnalysisResults result) {
		
		System.out.println("=====================");
		System.out.println("Results of App " + result.appName);
		System.out.println("---------------------");
		System.out.println("Basic Info :");
		System.out.println("\nUsed libs: ");
		for (String lib : result.libUsed) 
			System.out.print(lib + " ,");
		System.out.println();
		
		System.out.println("\nNetwork callsites ");
		for (String sink : result.sinks)
			System.out.println(sink);
		
		System.out.println("\nPost requests ");
		for (String post : result.postMethods)
			System.out.println(post);
		
		System.out.println("---------------------");
		for (Entry<String, APIStats> entry : result.APIOutputs.entrySet()) {
			String api = entry.getKey();
			APIStats stats = entry.getValue();
			System.out.println(api);
			System.out.println("Type: " + stats.type);
			
			System.out.println("Missed path: " + stats.missedAPIPaths.size());
			for (ArrayList<String> path : stats.missedAPIPaths) {
				PathHelper.prettyPrint(path);
			}
			
			System.out.println("Invoked path: " + stats.inovkedAPIPaths.size());
			for (ArrayList<String> path : stats.inovkedAPIPaths) {
				PathHelper.prettyPrint(path);
			}
			
			System.out.println("No entry path: " + stats.noEntryPaths.size());
			for (ArrayList<String> path : stats.noEntryPaths) {
				PathHelper.prettyPrint(path);
			}
			
			System.out.println("No Activty/Service path: " + stats.noSensitiveTypePaths.size());
			for (ArrayList<String> path : stats.noSensitiveTypePaths) {
				PathHelper.prettyPrint(path);
			}
			
			if (stats.type == APIType.RETRY || stats.type == APIType.BOTH) {
				System.out.println("No retry in Activity: " + stats.noRetryActivityPaths.size());
				System.out.println("Over retry in Service: " + stats.overRetryServicePaths.size());
				System.out.println("Over retry in Post: " + stats.overRetryPostPaths.size());
			}
			
			System.out.println("---------------------");
		}
		
		System.out.println("Incomplete paths: " + result.incompletePaths.size());
		for (ArrayList<String> path : result.incompletePaths) {
			PathHelper.prettyPrint(path);
		}
		System.out.println("---------------------");
		
		System.out.println("Miss Resp null check: " + result.noRespCheckPaths.size());
		for (ArrayList<String> path : result.noRespCheckPaths) {
			PathHelper.prettyPrint(path);
		}
		System.out.println("---------------------");
		
		System.out.println("Has Resp null check: " + result.hasRespCheckPaths.size());
		for (ArrayList<String> path : result.hasRespCheckPaths) {
			PathHelper.prettyPrint(path);
		}
		System.out.println("---------------------");
		System.out.println("Monitor connectivity change: " + result.connReceivers.size());
		for (String s : result.connReceivers)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("No error message in Activity: " + result.noAlertsInActivity.size());
		for (String s : result.noAlertsInActivity)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("No error message in NonType: " + result.noAlertsInNonType.size());
		for (String s : result.noAlertsInNonType)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("Has error message in Activity: " + result.alertsInActivity.size());
		for (String s : result.alertsInActivity)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("All error callbacks:");
		for (Entry<String, HashSet<String>> entry : result.errorCallbacks.entrySet()) {
			System.out.println("\n" + entry.getKey());
			for (String s : entry.getValue()) {
				System.out.println("\t" + s);
			}
		}
		System.out.println("---------------------");
		System.out.println("Self retry: " + result.selfRetryMethods.size());
		for (String s : result.selfRetryMethods)
			System.out.println(s);
		System.out.println("---------------------");
	}
	
	void showOneApp() {
		try {	
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				if (obj.appName.equals(target)) {
					showAppDetail(obj);
					break;
				}
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			System.err.print("Cannot find file " + file.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void show() {
		showOneApp();
	}
	
}
