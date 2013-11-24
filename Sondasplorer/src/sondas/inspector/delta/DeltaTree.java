package sondas.inspector.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.inspector.probes.delta.MethodNode;
import sondas.utils.Mutex;
import sondas.utils.ReentrantLock;
import sondas.utils.SondasUtils;


/*
 * Nota de Rendimiento:
 * 	Ahora al capturar se va construyendo la tabla de entrypoints.
 *  Al serializar mediante marshall de sondasplorer no se envia esta tabla ya que en el unmarshall se recomponen los entrypoints
 *  Al serializar mediante java se envia todo
 */

public class DeltaTree implements IDeltaMarshable, Serializable
{
	private static final long serialVersionUID = -1750149060604903296L;
	private static final String sonda="sonda"; // Demo
	private MethodNode currentNode;
	private MethodNode startNode;
	private long completionTs;
	private int entryPointLevel;
	private int recursivityLevel;
	private int maxRecursivityLevel=getRecursivityLevel();
	transient public ReentrantLock mutex = new ReentrantLock();
	private int tid;
	private boolean registered;

	private HashMap<Integer, IGlobalStats> globalStatsMap = new HashMap<Integer, IGlobalStats>();

	// Variables auxiliares para optimizar la mezcla de arboles
	transient private MethodNode _treeCurrentNode;
	transient private boolean _treeCurrentNodeFound;
	transient private Map subtreesMap;
	transient private Map<Integer, MethodNode> entryNodes;

	transient static private DataOutputStream currentDos;

	public void composeSubtreeMap(Map subtreesMap) {
		this.subtreesMap = subtreesMap;
		for (MethodNode entryNode:entryNodes.values()) {
			composeSubtreeMap(entryNode);	
		}
	}

	private void composeSubtreeMap(MethodNode node) {
		// Añado el nodo actual
		int id = node.getId();
		if (id!=-1) {
			Set subTrees = (Set)subtreesMap.get(id);
			if (subTrees==null) {
				subTrees = new HashSet();
				subtreesMap.put(id, subTrees);
			} 
			subTrees.add(node);
		}
		// Añado sus hijos
		for (MethodNode childNode:node.children.values()) {
			composeSubtreeMap(childNode);
		}
	}

	public DeltaTree softCopyAndClear() 
	{
		DeltaTree dtResp = new DeltaTree();

		// Hago la soft copy
		dtResp.currentNode = currentNode;
		dtResp.startNode = startNode;
		dtResp.completionTs = completionTs;
		dtResp.entryPointLevel = entryPointLevel;
		dtResp.recursivityLevel = recursivityLevel;
		dtResp.globalStatsMap = globalStatsMap;
		dtResp.tid = tid;

		// Reseteo el arbol conservando el estado
		currentNode = null;
		startNode = null;
		completionTs = 0;
		globalStatsMap = new HashMap<Integer, IGlobalStats>();

		return dtResp;
	}

	/**
	 * Vacia el arbol conservando las variables de estado.
	 * Debe ser llamado cuando el arbol es enviado
	 */
	public void clear() {
		currentNode = null;
		startNode = null;
		completionTs = 0;
		globalStatsMap.clear();		
	}

	/**
	 * Indica que el arbol esta en un estado completado, es decir,
	 * que no se esta esperando mas datos para cerrar nodos abiertos.
	 */
	public boolean hasCompletedState() {
		return entryPointLevel==0 && recursivityLevel==0;
	}

	public void setDeltaTimestamp(long ts) {
		completionTs=ts;
	}

	public DeltaTree(){
		this (0);
	}

	public void setRegistered(boolean state) {
		registered=state;
	}

	public boolean isRegistered() {
		return registered;
	}

	public DeltaTree(int tid) {
		initialize();
		this.tid = tid;
	}

	/**
	 * Establece las variables de estado entryPointLevel y recursivityLevel
	 */
	public void setState(DeltaTree _tree) {
		entryPointLevel=_tree.entryPointLevel;
		recursivityLevel=_tree.recursivityLevel;
	}

	/*
	public DeltaTree(long baseTimeStamp, DeltaTree previousTree, int tid) {
		initialize();

		this.baseTs=baseTimeStamp;
		this.tid = tid;

		if (previousTree!=null) {
			this.entryPointLevel=previousTree.getEntryPointLevel();
			this.recursivityLevel=previousTree.recursivityLevel;
		} else {
			this.entryPointLevel = 0;
			this.recursivityLevel = 0;
		}
	}
	 */

