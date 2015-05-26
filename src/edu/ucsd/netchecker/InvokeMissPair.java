package edu.ucsd.netchecker;

public class InvokeMissPair {
	int nInvoked;
	int nMissed;
	
	public InvokeMissPair() {
		this.nInvoked = 0;
		this.nMissed = 0;
	}
	
	public InvokeMissPair(int invoke, int miss) {
		this.nInvoked = invoke;
		this.nMissed = miss;
	}
	
	public int getInvokedCount() { return this.nInvoked; }
	public int getMissedCount() { return this.nMissed; }
	
	public void setInvokeCount(int i) { this.nInvoked = i;}
	public void setMissedCount(int i) { this.nMissed = i; }
}
