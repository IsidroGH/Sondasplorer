package sondas.dispatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;

import sondas.ConversationManager;
import sondas.dispatcher.inspector.IMessagesReceiver;

public class AgentClientConnection /*extends Thread*/ 
{
	private BufferedReader clientIn;
	private BufferedWriter clientOutText;
	private OutputStream clientOutBin;
	BufferedReader agentIn;
	BufferedWriter agentOut;
	private Set agentsInUse;
	private Socket clientSocket;
	private Socket agentSocket;
	private String agent;
	private int pid;
	private Dispatcher dispatcher;
	private String agentHost;
	private int agentPort;
	private boolean connected;

	public AgentClientConnection(String agent, int pid, Dispatcher dispatcher, BufferedReader in, BufferedWriter outText, OutputStream outBin, Socket clientSocket, Set agentsInUse) {
		this.dispatcher = dispatcher;
		this.clientIn=in;
		this.clientOutText=outText;
		this.clientOutBin=outBin;
		this.agentsInUse=agentsInUse;
		this.clientSocket=clientSocket;
		this.agent=agent;
		this.pid=pid;

		// Conecto con el agente
		agentHost = dispatcher.getAgentHost();
		agentPort = dispatcher.getAgentMessagesPort();
	}

	private void connectToAgent(String host, int port) throws Exception { 
		agentSocket = new Socket(host, port);
		agentIn = new BufferedReader(new InputStreamReader(agentSocket.getInputStream()));
		agentOut = new BufferedWriter(new OutputStreamWriter(agentSocket.getOutputStream()));
		// Leo el mensaje de bienvenida
		String resp = agentIn.readLine();
		System.out.println("Connected to Agent: "+host+":"+port+", "+resp);
	}

	/*
	private String sendAgentCommand(String cmd) throws IOException {
		writeln(agentOut, cmd);
		String resp = agentIn.readLine();
		return resp;
	}*/

	// Si el comando empieza por A, es para el agente, en caso contrario se pasa al Processor
	public void doWork() {
		IMessagesReceiver pcr = (IMessagesReceiver) dispatcher.getProcessor(pid);

		try {
			while(true) {
				System.err.println("AgentClientConnection: waiting for command");
				String cmd = clientIn.readLine();
				if (cmd==null) {
					System.err.println("AgentClientConnection: Client connection closed");
					return;
				}
				System.err.println("AgentClientConnection cmd:"+cmd);
				if (cmd.startsWith("A ")) {
					// Mensaje para el agente
					cmd = cmd.substring(2);
					RespBean resp = ConversationManager.sendCommand(agentIn, agentOut, cmd);
					ConversationManager.sendResponse(resp.code, clientOutText,resp.msg);
				} else {
					// Mensaje para el Processor
					if (cmd.equals("disconnect")) {
						try {agentSocket.close();} catch (Exception e) {}
						ConversationManager.sendResponse(ConversationManager.Ok, clientOutText,"Disconnected");
						connected=false;
					} else if (cmd.equals("connect")) {
						try {
							connectToAgent(agentHost, agentPort);
							ConversationManager.sendResponse(ConversationManager.Ok, clientOutText,"Connected");
							connected=true;
						} catch (Exception e) {
							ConversationManager.sendResponse(ConversationManager.Error, clientOutText,e.getMessage());
						}
					} else if (cmd.equals("exit")) {
						if (connected) {
							try {agentSocket.close();} catch (Exception e) {}
						}
						ConversationManager.sendResponse(ConversationManager.Ok, clientOutText,"Agent session finished");
						return;
					} else {
						// Peticion para los workers
						RespBean resp = pcr.sendCommand(cmd, clientOutText, clientOutBin);

						if (resp.code!=ConversationManager.Ignored) {
							ConversationManager.sendResponse(resp.code, clientOutText,resp.msg);
						}
					}
				}
			}
		} catch (SocketException e) {

			if (!clientSocket.isClosed()) {
				e.printStackTrace();
				System.err.println("Error in socket");
				try {clientSocket.close();} catch (IOException e1) {}

			} else {
				agentsInUse.remove(agent+":"+pid);
			}
		} catch (Throwable e) {
			System.err.println("Error in conversation");
			e.printStackTrace();
			try {
				ConversationManager.sendResponse(ConversationManager.Error, clientOutText,"Error in agent client connection");
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			try {clientSocket.close();} catch (IOException e1) {}
			agentsInUse.remove(agent+":"+pid);
		}
	}

	/*
	private static void writeln(int code, Writer out, String text) {
		write(out, code+" "+text+"\r\n");
	}

	private static void writeln(Writer out, String text) {
		write(out, text+"\r\n");
	}

	private static void write(Writer out, String text) {
		try {
			out.write(text);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/


}