	/**
	 * Devuelve una copia del la globalMap
	 */
	public Map<Integer, IGlobalStats> getGlobalMapCloned() {
		HashMap<Integer, IGlobalStats> resp = new HashMap<Integer, IGlobalStats>();
		for (int key:globalStatsMap.keySet()) {
			IGlobalStats gsCloned = (IGlobalStats)globalStatsMap.get(key).clone();
			resp.put(key, gsCloned);
		}
		return resp;
	}

	public Map<Integer, IGlobalStats> getGlobalMap() {
		return globalStatsMap;
	}

	public void addGlobalStats(int id, IGlobalStats stats) {
		globalStatsMap.put(id,stats);
	}

	public int getTid() {
		return tid;
	}

	private void initialize() {
	}
	
	static private int getRecursivityLevel() {
		// TODO parametrizar
		// Licensed
		return 10;
		
		// Demo
		//return sonda.length();
	}

	public boolean isEmpty() {
		return startNode==null;
	}

	public int getEntryPointLevel() {
		return entryPointLevel;
	}

	public boolean hasChildNode(int id) {
		return (currentNode!=null && currentNode.hasChild(id));
	}

	public long getTimeStamp() {
		return completionTs;
	}

	/**
	 * Informa de la apertura de un nodo que ya existe.
	 * Para saber si existe, hay que invocar previamente al metodo hasChildNode.
	 * Actualiza el ts de la apertura.
	 */
	public MethodNode openNode(int id, long ts) 
	{
		currentNode = currentNode.open(id, ts);

		// No necesito comprobar si estoy en un entrypoint ni si se ha superado
		// el nivel de recursividad porque si no no existiría el nodo

		if (currentNode.isEntryPoint) {
			entryPointLevel++; 
		}

		recursivityLevel++;

		return currentNode;
	}

	/**
	 * Informa de la apertura de un nodo que no existe aun.
	 * Para saber si existe, hay que invocar previamente al metodo hasChildNode.
	 * Actualiza el ts de la apertura.
	 */
	public void openNode(MethodNode node, long ts) 
	{
		String aux;
		if (currentNode==null) {
			aux=null;
		} else {
			aux=""+currentNode.getId();
		}
		
		// Solo tengo en cuenta el nuevo nodo si entrypoint>0
		if (entryPointLevel>0 || node.isEntryPoint) 
		{
			if (recursivityLevel<maxRecursivityLevel) {
				// Solo tengo en cuenta el nodo si no se ha superado el nivel maximo recursividad

				if (currentNode==null) {
					// El arbol empieza abriendo
					// El currentNode va ser el nodo que estoy abriendo
					currentNode = node;
					startNode = currentNode;
					currentNode.startOpen(ts);
				} else {
					currentNode.open(node, ts);
					currentNode = node;
				}

				if (node.isEntryPoint) {
					entryPointLevel++;
				}
			}

			recursivityLevel++;
		}

	}

	/**
	 * Devuelve la GlobalStats para el tipo de metrica e id especificado
	 */
	public IGlobalStats getGlobalStats(int id) {
		return globalStatsMap.get(id);
	}

	/**
	 * nodeId indica el ID del nodo relacionado con la sonda. Se usa para clasificar jerarquicamente los resultados globales de cada uno
	 * de los nodos.
	 * 
	 * Mode:
	 * 2 = End
	 * 3 = Exception
	 */
	public void closeNode(int nodeId, long ts, int mode, boolean isEntryPoint) 
	{
		// Solo tengo en cuenta el cierre del nodo si entrypoint>0
		if (entryPointLevel>0) 
		{
			if (recursivityLevel<=maxRecursivityLevel) {
				MethodNode auxCurrentNode;

				if (currentNode==null) {
					// El arbol empieza cerrando
					auxCurrentNode = MethodNode.createUndefinedNode();
					startNode = auxCurrentNode;
					//closingRootNode = true;
				} else {
					auxCurrentNode = currentNode;	
				}

				// Posiciono el cursor en el padre
				MethodNode parent = auxCurrentNode.getParent();
				if (parent==null) {
					// No tiene padre aun => creo un padre indeterminado
					parent = auxCurrentNode.createUndefinedParent();
				}  

				currentNode = parent;

				auxCurrentNode.close(nodeId, ts, mode, globalStatsMap);

				if (isEntryPoint) {
					entryPointLevel--;
				}
			}

			recursivityLevel--;
		} 
	}



