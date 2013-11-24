package sondas.client.inspector.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.management.RuntimeErrorException;
import javax.swing.ProgressMonitorInputStream;

import sondas.Metadata;
import sondas.client.inspector.ClientException;
import sondas.client.inspector.ClientInspectorRuntime;
import sondas.client.inspector.Preferences;
import sondas.client.inspector.view.nodes.IProbeViewer;
import sondas.client.inspector.view.nodes.MethodProbeViewer;
import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.dispatcher.inspector.IFactory;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.Stamp;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.probes.delta.MethodNode;
import sondas.utils.SondasUtils;

public class Model {
	private DecimalFormat dec;
	final private int fullProbePid;
	final private int deltaProbePid;
	final private int MetadataPid=0;
	private DeltaTree resultTree;
	private HashMap<Integer, DeltaTree> threadsDeltaTrees;
	private StringsCache stringsCache;
	private StringsCache cachedStringsCache;
	private long iniTs;
	private long endTs;
	private Map< Long/*TimeStamp*/, Map<Integer,IGlobalStats> > timedGlobalMaps;
	private Map<Integer/*Id*/,String /*Name*/> catDict;
	private Map<String,Integer> catDictInv;
	private static Map<Integer,String> methodNames;
	private int methodNamesCount;
	final private String licensesServer;
	final private HashSet<String> entryPointMethods;
	private long minTs=-1;
	private long maxTs=-1;
	private DataOutputStream dataCachedTree;
	private DataOutputStream indexCachedTree;

	private long lastCurrentTs=-1;
	private long lastTs=-1;
	private String cacheFile;
	private String indexFile;
	boolean isDataCached;

	// Contiene por cada tipo de metrica las entradas a mostrar en el arbol de metricas temporales
	private Map<String, StatsTreeData> statsTreeDataMap;
	private Map<Integer, Set<MethodNode>> subTreesMap = new HashMap<Integer, Set<MethodNode>>();

	public Model(DataInputStream dis, String cacheFile, String indexFile, boolean isDataCached, int recursivityLevel, long minTs, long maxTs, long iniTs, long endTs, String licensesServer) throws IOException, ClientException 
	{
		if (!isDataCached) {
			// Cuando los datos estan cacheados no cargo de nuevo la metadata
			// Cuando estoy abriendo una prueba, isDataCached=false => reseteo el mapa
			Model.methodNames=new HashMap<Integer, String>();
		}
		
		this.iniTs = iniTs;
		this.endTs = endTs;
		this.minTs = minTs;
		this.maxTs = maxTs;
		this.isDataCached=isDataCached;
		this.cacheFile = cacheFile;
		this.indexFile = indexFile;

		this.licensesServer=licensesServer;

		// Registro los metodos definidos como entry points
		entryPointMethods = new HashSet<String>();
		int methodMetric = InspectorRuntime.getPropertyAsInteger("Metrics", "MethodMetric");
		if (InspectorRuntime.hasProperty("MethodProbe", "EntryPoint")) {
			List eps = InspectorRuntime.getMultipleProperty("MethodProbe", "EntryPoint");
			for (int i=0;i<eps.size();i++) {
				entryPointMethods.add(methodMetric+":"+(String)eps.get(i));
			}
		}

		fullProbePid = ClientInspectorRuntime.getFullProbeId();
		deltaProbePid = ClientInspectorRuntime.getDeltaProbeId();

		threadsDeltaTrees = new HashMap<Integer, DeltaTree>();
		stringsCache = new StringsCache();
		cachedStringsCache = new StringsCache();

		dec = new DecimalFormat();
		dec.setMinimumFractionDigits(2);
		dec.setMaximumFractionDigits(2);


		timedGlobalMaps = new HashMap< Long, Map<Integer,IGlobalStats> >();
		statsTreeDataMap = new HashMap<String, StatsTreeData>();

		//dis.readInt();

		// Leo la cabecera
		readHeader(dis);

		try {
			if (isDataCached) {
				loadCache(dis);
			} else {
				prepareCache();	
			}


			resultTree = doProcess(dis);
			/*
			try {
				resultTree = doProcessLicensed(dis, iniTs, endTs);
			} catch (java.net.ConnectException e) {
				e.printStackTrace();
				throw new ClientException("Error connecting licenses server: "+licensesServer);
			}
			 */
		} finally {
			if (dis!=null) {
				try {dis.close();} catch(Exception e){};
			}
			if (dataCachedTree!=null) {
				try {dataCachedTree.close();} catch(Exception e){};
			}
			if (indexCachedTree!=null) {
				try {indexCachedTree.close();} catch(Exception e){};
			}
		}
	}
	
