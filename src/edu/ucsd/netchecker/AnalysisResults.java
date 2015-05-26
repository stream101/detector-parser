package edu.ucsd.netchecker;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


/*
 * Output for one application: 
 * (1) connectivity check
 * If all request-entry paths have check or miss check, only output 1 error; 
 * otherwise, output 1 error for each different path. Also record 1 for each correct path 
 * (2) missing apis
 * For each *request*, output 1 error for each missing api 
 * (3) over retry 
 * For each *request*, at most count 1 over retry error in service/post. 
 * Each error is related with multiple reachable entry points
 * (4) No failure notification 
 * For each request callback, output 1 error if no failure notification; 
 * For volley request callback, output 1 error if not using suberror type
 * (5) missing response check
 * For every *response*, output 1 error for each missing response check api
 * (6) self-implemented retry 
 * For each request, output 1 if it contains self-retry
*/
public class AnalysisResults {
	public String appName;
	public String apkFile;
	TreeMap<String, RequestStats> requestOutputs = new TreeMap<String, RequestStats>();
	public ArrayList<ArrayList<String>> noRespCheckPaths = new ArrayList<ArrayList<String>>();
	public ArrayList<ArrayList<String>> hasRespCheckPaths = new ArrayList<ArrayList<String>>();
	public HashMap<String, String> sinks = new HashMap<String, String>();
	public HashSet<String> postMethods = new HashSet<String>();
	public HashSet<String> libUsed = new HashSet<String>();
	public TreeMap<String, HashSet<String>> alertsInActivity = new TreeMap<String,HashSet<String>>();
	public TreeMap<String, HashSet<String>> noAlertsInActivity = new TreeMap<String, HashSet<String>>();
	public HashMap<String, HashSet<String>> errorCallbacks = new HashMap<String, HashSet<String>>();//callback and reachable entries  
	public TreeMap<String, HashSet<String>>  noAlertsInNonType = new TreeMap<String, HashSet<String>> ();
	public TreeMap<String, HashSet<String>>  alertsInNonType = new TreeMap<String, HashSet<String>> ();
	public HashSet<CheckRspStats> checkRspResults = new HashSet<CheckRspStats>();
    public HashMap<String, HashSet<CheckRspStats>> hasRspCheckOutputs = new HashMap<String, HashSet<CheckRspStats>>();
	public HashMap<String, HashSet<CheckRspStats>> missRspCheckOutputs = new HashMap<String, HashSet<CheckRspStats>>();
	public ArrayList<String> hasSubErrorHandlers = new ArrayList<String>();
	public ArrayList<String> noSubErrorHandlers = new ArrayList<String>();
	
	public AnalysisResults(String apkFileLocation, String appPackageName) {
		appName = appPackageName;
		apkFile = apkFileLocation;
	}
	
	public enum APIType {
		NONE,
		AVAIL,
		TIMEOUT,
		RETRY,
		BOTH
	}
	
	public enum CheckRspType {
		NULL,
		STATUS,
	}

	public class RequestStats {
		HashSet<ConfigAPIStats> missedAPIs = new HashSet<ConfigAPIStats>();
		HashSet<ConfigAPIStats> invokedAPIs = new HashSet<ConfigAPIStats>();
		AvailAPIStats availCheckStat = new AvailAPIStats();
		HashMap<String, ArrayList<String>> overRetryServiceMethodAndEntries = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> overRetryPostMethodAndEntries = new HashMap<String, ArrayList<String>>();
		HashSet<String> selfRetryMethods = new HashSet<String>();
		ArrayList<String> propagatePath = new ArrayList<String>();
		int nDefaultOverRetryService = 0;
		int nDefaultOverRetryPost = 0;
		
		public void addMissedAPI(ConfigAPIStats a) { missedAPIs.add(a); }
		public void addInvokedAPI(ConfigAPIStats a) { invokedAPIs.add(a); }
		public void addOverRetryServiceMethodAndEntry(String declaredMethod, String entry) { 
			if (!this.overRetryServiceMethodAndEntries.keySet().contains(declaredMethod)) 
				this.overRetryServiceMethodAndEntries.put(declaredMethod, new ArrayList<String>());
			ArrayList<String> v = this.overRetryServiceMethodAndEntries.get(declaredMethod);
			v.add(entry);
			this.overRetryServiceMethodAndEntries.put(declaredMethod, v);
		} 
		
		public void addOverRetryPostMethodAndEntry(String declaredMethod, String entry) {
			if (!this.overRetryPostMethodAndEntries.keySet().contains(declaredMethod)) 
				this.overRetryPostMethodAndEntries.put(declaredMethod, new ArrayList<String>());
			ArrayList<String> v = this.overRetryPostMethodAndEntries.get(declaredMethod);
			v.add(entry);
			this.overRetryPostMethodAndEntries.put(declaredMethod, v);
		}
		
		public void addSelfRetryMethod(String s) { this.selfRetryMethods.add(s);}
		public void setPropagatePath(ArrayList<String> p) { this.propagatePath = p; }
		