	private void detach(MethodNode node) {
		if (node==startNode) {
			startNode = node.getParent();
		}

		node.detach();
	}

	/**
	 * Copia un delta tree.
	 * N copia la global
	 */
	private void copy(DeltaTree _tree) {
		_treeCurrentNode = _tree._treeCurrentNode;
		_treeCurrentNodeFound = _tree._treeCurrentNodeFound;
		currentNode=_tree.currentNode;
		entryPointLevel=_tree.entryPointLevel;
		startNode=_tree.startNode;
		recursivityLevel=_tree.recursivityLevel;
		tid = _tree.tid;
		//globalStatsMap = _tree.globalStatsMap;
	}
	
	public void generateEntryNodes() {
		if (entryNodes==null) {
			entryNodes = new HashMap<Integer, MethodNode>();
		}
		
		if (startNode!=null) {
			// Si es null es porque se trata de un DT vacio
			for (MethodNode node:getRootNode().children.values()) {
				entryNodes.put(node.getId(), node);
			}
		}
		
	}

	/**
	 * Mezcla la global
	 * Destruye _tree
	 */	
	public void merge(DeltaTree _tree) 
	{
		// Mezclo cada uno de los entrynodes partiendo de tree
		for (int entryPointId:entryNodes.keySet()) {
			MethodNode _entryPointNode = _tree.entryNodes.get(entryPointId);
			if (_entryPointNode!=null) {
				// Mezclo si _tree tiene ese entrypoint
				MethodNode entryPointNode = entryNodes.get(entryPointId);
				mergeNodes(entryPointNode, _entryPointNode);
				_tree.entryNodes.remove(entryPointId);
			}
		}

		// Mezclo cada uno de los entrynodes partiendo de _tree
		for (int _entryPointId:_tree.entryNodes.keySet()) {
			MethodNode entryPointNode = entryNodes.get(_entryPointId);
			MethodNode _entryPointNode = _tree.entryNodes.get(_entryPointId);
			if (entryPointNode!=null) {
				// Mezclo si _tree tiene ese entrypoint
				mergeNodes(entryPointNode, _entryPointNode);
			} else {
				entryNodes.put(_entryPointId,_entryPointNode);
			}
		}
		
		// Mezclo la global
		for (int _gsId:_tree.globalStatsMap.keySet()) {
			if (!globalStatsMap.containsKey(_gsId)) {
				globalStatsMap.put(_gsId, _tree.globalStatsMap.get(_gsId));
			} else {
				globalStatsMap.get(_gsId).merge(_tree.globalStatsMap.get(_gsId));
			}
		}
		
		
	}
	
	private void mergeNodes(MethodNode node, MethodNode _node) {
		// Mezclo los nodos
		node.merge(_node);

		// Mezclo los hijos

		// Mezclo cada uno de los hijos partiendo de node
		for (MethodNode childNode:node.getChildren()) {
			MethodNode _childNode = _node.getChild(childNode.getId());
			if (_childNode!=null) {
				// Mezclo si _node tiene ese hijo
				mergeNodes(childNode, _childNode);
				_node.children.remove(childNode.getId());
			}
		}

		// Mezclo cada uno de los hijos partiendo de _node
		for (MethodNode _childNode:_node.getChildren()) {
			MethodNode childNode = node.getChild(_childNode.getId());
			if (childNode!=null) {
				// Mezclo si _tree tiene ese entrypoint
				mergeNodes(childNode, _childNode);
			} else {
				node.children.put(_childNode.getId(),_childNode);
			}
		}

	}


	/**
	 * Combina en el tiempo el arbol actual con los arboles de entrada
	 * Modifica todos los arboles de entrada
	 */
	public void combine(DeltaTree...treeList){
		for (DeltaTree _tree:treeList){
			combine(_tree);
		}
	}

	private void combineGlobal(DeltaTree _tree) {
		// Mezclo la global
		for (int _gsId:_tree.globalStatsMap.keySet()) {
			if (!globalStatsMap.containsKey(_gsId)) {
				globalStatsMap.put(_gsId, _tree.globalStatsMap.get(_gsId));
			} else {
				globalStatsMap.get(_gsId).merge(_tree.globalStatsMap.get(_gsId));
			}
		}
	}

