package sondas.dispatcher;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import sondas.Config;
import sondas.ConversationManager;
import sondas.Global;
import sondas.Metadata;
import sondas.utils.SondasUtils;

/**
 * Existira un hilo Dispatcher por cada agente
 */
public class Dispatcher extends Thread
{
	final private static String version="SondasPlorer 0.1";
	private static Config cfg;
	private static HashMap dispatchers = new HashMap();
	private static ServerSocket serverSocket;
	private static String agentsList;
	private static Set agentsInUse = Collections.synchronizedSet(new HashSet());
	private Map catDict;	
	private Socket agent;
	private DataInputStream agentInputStream;
	private boolean exit;
	private String agentHost;
	private int agentDataPort;
	private int agentMessagesPort;
	private String agentName;
	private HashMap processors; // pid-instance
	private Set processorInstances;
	private volatile boolean connectedToAgent;

	private Dispatcher(String agentName) {
		agentHost = cfg.getProperty(agentName, "host");
		agentDataPort = cfg.getPropertyAsInteger(agentName,"data.port");
		agentMessagesPort = cfg.getPropertyAsInteger(agentName,"messages.port");
		this.agentName = agentName;

		loadProcessors();
	}

	public String getAgentName() {
		return agentName;
	}

	public boolean isConnectedToAgent() {
		return connectedToAgent;
	}

	public String getAgentHost() {
		return agentHost;
	}

	public int getAgentDataPort() {
		return agentDataPort;
	}

	public int getAgentMessagesPort() {
		return agentMessagesPort;
	}

	private static void setupDispatcher() throws IOException 
	{
		System.out.println("Loading Sondasplorer Dispatcher...");

		/*
		String cfgFile = System.getProperty("sondasplorer.cfg");		
		if (cfgFile==null) {
			throw new RuntimeException("Undefined sondasplorer.cfg");
		} else {
			System.out.println("Sondasplorer config: "+cfgFile);	
		}

		Global.loadConfiguration(cfgFile);
		 */

		// Leo el fichero de configuracion
		String cfgFile = System.getProperty("sondas.dispatcher.cfg");
		if (cfgFile==null) {
			throw new RuntimeException("Undefined sondas.dispatcher.cfg");
		} else {
			System.out.println("Dispatcher config: "+cfgFile);	
		}

		cfg = new Config(cfgFile);

		String agents = cfg.getProperty("General","agents");
		loadAgents(agents);

		// Creo el receptor de mensajes
		createMessagesReceiver();
	}

	private static void createMessagesReceiver() {
		int port = cfg.getPropertyAsInteger("General","port");
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't create dispatcher server",e);
		}

		System.out.println("Dispatcher server listening on port "+port);



		/*

		Set agentsInUse = Collections.synchronizedSet(new HashSet());

		while(true) 
		{
			Socket clientSocket=null;

			try {
				clientSocket = serverSocket.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

				String cmd = in.readLine();

				if (cmd.startsWith("connect ")) 
				{
					// El comando esperado es: connect agent pid
					StringTokenizer st = new StringTokenizer(cmd," ");
					st.nextToken();
					String agent = st.nextToken();
					int pid = Integer.valueOf(st.nextToken());

					String agentPidKey = agent+":"+pid;
					if (agentsInUse.contains(agentPidKey)) {
						writeln(Dispatcher.Error,out, "Agent:Pid in use");
						try {clientSocket.close();} catch (IOException e1) {}
					} else {
						agentsInUse.add(agentPidKey);
						Dispatcher dispatcher = (Dispatcher) dispatchers.get(agent);
						if (dispatcher==null) {
							writeln(Dispatcher.Error,out, "Agent not defined: "+agent);
							throw new RuntimeException("Agent not defined");
						}
						writeln(Dispatcher.Ok,out, "Connected");
						DispatcherClientConnection dcc = new DispatcherClientConnection(agent, pid, dispatcher, in, out, clientSocket, agentsInUse);
						dcc.start();				
					}
				} else if (cmd.equals("get agents")) {
					writeln(Dispatcher.Ok, out,agentsList);
					try {clientSocket.close();} catch (IOException e1) {}
				} else {
					throw new RuntimeException("Protocol error. Invalid command: "+cmd);
				}

			} catch (Throwable e) {
				e.printStackTrace();
				if (clientSocket!=null) {
					try {clientSocket.close();} catch (IOException e1) {}
				}
			}
		}*/

