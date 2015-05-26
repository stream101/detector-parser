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

import edu.ucsd.netchecker.AnalysisResults.APIType;
import edu.ucsd.netchecker.AnalysisResults.AvailAPIStats;
import edu.ucsd.netchecker.AnalysisResults.CheckRspStats;
import edu.ucsd.netchecker.AnalysisResults.ConfigAPIStats;
import edu.ucsd.netchecker.AnalysisResults.RequestStats;


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
		boolean hasRetryAPI = false;
		int n_overRetryInService=0, n_overRetryInPost=0 ;
		int n_availInvoke = 0, n_availMiss = 0;
		int n_timeoutInvoke = 0, n_timeoutMiss = 0, n_retryInvoke = 0, n_retryMiss =0;
		int n_selfRetry = 0;
		
		preProcessResult(result);
		System.out.println("=====================");
		System.out.println("Results of App " + result.appName);
		System.out.println("---------------------");
		for (Entry<String, RequestStats> entry: result.requestOutputs.entrySet()){
			String request = entry.getKey();
			RequestStats stats = entry.getValue();
			System.out.println(request);
			
			//print connectivity check status
			AvailAPIStats avail = stats.getAvailCheckStat();
			System.out.println("\nAVAIL API: " + "miss " + avail.getMissCheckPaths().size() + " , invoke " + avail.getInvokeCheckLocations().size());
			if (avail.getMissCheckPaths().size() > 0) {
				n_availMiss += avail.getMissCheckPaths().size() ;
				for (ArrayList<String> p : avail.getMissCheckPaths()) {
					System.out.println("AVAIL missed path: ");
					PathHelper.prettyPrint(p);
				}
			}
			
			if (avail.getInvokeCheckPaths().size()>0) {
				n_availInvoke += avail.getInvokeCheckPaths().size();
				for (String s : avail.getInvokeCheckLocations())
					System.out.println("AVAIL invoked location: " + s);
				
				for (ArrayList<String> p : avail.getInvokeCheckPaths()) {
					System.out.println("AVAIL invoked path: ");
					PathHelper.prettyPrint(p);
				}
			}
			
			//print config API status
			if (stats.getMissedAPIs().size()>0) {
				System.out.println("\nMissed APIs : ");
				for (ConfigAPIStats c : stats.getMissedAPIs()) {
					System.out.println(c.getAPIName());
					if (c.getType().equals(APIType.BOTH) || c.getType().equals(APIType.RETRY)) {
						n_retryMiss += 1;
						hasRetryAPI = true;
					}
					else if (c.getType().equals(APIType.TIMEOUT))
						n_timeoutMiss += 1;
				}
			}
			
			if (stats.getInvokedAPIs().size()>0) {
				System.out.println("\nInvoked APIs :");
				for(ConfigAPIStats c: stats.getInvokedAPIs()) {
					System.out.println(c.getAPIName());
					if (c.getType().equals(APIType.BOTH) || c.getType().equals(APIType.RETRY)) {
						n_retryInvoke += 1;
						hasRetryAPI = true;
					}
					else if (c.getType().equals(APIType.TIMEOUT))
						n_timeoutInvoke += 1;
				}
			}
			
		
			if (stats.getOverRetryServiceMethodAndEntries().keySet().size() >0){
				n_overRetryInService += stats.getOverRetryServiceMethodAndEntries().keySet().size();
				System.out.println("\nOver retry in service : ");
				for (Entry<String, ArrayList<String>> e : stats.getOverRetryServiceMethodAndEntries().entrySet()) {
					System.out.println("retry method: " + e.getKey());
					System.out.println("associated entry points: ");
					for (String s : e.getValue())
						System.out.print(s);																																			
					System.out.println();
				}
			}
					
			if(stats.getOverRetryPostMethodAndEntries().keySet().size()>0){
				n_overRetryInPost += stats.getOverRetryPostMethodAndEntries().keySet().size();
				System.out.println("\nOver retry in post : ");
				for (Entry<String, ArrayList<String>> e : stats.getOverRetryPostMethodAndEntries().entrySet()){
					System.out.println("retry method: " + e.getKey());
					System.out.println("associated entry points: ");
					for (String s : e.getValue())
						System.out.print(s);																																				
					System.out.println();
				}
			}
			
			if (stats.getSelfRetryMethods().size()>0) {
				n_selfRetry += stats.getSelfRetryMethods().size();
				System.out.println("self retry method: ");
				for (String s : stats.getSelfRetryMethods()) 
					System.out.println(s);
			}
			
			System.out.println("\npropagate path: ");
			PathHelper.prettyPrint(stats.getPropagatePath());
			
			System.out.println("---------------------");
		}	
		
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
		System.out.println("Basic Info :");
		System.out.println("\nUsed libs: ");
		for (String lib : result.libUsed) 
			System.out.print(lib + " ,");
		System.out.println();
		
		System.out.println("---------------------");
		System.out.println("Summary:\n");
		//Print out summary here
		
		//int unknownRetryInService=0, unknownRetryInPost=0;
		System.out.println("Avail check API: miss " + n_availMiss  + " , invoke " + n_availInvoke);
		System.out.println("Timeout API: miss " + n_timeoutMiss + " , invoke " + n_timeoutInvoke);
		System.out.println("Retry API: miss " + n_retryMiss + " , invoke " + n_retryInvoke);
		
		if (hasRetryAPI) {
			System.out.println("Over retry in Service: " + n_overRetryInService);
			System.out.println("Over retry in Post: " + n_overRetryInPost);
		}
		System.out.println("Error message in Activity: miss " + n_noAlertsInActivity + " , invoke " + n_alertsInActivity);
		System.out.println("Sub volley error: miss " + result.hasSubErrorHandlers.size() + " , invoke " + result.noSubErrorHandlers.size());
		System.out.println("No response check: miss " + n_missRspCheckOutputs + " , invoke " + n_hasRspCheckOutputs);
		System.out.println("Self retry: " +  n_selfRetry);
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