	static public Map<Integer,String> getMethodNames() {
		return methodNames;
	}

	private void loadCache(DataInputStream dis) throws ClientException {
		try {
			DataInputStream indexDis = new DataInputStream(new FileInputStream(indexFile));
			int headerLen = indexDis.readInt();
			while(true) {
				// Voy leyendo los timestamps hasta que sea >= que iniTs
				long ts = indexDis.readLong();
				long pos = indexDis.readInt();
				if (ts>=iniTs) {
					dis.skip(pos-headerLen);
					break;
				}
			}
		} catch (EOFException e1) {
			throw new ClientException("Index not found. Cache corruption");
		} catch (IOException e) {
			throw new ClientException("Error reading cache",e);
		}
	}

	private void prepareCache() throws ClientException {
		try {
			dataCachedTree = new DataOutputStream(new FileOutputStream(cacheFile));
			indexCachedTree = new DataOutputStream(new FileOutputStream(indexFile));

			// Escribo la cabecera
			SondasUtils.writeHeader(catDictInv, dataCachedTree);
			// Escribo en el indice la longitud de la cabecera
			indexCachedTree.writeInt(dataCachedTree.size());
		} catch (IOException e) {
			throw new ClientException("Error creating cache",e);
		}
	}

	public long getMinTs() {
		return minTs;
	}

	public long getMaxTs() {
		return maxTs;
	}

	private DeltaTree doProcessLicensed(DataInputStream dis, long iniTs, long endTs) throws IOException {
		// Leo la cabecera
		readHeader(dis);
		//return ClientDeltaComposer.doProcessLicensed(dis, iniTs, endTs, timedGlobalMaps, statsTreeDataMap, licensesServer);
		return null;
	}

	/**
	 * Devuelve un array de StatsTreeDatas cada uno de ellos asociado a un tipo de metrica
	 */
	public StatsTreeData[] getStatsTreeDatas() {
		StatsTreeData[] resp = new StatsTreeData[statsTreeDataMap.size()];
		int i=0;
		for (StatsTreeData item:statsTreeDataMap.values()) {
			resp[i++]=item;
		}
		return resp;
	}

	public Map<Integer,String> getCategoriesDictionary() {
		return catDict;
	}

	public  Map< Long/*TimeStamp*/, Map<Integer,IGlobalStats> > getTimedGlobals() {
		return timedGlobalMaps;
	}

	public Map<Integer, IGlobalStats> getGlobalStats() {
		return resultTree.getGlobalMap();
	}

	private boolean fullModeProcess(DataInputStream dis) throws IOException {
		/*
		boolean applyZoner = iniTs != -1 && endTs != -1;

		// El marshall se hace en Stamp
		int nodeId = dis.readInt();
		int tid = dis.readInt();
		long ts = dis.readLong();
		byte metricType = dis.readByte();

		ITrace trace = unmarshallTrace(metricType, dis);

		if (applyZoner) {
			if (ts < iniTs) {
				// No tengo en cuenta la traza
				return true;
			} else if (ts > endTs) {
				// Descarto el resto de metricas
				return false;
			}
		}

		// Automezcla en dispatcher
		// Obtengo el deltatree asociado al hilo
		DeltaTree tree = threadsDeltaTrees.get(tid);
		if (tree == null) {
			tree = new DeltaTree(tid);
			threadsDeltaTrees.put(tid, tree);
		}

		String id = trace.getKey();

		if (trace.getMode() == Constants.Start) {
			if (tree.hasChildNode(id)) {
				tree.openNode(id, ts);
				// update con trace TODO
			} else {
				IDeltaNodeFactory factory = InspectorRuntime.getDeltaProbeFactory(metricType);
				MethodNode node = factory.createNode(trace, stringsCache, metricType, id);
				tree.openNode(node, ts);
			}
		} else {
			// Cierro
			if (metricType == 1) {
				tree.closeNode(nodeId, ts, trace.getMode(), true);
			} else if (metricType == 2) {
				tree.closeNode(nodeId, ts, trace.getMode(), false);
			}
		}
		 */
		return true;
	}

