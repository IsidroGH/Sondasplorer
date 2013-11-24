package sondas.inspector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import sondas.Config;
import sondas.Metadata;
import sondas.SondasLoader;
import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.dispatcher.inspector.IFactory;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.IInspectorProbe;
import sondas.inspector.probes.InspectorBaseProbe;
import sondas.inspector.probes.full.MethodsFactory;
import sondas.inspector.probes.full.ServletsFactory;
import sondas.utils.DataSender;
import sondas.utils.MultipleRelation;
import sondas.utils.SondasUtils;
import sondas.utils.TerminateSignal;

//TODO
//Esta clase debe implementar las propiedades definidas en sondas.dispatcher.inspector

public class InspectorRuntime 
{
	private Map metricsTypes;
	private Map factories;
	private Map deltaNodeFactories;
	//private Map properties;
	private Map threadStores;
	private static InspectorRuntime instance = new InspectorRuntime();
	private ArrayList<IInspectorProbe> observedProbes;
	private ArrayList<DeltaTree> deltaTreeList;
	private int deltaTime;
	final private String deltaTreeKey;
	static protected Config config;
	private int sendingInterval; // Tiempo en seg en el que se van a enviar los detalTrees
	final private int DefaultSendingInterval = 15; 
	private boolean isDeltaMode;
	final public Object deltaTreeLock = new Object();
	private DeltaTreeSender deltaTreeSender;
	private int methodNamesStartIndex;

	public static InspectorRuntime getInstance() {
		return instance;
	}

	protected InspectorRuntime() 
	{
		sendingInterval = -1; // No definido
		metricsTypes = new HashMap();
		factories = new HashMap();
		deltaNodeFactories = new HashMap();
		//properties = new HashMap();
		threadStores = new HashMap();
		observedProbes = new ArrayList<IInspectorProbe>();
		deltaTreeList = new ArrayList<DeltaTree>();

		String inspectorCfgFile = System.getProperty("inspector.cfg");
		if (inspectorCfgFile==null) {
			throw new RuntimeException("Property inspector.cfg not defined");
		}

		try {
			configureInspector(inspectorCfgFile);
		} catch (Throwable e) {
			throw new RuntimeException("Error processing Inspector configuration file: "+inspectorCfgFile,e);
		}

		//deltaTreeKey=properties.get("DeltaModePid")+":dt";
		deltaTreeKey=getProperty("DeltaModePid")+":dt";
	}

	public void init(boolean startDeltaSender) {
		if (startDeltaSender) {
			// Creo el hilo de envio de deltatrees
			deltaTreeSender = new DeltaTreeSender();
			deltaTreeSender.start();
		}
	}

	public int getDeltaTime() {
		return deltaTime;
	}

	/**
	 * Sincronizado porque es llamado externamente por multiples hilos
	 * No es necesario hacer los metodos privados de esta clase sincronizados porque 
	 * las actualizaciones a threadStore, deltaTreeList y threadStores van a ser hechas
	 * desde un lock lo que asegura que no se va a llamar a registerDeltaTree desde otro hilo.
	 * Si que tenemos que sincronizar lockDeltaTrees porque es el encargado de establecer el lock.
	 * (Thread confinment)
	 */
	synchronized public void registerDeltaTree(DeltaTree dt, Map threadStore) {
		threadStore.put(deltaTreeKey, dt);
		deltaTreeList.add(dt);
		threadStores.put(dt,threadStore);
		dt.setRegistered(true);
	}

	synchronized private void unregisterDeltaTree(DeltaTree dt) {
		Map threadStore = (Map)threadStores.get(dt);
		deltaTreeList.remove(dt);
		threadStores.remove(dt);
		threadStore.remove(deltaTreeKey);
		dt.setRegistered(false);
	}

	synchronized private void lockDeltaTrees(ArrayList<DeltaTree> trees) throws InterruptedException {
		for (int i=0;i<trees.size();i++) {
			DeltaTree dt = trees.get(i);
			dt.mutex.acquire();
		}
	}

