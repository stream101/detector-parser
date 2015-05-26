package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.Gson;

import edu.ucsd.netchecker.AnalysisResults.CheckRspStats;
import edu.ucsd.netchecker.AnalysisResults.RequestStats;

public class GetTotal {
	boolean showLib;
	boolean showSummary;
	String inputFile;	
	int nMissAvailTotal;
	int nInvokeAvailTotal;
	int nMissTimeoutTotal;
	int nInvokeTimeoutTotal;
	int nMissRetryTotal;
	int nInvokeRetryTotal;
	int nOverRetryServiceTotal;
	int nOverRetryPostTotal;
	int nInvokeRespCheckTotal;
	int nMissRespCheckTotal;
	int nValidApps, nNoAvlApps, nNoTimeoutApps, nNoRetryApps;
	int nOverRetryServiceApps, nOverRetryPostApps, nHasRetryAPIApps;
	int nDefaultOverRetryInServiceTotal = 0, nDefaultOverRetryInPostTotal = 0;
	int nAlertsInActivityTotal, nNoAlertsInActivityTotal, nHasActivityCallsiteApps;
	int nAlertsInNative, nNativeCallbacks, nAlertsInCallback, nObviouscallbacks;
	int nNoErrMsgApps, nNoSubErrVolleyApps, nHasSubErrVolleyApps, nNoSubErrorCallbacks, nHasSubErroCallbacks;
	int nSelfRetryApps;
	
	//map from one api to a list of app's invoke ratio. e.g. setTimeOut -> {0.3 (app1), 0.35 (app2), 0 (app3), ...}
	TreeMap<String, ArrayList<InvokeMissPair>> apiInvokeMissMap = new TreeMap<String, ArrayList<InvokeMissPair>>();
	TreeMap<String, Double> apiInvokeRatio = new TreeMap<String, Double>();
	//ArrayList<Integer> pathNum = new ArrayList<Integer>();
	ArrayList<Integer> nRequestDist = new ArrayList<Integer>();
	ArrayList<Double> missAvlRatio = new ArrayList<Double>();
	ArrayList<Double> missTimeoutRatio = new ArrayList<Double>();
	ArrayList<Double> missRetryRatio = new ArrayList<Double>();
	ArrayList<Double> missErrMsgRatio = new ArrayList<Double>();
	
	final String output="stats.txt";
	String inFile;
	FileWriter writer;
	final String field[] = {"app","libs","Sink","Post",
					  "mAvl","iAvl","mTime","iTime","mRetr","iRetr",
					  "NRA","ORS","ORP", "UNRS", "UNRP",
					  "mRsp","iRsp", 
					  "alert","mAlert", "alertNon","mAlertNon", 
					  "subErr", "mSubErr",
					  "Retry","Receiver"};
	
    // do not include okhttp.onError(). it is in background. 
	final String obviousErrorCallbacks[] = {"onFailure", "onError", "onErrorResponse"};
			
	public GetTotal(String inputFile) {	
		this.inFile = inputFile;
	}
	
	void recordInvokeMissCount(GetAllRequests allReq)  {
		for (Entry<String, InvokeMissPair> entry : allReq.getApiInvokeMissNumber().entrySet()) {
			String api = entry.getKey();
			if (!apiInvokeMissMap.containsKey(api))		
				apiInvokeMissMap.put(api, new ArrayList<InvokeMissPair>());
			InvokeMissPair p = entry.getValue();
			ArrayList<InvokeMissPair> value = apiInvokeMissMap.get(api);
			value.add(p);
			apiInvokeMissMap.put(api,value);
		}
		
	}
	
	void computeAPIInvokeRatio() {
		for (Entry<String, ArrayList<InvokeMissPair>> entry : apiInvokeMissMap.entrySet()) {
			int totalInvoke = 0, totalMiss = 0;
			String api = entry.getKey();
			for (InvokeMissPair p : entry.getValue()) {
				totalInvoke += p.getInvokedCount();
				totalMiss += p.getMissedCount();
			}
			if (totalInvoke+totalMiss != 0)
				this.apiInvokeRatio.put(api, new Double((double)totalInvoke/(totalInvoke+totalMiss)));
			else 
				this.apiInvokeRatio.put(api, 0.0);
		}
	}
	
