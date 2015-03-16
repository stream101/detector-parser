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
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;


/*
 * Output format: 
*/
public class AnalysisResults {
	public String appName;
	public String apkFile;
	public TreeMap<String, APIStats> APIOutputs = new TreeMap<String, APIStats>();
	public ArrayList<ArrayList<String>> noRespCheckPaths = new ArrayList<ArrayList<String>>();
	public ArrayList<ArrayList<String>> hasRespCheckPaths = new ArrayList<ArrayList<String>>();
	public HashMap<String, String> sinks = new HashMap<String, String>();
	public HashSet<String> postMethods = new HashSet<String>();
	public HashSet<String> libUsed = new HashSet<String>();
	public ArrayList<ArrayList<String>> incompletePaths = new ArrayList<ArrayList<String>>();
	public HashSet<String> connReceivers = new HashSet<String>();
	public HashSet<String> selfRetryMethods = new HashSet<String>();
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
	
	public class APIStats {
		public APIType type = APIType.NONE;
		public ArrayList<ArrayList<String>> missedAPIPaths = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> inovkedAPIPaths = new  ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> noRetryActivityPaths = new  ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> overRetryServicePaths = new  ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> overRetryPostPaths = new  ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> noEntryPaths = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> noSensitiveTypePaths = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> unKnownRetryServicePaths = new ArrayList<ArrayList<String>>();
		public ArrayList<ArrayList<String>> unKnownRetryPostPaths = new ArrayList<ArrayList<String>>();
		
		public void setType(String signature, int retryIdx, int timeoutIdx) {
			final String netInfoSig= "<android.net.ConnectivityManager: android.net.NetworkInfo getActiveNetworkInfo()>";
			
			if (signature.equals(netInfoSig))
				type = APIType.AVAIL;
			else if (retryIdx >=0 && timeoutIdx >=0)
				type = APIType.BOTH;
			else if (retryIdx >=0)
				type = APIType.RETRY;
			else if (timeoutIdx >=0)
				type = APIType.TIMEOUT;
		}
	}
	
	public class CheckRspStats {
		public CheckRspStats( CheckRspType type, String callsite) {
			this.type = type;
			this.location = callsite;
		}
		//String lib;
		CheckRspType type;
		String location;
	}
	
	//public void addFormatedOutput(FormatedOutput o) { formatedOutputs.add(o); }
	public TreeMap<String, APIStats>  getAPIUsages() { return this.APIOutputs; }
	
	public void addAPI(String api) {
		if (!APIOutputs.containsKey(api))
			APIOutputs.put(api, new APIStats());
	}
	
	//public void addTimeoutValue(long timeout) { timeoutValues.add(new Long(timeout)); }
	
	//public void addFoundSink(SootMethod m) {	foundSinks.add(m);}


}
