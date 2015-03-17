package edu.ucsd.netchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.ucsd.netchecker.AnalysisResults.APIStats;
import edu.ucsd.netchecker.AnalysisResults.APIType;


public class GetAPIStats {
	TreeMap<String, APIStats> info;
	int missAvailCheck = 0,invokeAvailCheck =0;
	int missTimeout=0, invokeTimeout=0;
	int missRetry=0, invokeRetry=0;
	int noRetryActivity=0, overRetryService=0, overRetryPost=0;
	int unknownRetryService=0, unknownRetryPost=0;
	boolean hasRetryAPI=false;
	int manualSetOverRetriesInService = 0, defaultOverRetriesInService = 0;
	int manualSetOverRetriesInPost = 0, defaultOverRetriesInPost = 0;
	//ArrayList<ArrayList<String>> numEntryToSinkPaths = new ArrayList<ArrayList<String>>();
	//do not replicate paths of the same lib e.g. setReadTimeout and setConnectTimeout.
	int appTotalMissTimeoutPaths=0, appTotalInvokeTimeoutPaths=0;  
	int appTotalMissRetryPaths=0, appTotalInvokeRetryPaths=0; //every sink have timeout api, but may not have retry api
	HashMap<String, Integer> visitedLib = new HashMap<String, Integer>();
	HashMap<String, String> apiToLib = new HashMap<String, String>();
	
	public GetAPIStats(TreeMap<String, APIStats> map) {
		info = map;
		//Init visited lib
		visitedLib.put("apache", 0);
		visitedLib.put("HttpURLConnection", 0);
		visitedLib.put("volley", 0);
		visitedLib.put("okhttp", 0);
		visitedLib.put("loopj", 0);
		visitedLib.put("turbomanage", 0);
		
		//Init api to lib mapping
		apiToLib.put("org.apache.http", "apache");
		apiToLib.put("java.net.HttpURLConnection", "hurl");
		apiToLib.put("com.android.volley", "volley");
		apiToLib.put("com.squareup.okhttp", "okhttp");
		apiToLib.put("com.loopj","aah");
		apiToLib.put("com.turbomanage", "basic");
		
	}
	
	void addToMap(String api, int invoked, int missed, TreeMap<String, Double> map) {
		if (!map.containsKey(api))
			map.put(api, new Double(0));
		if (missed+invoked != 0) {
			double newValue = map.get(api).doubleValue() + (double)invoked/(missed+invoked);
			map.put(api, new Double(newValue));
			//System.out.println("put to map: " + api + " , " + newValue);
		}
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
	
	String getLibFromAPI(String api) {
		for (Entry<String,String>entry : apiToLib.entrySet()) {
			if (api.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	void setAPILibVisited(String api) {
		for (Entry<String,Integer>entry : visitedLib.entrySet()) {
			if (api.contains(entry.getKey())) {
				 entry.setValue(1);
				 //this.apiLib = entry.getKey();
				 return;
			}
		}
	}
	
	void getWrongRetryCauses(APIStats stats) {
		ArrayList<ArrayList<String>> missedAPIPaths = stats.missedAPIPaths;
		ArrayList<ArrayList<String>> invokedAPIPaths = stats.inovkedAPIPaths;
		ArrayList<ArrayList<String>> noRetryActivityPaths = stats.noRetryActivityPaths;
		ArrayList<ArrayList<String>> overRetryServicePaths = stats.overRetryServicePaths;
		ArrayList<ArrayList<String>> overRetryPostPaths = stats.overRetryPostPaths;
		
		for (ArrayList<String> path : overRetryServicePaths) {
			PathHelper.prettyPrint(path);
					
			if (PathHelper.pathContainedIn(path, missedAPIPaths)) {
				//System.out.println("Find over retry by default in serivce! ");
				this.defaultOverRetriesInService += 1;
			}
			else {
				//System.out.println("Find over retry manually set in serivce! ");
				this.manualSetOverRetriesInService += 1;
			}
		}
		
		for (ArrayList<String> path : overRetryPostPaths) {
			if (PathHelper.pathContainedIn(path, missedAPIPaths)) {
				//System.out.println("Find over retry by default in post! ");
				this.defaultOverRetriesInPost += 1;
			}
			else {
				//System.out.println("Find over retry manually set in post! ");
				this.manualSetOverRetriesInPost += 1;
			}
		}
	}
	/*
	 * Because one path can have multiple timeout/retry APIs, usually if one is missing,
	 * others will miss too. e.g. setSoTimeout and setReadTimeout usually appear together 
	 * or none. So To avoid duplicates, we use isAPILibVisited to record if corresponding lib's 
	 * paths have been already recorded. 
	 */
	void AnalyzeStats(APIStats stats, String api) {
		int miss = stats.missedAPIPaths.size();
		int invoke = stats.inovkedAPIPaths.size();
		//System.out.println("api " + api + " ,type " + stats.type);//xinxin.debug
		
		if (stats.type == APIType.TIMEOUT || stats.type == APIType.BOTH) {
			invokeTimeout += invoke;
			missTimeout += miss;
		    if (!isAPILibVisited(api)) {
		    	appTotalMissTimeoutPaths += miss;
		    	appTotalInvokeTimeoutPaths += invoke;
			}
		  //  addToMap(api, invoke, miss, this.invokeTimeoutAPIMap);
		}
		if (stats.type == APIType.RETRY || stats.type == APIType.BOTH) {		
			invokeRetry += invoke;
			missRetry +=  miss;
			if (!isAPILibVisited(api)) {
				appTotalMissRetryPaths += miss;
				appTotalInvokeRetryPaths += invoke;
			}
			this.hasRetryAPI = true;
			
			System.out.println("has retry api " + api);//xinxin.debug
			
			getWrongRetryCauses(stats);
		}
		if (stats.type == APIType.AVAIL) {
			invokeAvailCheck += invoke;
			missAvailCheck +=  miss;
		}
		
		noRetryActivity +=stats.noRetryActivityPaths.size();
		overRetryService += stats.overRetryServicePaths.size();
		overRetryPost += stats.overRetryPostPaths.size();
		unknownRetryService += stats.unKnownRetryServicePaths.size();
		unknownRetryPost += stats.unKnownRetryPostPaths.size();
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