	void recordWrongRetries(GetAllRequests stat) {
		this.nDefaultOverRetryInServiceTotal += stat.nDefaultOverRetriesInService;
		this.nDefaultOverRetryInPostTotal += stat.nDefaultOverRetriesInPost;
	}
	
	/* Categorize the error callback types -- if have direct error callback such as onErrorResponse */
	void  ComputeCallbacTypes(TreeMap<String, HashSet<String>> alertsInActivity, TreeMap<String, HashSet<String>> noAlertsInActivity) {
		for (Entry<String, HashSet<String>> entry : alertsInActivity.entrySet()) {			
			String callback = entry.getKey();
			boolean obviousCallback = false;
			int callsites = entry.getValue().size();
			for (int i=0; i<this.obviousErrorCallbacks.length; i++) {
				if (callback.contains(obviousErrorCallbacks[i])) {
					obviousCallback = true;
				}
			}
			if (obviousCallback) {
				this.nAlertsInCallback += callsites;
				this.nObviouscallbacks += callsites;
			}
			else {
				this.nAlertsInNative += callsites;
				this.nNativeCallbacks += callsites;
			}
		}
		
		for (Entry<String, HashSet<String>> entry : noAlertsInActivity.entrySet()) {
			String callback = entry.getKey();
			boolean obviousCallback = false;
			int callsites = entry.getValue().size();
			for (int i=0; i<this.obviousErrorCallbacks.length; i++) {
				if (callback.contains(obviousErrorCallbacks[i])) {
					obviousCallback = true;
				}
			}
			if (obviousCallback) {
				this.nObviouscallbacks += callsites;
			}
			else 
				this.nNativeCallbacks += callsites;
		}
		
	}
	
