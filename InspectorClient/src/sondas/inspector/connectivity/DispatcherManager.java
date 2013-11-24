package sondas.inspector.connectivity;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import sondas.ConversationException;
import sondas.ConversationManager;
import sondas.client.inspector.ClientException;
import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.DispatcherException;
import sondas.dispatcher.RespBean;

public class DispatcherManager
{
	private String host;
	private int port;
	private Socket con; // Conexion con el Dispatcher
	private BufferedWriter outText;
	private BufferedReader inText;
	private OutputStream outBin;
	private InputStream inBin;
	private boolean captureStarted;
	private String agent;
	private boolean connected;
	private int capPort;


	public DispatcherManager(String host, int port, int capPort) {
		this.host=host;
		this.port=port;
		this.capPort=capPort;
	}

	public String getAgent() {
		return agent;
	}
	
	
	public void connect() throws ClientException
	{
		try {
			disconnect();
			
			System.err.println("Dispatcher Manager new Connection");
			con = new Socket(host,port);
			outBin = con.getOutputStream();
			inBin = con.getInputStream();
			inText = new BufferedReader(new InputStreamReader(inBin));
			outText = new BufferedWriter(new OutputStreamWriter(outBin));
			connected = true;
		} catch (Exception e) {
			connected=false;
			throw new ClientException("Error connecting to Dispatcher",e);
		}
	}

	public void setAgent(String agent) {
		this.agent=agent;
	}

	public void unsetAgent() {
		this.agent=null;
	}

	public void disconnect() {
		connected=false;
		try {con.close();} catch (Exception e) {}
	}

	public List<String> getAgents() throws ClientException 
	{
		boolean ok=false;
		ArrayList<String> resp = new ArrayList<String>();

		try {
			RespBean aux = ConversationManager.sendCommand(inText, outText, "get agents");

			if (aux.code==ConversationManager.Error) {
				throw new ClientException(aux.msg);
			}

			StringTokenizer st = new StringTokenizer(aux.msg,",");
			while (st.hasMoreTokens()) {
				resp.add(st.nextToken());
			}
			ok=true;
		} catch (IOException e) {
			throw new ClientException("Error retrieving agents",e);
		} finally {
			if (!ok) {
				disconnect();
			}
		}
		return resp;
	}

	public boolean existsCapture(String captureName) throws ClientException {
		/*
		set agent Agente1 2 // Entro en la sesion del agente
		exists capture cap1
		exit
		 */
		boolean ok=false;
		checkState();

		try {
			sendPartialCmd("set agent "+agent+" 2");
			RespBean resp = sendPartialCmd("exists capture "+captureName);
			sendPartialCmd("exit");
			boolean exists = resp.msg.equals("1");
			ok=true;
			return exists;
		} catch (ConversationException e) {
			throw new ClientException(e.getMessage(),e);
		} catch (IOException e2) {
			throw new ClientException("Error checking capture",e2);
		} finally {
			if(!ok) {
				disconnect();
			}
		}

	}

	private void checkState() throws ClientException {
		if (agent==null) {
			throw new ClientException("No agent established");
		}
		
		if (con.isClosed()) {
			connect();
		}
		
		if (!connected) {
			connect();
		}
	}

	public void startCapture(String captureName, int slice) throws ClientException {
		/*
		set agent Agente1 2 // Entro en la sesion del agente
		connect // Conecto con el agente
		set capture cap1
		A module inspector
		A set delta
		A start
		A stop
		end capture
		disconnect // Desconecto del agente
		exit // Salgo de la sesion del agente
		exit // Salgo de la sesion del dispatcher
		 */
		boolean ok = false;
		checkState();

		try {
			sendPartialCmd("set agent "+agent+" 2");
			sendPartialCmd("connect");
			sendPartialCmd("set capture "+captureName);
			sendPartialCmd("A module inspector");
			sendPartialCmd("A set delta");
			sendPartialCmd("A set slice "+slice);
			sendPartialCmd("A start");

			captureStarted=true;
			ok=true;
		} catch (ConversationException e) {
			throw new ClientException("Error starting capture",e);
		} catch (IOException e2) {
			throw new ClientException("Error starting capture",e2);
		} finally {
			if (!ok) {
				disconnect();
			}
		}
	}

	public void stopCapture() throws ClientException {
		/*
		set agent Agente1 2 // Entro en la sesion del agente
		connect // Conecto con el agente
		set capture cap1
		A module inspector
		A set delta
		A start
		A stop
		end capture
		disconnect // Desconecto del agente
		exit // Salgo de la sesion del agente
		exit // Salgo de la sesion del dispatcher
		 */

		boolean ok=false;
		checkState();

		if (!captureStarted) {
			throw new ClientException("Capture not started");
		}

		try {
			sendPartialCmd("A stop");
			sendPartialCmd("end capture");
			sendPartialCmd("disconnect");
			sendPartialCmd("exit"); // Desconecto del agente
			captureStarted=false;
			ok=true;
		} catch (ConversationException e) {
			throw new ClientException("Error stoping capture",e);
		} catch (IOException e2) {
			throw new ClientException("Error starting capture",e2);
		} finally {
			if (!ok) {
				disconnect();
			}
		}
	}
	
	public List<String> getCaptures() throws ClientException {
		/*
		set agent Agente1 2 // Entro en la sesion del agente
		get captures
		exit
		 */
		ArrayList<String> captures = new ArrayList<String>();
		
		boolean ok=false;
		checkState();

		try {
			sendPartialCmd("set agent "+agent+" 2");
			RespBean resp = sendPartialCmd("get captures");
			sendPartialCmd("exit");
			
			StringTokenizer st = new StringTokenizer(resp.msg,",");
			while (st.hasMoreTokens()) {
				captures.add(st.nextToken());
			}
			ok=true;
			return captures;
		} catch (ConversationException e) {
			throw new ClientException(e.getMessage(),e);
		} catch (IOException e2) {
			throw new ClientException("Error retrieving captures",e2);
		} finally {
			if (!ok) {
				disconnect();
			}
		}
	}
	
	public InputStream getCapture(String captureName) throws ClientException {
		/*
		set agent Agente1 2 // Entro en la sesion del agente
		get capture capture
		(se cierra el canal una vez leido)
		 */
		InputStream resp = null;
		try {
			Socket server = new Socket(host,capPort);
			BufferedWriter os = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
			os.write(agent+";"+captureName+"\r\n");
			os.flush();
			resp =  server.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return resp;
		
		
/*
		try {
			sendPartialCmd("set agent "+agent+" 2");
			RespBean resp = sendPartialCmd("get capture "+captureName);
			System.err.println("Capture size: "+resp.msg+","+con.isClosed());
			
			connected=false;
			return inBin;
		} catch (ConversationException e) {
			disconnect();
			throw new ClientException(e.getMessage(),e);
		} catch (IOException e2) {
			disconnect();
			throw new ClientException("Error retrieving captures",e2);
		} 
*/		
	}
	
	public boolean isConnected() {
		return connected;
	}

	private RespBean sendPartialCmd(String cmd) throws IOException, ConversationException {
		return ConversationManager.sendPartialCommand(inText, outText, cmd);
	}

	private RespBean sendCmd(String cmd) throws IOException {
		return ConversationManager.sendCommand(inText, outText, cmd);
	}
}
