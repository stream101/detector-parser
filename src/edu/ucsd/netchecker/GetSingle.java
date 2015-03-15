package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.gson.Gson;

import edu.ucsd.netchecker.AnalysisResults.APIStats;
import edu.ucsd.netchecker.AnalysisResults.APIType;
import edu.ucsd.netchecker.AnalysisResults.CheckRspStats;

public class GetSingle {
	String file;
	String target;
	int n_noAlertsInActivity;
	int n_alertsInActivity;
	int n_noAlertsInNonType;
	int n_alertsInNonType;
	int n_missRspCheckOutputs;
	int n_hasRspCheckOutputs;
	
	public GetSingle(String inputFile, String app) {
		this.file = inputFile;
		this.target = app;
	}

	int computeAlerts (TreeMap<String, HashSet<String>> map) {
		int i = 0;
		for (Entry<String, HashSet<String>> entry : map.entrySet()) {
			i += entry.getValue().size();
		}
		return i;
	}
	
	int computeRspCheck(HashMap<String, HashSet<CheckRspStats>> map) {
		int i = 0;
		for (Entry<String, HashSet<CheckRspStats>> entry : map.entrySet()) {
			i += entry.getValue().size();
		}
		return i;
	}
	void preProcessResult(AnalysisResults result) {
		n_noAlertsInActivity = computeAlerts(result.noAlertsInActivity);
	    n_alertsInActivity = computeAlerts(result.alertsInActivity);
		n_noAlertsInNonType = computeAlerts(result.noAlertsInNonType) ;
		n_alertsInNonType = computeAlerts(result.alertsInNonType);
		n_missRspCheckOutputs = computeRspCheck(result.missRspCheckOutputs);
		n_hasRspCheckOutputs = computeRspCheck(result.hasRspCheckOutputs);
	}
	
	void showAppDetail(AnalysisResults result) {
		preProcessResult(result);
		
		System.out.println("=====================");
		System.out.println("Results of App " + result.appName);
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
			
			if (stats.type==APIType.BOTH || stats.type == APIType.RETRY) {
				System.out.println("No retry in Activity path: " + stats.noRetryActivityPaths.size());
				for (ArrayList<String> path : stats.noRetryActivityPaths) {
					PathHelper.prettyPrint(path);
				}
				
				System.out.println("Over retry in Service path: " + stats.overRetryServicePaths.size());
				for (ArrayList<String> path : stats.overRetryServicePaths) {
					PathHelper.prettyPrint(path);
				}
				
				System.out.println("Over retry in Post path: " + stats.overRetryPostPaths.size());
				for (ArrayList<String> path : stats.overRetryPostPaths) {
					PathHelper.prettyPrint(path);
				}
			}
			
			System.out.println("---------------------");
		}
				
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
		System.out.println("Monitor connectivity: " + result.connReceivers.size());
		for (String s: result.connReceivers)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("No error message in Activity: " + this.n_noAlertsInActivity);
		for (Entry<String, HashSet<String>> entry : result.noAlertsInActivity.entrySet()) {
			System.out.println("\n" + entry.getKey());
			for (String s : entry.getValue()) {
				System.out.print("[ ");
				System.out.print(s + " , ");
				System.out.print(" ]");
			}
			System.out.println();
		}				
		System.out.println("---------------------");
		System.out.println("No error message in NonType: " + this.n_noAlertsInNonType);
		for (Entry<String, HashSet<String>> entry : result.noAlertsInNonType.entrySet()) {
			System.out.println("\n" + entry.getKey());
			for (String s : entry.getValue()) {
				System.out.print("[ ");
				System.out.print(s + " , ");
				System.out.print(" ]");
			}
			System.out.println();
		}
		System.out.println("---------------------");
		System.out.println("Has error message in NonType: " + this.n_alertsInNonType);
		for (Entry<String, HashSet<String>> entry : result.alertsInNonType.entrySet()) {
			System.out.println("\n" + entry.getKey());
			for (String s : entry.getValue()) {
				System.out.print("[ ");
				System.out.print(s + " , ");
				System.out.print(" ]");
			}
			System.out.println();
		}
		System.out.println("---------------------");
		System.out.println("Has error message in Activity: " + this.n_alertsInActivity);
		for (Entry<String, HashSet<String>> entry : result.alertsInActivity.entrySet()) {
			System.out.println("\n" + entry.getKey());
			for (String s : entry.getValue()) {
				System.out.print("[ ");
				System.out.print(s + " , ");
				System.out.print(" ]");
			}
			System.out.println();
		}
		System.out.println("---------------------");
		System.out.println("Sub volley error: " + result.hasSubErrorHandlers.size());
		for(String s : result.hasSubErrorHandlers) {
			System.out.println("\t" + s);
		}
		System.out.println("---------------------");
		System.out.println("No response check: " + this.n_missRspCheckOutputs);
		for (Entry<String, HashSet<CheckRspStats>> entry : result.missRspCheckOutputs.entrySet()) {
			String lib = entry.getKey();
			HashSet<CheckRspStats> statSet = entry.getValue();
			for (CheckRspStats stat : statSet) {
				System.out.println(lib + " , " + stat.type + " , " + stat.location);
			}
		}
		System.out.println("---------------------");
		System.out.println("Has response check: " + this.n_hasRspCheckOutputs);
		for (Entry<String, HashSet<CheckRspStats>> entry: result.hasRspCheckOutputs.entrySet()) {
			String lib = entry.getKey();
			HashSet<CheckRspStats> statSet = entry.getValue();
			for (CheckRspStats stat : statSet) {
				System.out.println(lib + " , " + stat.type + " , " + stat.location);
			}
		}
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
		System.out.println("Basic Info :");
		System.out.println("\nUsed libs: ");
		for (String lib : result.libUsed) 
			System.out.print(lib + " ,");
		System.out.println();
		