	void addToTotal(AnalysisResults result, GetAllRequests stat, int invokeRespCheck, int missRespCheck,
			int alertsInActivity, int noAlertsInActivity, int selfRetry) {
		this.nValidApps += 1;  //valid app at least have one request
		
		/* Forget to invoke */
		this.nMissAvailTotal += stat.nMissAvailCheck;
		this.nInvokeAvailTotal += stat.nInvokeAvailCheck;
		this.nMissTimeoutTotal += stat.nMissTimeout;
		this.nInvokeTimeoutTotal += stat.nInvokeTimeout;
		this.nMissRetryTotal += stat.nMissRetry;
		this.nInvokeRetryTotal += stat.nInvokeRetry;
		//int totalPaths = stat.missAvailCheck + stat.invokeAvailCheck;			
		//this.pathNum.add(totalPaths);   //per app total paths
		this.nRequestDist.add(stat.nRequests); //per app network requests distribution
		if (stat.nInvokeAvailCheck == 0)  //#apps do not invoke conn check
			this.nNoAvlApps += 1;  
		if (stat.nInvokeRetry == 0 && stat.hasRetryAPI)       //#apps do not invoke retry api
			this.nNoRetryApps += 1;
		if(stat.nInvokeTimeout == 0)     //#apps do not inovke timeout api
			this.nNoTimeoutApps += 1;
		// per app ratio of paths that do not invoke conn check. Exclude apps that do not invoke AVAIL check at all
		if (stat.nInvokeAvailCheck > 0)
			this.missAvlRatio.add((double)stat.nMissAvailCheck/(stat.nMissAvailCheck + stat.nInvokeAvailCheck));
		// per app ratio of paths that do not invoke timeout api. If one path has more than 1 timeout api, we only count 1
		//Exclude those do not check timeout at all 
	    if (stat.nInvokeTimeout > 0)
	    	this.missTimeoutRatio.add((double) stat.nMissTimeout/(stat.nMissTimeout + stat.nInvokeTimeout));
	    // per app ratio of paths that do not invoke retry api. Remember not all paths have retry apis. 
	    //Exclude those do not have retry at all
	    if (stat.nInvokeRetry > 0) 
	    	this.missRetryRatio.add((double) stat.nMissRetry/(stat.nMissRetry + stat.nInvokeRetry));
	    
		recordInvokeMissCount(stat);

		/* Misbehaviors */
		this.nOverRetryServiceTotal += stat.nOverRetryService; 
		this.nOverRetryPostTotal += stat.nOverRetryPost;
		if (stat.nOverRetryService > 0) 
			this.nOverRetryServiceApps += 1;
		if (stat.nOverRetryPost > 0) 
			this.nOverRetryPostApps += 1;
		if(stat.hasRetryAPI)
			this.nHasRetryAPIApps += 1;
		recordWrongRetries(stat);
		
		/* Response Check */
		this.nInvokeRespCheckTotal += invokeRespCheck;
		this.nMissRespCheckTotal += missRespCheck;
		
		/* Error Messages */
		this.nAlertsInActivityTotal += alertsInActivity;
		this.nNoAlertsInActivityTotal += noAlertsInActivity;
		//call sites initiaed by an activity
		if(alertsInActivity + noAlertsInActivity != 0 )
			this.nHasActivityCallsiteApps += 1;
		//per app ratio of paths that do not have UI message when the request is initiated by activity
		//Exclude those that do not have err msg at all
		if (alertsInActivity > 0)
			this.missErrMsgRatio.add((double)noAlertsInActivity/(alertsInActivity + noAlertsInActivity)); 	    
	    ComputeCallbacTypes(result.alertsInActivity, result.noAlertsInActivity);
	    if (result.hasSubErrorHandlers.size() + result.noSubErrorHandlers.size() != 0) {
	    	if(result.hasSubErrorHandlers.size() == 0)
	    	   this.nNoSubErrVolleyApps += 1;
	    	else
	    	   this.nHasSubErrVolleyApps += 1;
	    	
	    	this.nHasSubErroCallbacks +=  result.hasSubErrorHandlers.size();
	    	this.nNoSubErrorCallbacks += result.noSubErrorHandlers.size();
	    }
	    
	    /* Self retry */
	    if(selfRetry > 0)
	    	this.nSelfRetryApps += 1;
	}
	
	int computeAlerts (TreeMap<String, HashSet<String>> map) {
		int i = 0;
		i = map.keySet().size();
		//for (Entry<String, HashSet<String>> entry : map.entrySet()) {			
			//i += entry.getValue().size();
		//}
		return i;
	}
	
	int computeRspCheck(HashMap<String, HashSet<CheckRspStats>> map) {
		int i = 0;
		for (Entry<String, HashSet<CheckRspStats>> entry : map.entrySet()) {
			i += entry.getValue().size();
		}
		return i;
	}
	
