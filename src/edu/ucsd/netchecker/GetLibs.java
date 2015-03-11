package edu.ucsd.netchecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.google.gson.Gson;

public class GetLibs {
	String file;
	
	public GetLibs(String f) {
		this.file = f; 
	}
	
	void showLibUsage() {
		ArrayList<String> apache = new ArrayList<String>(); 
		ArrayList<String> hurl = new ArrayList<String>(); 
		ArrayList<String> volley = new ArrayList<String>(); 
		ArrayList<String> aah = new ArrayList<String>(); 
		ArrayList<String> basic = new ArrayList<String>(); 
		ArrayList<String> okhttp = new ArrayList<String>(); 
		
		try {	
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				for (String s : obj.libUsed) {
					if (s.equals("apache"))
						apache.add(obj.appName);
					else if (s.equals("hurl"))
						hurl.add(obj.appName);
					else if (s.equals("volley"))
						volley.add(obj.appName);
					else if (s.equals("aah"))
						aah.add(obj.appName);
					else if (s.equals("basic"))
						basic.add(obj.appName);
					else if (s.equals("okhttp"))
						okhttp.add(obj.appName);
				}
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			System.err.print("Cannot find file " + file.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\napache lib: " + apache.size());
		for (String s: apache)
			System.out.println(s);
		System.out.println("\nhurl lib: " + hurl.size());
		for (String s: hurl)
			System.out.println(s);
		System.out.println("\nvoley lib: " + volley.size());
		for (String s: volley)
			System.out.println(s);
		System.out.println("\naah lib: " + aah.size());
		for(String s: aah)
			System.out.println(s);
		System.out.println("\nbasic lib: " + basic.size());
		for(String s: basic)
			System.out.println(s);
		System.out.println("\nokhttp lib: " + okhttp.size());
		for(String s: okhttp)
			System.out.println(s);
	}
	
	public void show() {
		showLibUsage();
	}
}