	private void unlockDeltaTrees(ArrayList<DeltaTree> trees) {
		for (int i=0;i<trees.size();i++) {
			DeltaTree dt = trees.get(i);
			dt.mutex.release();
		}
	}

	public void signalTerminateSending() throws IOException {
		try {
			lockDeltaTrees(deltaTreeList);
			DataSender.sendImmediate(TerminateSignal.signal);
		} catch (InterruptedException e) {
		} finally {
			unlockDeltaTrees(deltaTreeList);
		}
	}

	synchronized public void sendDeltas(int pid, long timeStamp) 
	{
		InspectorPacket packet = new InspectorPacket();
		ArrayList treesToUnregister = new ArrayList();
		ArrayList treesToUnlock = new ArrayList();


		try {
			lockDeltaTrees(deltaTreeList);

			// send data

			// Demo
			/*
			{
				treesToUnlock.add(deltaTreeList.get(0));

				deltaTreeList.get(0).setDeltaTimestamp(timeStamp);

				treesToSend.add(deltaTreeList.get(0).softCopyAndClear());

				if (deltaTreeList.get(0).hasCompletedState()) {
					treesToUnregister.add(deltaTreeList.get(0));
				}
			}
			*/

			// Licensed 
			for (int i=0;i<deltaTreeList.size();i++) {
				DeltaTree dt = deltaTreeList.get(i);
				treesToUnlock.add(dt);

				dt.setDeltaTimestamp(timeStamp);

				packet.treesList.add(dt.softCopyAndClear());

				if (dt.hasCompletedState()) {
					treesToUnregister.add(dt);
				}
			}
			



			for (int i=0;i<treesToUnregister.size();i++) {
				DeltaTree dt = (DeltaTree) treesToUnregister.get(i);
				// Si el arbol esta completado es un cadidato a que el hilo haya
				// muerto por lo que lo elimino para no dejarlo en memoria indefinidamente.
				// En el caso que el hilo no estuviera muerto, la sonda creará un nuevo
				// arbol cuando lo necesite
				unregisterDeltaTree(dt);
			}


		} catch (Throwable e) {
			System.err.println("Exception while processing deltas before sending");
			e.printStackTrace();
			stopCapture();
		} finally {
			unlockDeltaTrees(treesToUnlock);
		}


		// Envio los arboles

		try {
			/*
			for (int i=0;i<treesToSend.size();i++) { 
				DeltaTree dt = (DeltaTree) treesToSend.get(i);
				DataSender.sendImmediate(pid, dt);
				// Vacio la cache de strings de los DT si procede  
			}*/

			//DataSender.sendImmediate(pid, treesToSend);
			
			// Envio la metadata
			// Obtiene los nuevos nombres de metodos (incremental)
			List methodNames = SondasLoader.getMethodNames(methodNamesStartIndex);
			
			if (methodNames!=null) {
				methodNamesStartIndex+=methodNames.size();
			}
			
			// Si existe metadata la envio
			if (methodNames!=null /* || Otras condiciones para determinar si existe o no metadata */) {
				Metadata metadata = new Metadata();	
				metadata.methodNames=methodNames;
				packet.metadata = metadata; 
			}
			
			DataSender.sendToPumper(pid, packet);
		} catch (Throwable e) {
			System.err.println("Exception while sending deltas");
			e.printStackTrace();
			stopCapture();
		}
	}