	//Show stats of one app 
	void showStatsOfOne(AnalysisResults result) throws IOException {
		String appName = result.appName;
		String apkLocation = result.apkFile;
		TreeMap<String, RequestStats> map = result.getRequestStats(); //map<API, RequestStats>
		//System.out.println("\nget stats of " + appName);//xinxin.debug
		GetAllRequests stat = new GetAllRequests(map); //API stats per app
		stat.compute();
		
		//No requests, ignore
		if (stat.nRequests == 0)
			return;
		
		int n_noAlertsInActivity = computeAlerts(result.noAlertsInActivity);
	    int n_alertsInActivity = computeAlerts(result.alertsInActivity);
		//int n_noAlertsInNonType = computeAlerts(result.noAlertsInNonType) ;
		//int n_alertsInNonType = computeAlerts(result.alertsInNonType);
		
		int n_missRspCheckOutputs = computeRspCheck(result.missRspCheckOutputs);
		int n_hasRspCheckOutputs = computeRspCheck(result.hasRspCheckOutputs);
		
		if (n_alertsInActivity == 0)
			this.nNoErrMsgApps += 1;
			
		int sinks = result.sinks.size();
		int posts = result.postMethods.size();
		
		List<String> libUsed = new ArrayList<String>(result.libUsed);
		Collections.sort(libUsed);
		int selfRetry = stat.nSelfRetry;
		int subVolleyErrors = result.hasSubErrorHandlers.size();
		int noSubVolleyErrors = result.noSubErrorHandlers.size();
	
		
		String out = String.format("%s;%s;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
										    appName, libUsed.toString(),
										    sinks, posts, 
											stat.nMissAvailCheck,stat.nInvokeAvailCheck, 
											stat.nMissTimeout, stat.nInvokeTimeout,
											stat.nMissRetry, stat.nInvokeRetry,
											stat.nOverRetryService, stat.nOverRetryPost,
											n_missRspCheckOutputs,n_hasRspCheckOutputs, 
											n_alertsInActivity,n_noAlertsInActivity, 
											subVolleyErrors, noSubVolleyErrors,
											selfRetry);
		
		writer.write(out);
		addToTotal(result, stat, n_hasRspCheckOutputs,n_missRspCheckOutputs,n_alertsInActivity, n_noAlertsInActivity, selfRetry);	
	}
	