	private void metadataProcess(DataInputStream dis) throws IOException, ClientException 
	{
		Metadata metadata = new Metadata(dis);
		
		List aux = metadata.methodNames;
		for (Iterator it = metadata.methodNames.iterator();it.hasNext();) {
			String methodName = (String) it.next();
			methodNames.put(++methodNamesCount, methodName);
		}
	}
	
	private boolean deltaModeProcess(DataInputStream dis) throws IOException, ClientException 
	{
		DeltaTree currentDt = new DeltaTree(dis, stringsCache);
		
		System.out.println(currentDt.toString(methodNames));

		// DEBUG
		//System.out.println(currentDt.toString());


		// FIN DEBUG


		long currentTs = currentDt.getTimeStamp();
		if (currentTs!=lastTs) {
			System.out.println("Time stamp: "+currentTs);
			lastTs = currentTs;
		}
		
		

		if (iniTs!=-1) {
			if (currentTs<iniTs) {
				return true;
			}
		}

		if (endTs!=-1) {
			if (currentTs>endTs) {
				return false;
			}
		}

		// Si no esta cacheado es porque es la primera vez que se esta
		// leyendo el stream. En este caso se cachea
		if (!isDataCached) {

			if (minTs==-1) {
				// Actualizo con el primer DT
				minTs = currentTs;
			}

			maxTs = currentTs;

			if (currentTs!=lastCurrentTs) {
				// Se trata de un nuevo timestamp => escribo el indice
				indexCachedTree.writeLong(currentTs);
				indexCachedTree.writeInt(dataCachedTree.size());

				// Actualizo el lastCurrentTs
				lastCurrentTs = currentTs;
			}

			// Cacheo el Dt
			cachedStringsCache.clear();
			currentDt.marshall(deltaProbePid, dataCachedTree, cachedStringsCache);
		}

		// Automezcla en dispatcher
		// Obtengo el deltatree asociado al hilo
		int tid = currentDt.getTid();
		DeltaTree composedDt = threadsDeltaTrees.get(tid);

		Map<Integer, IGlobalStats> timedGlobalMap = timedGlobalMaps.get(currentDt.getTimeStamp());

		// Obtengo una copia de la gs porque cuando se componga el arbol total resultado se iran alterando cada uno de los parciales
		//Map<String,GlobalStats> gsCloned = currentDt.getGlobalMapCloned();

		// Obtengo una copia del composedGs
		Map<Integer,IGlobalStats> formerGs=null;
		if (composedDt!=null){
			formerGs = composedDt.getGlobalMapCloned();
		} 

		// Actualizo el composedGs con el arbol que acaba de llegar 
		if (composedDt == null) {
			threadsDeltaTrees.put(tid, currentDt);
			composedDt=currentDt;
		} else {
			composedDt.combine(currentDt);
		}

		// Calculo la Global del intervalo actual como la diferencia entre el composedGs nuevo y el composedGs antiguo 
		Map<Integer,IGlobalStats> intervalGs = getDiferential(composedDt.getGlobalMap(),formerGs);

		// Registro la global por si existen nuevas entradas sin registrar 
		registerTimedStats(intervalGs);

		// Automezcla en disptacher
		// Mezclamos las estadisticas globales de los distintos hilos correspondientes al mismo timestamp
		if (timedGlobalMap!=null) {
			mergeGlobals(timedGlobalMap,intervalGs);	
		} else {
			timedGlobalMaps.put(currentDt.getTimeStamp(), intervalGs);
		}

		return true;
	}

	/**
	 * Devuelve el diferencia a-b
	 * @throws ClientException 
	 */
	private Map<Integer,IGlobalStats> getDiferential(Map<Integer,IGlobalStats> a, Map<Integer,IGlobalStats> b) throws ClientException {
		Map<Integer,IGlobalStats> resp = new HashMap<Integer, IGlobalStats>();

		for (int gsId:a.keySet()) {
			IGlobalStats aGs = a.get(gsId);
			int metricType = aGs.getMetricType();
			IProbeViewer probeViewer = ClientInspectorRuntime.getInstance().getNodeViewer(metricType);

			// Solo muestro las estadisticas temporales si el nodo se ha definido visible en el viewer
			// o si el metodo se ha definido explicitamente como entrypoint en inspector.cfg apartado [MethodProbe]
			if (probeViewer.isTimeStatsVisible() || entryPointMethods.contains(aGs.getMetricType()+":"+aGs.getName(methodNames))) 
			{
				if (b==null || !b.containsKey(gsId)) {
					// Si no existe b o no existe en b, es nuevo y lo pongo directamente en la respuesta
					resp.put(gsId, (IGlobalStats)a.get(gsId).clone());
				} else {
					IGlobalStats diference = a.get(gsId).getDiferential(b.get(gsId));
					resp.put(gsId, diference);
				}
			}
		}

		return resp;
	}