		System.out.println("\nNetwork callsites ");
		for (Entry<String,String> sinkEntry : result.sinks.entrySet())
			System.out.println(sinkEntry.getKey() + " <- " + sinkEntry.getValue());
		
		System.out.println("\nPost requests ");
		for (String post : result.postMethods)
			System.out.println(post);
		
		System.out.println("---------------------");
		System.out.println("Summary:\n");
		//Print out summary here
		boolean hasRetryAPI = false;
		int noRetryInActivity = 0, overRetryInService=0, overRetryInPost=0 ;
		for (Entry<String, APIStats> entry : result.APIOutputs.entrySet()) {
			String api = entry.getKey();
			APIStats stats = entry.getValue();
			System.out.println(api + " , miss " + stats.missedAPIPaths.size() + " , inovke " + stats.inovkedAPIPaths.size());
			if (stats.type == APIType.RETRY || stats.type == APIType.BOTH) {
				hasRetryAPI = true;		
				noRetryInActivity += stats.noRetryActivityPaths.size();
				overRetryInService += stats.overRetryServicePaths.size();
				overRetryInPost += stats.overRetryPostPaths.size();
			}
		}
		
		if (hasRetryAPI) {
			System.out.println("No retry in Activity: " + noRetryInActivity);
			System.out.println("Over retry in Service: " + overRetryInService);
			System.out.println("Over retry in Post: " + overRetryInPost);
		}
		System.out.println("No error message in Activity: " + n_noAlertsInActivity);
		System.out.println("Has error message in Activity: " + n_alertsInActivity);
		System.out.println("No error message in NonType: " + n_noAlertsInNonType);
		System.out.println("Has error message in NonType: " + n_alertsInNonType);
		System.out.println("Sub volley error: " + result.hasSubErrorHandlers.size() + " / " + result.noSubErrorHandlers.size());
		System.out.println("No response check: " + n_missRspCheckOutputs);
		System.out.println("Has response check: " + n_hasRspCheckOutputs);
		System.out.println("Monitor connectivity: " + result.connReceivers.size());
		System.out.println("Self retry: " + result.selfRetryMethods.size());
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
