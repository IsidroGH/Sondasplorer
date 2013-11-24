package sondas.dispatcher.inspector;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.stream.FileImageInputStream;

import sondas.ConversationManager;
import sondas.Metadata;
import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.RespBean;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.DeltaTree;
import sondas.utils.SondasUtils;

public class DeltaModeFileWorker implements IDeltaWorker, IMessagesReceiver
{
	private DataOutputStream dos;
	private StringsCache stringsCache;
	private int pid;
	private Dispatcher dispatcher;
	private String path;
	private String agentName;
	private byte buffer[]=new byte[1024];

	public DeltaModeFileWorker(Dispatcher dispatcher) {
		stringsCache = new StringsCache();
		pid = InspectorRuntime.getDeltaProbeId();
		this.dispatcher=dispatcher;
		path = InspectorRuntime.getProperty("FileWorker.path");
		agentName = dispatcher.getAgentName();
	}

	private void setOutputFile(String fileName) {
		if (fileName==null) {
			fileName="inspector_delta_"+System.currentTimeMillis();
		}
		
		try {
			System.err.println("Sending deltas to file");
			dos = new DataOutputStream(new FileOutputStream(path+"/"+fileName));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Error loading FileWorker",e);
		}
	}

	public synchronized void execute(DeltaTree tree) {
		try {
			 // TEST DEBUG 
			/*
			if (dos==null) {
				System.err.println("DMFW: Debug");
				agentName="Debug";
				setCapture("debug");
			}
			*/
			
			if (dos!=null) {
				tree.marshall(pid, dos, stringsCache);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error in Inspector: FileWorker",e);
		}
	}
	
	public synchronized void executeMetadata(Metadata metadata) {
		try {
			 // TEST DEBUG 
			/*
			if (dos==null) {
				System.err.println("DMFW: Debug");
				agentName="Debug";
				setCapture("debug");
			}
			*/
			
			if (dos!=null) {
				metadata.marshall(0, dos);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error in Inspector: FileWorker",e);
		}
	}
	
	private synchronized void setCapture(String name) {
		String fileName = agentName+"_"+name+".ins";
		setOutputFile(fileName);
		// Escribo la cabecera
		try {
			SondasUtils.writeHeader(dispatcher.getCategoriesDictionary(), dos);
		} catch (IOException e) {
			throw new RuntimeException("Error in setting file",e);
		}
	}

	public synchronized RespBean sendCommand(String cmd, BufferedWriter outText, OutputStream outBin) {
		RespBean resp = new RespBean();
		resp.msg="Ok";
		
		System.err.println("DeltaModeFileWorker debug: "+cmd);
		if (cmd.startsWith("set capture ")) {
			setCapture(cmd.substring(12));
		} else if (cmd.equals("end capture")) {
			if (dos!=null) {
				try {
					System.err.println("Flushing and closing file");
					dos.flush();
					dos.close();
					dos=null;
				} catch (IOException e1) {}
			}
		} else if (cmd.equals("get captures")) {
			File dir = new File(path);
			FilenameFilter fnf = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.startsWith(agentName) && name.endsWith(".ins"));
				}
			};

			String files[] = dir.list(fnf);
			StringBuilder sb = new StringBuilder();
			for (int i=0;i<files.length;i++) {
				sb.append(files[i].substring(agentName.length()+1, files[i].length()-4)).append(",");
			}
			resp.code=ConversationManager.Ok;
			resp.msg=sb.toString();
		} else if (cmd.startsWith("exists capture ")) {
			String captureFile = agentName+"_"+cmd.substring(15)+".ins";
			File cap = new File(path+"/"+captureFile);
			if (cap.exists()) {
				resp.code = ConversationManager.Ok;
				resp.msg ="1";
			} else {
				resp.code=ConversationManager.Ok;
				resp.msg="0";
			}
		} else {
			resp.code = ConversationManager.Ignored;
			resp.msg=cmd;
		}

		return resp;
	}
}