	/**
	 * Combina en el tiempo el arbol actual con el arbol de entrada
	 * Modifica el arbol de entrada
	 */
	public void combine(DeltaTree _tree) 
	{
		// Combino la global
		// Puede ser que el tree o _tree no tenga nodos pero si tenga datos en la global
		combineGlobal(_tree);

		if (_tree.startNode==null) {
			// Si el arbol2 esta vacio no mezclo nodos
			// Esto puede ocurrir si no nay datos, hay datos fuera de entrypoint,
			// o cuando el maxnivel de recursividad se ha alcanzado
			// En el caso de que sea por max nivel de recursiv, habria que actualizar
			// en el arbol1, la variable de estado.
			if (_tree.recursivityLevel>0) {
				recursivityLevel=_tree.recursivityLevel;
			}

			return;
		}

		if (startNode==null) {
			// Si el arbol1 esta vacio, copio el arbol2 aqui
			// Copio todo menos la global que tengo que combinarla
			copy(_tree);

			return;
		}

		MethodNode _rootNode = _tree.startNode;

		MethodNode _auxCurrentNode = _rootNode.getParent();
		_treeCurrentNode = _tree.currentNode;
		_treeCurrentNodeFound = false;

		// Elimino el rootNode del arbol para que a la hora de mezclar
		// su padre no se mezcle el mismo otra vez como hijo
		_tree.detach(_rootNode);

		MethodNode auxCurrentNode = currentNode;

		if (_rootNode.isClosingNode()) {
			// Empieza cerrando
			// Actualizo el currentnode mismo
			combine(auxCurrentNode,_rootNode);
		} else {
			// Si no empieza cerrando es que empieza abriendo
			// Actualizo como hijo del currentnode
			// El currentnode se actualiza al hijo actualizado
			MethodNode childNode;
			if (auxCurrentNode.hasChild(_rootNode.getId())) {
				childNode = auxCurrentNode.getChild(_rootNode.getId());
				combine(childNode, _rootNode);
				auxCurrentNode.openForCombine(_rootNode, _rootNode.getOpeningTs(), false);
				auxCurrentNode = childNode;
			} else {
				auxCurrentNode.openForCombine(_rootNode, _rootNode.getOpeningTs(), true);
				auxCurrentNode = _rootNode;
			}
		}

		// Recorro todos los padres y los voy mezclando en el arbol actual
		while (_auxCurrentNode!=null) 
		{			
			if (auxCurrentNode.hasParent()) {
				auxCurrentNode = auxCurrentNode.getParent();
			} else {
				auxCurrentNode = auxCurrentNode.createUndefinedParent();
			}

			combine(auxCurrentNode, _auxCurrentNode);

			MethodNode _mergedNode = _auxCurrentNode;
			_auxCurrentNode = _mergedNode.getParent();
			_mergedNode.detach();
		}

		if (!_treeCurrentNodeFound) {
			// Si se ha encontrado es porque se ha mezclado
			// Si no se ha encontrado es porque el nodo no existia en el arbol origen y se ha copiado 
			// con sus hijos directamente sin pasar por cada uno de sus descendientes. En este
			// caso el currentNode final sera el mismo del arbol a mezclar
			currentNode = _treeCurrentNode;
		} 

		entryPointLevel = _tree.entryPointLevel; 
		recursivityLevel = _tree.recursivityLevel;

	}

	private  void combine(MethodNode node, MethodNode _node) 
	{
		if (_node==_treeCurrentNode) {
			currentNode = node;
			_treeCurrentNodeFound = true;
		}

		// Mezclo los hijos		
		for (Iterator<MethodNode> it=_node.getChildren().iterator();it.hasNext();) {
			MethodNode _childNode = it.next();
			MethodNode childNode;
			if (node.hasChild(_childNode.getId())) {
				childNode = node.getChild(_childNode.getId());
				combine(childNode,_childNode);
			} else {
				node.addChild(_childNode);
			}			
		}

		node.combine(_node, globalStatsMap);
	}


