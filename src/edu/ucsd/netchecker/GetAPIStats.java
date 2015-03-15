package edu.ucsd.netchecker;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.ucsd.netchecker.AnalysisResults.APIStats;
import edu.ucsd.netchecker.AnalysisResults.APIType;


public class GetAPIStats {
	TreeMap<String, APIStats> info;
	int missAvailCheck,invokeAvailCheck;
	int missTimeout, invokeTimeout;
	int missRetry, invokeRetry;
	int noRetryActivity, overRetryService, overRetryPost;
	//ArrayList<ArrayList<String>> numEntryToSinkPaths = new ArrayList<ArrayList<String>>();
	//do not replicate paths of the same lib e.g. setReadTimeout and setConnectTimeout.
	int appTotalMissTimeoutPaths;  
	int appTotalMissRetryPaths, appTotalInvokeRetryPaths; //every sink have timeout api, but may not have retry api
	HashMap<String, Integer> visitedLib = new HashMap<String, Integer>();
	
	public GetAPIStats(TreeMap<String, APIStats> map) {
		info = map;
		visitedLib.put("apache", 0);
		visitedLib.put("HttpURLConnection", 0);
		visitedLib.put("volley", 0);
		visitedLib.put("okhttp", 0);
		visitedLib.put("loopj", 0);
		visitedLib.put("turbomanage", 0);
	}
	
	boolean isAPILibVisited(String api) {
		for (Entry<String,Integer>entry : visitedLib.entrySet()) {
			if (api.contains(entry.getKey())) {
				if (entry.getValue() != 0) {
					return true;	
				}
			}
		}
	   return false;
	}
	
	void setAPILibVisited(String api) {
		for (Entry<String,Integer>entry : visitedLib.entrySet()) {
			if (api.contains(entry.getKey())) {
				 entry.setValue(1);
				 return;
			}
		}
	}
	
	void AnalyzeStats(APIStats stats, String api) {
		int miss = stats.missedAPIPaths.size();
		int invoke = stats.inovkedAPIPaths.size();
		
		if (stats.type == APIType.TIMEOUT || stats.type == APIType.BOTH) {
			invokeTimeout += invoke;
			missTimeout += miss;
		    if (!isAPILibVisited(api)) {
		    	appTotalMissTimeoutPaths += miss;
			}
		}
		else if (stats.type == APIType.RETRY || stats.type == APIType.BOTH) {
			invokeRetry += invoke;
			missRetry +=  miss;
			if (!isAPILibVisited(api)) {
				appTotalMissRetryPaths += miss;
				appTotalInvokeRetryPaths += invoke;
			}
		}
		else if (stats.type == APIType.AVAIL) {
			invokeAvailCheck += invoke;
			missAvailCheck +=  miss;
		}
		
		noRetryActivity +=stats.noRetryActivityPaths.size();
		overRetryService += stats.overRetryServicePaths.size();
		overRetryPost += stats.overRetryPostPaths.size();
			
		setAPILibVisited(api);
	}
	
	void compute() {
		for (Entry<String, APIStats> entry : info.entrySet()) {
			APIStats stats = entry.getValue();
			String api = entry.getKey();
			AnalyzeStats(stats, api);			
		}
	}
}
