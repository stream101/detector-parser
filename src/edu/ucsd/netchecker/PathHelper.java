package edu.ucsd.netchecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class PathHelper {
	public static boolean isVisitedPath(ArrayList<String> p, ArrayList<ArrayList<String>> paths) {
		boolean isVisited = false;
		
		for (ArrayList<String> s : paths) {
			if (isVisited)
				break;
			
			int size = Math.min(s.size(), p.size());
			//Return true if prefix is the same
			int i;
			for (i=0; i<size; i++ ) {
				if (!p.get(i).equals(s.get(i))) {
					break;
				}
			}
			
			if (i == size)  //have compared all p's elements
				isVisited = true;
		}
		return isVisited;
	}
	
	public static boolean pathContainedIn( ArrayList<String> tgt, ArrayList<ArrayList<String>> paths) {
		boolean contain = false;
		for (ArrayList<String> path : paths) {
			if (path.size() != tgt.size())
				continue;
			
			int i;
			for (i = 0; i<tgt.size();i++) {
				if (tgt.get(i) != path.get(i))
					break;
			}
			
			if (i == tgt.size()) {
				contain = true;
				break;
			}
		}
		return contain;
	}
	
	public static ArrayList<String> transformArrayToList(String[] path) {
		ArrayList<String> newPath = new ArrayList<String>(Arrays.asList(path));
		newPath.removeAll(Collections.singleton(null));  //remove null
		Collections.reverse(newPath);
		return newPath;
	}
	
	public static void addOnePath(ArrayList<String> p, ArrayList<ArrayList<String>> paths) {
		paths.add(p);
	}
	
	public static void prettyPrint(ArrayList<String> path) {
		for (int i = 0 ; i<path.size();i++) {
			for (int j = 0; j<i; j++) 
				System.out.print(".");
			System.out.println(path.get(i));
		}
		System.out.println();
	}
	
	
}