		public AvailAPIStats getAvailCheckStat() { return this.availCheckStat; }
		public HashSet<ConfigAPIStats> getMissedAPIs() { return this.missedAPIs; }
		public HashSet<ConfigAPIStats> getInvokedAPIs() { return this.invokedAPIs; }
		public HashMap<String, ArrayList<String>> getOverRetryServiceMethodAndEntries () {return this.overRetryServiceMethodAndEntries;}
		public HashMap<String, ArrayList<String>> getOverRetryPostMethodAndEntries() {return this.overRetryPostMethodAndEntries;}
		public HashSet<String> getSelfRetryMethods() {return this.selfRetryMethods;}
		public ArrayList<String> getPropagatePath() { return this.propagatePath; } 
		public int getNumDefaultOverRetryService() { return this.nDefaultOverRetryService; }
		public int getNumDefaultOverRetryPost() { return this.nDefaultOverRetryPost; }
	}
	
	public class AvailAPIStats {
		//May have different paths. if all paths have check or miss check, only output one
		//otherwise, output different paths
		ArrayList<ArrayList<String>> invokeCheckPaths;
		ArrayList<ArrayList<String>> missCheckPaths;
		ArrayList<String> invokeCheckLocations;
		
		public void setInvokeCheckPaths(ArrayList<ArrayList<String>> p) {this.invokeCheckPaths = p;}
		public void setMissCheckPaths(ArrayList<ArrayList<String>> p) {this.missCheckPaths = p;}
		public void setInvokeCheckLocations(ArrayList<String> p) { this.invokeCheckLocations = p;}
		
		public ArrayList<ArrayList<String>>  getInvokeCheckPaths() {return this.invokeCheckPaths;}
		public ArrayList<ArrayList<String>> getMissCheckPaths() { return this.missCheckPaths; }
		public ArrayList<String> getInvokeCheckLocations() { return this.invokeCheckLocations; }
	}
	
	public class ConfigAPIStats {
		private String api;
		private APIType type = APIType.NONE;
		private int retryIdx = -1;
		
		public ConfigAPIStats(String s) {
			api = s;
		}
		
		public String getAPIName() { return api; }
		public APIType getType() { return this.type; }
		public int getRetryIdx () {return this.retryIdx;}
	}
	
	public void addRequest(String request) {
		if (!requestOutputs.containsKey(request))
			requestOutputs.put(request, new RequestStats());
	}
	
	public TreeMap<String, RequestStats>  getRequestStats() { return this.requestOutputs; }
	
	public class CheckRspStats {
		public CheckRspStats( CheckRspType type, String callsite) {
			this.type = type;
			this.location = callsite;
		}
		//String lib;
		CheckRspType type;
		String location;
	}
	
	public void addHasRspCheckStat (String lib, CheckRspType type, String callsite) {
		if (!hasRspCheckOutputs.containsKey(lib))
			hasRspCheckOutputs.put(lib, new HashSet<CheckRspStats>());
		
		HashSet<CheckRspStats> stat = hasRspCheckOutputs.get(lib);
		//stat.type = type;
		stat.add(new CheckRspStats(type, callsite));
		
		hasRspCheckOutputs.put(lib,stat); 
	}
	
	public void addMissRspCheckStat(String lib, CheckRspType type, String callsite) {
		if (!missRspCheckOutputs.containsKey(lib))
			missRspCheckOutputs.put(lib, new HashSet<CheckRspStats>());
		
		HashSet<CheckRspStats> stat = missRspCheckOutputs.get(lib);
		stat.add(new CheckRspStats(type, callsite));
		
		missRspCheckOutputs.put(lib, stat);
	}
	
	public void addErrorCallback(String callback) {
		if (errorCallbacks.get(callback) == null)
			errorCallbacks.put(callback, new HashSet<String>());
	}
	
	public void addErrorCallback(String callback, String entry) {
		if (errorCallbacks.get(callback) == null)
			errorCallbacks.put(callback, new HashSet<String>());
		
		HashSet<String> value = errorCallbacks.get(callback);
		value.add(entry);
		
		errorCallbacks.put(callback, value);
	}
	
	public void addNoAlertInNoneType(String callback, String entry) { 
		if (this.noAlertsInNonType.get(callback) == null)
			this.noAlertsInNonType.put(callback,  new HashSet<String>());
		HashSet<String> value = noAlertsInNonType.get(callback);
		value.add(entry);
		noAlertsInNonType.put(callback, value);
	}
	
	public void addAlertInNoneType(String callback, String entry) { 
		if (this.alertsInNonType.get(callback) == null)
			this.alertsInNonType.put(callback,  new HashSet<String>());
		HashSet<String> value = alertsInNonType.get(callback);
		value.add(entry);
		alertsInNonType.put(callback, value);
	}
	
	public void addAlertInActivity(String callback, String entry) { 
		if (alertsInActivity.get(callback) == null)
			alertsInActivity.put(callback, new HashSet<String>());
		
		HashSet<String> value = alertsInActivity.get(callback);
		value.add(entry);
		this.alertsInActivity.put(callback,value);
	}
	
	public void addMissedAlertInActivity(String callback, String entry) { 
		if (noAlertsInActivity.get(callback) == null)
			this.noAlertsInActivity.put(callback, new HashSet<String>());
	
		HashSet<String> value = this.noAlertsInActivity.get(callback);
		value.add(entry);
		this.noAlertsInActivity.put(callback,value);
	}
	
	public void addNewSink(String caller, String stmt) {
		sinks.put(stmt, caller);
	}
	
	public void addHasSubErrorHandlers(String sm) {
		hasSubErrorHandlers.add(sm);
	}
	
	public void addMissSubErrorHandlers(String sm) {
		noSubErrorHandlers.add(sm);
	}
	
	
}
