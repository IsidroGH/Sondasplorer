package sondas.dispatcher.inspector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import sondas.ConversationManager;
import sondas.inspector.InspectorRuntime;

class CapturesServerInstance extends Thread
{
	private Socket clientSocket;
	private byte buffer[]=new byte[1024];

	public CapturesServerInstance(Socket clientSocket) 
	{
		this.clientSocket=clientSocket;
	}

	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			OutputStream out = clientSocket.getOutputStream();

			String path = InspectorRuntime.getProperty("FileWorker.path");

			String cmd = in.readLine();
			// agent;capture
			StringTokenizer st = new StringTokenizer(cmd,";");
			String agentName = st.nextToken();
			String capture = st.nextToken();

			String captureFile = agentName+"_"+capture+".ins";
			File cap = new File(path+"/"+captureFile);
			if (cap.exists()) {
				FileInputStream fis = null;
				try {
					// Envio el fichero
					fis = new FileInputStream(cap);
					int readed;
					do {
						readed = fis.read(buffer);
						out.write(buffer,0,readed);
					} while (readed==buffer.length);
					out.flush();
				} finally {
					if (fis!=null) {
						try{fis.close();} catch (Exception e) {}
					}
				}
			}
		} catch (Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}

public class CapturesServer extends Thread {
	private ServerSocket serverSocket;

	public CapturesServer() throws IOException {
		int port = InspectorRuntime.getPropertyAsInteger("Dispatcher", "Captures.Port");
		
		serverSocket = new ServerSocket(port);
	}

	public void run() {
		while(true) 
		{
			Socket clientSocket=null;

			try {
				clientSocket = serverSocket.accept();
				CapturesServerInstance instance = new CapturesServerInstance(clientSocket);
				instance.start();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}


}
