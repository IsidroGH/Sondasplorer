package sondas.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import sondas.Global;

public class Logger {
	static private PrintWriter writer;
	static private SimpleDateFormat sdf;
	
	static {
		String logsFile = Global.getProperty("Agent", "logs.path")+"/sondasplorer.log";
		sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		
		try {
			writer = new PrintWriter(new FileWriter(logsFile,true),true);
			writer.println(sdf.format(new Date())+" Sondasplorer loaded");
			writer.flush();
		} catch (IOException e) {
			System.err.println("Sondasplorer: Couldn't open logs file: "+logsFile);
			e.printStackTrace();
		}
	}
	
	public static void write(String text) {
		if (writer!=null) {
			writer.println(text);
			writer.flush();
		}
	}
}
