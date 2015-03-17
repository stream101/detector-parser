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

import edu.ucsd.netchecker.AnalysisResults.APIStats;
import edu.ucsd.netchecker.AnalysisResults.CheckRspStats;

public class GetTotal {
	boolean showLib;
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
	int numValidApps, numNoAvlApps, numNoTimeoutApps, numNoRetryApps;
	int numNoRetryActivityApps, numOverRetryServiceApps, numOverRetryPostApps, numHasRetryAPIApps;
	int manualOverRetryInServiceTotal=0, defaultOverRetryInServiceTotal = 0;
	int manualOverRetryInPostTotal = 0, defaultOverRetryInPostTotal = 0;
	int alertsInActivityTotal, noAlertsInActivityTotal;
	int selfRetryTotal;
	//map from lib to a list of app's invoke ratio. e.g. apache -> {0.3, 0.35, 0, ...}
	//TreeMap<String, ArrayList<Double>> invokeTimeoutAllMap = new TreeMap<String, ArrayList<Double>>();
	TreeMap<String, ArrayList<InvokeMissPair>> apiInvokeMissMap = new TreeMap<String, ArrayList<InvokeMissPair>>();
	TreeMap<String, Double> apiInvokeRatio = new TreeMap<String, Double>();
	
	final String output="stats.txt";
	String inFile;
	FileWriter writer;
	String field[] = {"app","libs","Sink","Post",
					  "mAvl","iAvl","mTime","iTime","mRetr","iRetr",
					  "NRA","ORS","ORP", "UNRS", "UNRP",
					  "mRsp","iRsp", 
					  "alert","mAlert", "alertNon","mAlertNon", 
					  "subErr", "mSubErr",
					  "Retry","Receiver"};

	
	ArrayList<Integer> pathNum = new ArrayList<Integer>();
	ArrayList<Double> missAvlRatio = new ArrayList<Double>();
	ArrayList<Double> missTimeoutRatio = new ArrayList<Double>();
	ArrayList<Double> missRetryRatio = new ArrayList<Double>();
		
	public GetTotal(String inputFile) {	
		this.inFile = inputFile;
	}
	