	public boolean isEquivalent(DeltaTree _tree) {
		// Busco el nodo padre de todos a partir del rootnode
		MethodNode parent = startNode;
		MethodNode _parent = _tree.startNode;

		while (parent.hasParent()) {
			parent = parent.getParent();
			_parent = _parent.getParent();
		}

		if(!areEquivalentNodes(parent, _parent)) {
			return false;
		}

		if(!areEquivalentNodes(currentNode, _tree.currentNode)) {
			return false;
		}

		// Compruebo la equivalencia de los entrypoints
		// Por rendimiento cuando se construye un deltatree desde una sonda no se genera los entrypoints
		// Se generan dinamicamente al deserializarlo
		/*
		if (entryPoints.size()!=_tree.entryPoints.size()) {
			return false;
		}
		for (String entryPointId:entryPoints.keySet()) {
			BasicNode ep = entryPoints.get(entryPointId);
			BasicNode _ep = _tree.getEntryPoints().get(entryPointId);
			if (_ep==null || !_ep.getId().equals(ep.getId())) {
				return false;
			}
		}
		 */

		// Compruebo la equivalencia de la global
		if (globalStatsMap.size()!=_tree.globalStatsMap.size()) {
			return false;
		}
		for (int gsId:_tree.globalStatsMap.keySet()) {
			IGlobalStats gs = globalStatsMap.get(gsId);
			IGlobalStats _gs = _tree.globalStatsMap.get(gsId);
			if (!gs.isEquivalent(_gs)){
				gs.isEquivalent(_gs);
				return false;
			}
		}

		return true;
	}

	private boolean areEquivalentNodes(MethodNode node, MethodNode _node) {
		if (node.getId()!=-1) {
			if (node.getId()!=_node.getId()) {
				return false;
			}
		} else {
			if (_node.getId()!=-1) {
				return false;
			}
		}

		/*
		if (!node.getCsvStats().equals(_node.getCsvStats())) {
			return false;
		}
		 */
		if (!node.isEquivalent(_node)) {
			node.isEquivalent(_node);
			return false;
		}


		// Compruebo sus hijos
		for (Iterator<MethodNode> it = node.getChildren().iterator(); it.hasNext(); ) {
			MethodNode childNode = it.next();
			MethodNode _childNode = _node.getChild(childNode.getId());
			if (!areEquivalentNodes(childNode, _childNode)) {
				return false;
			}
		}

		return true;
	}


	public String toString(Map methodNames) {
		// Busco el nodo padre de todos a partir del rootnode
		if (startNode!=null) {
			MethodNode parent = getRootNode(); 

			StringBuilder sb = new StringBuilder();
			StringBuilder stats = new StringBuilder();

			writeNodeAndChildren(parent, sb, stats,methodNames);

			for (Iterator it = globalStatsMap.keySet().iterator();it.hasNext();) {
				int key = (Integer)it.next();
				IGlobalStats gs = (IGlobalStats) globalStatsMap.get(key);
			}

			return sb.toString();
			//return sb.append("\nStats:\n").append(stats).append("\n").toString();
		} else {
			return "";
		}
	}


	private void writeNodeAndChildren(MethodNode node, StringBuilder sb, StringBuilder stats, Map methodNames) 
	{
		// Entrada del nodo
		if (node.hasDefinedId()) {
			sb.append(node.getName(methodNames)).append(",");
			stats.append(node.getStats());
		}

		// escribo sus hijos
		for (Iterator<MethodNode> it = node.getChildren().iterator(); it.hasNext(); ) {
			writeNodeAndChildren(it.next(), sb, stats, methodNames);
		}

		// Salida del nodo
		if (node.hasDefinedId()) {
			sb.append("/").append(node.getName(methodNames)).append(",");
		}
	}


	/////////////////////////////////////////////
	//          MARSHALL FUNCTIONALITY         //
	/////////////////////////////////////////////
	public DeltaTree(DataInputStream dis, StringsCache stringsCache) throws IOException
	{
		initialize();

		completionTs = dis.readLong();
		entryPointLevel = dis.readShort();
		recursivityLevel = dis.readShort();
		maxRecursivityLevel = dis.readShort();
		tid = dis.readInt();

		// Deserializo la global
		int gsCount = dis.readInt();
		for (int i=0;i<gsCount;i++) {
			//Leo el tipo de metrica asociado a la global
			int metricType = dis.readShort();
			IDeltaNodeFactory factory = InspectorRuntime.getDeltaProbeFactory(metricType);
			IGlobalStats gs = factory.createGlobalStats(dis, stringsCache, metricType);
			globalStatsMap.put(gs.getId(),gs);
		}

		boolean hasNodes = dis.readBoolean();
		if (hasNodes) {
			unmarshallNode(dis, stringsCache);
		}
	}

