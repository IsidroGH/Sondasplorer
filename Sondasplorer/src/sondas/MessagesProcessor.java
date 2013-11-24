package sondas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import sondas.utils.DataSender;
import sondas.utils.IMarshable;
import sondas.utils.SignaledBuffer;
import sondas.utils.SondasUtils;

public class MessagesProcessor extends Thread {
	private ServerSocket serverSocket=null;
	private Socket clientSocket = null;
	private int port;
	private BufferedReader in;
	private BufferedWriter out;
	private static HashMap modules = new HashMap();

	public MessagesProcessor(int port) {
		this.port = port;

		configureModules();
	}

	private void configureModules() {
		int modulesCount = Global.getPropertyAsInteger("MessagesProcessor", "modules.count");
		for (int i=1;i<=modulesCount;i++) {
			String className = Global.getProperty("MessagesProcessor", "module."+i);

			// Instancio
			BaseMessagesProcessorModule module = (BaseMessagesProcessorModule) SondasUtils.newInstance(className);
			modules.put(module.getName(), module);	
		}		
	}

	public static BaseMessagesProcessorModule getModule(String name) {
		return (BaseMessagesProcessorModule) modules.get(name);
	}

	public static BaseMessagesProcessorModule getBaseModule() {
		return (BaseMessagesProcessorModule) modules.get("");
	}

	

	public void run() 
	{
		while (true) {
			try {
				serverSocket = new ServerSocket(port);
			} catch (IOException e) {
				System.out.println("Could not listen on port "+port);
				return;
			}

			System.out.println("Commands processor listening on port "+port);

			while(true) 
			{
				try {
					clientSocket = serverSocket.accept();
				} catch (Throwable e) {
					e.printStackTrace();
					try {serverSocket.close();} catch (IOException ex){}
					break;
				}

				// Indica el modulo receptor de mensajes
				// Si es "" va al general. Si contiene un valor es el nombre del modulo
				String state="";

				try {
					String inputLine;

					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					
					ConversationManager.sendResponse(ConversationManager.Ok, out, "Sondasplorer messages processor v0.1");
					
					///write(">");
					while ((inputLine = in.readLine()) != null) {
						inputLine = inputLine.trim();
						if (inputLine.equals("")) {
							///write(state+">");
							continue;
						}
						
						if (state.equals("")) {
							if (inputLine.startsWith("module ")) {
								StringTokenizer st = new StringTokenizer(inputLine);
								st.nextToken();
								String newState = st.nextToken();
								if (!modules.containsKey(newState)) {
									ConversationManager.sendResponse(ConversationManager.Error, out, "Module not defined");
									///write(state+">");
									continue;
								} else {
									state = newState;
									///write(state+">");
									ConversationManager.sendResponse(ConversationManager.Ok, out, "Ok");
									continue;
								}
							}
						}

						if (inputLine.equals("exit")) {
							if (state.equals("")) {
								break;
							} else {
								state="";
								///write(">");
								continue;
							}
						}

						getModule(state).execute(inputLine, out);
						
						///write(state+">");
					}
				} catch (EOFException e1) {
					
				} catch (SocketException e3) {
					System.err.println("Connection error: "+e3.getMessage());
				} catch (Throwable e2) {
					System.err.println("Protocol error: ");
					e2.printStackTrace();
				} finally {
					//InspectorRuntime.getInstance().stopCapture();
					
					getModule(state).abort();
					
					if (clientSocket!=null) {
						try {clientSocket.close();} catch (IOException ex){}
					}
				}
			}
		}

	}

}