	/**
	 * Recorre cada una de las metricas globales creando la entrada correspondiente asociada a MetricsTree
	 * @throws ClientException 
	 */
	private void registerTimedStats(Map<Integer, IGlobalStats> globalStatsMap) throws ClientException 
	{
		for (IGlobalStats gs:globalStatsMap.values()) {
			int metricType = gs.getMetricType();
			IProbeViewer probeViewer = ClientInspectorRuntime.getInstance().getNodeViewer(metricType);
			
			//if (nodeViewer.isTimeStatsVisible()) {
			probeViewer.registerTimedStats(gs,statsTreeDataMap);
			//}
		}
	}

	/**
	 * Mezcla las globas colocando el destino en el primer parametro
	 */
	private void mergeGlobals(Map<Integer, IGlobalStats> globalMap, Map<Integer, IGlobalStats> dtGlobalMap) 
	{
		for (int key:dtGlobalMap.keySet()){
			if (globalMap.containsKey(key)) {
				globalMap.get(key).merge(dtGlobalMap.get(key));
			} else {
				globalMap.put(key, dtGlobalMap.get(key));
			}
		}
	}

	private void readHeader(DataInputStream dis) throws IOException {
		catDict = new HashMap<Integer, String>();
		catDictInv = new HashMap<String, Integer>();
		
		SondasUtils.readHeader(catDictInv, dis);
		for (String catName:catDictInv.keySet()) {
			int catId = catDictInv.get(catName);
			catDict.put(catId, catName);
		}
		
		// Debug
		/*
		System.err.println("DEBUG Categories");
		catDict.put(0,"Servlets");
		catDict.put(1,"Methods");
		catDict.put(2,"Database");
		 */
	}



	private DeltaTree doProcess(DataInputStream dis) throws IOException {

		DeltaTree _resultTree = new DeltaTree();

		while (true) {
			try {
				int pid = dis.readShort();
				if (pid == fullProbePid) {
					boolean cont = fullModeProcess(dis);
					if (!cont) {
						break;
					}
				} else if (pid == deltaProbePid) {
					boolean cont = deltaModeProcess(dis);
					if (!cont) {
						break;
					}
				} else if (pid == MetadataPid) {
					metadataProcess(dis);
				} else {
					throw new RuntimeException("Not a valid Inspector stream");
				}

			} catch (EOFException e1) {
				break;
			} catch (InterruptedIOException e2) {
				throw e2;
			} catch (Throwable e) {
				e.printStackTrace();
				throw new RuntimeException("Error reading data", e);
			}
		}

		_resultTree.generateEntryNodes();

		// Mezclo todos los arboles asociados a cada uno de los hilos
		for (DeltaTree tree : threadsDeltaTrees.values()) 
		{
			tree.generateEntryNodes();
			_resultTree.merge(tree);
		}

		// Recompongo el mapa de subarboles
		_resultTree.composeSubtreeMap(subTreesMap);

		return _resultTree;		
	}



	private ITrace unmarshallTrace(int metricType, DataInputStream dis) throws IOException {
		// Obtengo la factoria a partir de metricType
		IFactory factory = ClientInspectorRuntime.getTraceFactory(metricType);

		// deserializo la traza y la devuelvo
		return factory.createTrace(dis);
	}

	public Set<MethodNode> getSubtrees(int id) {
		return subTreesMap.get(id);
	}

	public static void main(String[] args) throws IOException, ClientException, InterruptedException {
		System.setProperty("inspector.cfg","D:/proyectos_ws/Sondasplorer/cfg/inspector.cfg");

		DataInputStream dis = null;

		//Thread.sleep(10000);

		try {


			FileInputStream fis = new FileInputStream(	"C:/tmp/sondas/Debug_debug.ins");
			dis = new DataInputStream(fis);

			Model model = new Model(dis, "c:/tmp/a", "c:/tmp/b", false, 100, -1, -1, -1, -1, "");
		} finally {
			try {
				if (dis != null)
					dis.close();
			} catch (Exception e) {
			}
		}
	}

}
