package sondas.dispatcher.inspector;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import sondas.dispatcher.Dispatcher;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.Stamp;

public class FullModeFileWorker implements IFullWorker 
{
	private DataOutputStream dos;
	private Dispatcher dis;
	
	public FullModeFileWorker(Dispatcher dis) {
		this.dis=dis;
	}
	
	private DataOutputStream getOutput() {
		DataOutputStream resp;
		try {
			String path = InspectorRuntime.getProperty("FileWorker.path");
			resp = new DataOutputStream(new FileOutputStream(path+"/inspector_full_"+System.currentTimeMillis()));
			return resp;
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error loading FileWorker",e);
		}
	}
	
	public void execute(Stamp stamp) 
	{
		if (dos==null) {
			dos=getOutput();
		}
		
		try {
			stamp.marshall(dos);
		} catch (IOException e) {
			throw new RuntimeException("Error in Inspector: FileWorker",e);
		}
	}
}
