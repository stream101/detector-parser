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
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.Gson;

import edu.ucsd.netchecker.AnalysisResults.APIStats;

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
	int alertsInActivityTotal, noAlertsInActivityTotal;
	int selfRetryTotal;
	final String output="stats.txt";
	String inFile;
	FileWriter writer;
	String field[] = {"app","libs","Sink","Post","mAvl","iAvl","mTime","iTime","mRetr","iRetr",
			"NRA","ORS","ORP","mRsp","iRsp","alert","mAlert","Retry"};

	
	ArrayList<Integer> pathNum = new ArrayList<Integer>();
	ArrayList<Double> missAvlRatio = new ArrayList<Double>();
	ArrayList<Double> missTimeoutRatio = new ArrayList<Double>();
	ArrayList<Double> missRetryRatio = new ArrayList<Double>();
	 
	
	public GetTotal(String inputFile) {	
		this.inFile = inputFile;
	}
	
	
	void addToTotal(GetAPIStats stat, int invokeRespCheck, int missRespCheck,
			int alertsInActivity, int noAlertsInActivity, int selfRetry) {
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
		int totalPaths = stat.missAvailCheck + stat.invokeAvailCheck;
		if(totalPaths != 0) {
			this.numValidApps += 1;  //valid app at least have one path
			this.pathNum.add(totalPaths);   //per app total paths
			if (stat.invokeAvailCheck == 0)
				this.numNoAvlApps += 1;
			if (stat.invokeRetry == 0)
				this.numNoRetryApps += 1;
			if(stat.invokeTimeout == 0)
				this.numNoTimeoutApps += 1;
			this.missAvlRatio.add((double)stat.missAvailCheck/(stat.missAvailCheck + stat.invokeAvailCheck));
		    this.missTimeoutRatio.add((double) stat.appTotalMissTimeoutPaths/totalPaths);
		    this.missRetryRatio.add((double) stat.appTotalMissRetryPaths/(stat.appTotalMissRetryPaths + stat.appTotalInvokeRetryPaths));
		}
		this.alertsInActivityTotal += alertsInActivity;
		this.noAlertsInActivityTotal += noAlertsInActivity;
		this.selfRetryTotal += selfRetry;
	}
	
	void showStatsOfOne(AnalysisResults result) throws IOException {
		String appName = result.appName;
		String apkLocation = result.apkFile;
		TreeMap<String, APIStats> map = result.getAPIUsages(); //map<API, APIstats>
		GetAPIStats stat = new GetAPIStats(map); //API stats per app
		stat.compute();
		
		//No path, ignore
		if (stat.invokeAvailCheck + stat.missAvailCheck == 0)
			return;
		
		int invokeRespCheck = result.hasRespCheckPaths.size();
		int missRespCheck = result.noRespCheckPaths.size();
		int sinks = result.sinks.size();
		int posts = result.postMethods.size();
		List<String> libUsed = new ArrayList<String>(result.libUsed);
		Collections.sort(libUsed);
		int alertsInActivity = result.alertsInActivity.size();
		int noAlertsInActivity = result.noAlertsInActivity.size();
		int selfRetry = result.selfRetryMethods.size();
		
		String out = String.format("%s;%s;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
										    appName, libUsed.toString(),
										    sinks, posts, 
											stat.missAvailCheck,stat.invokeAvailCheck, 
											stat.missTimeout, stat.invokeTimeout,
											stat.missRetry, stat.invokeRetry,
											stat.noRetryActivity, stat.overRetryService, stat.overRetryPost,
											missRespCheck,invokeRespCheck, 
											alertsInActivity,noAlertsInActivity, selfRetry);
		
		writer.write(out);
		addToTotal(stat, invokeRespCheck, missRespCheck, alertsInActivity, noAlertsInActivity, selfRetry);
		
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
		System.out.println("paths number:");
		//Collections.sort(this.pathNum);
		TreeMap<Integer, Integer> mapp = CDF.plotICDF(this.pathNum);
		for(Entry<Integer, Integer> entry : mapp.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("===================");
		System.out.println("No avail apps, " + this.numNoAvlApps+"/"+this.numValidApps + " ," + (double)this.numNoAvlApps/this.numValidApps  );
		System.out.println("No timeout apps," +this.numNoTimeoutApps + "/" +this.numValidApps + " ,"+(double)this.numNoTimeoutApps/this.numValidApps);
		System.out.println("No retry apps, " + this.numNoRetryApps+"/"+this.numValidApps + " ," +(double)this.numNoRetryApps/this.numValidApps);
		System.out.println("===================");
		System.out.println("Miss Avail Ratio:");
		TreeMap<String, Integer> mapa = CDF.plotDCDF(this.missAvlRatio);
		for(Entry<String, Integer> entry : mapa.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("===================");
		System.out.println("Miss Timeout Ratio:");
		TreeMap<String, Integer> mapt = CDF.plotDCDF(this.missTimeoutRatio);
		for(Entry<String, Integer> entry : mapt.entrySet())
			System.out.println(entry.getKey() + "," + entry.getValue());
		System.out.println("===================");
		/*System.out.println("Miss Retry Ratio:");
		for(Double d : this.missRetryRatio)
			System.out.println(d);
		System.out.println("===================");
		*/
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