	void showStatsOfAll() {
	/*	System.out.format("%s; %d; %d; %d; %d; %d; %d; %d; %d; %d; %d; %d\n",
				"Total", 
				this.missAvailTotal,this.invokeAvailTotal, 
				this.missTimeoutTotal, this.invokeTimeoutTotal,
				this.missRetryTotal, this.invokeRetryTotal,
				this.noRetryActivityTotal, this.overRetryServiceTotal, this.overRetryPostTotal,
				this.missRespCheckTotal, this.invokeRespCheckTotal);*/
		
		System.out.println("===================");
		System.out.println("MISS INVOKE: ");
		System.out.println("===================");	
		System.out.println("-------------------");
		System.out.println("No avail api apps, " + this.nNoAvlApps+"/"+this.nValidApps + " ," + (double)this.nNoAvlApps/this.nValidApps  );
		System.out.println("No timeout api apps," +this.nNoTimeoutApps + "/" +this.nValidApps + " ,"+(double)this.nNoTimeoutApps/this.nValidApps);
		System.out.println("No retry api apps, " + this.nNoRetryApps+"/"+this.nHasRetryAPIApps + " ," +(double)this.nNoRetryApps/this.nHasRetryAPIApps);
		System.out.println("Self retry apps, " + this.nSelfRetryApps+"/"+this.nValidApps + " ," + (double)this.nSelfRetryApps/this.nValidApps);
		System.out.println("-------------------");
		System.out.println("requests number:");
		TreeMap<Integer, Double> mapp = CDF.plotICDF(this.nRequestDist);
		for(Entry<Integer, Double> entry : mapp.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Avail Ratio:");
		TreeMap<String, Double> mapa = CDF.plotDCDF(this.missAvlRatio);
		for(Entry<String, Double> entry : mapa.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Timeout Ratio:");
		TreeMap<String, Double> mapt = CDF.plotDCDF(this.missTimeoutRatio);
		for(Entry<String, Double> entry : mapt.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Retry Ratio:");
		TreeMap<String, Double> mapR = CDF.plotDCDF(this.missRetryRatio);
		for(Entry<String, Double> entry : mapR.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		computeAPIInvokeRatio();
		System.out.println("API invoke ratios:");
		for(Entry<String, Double> entry : this.apiInvokeRatio.entrySet()){
			String api = entry.getKey();
			double ratio = entry.getValue();
			System.out.println(api + "  , " + ratio);
		}
		System.out.println("-------------------");
		System.out.println("====================");
		System.out.println("WRONG BEHAVIOR: ");	
		System.out.println("====================");
		System.out.println("Over retry in service apps, " + this.nOverRetryServiceApps +"/"+this.nHasRetryAPIApps + 
							" ," + (double)this.nOverRetryServiceApps/this.nHasRetryAPIApps);
		
		System.out.println("Over retry in post apps, " + this.nOverRetryPostApps + "/"+this.nHasRetryAPIApps + 
							" ," + (double)this.nOverRetryPostApps/this.nHasRetryAPIApps);
		System.out.println("-------------------");
		System.out.println("Over retry in service by default, " + 
							this.nDefaultOverRetryInServiceTotal +"/" + this.nOverRetryServiceTotal +
							" , " + (double) this.nDefaultOverRetryInServiceTotal/this.nOverRetryServiceTotal);				
		
		System.out.println("Over retry in post by default, " + 
							this.nDefaultOverRetryInPostTotal + "/" +  this.nOverRetryPostTotal +
							" , " + (double) this.nDefaultOverRetryInPostTotal/this.nOverRetryPostTotal);
	
		System.out.println("====================");
		System.out.println("ERROR MESSAGE: ");	
		System.out.println("====================");
		System.out.println("No error message apps, " + this.nNoErrMsgApps + "/" + this.nHasActivityCallsiteApps + 
							" , " + (double)this.nNoErrMsgApps/this.nHasActivityCallsiteApps);
		
		int callsitesInActivity = this.nAlertsInActivityTotal + this.nNoAlertsInActivityTotal;
		System.out.println("No error message activity callsites: " + this.nNoAlertsInActivityTotal + "/" + callsitesInActivity +
							" , " + (double)this.nNoAlertsInActivityTotal/callsitesInActivity);
		System.out.println("-------------------");
		System.out.println("Has error message in native/okhttp, " + this.nAlertsInNative + "/" + this.nNativeCallbacks +
							" , " + (double) this.nAlertsInNative/this.nNativeCallbacks);
		
		System.out.println("Has error message in easy callback, " + this.nAlertsInCallback + "/" + this.nObviouscallbacks +
							" , " + (double)this.nAlertsInCallback/this.nObviouscallbacks);
				
		System.out.println("-------------------");
		
		int numVolleyApps =  this.nNoSubErrVolleyApps + this.nHasSubErrVolleyApps;
		System.out.println("No sub error volley apps, " + this.nNoSubErrVolleyApps + "/" + numVolleyApps +
						   " , " + (double) this.nNoSubErrVolleyApps/numVolleyApps);
		
		int numVolleyCallbacks = this.nNoSubErrorCallbacks + this.nHasSubErroCallbacks;
		System.out.println("No sub error volley callbacks, " + this.nNoSubErrorCallbacks + "/" + numVolleyCallbacks + 
							" , " + (double) this.nNoSubErrorCallbacks/numVolleyCallbacks);
		
		System.out.println("-------------------");
		System.out.println("Miss error message ratio:");
		TreeMap<String, Double> mapEM = CDF.plotDCDF(this.missErrMsgRatio);
		for(Entry<String, Double> entry : mapEM.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("====================");
		System.out.println("INVALID RESPONSE: ");	
		System.out.println("====================");
		int checks = this.nInvokeRespCheckTotal + this.nMissRespCheckTotal;
		System.out.println("Miss response check sites, " + this.nMissRespCheckTotal + "/" + checks +
							" , " + (double)this.nMissRespCheckTotal/checks);
		
	}

	void showTotalStats () {
		try {
			
			FileInputStream fis = new FileInputStream(inFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			this.writer = new FileWriter(output);
			while ((line = br.readLine()) != null) { 
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				showStatsOfOne(obj); //one app stats
			}
			br.close();
			this.writer.flush();
			this.writer.close();
		} catch (FileNotFoundException e) {
			System.err.print("Cannot find file " + inFile.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		showStatsOfAll();
	}
	
	

	public void show() {
	    showTotalStats();
	
	}

}
