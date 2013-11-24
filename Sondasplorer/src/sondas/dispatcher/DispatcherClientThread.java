package sondas.dispatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import sondas.ConversationManager;

public class DispatcherClientThread extends Thread 
{
	private Socket clientSocket;
	private BufferedReader in;
	private BufferedWriter outText;
	private OutputStream outBin;
	private Set agentsInUse;
	private Map dispatchers;
	private String agentsList;

	public DispatcherClientThread(Socket clientSocket, Set agentsInUse, Map dispatchers, String agentsList) {
		this.clientSocket = clientSocket;
		this.agentsInUse=agentsInUse;
		this.dispatchers=dispatchers;
		this.agentsList = agentsList;
	}

	public void run() 
	{
		String agentPidKey = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outBin = clientSocket.getOutputStream();
			outText = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			OutputStream o;

			while(true) {

				String cmd = in.readLine();
				
				if (cmd==null) {
					System.err.println("Dispatcher: Client connection closed");
					return;
				}
				
				if (cmd.startsWith("set agent ")) 
				{
					// El comando esperado es: set agent pid
					StringTokenizer st = new StringTokenizer(cmd," ");
					st.nextToken();
					st.nextToken();
					String agent = st.nextToken();
					int pid = Integer.valueOf(st.nextToken());

					agentPidKey = agent+":"+pid;
					if (agentsInUse.contains(agentPidKey)) {
						ConversationManager.sendResponse(ConversationManager.Error,outText, "Agent:Pid in use");
						continue;
					} else {
						agentsInUse.add(agentPidKey);
						Dispatcher dispatcher = (Dispatcher) dispatchers.get(agent);
						if (dispatcher==null) {
							ConversationManager.sendResponse(ConversationManager.Error,outText, "Agent not defined: "+agent);
							continue;
						} /*else if (!dispatcher.isConnectedToAgent()) {
							ConversationManager.sendResponse(ConversationManager.Error,outText, "Not connected to agent: "+agent);
							continue;
						}*/
						ConversationManager.sendResponse(ConversationManager.Ok,outText, "Established");
						AgentClientConnection dcc = new AgentClientConnection(agent, pid, dispatcher, in, outText, outBin, clientSocket, agentsInUse);
						dcc.doWork();
						//dcc.join();
						agentsInUse.remove(agentPidKey);
					}
				} else if (cmd.equals("exit")) {
					ConversationManager.sendResponse(ConversationManager.Ok, outText,"Done");
					try {clientSocket.close();} catch (Exception e) {}
					return;
				} else if (cmd.equals("get agents")) {
					ConversationManager.sendResponse(ConversationManager.Ok, outText,agentsList);
				} else {
					ConversationManager.sendResponse(ConversationManager.Error,outText,"Protocol error. Invalid command: "+cmd);
				}
			}
		} catch (SocketException e1) {
			System.err.println("Dispatcher: Client disconnected");
		} catch (Throwable e) {
			System.err.println("Dispatcher: Client connection error");
			e.printStackTrace();
		} finally {
			agentsInUse.remove(agentPidKey);
			try {clientSocket.close();}catch(Throwable e1){}
		}
	}
}
