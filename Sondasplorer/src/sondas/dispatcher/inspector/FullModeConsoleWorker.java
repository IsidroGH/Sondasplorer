package sondas.dispatcher.inspector;

import sondas.dispatcher.Dispatcher;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.Stamp;

public class FullModeConsoleWorker implements IFullWorker 
{
	public FullModeConsoleWorker(Dispatcher dis) {
		
	}
	
	public void execute(Stamp stamp) 
	{
		ITrace trace = stamp.getTrace();
		
		if (trace.getMode()==Constants.Start) {
			System.out.println(stamp.getTid()+":"+stamp.getTs()+":I:"+trace.getName());
		} else if (trace.getMode()==Constants.End) {
			System.out.println(stamp.getTid()+":"+stamp.getTs()+":O");
		} if (trace.getMode()==Constants.Exception) {
			System.out.println(stamp.getTid()+":"+stamp.getTs()+":E");
		}
	}
}