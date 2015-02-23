import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import soot.jimple.infoflow.android.analysis.AnalysisResults;

import com.google.gson.Gson;


public class Parser {
	private static void printUsage() {
		 System.out.println("Incorrect arguments: [0] = parsed-file");
	}
	
	public static void main(final String[] args) {
		if (args.length < 1) {
			printUsage();
			return;
		}
		
		try {
			File file = new File(args[0]);
			FileInputStream fis = new FileInputStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				Gson gson = new Gson();
				AnalysisResults obj = gson.fromJson(line, AnalysisResults.class);   
				System.out.println("read result: "+obj.noRespCheckPaths.toString());
			
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
