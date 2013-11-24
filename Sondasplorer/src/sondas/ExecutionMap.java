package sondas;

import java.util.HashMap;

public class ExecutionMap extends HashMap 
{
	private static int exIdSeq;
	
	public  synchronized int getNextExId() {
		return ++exIdSeq;
	}
}