		while(true) 
		{
			Socket clientSocket=null;

			try {
				clientSocket = serverSocket.accept();
				DispatcherClientThread dct = new DispatcherClientThread(clientSocket, agentsInUse, dispatchers, agentsList);
				dct.setDaemon(true);
				dct.start();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}






	private static void loadAgents(String agentsCsv) {
		agentsList = agentsCsv;
		List agents = SondasUtils.parseCvsString(agentsCsv);
		for (int i=0;i<agents.size();i++) {
			String agentName = (String)agents.get(i);
			Dispatcher dis = new Dispatcher(agentName);
			dis.setDaemon(true);
			dispatchers.put(agentName, dis);
			dis.start();
		}
	}


	public Map getCategoriesDictionary() {
		return catDict;
	}

	public IProcessor getProcessor(int pid) {
		return (IProcessor) processors.get(pid);
	}

	public void run() 
	{
		while (!isInterrupted()) { 
			try {
				manageAgentConnection();
			} catch (SocketException e1) {
				System.err.println("Connection to agent "+agentName+" lost. Retrying in 15 seconds");
			} catch (Throwable e) {
				e.printStackTrace();
				System.err.println("Connection to agent "+agentName+" lost. Retrying in 15 seconds");
			}

			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	public void manageAgentConnection() throws UnknownHostException, IOException 
	{
		// DEBUG!!
		// Conecto con agente
		/*
		System.out.println("Debug mode on");
		//agentInputStream = new DataInputStream(new FileInputStream("d:/tmp/prfa_carga.trz"));		
		agentInputStream = new DataInputStream(new FileInputStream("d:/tmp/carga_mt.trz"));
		 */

		try {
			agent = new Socket(agentHost, agentDataPort);
			agentInputStream = new DataInputStream(agent.getInputStream());

			// Leo la version del agente
			// sondasplorer 0.1
			String version = SondasUtils.readString(agentInputStream);
			if (!version.equals(this.version)) {
				throw new RuntimeException("Agent version mismatch: "+version+", Dispatcher version: "+this.version);
			}
			System.out.println("Connected");

			// Leo la cabecera
			String _agentName = SondasUtils.readString(agentInputStream);
			if (!agentName.equals(_agentName)) {
				throw new RuntimeException("Remote agent mismatch: "+agentName+","+_agentName);
			}
			// Leo las categorias
			catDict = new HashMap();

			SondasUtils.readHeader(catDict, agentInputStream);

			connectedToAgent=true;
			Metadata metadata;


			while (!exit) {
				// Leo el codigo de la sonda
				int pid;

				try {
					pid = agentInputStream.readShort();
					if (pid==-1) {
						// TerminateSignal recibido
						return;
					}
				} catch (EOFException e) {
					// El sevidor ha cerrado el canal
					return;
				} 

				// Obtengo el procesador
				IProcessor processor = (IProcessor) processors.get(Integer.valueOf(pid));

				// Invoco al procesador
				try {
					processor.execute(agentInputStream);
				} catch (Throwable e) {
					throw new RuntimeException("Error in processor, sync lost. Exiting",e);
				}
			} 
		}finally {
			if (agent!=null) {
				connectedToAgent=false;
				try {agent.close();} catch (Throwable e) {}
			}
		}
	}

	private void loadProcessors() {
		processors = new HashMap();
		processorInstances = new HashSet();

		List procs = cfg.getMultipleProperty(agentName, "processor");
		for (int i=0;i<procs.size();i++) {
			String processorLine = (String) procs.get(i);
			defineProcessor(processorLine);
		}

	}

	private void defineProcessor(String processorLine) 
	{
		// ProbeId;ProcessorClass;Worker1;Worker2;...;WorkerN
		StringTokenizer st = new StringTokenizer(processorLine,";");
		int pid = Integer.valueOf(st.nextToken());
		String processorClassName = st.nextToken();

		List workers = new ArrayList();

		try {
			while (st.hasMoreTokens()) {
				String workerClazz = st.nextToken();
				// Instancio el worker y lo añado a la lista
				Class clazz = Class.forName(workerClazz);
				Constructor ctor = clazz.getConstructor(Dispatcher.class);
				workers.add(ctor.newInstance(this));
			}
		} catch (Exception e) {
			throw new RuntimeException("Error loading processor",e);
		}

		// Instancio la clase 
		IProcessor processor = (IProcessor) SondasUtils.newInstance(processorClassName);
		processor.setWorkers(workers);

		processors.put(Integer.valueOf(pid), processor);

		processorInstances.add(processor);
	}



	public static void main(String[] args) throws Exception {
		/*
		System.setProperty("sondasplorer.cfg", "D:/proyectos_ws/Sondasplorer/cfg/sondasplorer.cfg");
		System.setProperty("sondas.dispatcher.cfg","D:/proyectos_ws/Sondasplorer/cfg/dispatcher.cfg");
		System.setProperty("inspector.cfg","D:/proyectos_ws/Sondasplorer/cfg/inspector.cfg");
		 */

		Dispatcher.setupDispatcher();
	}
}
