package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import soot.jimple.infoflow.android.analysis.result.AnalysisResults;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIStats;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIType;
import soot.jimple.infoflow.android.util.PathHelper;

import com.google.gson.Gson;

public class GetResult {
	String targetApp;
	boolean showSummary;
	String inputFile;	
	int missAvailTotal;
	int invokeAvailTotal;
	int missTimeoutTotal;
	int invokeTimeoutTotal;
	int missRetryTotal;
	int invokeRetryTotal;
	int noRetryActivityTotal;
	int overRetryServiceTotal;
	int overRetryPostTotal;
	int invokeRespCheckTotal;
	int missRespCheckTotal;
	int hasConnMonitorTotal;
	
	public GetResult(String inputFile) {this.inputFile = inputFile;}
	
	public void setShowSummary(boolean totalSummary) {	this.showSummary = totalSummary;}

	public void setTargetApp(String showApp) {this.targetApp = showApp;}
	
	class getAppStats {
		HashMap<String, APIStats> info;
		int missAvailCheck,invokeAvailCheck;
		int missTimeout, invokeTimeout;
		int missRetry, invokeRetry;
		int noRetryActivity, overRetryService, overRetryPost;
		
		getAppStats(HashMap<String, APIStats> map) {
			info = map;
		}
		
		void AnalyzeStats(APIStats stats) {
			int miss = stats.missedAPIPaths.size();
			int invoke = stats.inovkedAPIPaths.size();
			
			if (stats.type == APIType.TIMEOUT || stats.type == APIType.BOTH) {
				invokeRetry += invoke;
				missTimeout +=  miss;
			}
			else if (stats.type == APIType.RETRY) {
				invokeRetry += invoke;
				missRetry +=  miss;
			}
			else if (stats.type == APIType.AVAIL) {
				invokeAvailCheck += invoke;
				missAvailCheck +=  miss;
			}
			
			noRetryActivity +=stats.noRetryActivityPaths.size();
			overRetryService += stats.overRetryServicePaths.size();
			overRetryPost += stats.overRetryPostPaths.size();
		}
		
		void compute() {
			for (Entry<String, APIStats> entry : info.entrySet()) {
				APIStats stats = entry.getValue();
				AnalyzeStats(stats);			
			}
		}
		
	}
	
	void addToTotal(getAppStats stat, int invokeRespCheck, int missRespCheck) {
		this.missAvailTotal += stat.missAvailCheck;
		this.invokeAvailTotal += stat.invokeAvailCheck;
		this.missTimeoutTotal += stat.missTimeout;
		this.invokeTimeoutTotal += stat.invokeTimeout;
		this.missRetryTotal += stat.missRetry;
		this.invokeRetryTotal += stat.invokeRetry;
		this.noRetryActivityTotal += stat.noRetryActivity;
		this.overRetryServiceTotal += stat.overRetryService; 
		this.overRetryPostTotal += stat.overRetryPost;
		this.invokeRespCheckTotal += invokeRespCheck;
		this.missRespCheckTotal += missRespCheck;
	}
	
	void showStatsOfOne(AnalysisResults result) {
		String appName = result.appName;
		String apkLocation = result.apkFile;
		HashMap<String, APIStats> map = result.getAPIUsages();
		getAppStats stat = new getAppStats(map);
		stat.compute();
		int invokeRespCheck = result.hasRespCheckPaths.size();
		int missRespCheck = result.noRespCheckPaths.size();
		int sinks = result.sinks.size();
		int posts = result.postMethods.size();
		
		
		System.out.format("%s, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d\n",
										    appName, 
											stat.missAvailCheck,stat.invokeAvailCheck, 
											stat.missTimeout, stat.invokeTimeout,
											stat.missRetry, stat.invokeRetry,
											stat.noRetryActivity, stat.overRetryService, stat.overRetryPost,
											missRespCheck,invokeRespCheck, sinks, posts);
		
		addToTotal(stat, invokeRespCheck, missRespCheck);
		
	}
	
	void showStatsOfAll() {
		System.out.format("%s; %d; %d; %d; %d; %d; %d; %d; %d; %d; %d; %d;%d\n",
				"Total", 
				this.missAvailTotal,this.invokeAvailTotal, 
				this.missTimeoutTotal, this.invokeTimeoutTotal,
				this.missRetryTotal, this.invokeRetryTotal,
				this.noRetryActivityTotal, this.overRetryServiceTotal, this.overRetryPostTotal,
				this.missRespCheckTotal, this.invokeRespCheckTotal, this.hasConnMonitorTotal);
	}

	void showTotalStats (File file) {
		System.out.println("mAvl;iAvl;mTime;iTime;mRetr;iRetr;NRA;ORS;ORP;mRsp;iRsp;Mon;#Sink;#Posts");
		try {
			
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				//System.out.println("read result: "+obj.noRespCheckPaths.toString());
				showStatsOfOne(obj);
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			System.err.print("Cannot fine file " + file.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		showStatsOfAll();
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
		System.out.println("---------------------");
		System.out.println("No Alert failure: ");
		for (String s : result.noAlertFailures)
			System.out.println(s);
		System.out.println("---------------------");
		System.out.println("Has Alert failure: ");
		for (String s : result.hasAlertFailures)
			System.out.println(s);
		System.out.println("---------------------");
	}
	
	void showOneApp(File file, String target) {
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
		File file = new File(inputFile);
		if(showSummary)
			showTotalStats(file);
		else {
			System.out.println(" app " + this.targetApp);
			showOneApp(file, this.targetApp );
		}
	}

}
