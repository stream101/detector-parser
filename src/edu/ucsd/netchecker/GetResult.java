package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map.Entry;

import soot.jimple.infoflow.android.analysis.result.AnalysisResults;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIStats;
import soot.jimple.infoflow.android.analysis.result.AnalysisResults.APIType;

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
	
	
	public GetResult(String inputFile) {
		this.inputFile = inputFile;
	}
	
	public void setShowSummary(boolean totalSummary) {
		this.showSummary = totalSummary;
		
	}

	public void setTargetApp(String showApp) {
		this.targetApp = showApp;
	}
	
	/*static void printResult() {
		System.out.format("Miss avail. check, %d/%d\n", missAvailCheck, (missAvailCheck+hasAvailCheck));
		System.out.format("Miss setting timeout, %d/%d\n", numMissedTimeout, (numInvokedTimeout + numMissedTimeout));
		System.out.format("Miss setting retry, %d/%d\n", numMissedRetry, (numInvokedRetry + numMissedRetry));
		System.out.format("Miss null response check, %d/%d\n", noNullRespCheck, (nullRespCheck + noNullRespCheck));
		System.out.format("No retry in activity, %d\n", noRetryInActivity);
		System.out.format("Over retry in Service, %d\n", overRetryInService);
		System.out.format("Over retry in Post, %d\n", overRetryInPost);
		System.out.format("Monitor conn change, %d\n", hasConnMonitor);
	}*/
	
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
	
	void addToTotal(getAppStats stat, int invokeRespCheck, int missRespCheck, int hasConnMonitor) {
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
		this.hasConnMonitorTotal += hasConnMonitor;
	}
	
	void showStatsOfOne(AnalysisResults result) {
		String appName = result.appName;
		String apkLocation = result.apkFile;
		HashMap<String, APIStats> map = result.getAPIUsages();
		getAppStats stat = new getAppStats(map);
		stat.compute();
		int invokeRespCheck = result.hasRespCheckPaths.size();
		int missRespCheck = result.noRespCheckPaths.size();
		int hasConnMonitor = (result.hasConnChangeMonitor? 1: 0);
		
		System.out.format("%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
											apkLocation, appName, 
											stat.missAvailCheck,stat.invokeAvailCheck, 
											stat.missTimeout, stat.invokeTimeout,
											stat.missRetry, stat.invokeRetry,
											stat.noRetryActivity, stat.overRetryService, stat.overRetryPost,
											invokeRespCheck, missRespCheck, hasConnMonitor);
		
		addToTotal(stat, invokeRespCheck, missRespCheck, hasConnMonitor);
		
	}
	
	void showStatsOfAll() {
		System.out.format("%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
				"Total", "", 
				this.missAvailTotal,this.invokeAvailTotal, 
				this.missTimeoutTotal, this.invokeTimeoutTotal,
				this.missRetryTotal, this.invokeRetryTotal,
				this.noRetryActivityTotal, this.overRetryServiceTotal, this.overRetryPostTotal,
				this.invokeRespCheckTotal, this.missRespCheckTotal, this.hasConnMonitorTotal);
	}

	void showTotalStats (File file) {
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
	
	
	public void show() {
		File file = new File(inputFile);
		if(showSummary)
			showTotalStats(file);
		else {
			
		}
	}

}