	private void configureInspector(String inspectorCfgFile) throws IOException
	{
		config = new Config(inspectorCfgFile);
		MultipleRelation category;

		// Cacheo Metrics
		category = config.getCategory("Metrics");
		for (Iterator it=category.getKeys().iterator();it.hasNext();) {
			Object key = it.next();
			String value = (String)category.getReference(key);
			metricsTypes.put(key, Integer.parseInt(value));
		}

		// Cacheo MetricFactories
		category = config.getCategory("MetricFactories");
		for (Iterator it=category.getKeys().iterator();it.hasNext();) {
			String key = (String) it.next();
			String value = (String)category.getReference(key);
			IFactory factory = (IFactory) SondasUtils.newInstance(value);
			factories.put(Integer.valueOf(key), factory);
		}

		// Cacheo MetricFactories
		category = config.getCategory("DeltaNodeFactories");
		for (Iterator it=category.getKeys().iterator();it.hasNext();) {
			String key = (String) it.next();
			Integer id = Integer.valueOf(key); 
			String value = (String)category.getReference(key);
			IDeltaNodeFactory factory = (IDeltaNodeFactory) SondasUtils.newInstance(value);
			deltaNodeFactories.put(id, factory);
		}


	}


	/*
	private void configureInspector(String inspectorCfgFile) throws IOException  xxx
	{
		String state="";

		BufferedReader input =  new BufferedReader(new FileReader(inspectorCfgFile));
		try {
			String line = null; //not declared within while loop

			while (( line = input.readLine()) != null)
			{
				line = line.trim();
				if (line.equals("")) {
					continue;
				}

				if (line.startsWith("#")) {
					continue;
				}

				if (line.startsWith("[")) {
					state=line;
				} else {
					KeyValueBean kv = new KeyValueBean(line);
					doAction(state, kv);
				}
			}
		} finally {
			input.close();
		}
	}

	protected void doAction(String state, KeyValueBean kv) {
		if (state.equals("[Metrics]")) {						
			metricsTypes.put(kv.key, Integer.valueOf(kv.value));
		} else if (state.equals("[MetricFactories]")) {
			Integer id = Integer.valueOf(kv.key); 
			IFactory factory = (IFactory) SondasUtils.newInstance(kv.value);
			factories.put(id, factory);
		} else if (state.equals("[DeltaNodeFactories]")) {
			Integer id = Integer.valueOf(kv.key); 
			IDeltaNodeFactory factory = (IDeltaNodeFactory) SondasUtils.newInstance(kv.value);
			deltaNodeFactories.put(id, factory);
		} else if (state.equals("[Properties]")) {
			properties.put(kv.key, kv.value);
		} 
	}*/

	public static IDeltaNodeFactory getDeltaProbeFactory(int metricType) {
		IDeltaNodeFactory resp = (IDeltaNodeFactory)instance.deltaNodeFactories.get(Integer.valueOf(metricType));
		if (resp==null) {
			throw new RuntimeException("Probe factory not defined for metric type: "+metricType);
		} else {
			return resp;
		}
	}


	public int getMetricType(String metric) {
		return ((Integer)metricsTypes.get(metric)).intValue();
	}

	public static int getFullProbeId() {
		return Integer.valueOf(getProperty("FullModePid"));
	}

	public static int getDeltaProbeId() {
		return Integer.valueOf(getProperty("DeltaModePid"));
	}

	public static IFactory getTraceFactory(int metricType) {
		return (IFactory)instance.factories.get(Integer.valueOf(metricType));
		/*
		// Debug
		if (metricType==1) {
			return new ServletsFactory(); 
		} else if (metricType==2) {
			return new MethodsFactory();
		} else {
			throw new RuntimeException("No soportado en debug");	
		}
		 */
	}

	public static String getProperty(String name) {
		String aux = config.getProperty("Properties", name);
		if (aux!=null) {
			return aux;
		} else {
			throw new RuntimeException("Property not defined in Inspector: "+name);
		}
	}

	public static String getProperty(String cat, String name) {
		String aux = config.getProperty(cat, name);
		if (aux!=null) {
			return aux;
		} else {
			throw new RuntimeException("Property not defined in Inspector: "+name);
		}
	}

	public static List getMultipleProperty(String cat, String name) {
		return config.getMultipleProperty(cat, name);
	}

	public static boolean hasProperty(String cat, String name) {
		return config.hasProperty(cat, name);
	}

	public static int getPropertyAsInteger(String name) {
		return config.getPropertyAsInteger("Properties", name);

	}

	public static int getPropertyAsInteger(String name, int defaultValue) {
		return config.getPropertyAsInteger("Properties", name, defaultValue);

	}

