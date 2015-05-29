package edu.ucsd.netchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;

import edu.ucsd.netchecker.AnalysisResults.APIType;
import edu.ucsd.netchecker.AnalysisResults.AvailAPIStats;
import edu.ucsd.netchecker.AnalysisResults.ConfigAPIStats;
import edu.ucsd.netchecker.AnalysisResults.RequestStats;


public class GetAllRequests {
	TreeMap<String, RequestStats> info;
	int nRequests = 0; //total num of requests
	int nMissAvailCheck = 0, nInvokeAvailCheck =0;  //total num of miss/invoke connectivity check
	int nMissTimeout=0, nInvokeTimeout=0;  //total num of miss/invoke timeout API
	int nMissRetry=0, nInvokeRetry=0;  //total num of miss/invoke retry API
	int nOverRetryService=0, nOverRetryPost=0;  //total num of over retry 
	boolean hasRetryAPI=false;    
	int nDefaultOverRetriesInService = 0, nDefaultOverRetriesInPost = 0; //total num of requests over retry due to default parameters
	int nSelfRetry = 0;
	TreeMap<String, InvokeMissPair> apiInvokeMissNumber = new TreeMap<String, InvokeMissPair>();
	//int appTotalMissTimeoutPaths=0, appTotalInvokeTimeoutPaths=0;  
	//int appTotalMissRetryPaths=0, appTotalInvokeRetryPaths=0; //every sink have timeout api, but may not have retry api
	//HashMap<String, Integer> visitedLib = new HashMap<String, Integer>();
	//HashMap<String, String> apiToLib = new HashMap<String, String>();
	
	public GetAllRequests(TreeMap<String, RequestStats> map) {
		info = map;
		//Init visited lib
		/*visitedLib.put("apache", 0);
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
		apiToLib.put("com.turbomanage", "basic");*/
		
	}
	
	
	public TreeMap<String, InvokeMissPair> getApiInvokeMissNumber() { return this.apiInvokeMissNumber; }
	/*void addToMap(String api, int invoked, int missed, TreeMap<String, Double> map) {
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
	*/
	/*void setAPILibVisited(String api) {
		for (Entry<String,Integer>entry : visitedLib.entrySet()) {
			if (api.contains(entry.getKey())) {
				 entry.setValue(1);
				 //this.apiLib = entry.getKey();
				 return;
			}
		}
	}*/
	
	void recordAPIInvokeTimes(String api) {
		if (!this.apiInvokeMissNumber.containsKey(api))
			apiInvokeMissNumber.put(api, new InvokeMissPair());
		
		InvokeMissPair pair = apiInvokeMissNumber.get(api);
		int nInvoke = pair.getInvokedCount() + 1;
		pair.setInvokeCount(nInvoke);
	}
	
	void recordAPIMissTimes(String api) {
		if (!this.apiInvokeMissNumber.containsKey(api))
			apiInvokeMissNumber.put(api, new InvokeMissPair());
		
		InvokeMissPair pair = apiInvokeMissNumber.get(api);
		int nMiss = pair.getMissedCount() + 1;
		pair.setMissedCount(nMiss);
	}
	
	/*
	 * Because one path can have multiple timeout/retry APIs, usually if one is missing,
	 * others will miss too. e.g. setSoTimeout and setReadTimeout usually appear together 
	 * or none. So To avoid duplicates, we use isAPILibVisited to record if corresponding lib's 
	 * paths have been already recorded. 
	 */
	void AnalyzeStats(RequestStats stats, String request) {
		this.nRequests += 1;
		
		AvailAPIStats avail = stats.getAvailCheckStat();
		this.nInvokeAvailCheck += avail.getInvokeCheckPaths().size(); 
		this.nMissAvailCheck += avail.getMissCheckPaths().size();
		
		HashSet<ConfigAPIStats> missedConfigs = stats.getMissedAPIs();
		for (ConfigAPIStats api : missedConfigs) {
			APIType type = api.getType();
			if (type.equals(APIType.RETRY) || type.equals(APIType.BOTH)) {
				this.nMissRetry += 1;
				this.hasRetryAPI = true;
			}
			if (type.equals(APIType.BOTH) || type.equals(APIType.TIMEOUT))
				this.nMissTimeout += 1;
			
			//Record invoke numbers of each API
			recordAPIInvokeTimes(api.getAPIName());
		}
		
		HashSet<ConfigAPIStats> invokedConfigs = stats.getInvokedAPIs();
		for (ConfigAPIStats api : invokedConfigs) {
			APIType type = api.getType();
			if (type.equals(APIType.RETRY) || type.equals(APIType.BOTH)) {
				this.nInvokeRetry += 1;
				this.hasRetryAPI = true;
			}
			if (type.equals(APIType.TIMEOUT) || type.equals(APIType.BOTH))
				this.nInvokeTimeout += 1;
			//Record miss numbers of each API
			recordAPIMissTimes(api.getAPIName());
		}
		
		
		
		HashMap<String, ArrayList<String>> overRetryInServie = stats.getOverRetryServiceMethodAndEntries();
		this.nOverRetryService += overRetryInServie.keySet().size();
		
		HashMap<String, ArrayList<String>> overRetryInPost = stats.getOverRetryPostMethodAndEntries();
		this.nOverRetryPost += overRetryInPost.keySet().size();
		
		this.nDefaultOverRetriesInService += stats.getNumDefaultOverRetryService();
		this.nDefaultOverRetriesInPost += stats.getNumDefaultOverRetryPost();
		
		this.nSelfRetry += stats.getSelfRetryMethods().size();
	}
	
	void compute() {
		for (Entry<String, RequestStats> entry : info.entrySet()) {
			RequestStats stats = entry.getValue();
			String request = entry.getKey();
			AnalyzeStats(stats, request);			
		}
	}
}