	/*void addToAllLibMap(TreeMap<String, Double> single,  TreeMap<String, ArrayList<Double>> map) {
		for (Entry<String, Double> entry : single.entrySet()) {
			String lib = entry.getKey();
			Double ratio = entry.getValue();
			if (!map.containsKey(lib))
				map.put(lib, new ArrayList<Double>());
			ArrayList<Double> list = map.get(lib);
			list.add(ratio);
			map.put(lib, list);
		}
	}
	
	void computeAllLibRatio(TreeMap<String, ArrayList<Double>> input, TreeMap<String, Double> output) {
		for (Entry<String,ArrayList<Double>> entry : input.entrySet()) {
			double total = 0;
			String lib = entry.getKey();
			for (Double ratio : entry.getValue()) {
				total += ratio.doubleValue();
			}
			int num = entry.getValue().size();
			output.put(lib, total/num);
		}		
	}
	*/
	void recordInvokeMissCount(TreeMap<String, APIStats> map)  {
		for (Entry<String, APIStats> entry : map.entrySet()) {
			APIStats stats = entry.getValue();
			String api = entry.getKey();
			if (!apiInvokeMissMap .containsKey(api))		
				apiInvokeMissMap .put(api, new ArrayList<InvokeMissPair>());
			InvokeMissPair p = new InvokeMissPair(stats.inovkedAPIPaths.size(), stats.missedAPIPaths.size());
			System.out.println("" + api + " , invoke " + stats.inovkedAPIPaths.size() + " , miss " + stats.missedAPIPaths.size()); //xinxin.debug
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
	
	void recordWrongRetries(GetAPIStats stat) {
		this.manualOverRetryInServiceTotal += stat.manualSetOverRetriesInService;  
		this.defaultOverRetryInServiceTotal += stat.defaultOverRetriesInService;
		this.manualOverRetryInPostTotal += stat.manualSetOverRetriesInPost;
		this.defaultOverRetryInPostTotal += stat.defaultOverRetriesInPost;
	}
	
	void addToTotal(TreeMap<String, APIStats> map, GetAPIStats stat, int invokeRespCheck, int missRespCheck,
			int alertsInActivity, int noAlertsInActivity, int selfRetry) {
		//forget to invoke
		this.missAvailTotal += stat.missAvailCheck;
		this.invokeAvailTotal += stat.invokeAvailCheck;
		this.missTimeoutTotal += stat.missTimeout;
		this.invokeTimeoutTotal += stat.invokeTimeout;
		this.missRetryTotal += stat.missRetry;
		this.invokeRetryTotal += stat.invokeRetry;
		int totalPaths = stat.missAvailCheck + stat.invokeAvailCheck;
		if(totalPaths != 0) {
			this.numValidApps += 1;  //valid app at least have one path 
			this.pathNum.add(totalPaths);   //per app total paths
			if (stat.invokeAvailCheck == 0)  //#apps do not invoke conn check
				this.numNoAvlApps += 1;  
			if (stat.invokeRetry == 0)       //#apps do not invoke retry api
				this.numNoRetryApps += 1;
			if(stat.invokeTimeout == 0)     //#apps do not inovke timeout api
				this.numNoTimeoutApps += 1;
			// per app ratio of paths that do not invoke conn check
			this.missAvlRatio.add((double)stat.missAvailCheck/(stat.missAvailCheck + stat.invokeAvailCheck));
			// per app ratio of paths that do not invoke timeout api. If one path has more than 1 timeout api, we only count 1
		    this.missTimeoutRatio.add((double) stat.appTotalMissTimeoutPaths/totalPaths);
		    // per app ratio of paths that do not invoke retry api. Remember not all paths have retry apis. 
		    int totalRetryPaths = stat.appTotalMissRetryPaths + stat.appTotalInvokeRetryPaths;
		    if (totalRetryPaths !=0) {
		    	this.missRetryRatio.add((double) stat.appTotalMissRetryPaths/(totalRetryPaths));
		    }
		}
		recordInvokeMissCount(map);

		//misbehaviors 
		this.noRetryActivityTotal += stat.noRetryActivity;
		this.overRetryServiceTotal += stat.overRetryService; 
		this.overRetryPostTotal += stat.overRetryPost;
		if (stat.noRetryActivity > 0) 
			this.numNoRetryActivityApps += 1;
		if (stat.overRetryService > 0) 
			this.numOverRetryServiceApps += 1;
		if (stat.overRetryPost > 0) 
			this.numOverRetryPostApps += 1;
		if(stat.hasRetryAPI)
			this.numHasRetryAPIApps += 1;
		recordWrongRetries(stat);
		
		//response check
		this.invokeRespCheckTotal += invokeRespCheck;
		this.missRespCheckTotal += missRespCheck;
		
		this.alertsInActivityTotal += alertsInActivity;
		this.noAlertsInActivityTotal += noAlertsInActivity;
		this.selfRetryTotal += selfRetry;

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
	
	
	void showStatsOfOne(AnalysisResults result) throws IOException {
		String appName = result.appName;
		String apkLocation = result.apkFile;
		TreeMap<String, APIStats> map = result.getAPIUsages(); //map<API, APIstats>
		System.out.println("\nget stats of " + appName);//xinxin.debug
		GetAPIStats stat = new GetAPIStats(map); //API stats per app
		stat.compute();
		
		//No path, ignore
		if (stat.invokeAvailCheck + stat.missAvailCheck == 0)
			return;
		
		int n_noAlertsInActivity = computeAlerts(result.noAlertsInActivity);
	    int n_alertsInActivity = computeAlerts(result.alertsInActivity);
		int n_noAlertsInNonType = computeAlerts(result.noAlertsInNonType) ;
		int n_alertsInNonType = computeAlerts(result.alertsInNonType);
		int n_missRspCheckOutputs = computeRspCheck(result.missRspCheckOutputs);
		int n_hasRspCheckOutputs = computeRspCheck(result.hasRspCheckOutputs);
		
		int sinks = result.sinks.size();
		int posts = result.postMethods.size();
		List<String> libUsed = new ArrayList<String>(result.libUsed);
		Collections.sort(libUsed);
		//int alertsInActivity = result.alertsInActivity.size();
		//int noAlertsInActivity = result.noAlertsInActivity.size();
		int selfRetry = result.selfRetryMethods.size();
		int subVolleyErrors = result.hasSubErrorHandlers.size();
		int noSubVolleyErrors = result.noSubErrorHandlers.size();
		int netReceiver = result.connReceivers.size();
		
		String out = String.format("%s;%s;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
										    appName, libUsed.toString(),
										    sinks, posts, 
											stat.missAvailCheck,stat.invokeAvailCheck, 
											stat.missTimeout, stat.invokeTimeout,
											stat.missRetry, stat.invokeRetry,
											stat.noRetryActivity, stat.overRetryService, stat.overRetryPost,
											stat.unknownRetryService, stat.unknownRetryPost,
											n_missRspCheckOutputs,n_hasRspCheckOutputs, 
											n_alertsInActivity,n_noAlertsInActivity, 
											n_alertsInNonType,n_noAlertsInNonType,
											subVolleyErrors, noSubVolleyErrors,
											selfRetry, netReceiver);
		
		writer.write(out);
		addToTotal(map, stat, n_hasRspCheckOutputs,n_missRspCheckOutputs,n_alertsInActivity, n_noAlertsInActivity, selfRetry);
		
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
		System.out.println("Miss invoke: ");
		System.out.println("===================");	
		System.out.println("-------------------");
		System.out.println("No avail api apps, " + this.numNoAvlApps+"/"+this.numValidApps + " ," + (double)this.numNoAvlApps/this.numValidApps  );
		System.out.println("No timeout api apps," +this.numNoTimeoutApps + "/" +this.numValidApps + " ,"+(double)this.numNoTimeoutApps/this.numValidApps);
		System.out.println("No retry api apps, " + this.numNoRetryApps+"/"+this.numValidApps + " ," +(double)this.numNoRetryApps/this.numValidApps);
		System.out.println("Self retry apps, " + this.selfRetryTotal);
		System.out.println("-------------------");
		System.out.println("paths number:");
		//Collections.sort(this.pathNum);
		TreeMap<Integer, Integer> mapp = CDF.plotICDF(this.pathNum);
		for(Entry<Integer, Integer> entry : mapp.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Avail Ratio:");
		TreeMap<String, Integer> mapa = CDF.plotDCDF(this.missAvlRatio);
		for(Entry<String, Integer> entry : mapa.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Timeout Ratio:");
		TreeMap<String, Integer> mapt = CDF.plotDCDF(this.missTimeoutRatio);
		for(Entry<String, Integer> entry : mapt.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("-------------------");
		System.out.println("Miss Retry Ratio:");
		for(Double d : this.missRetryRatio)
			System.out.println(d);
		TreeMap<String, Integer> mapR = CDF.plotDCDF(this.missRetryRatio);
		for(Entry<String, Integer> entry : mapR.entrySet())
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
		System.out.println("Wrong behavior: ");	
		System.out.println("====================");
		System.out.println("No retry in activity apps, " + this.numNoRetryActivityApps+"/"+this.numHasRetryAPIApps + " ," + 
							(double)this.numNoRetryActivityApps/this.numHasRetryAPIApps);
		
		System.out.println("Over retry in service apps, " + this.numOverRetryServiceApps +"/"+this.numHasRetryAPIApps + " ," +
							(double)this.numOverRetryServiceApps/this.numHasRetryAPIApps);
		
		System.out.println("Over retry in post apps, " + this.numOverRetryPostApps + "/"+this.numHasRetryAPIApps + " ," +
							(double)this.numOverRetryPostApps/this.numHasRetryAPIApps);
		System.out.println("-------------------");
		int totalOverRetryInService = this.defaultOverRetryInServiceTotal+this.manualOverRetryInServiceTotal;
		int totalOverRetryInPost = this.defaultOverRetryInPostTotal + this.manualOverRetryInPostTotal;
		System.out.println("Over retry in service by default, " + 
							this.defaultOverRetryInServiceTotal +"/" + totalOverRetryInService +
							" , " + (double) this.defaultOverRetryInServiceTotal/totalOverRetryInService);				
		
		System.out.println("Over retry in post by default, " + 
							this.defaultOverRetryInPostTotal + "/" +  totalOverRetryInPost +
							" , " + (double) this.defaultOverRetryInPostTotal/totalOverRetryInPost);
		System.out.println("-------------------");
	}

	void showTotalStats () {
		//System.out.println("mAvl;iAvl;mTime;iTime;mRetr;iRetr;NRA;ORS;ORP;mRsp;iRsp;Sinks;Posts;alertsInActivity,noAlertsInActivity, selfRetry");
		//for (String s : this.field) {
		//	System.out.print(s + ";");
		//}
		//System.out.println();
		try {
			
			FileInputStream fis = new FileInputStream(inFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			this.writer = new FileWriter(output);
			while ((line = br.readLine()) != null) {
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				//System.out.println("read result: "+obj.noRespCheckPaths.toString());
				showStatsOfOne(obj);
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