	public static int getPropertyAsInteger(String cat, String name) {
		return config.getPropertyAsInteger(cat, name);		
	}

	public static int getPropertyAsInteger(String cat, String name, int defaultValue) {
		return config.getPropertyAsInteger(cat, name, defaultValue);		
	}


	/*
	public static String getProperty(String name) {
		String aux = (String) instance.properties.get(name);

		if (aux!=null) {
			return aux;
		} else {
			throw new RuntimeException("Property not defined in Inspector: "+name);
		}
	}*/



	public void observeProbe(IInspectorProbe probe) {
		observedProbes.add(probe);
	}

	public void setFullMode() {
		isDeltaMode=false;
		for (IInspectorProbe probe:observedProbes) {
			probe.setFullMode();
		}
	}

	public void setDeltaMode() {
		isDeltaMode=true;
		for (IInspectorProbe probe:observedProbes) {
			probe.setDeltaMode();
		}
	}

	public void setSlice(int slice) {
		this.sendingInterval=slice;
	}

	private void reset() {
		for (Iterator it=threadStores.values().iterator();it.hasNext();) {
			Map threadStore = (Map) it.next();
			threadStore.clear();
		}
		
		deltaTreeList.clear();
		threadStores.clear();
	}
	
	synchronized public void startCapture() 
	{
		// Elinino la informacion que queda entre captura y captura
		/*
		for (int i=0;i<deltaTreeList.size();i++) {
			DeltaTree dt = (DeltaTree) deltaTreeList.get(i);
			unregisterDeltaTree(dt);
		}
		*/
		
		reset();
		
		if (sendingInterval==-1) {
			// Si el periodo de envio no esta definido se usa el de por defecto
			sendingInterval = DefaultSendingInterval;
		}

		if (isDeltaMode) {
			deltaTreeSender.startSending(sendingInterval);
		}

		for (IInspectorProbe probe:observedProbes) {
			probe.startCapture();
		}
		
		methodNamesStartIndex = 0;
	}

	synchronized public void stopCapture() {		
		sendingInterval = -1;

		deltaTreeSender.stopSending();

		for (IInspectorProbe probe:observedProbes) {
			probe.stopCapture();
		}
		
		for (int i=0;i<deltaTreeList.size();i++) {
			DeltaTree dt = (DeltaTree) deltaTreeList.get(i);
			unregisterDeltaTree(dt);
		}
	}

	/**
	 * Gestiona el envio de todos los DeltaTree de forma periodica.
	 * Se inicia el hilo desde MPInspector
	 */
	class DeltaTreeSender extends Thread 
	{
		int pid;
		int resolution;
		public volatile boolean exit=false;
		public volatile boolean active=false;

		public DeltaTreeSender () {
			pid = InspectorRuntime.getDeltaProbeId();
		}

		public void setResolution(int resolution) {
			this.resolution=resolution;
		}

		public void startSending(int resolution) {
			this.resolution=resolution*1000;

			synchronized (this) {
				active=true;
				notifyAll();
			}
		}

		public void stopSending() {
			active=false;
			interrupt();
		}


		public void run() 
		{
			/*
			if (1==1) {
				System.out.println("PARANDO EL SENDER");
				return;
			}
			 */

			int elapsedTime=0;
			System.err.println("DeltaTreeSender started");
			while(!exit) {
				try {
					synchronized (this) {
						while (!active) {
							wait();
						}
					}

					Thread.sleep(resolution-elapsedTime);
					long tsStart = System.currentTimeMillis();
					sendDeltas(pid, tsStart-(resolution/2));

					int elapsepTime = (int)(System.currentTimeMillis()-tsStart);
					System.out.println("Debug: Sending time is "+elapsepTime);
					if (elapsepTime>resolution) {
						elapsedTime = resolution;
						System.out.println("Warning: Sending time is greater than resolution");
					}

				} catch (InterruptedException e) {
					active=false;
				}
			}
		}
	}

}
