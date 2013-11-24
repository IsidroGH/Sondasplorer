package sondas.dispatcher.inspector;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import sondas.dispatcher.Dispatcher;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.Stamp;

public class FullModeTextFileWorker implements IFullWorker 
{
	private Writer output;


	public FullModeTextFileWorker(Dispatcher dis) {
		try {
			String path = InspectorRuntime.getProperty("FileWorker.path");
			output = new BufferedWriter(new FileWriter(path+"/inspector_text_"+System.currentTimeMillis()));
		} catch (IOException e) {
			throw new RuntimeException("Error loading TextFileWorker",e);
		}
	}
	
	public void execute(Stamp stamp) 
	{
		try {
			ITrace trace = stamp.getTrace();
			
			if (trace.getMode()==Constants.Start) {
				output.write(stamp.getTid()+":"+stamp.getTs()+":I:"+trace.getName()+"\n");
			} else if (trace.getMode()==Constants.End) {
				output.write(stamp.getTid()+":"+stamp.getTs()+":O\n");
			} if (trace.getMode()==Constants.Exception) {
				output.write(stamp.getTid()+":"+stamp.getTs()+":E\n");
			}
			output.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error in Inspector: FileWorker",e);
		}
	}
}