	/**
	 * Envia en DeltaTree compuesto
	 */
	public void marshallComposed(DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		// Envio la global
		dos.writeInt(globalStatsMap.size());
		for (IGlobalStats gs:globalStatsMap.values()) {
			gs.marshall(dos, stringsCache);
		}
	}

	public static DeltaTree unmarshallComposed(DataInputStream dis, StringsCache stringsCache) throws IOException {
		DeltaTree resp = new DeltaTree();

		// Recibo la global
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			int metricType = dis.readShort();
			IDeltaNodeFactory factory = InspectorRuntime.getDeltaProbeFactory(metricType);
			IGlobalStats gs = factory.createGlobalStats(dis, stringsCache, metricType);
			resp.globalStatsMap.put(gs.getId(),gs);
		}

		return resp;		
	}

	/*
	public void marshall_javaserial(int pid, DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		dos.writeShort(pid);
		ObjectOutputStream oos = new ObjectOutputStream(dos);
		//System.out.println("DT marshall start:"+System.currentTimeMillis());
		oos.writeObject(this);
		//System.out.println("DT marshall end:"+System.currentTimeMillis());
		return;
	}*/

	public void marshall(int pid, DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		if (dos!=currentDos) {
			// Si el canal es diferente al ultimo borro la cache de textos
			currentDos=dos;
			stringsCache.clear();
		}

		/*
		if (startNode==null) {
			// El arbol esta vacio
			return;
		}*/

		dos.writeShort(pid);

		// Envio los atributos que no se pueden recomponer en destino
		dos.writeLong(completionTs);
		dos.writeShort(entryPointLevel);
		dos.writeShort(recursivityLevel);
		dos.writeShort(maxRecursivityLevel);
		dos.writeInt(tid);

		// Envio la global
		dos.writeInt(globalStatsMap.size());
		for (IGlobalStats gs:globalStatsMap.values()) {
			gs.marshall(dos, stringsCache);
		}


		if (startNode!=null) {
			dos.writeBoolean(true);
			// El arbol tiene nodos
			// Envio el arbol a partir del padre
			MethodNode parent = getRootNode();

			marshallNode(parent, dos, stringsCache);
		} else {
			dos.writeBoolean(false);
		}
	}


	private MethodNode unmarshallNode(DataInputStream dis, StringsCache stringsCache) throws IOException 
	{
		MethodNode node = null;

		boolean hasNodes = dis.readBoolean(); 

		if (hasNodes){
			node = MethodNode.unmarshallIntrinsic(dis, stringsCache);
			// leo la metadata
			byte metadata = dis.readByte();
			if ((metadata&1)==1) {
				// Es currentNode
				currentNode = node;
			}
			if ((metadata&4)==4) {
				// Es rootNode
				startNode = node;
			}

			// Leo los hijos
			int childrenCount = dis.readShort();
			for (int i=0;i<childrenCount;i++) {
				MethodNode childNode = unmarshallNode(dis, stringsCache);
				node.addChild(childNode);
			}
		}

		return node;
	}


	private MethodNode getRootNode() {
		MethodNode parent = startNode;
		while (parent.hasParent()) {
			parent = parent.getParent();
		}

		return parent;
	}

	/**
	 * Envia el nodo y sus hijos
	 * Si durante el envio recursivo se produce algun problema en el canal
	 * se descarta todo el envio
	 * @throws IOException 
	 */
	private void marshallNode(MethodNode node, DataOutputStream dos, StringsCache stringsCache) throws IOException {
		if (node!=null) {
			dos.writeBoolean(true); // Hay datos de nodos
			// Envio el nodo sin hijos ni padre
			node.marshallIntrinsic(dos, stringsCache);

			// Envío metadata del nodo
			// 	1 => currentNode
			// 	2 => entryPoint
			//  4 => rootNode
			//currentDos.writeByte((currentNode==node?1:0) + (entryPoints.containsValue(node)?2:0)+ (startNode==node?4:0));
			dos.writeByte((currentNode==node?1:0) + (node.isEntryPoint?2:0)+ (startNode==node?4:0));

			// Envío el número de hijos
			dos.writeShort(node.getChildrenCount());

			// Envío los hijos del nodo
			for (Iterator<MethodNode> it=node.getChildren().iterator();it.hasNext();) {
				marshallNode(it.next(), dos, stringsCache);
			}
		} else {
			dos.writeBoolean(false); // No hay datos de nodos
		}
	}
}
