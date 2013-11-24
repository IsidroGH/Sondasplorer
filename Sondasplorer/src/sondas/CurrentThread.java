package sondas;

public class CurrentThread 
{
	private static int threadSeqNumber;
	
	public int tid;
	
	public CurrentThread() {
		tid = nextThreadId();
	}
	
	private static synchronized int nextThreadId() {
	    return ++threadSeqNumber;
	}
}
