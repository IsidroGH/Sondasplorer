package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleGlobalStats;
import sondas.utils.Names;
import sondas.utils.SondasUtils;

/* GlobalStats (gs)
La gs normalmente va asociada a un metrictype:method
En el caso que un tipo de nodo necesite ampliar la informacion contenida en la gs estandar
deberá extender la clase GlobalStats y sobreescribiendo el metodo createGlobalStats para que
devuelva el nuevo tipo de GlobalStats. En este caso, la gs seguirá asociada al mismo metrictype:method.
Si necesitara simplemente mantener algún tipo de estadística de forma global pero no asociada al metrictype:method,
por ejemplo cualquier ejecución de un preparedStatement independientemente del método execute que use, no necesito un
GlobalStats específico ya que iría asociado a tantos metrictype:method como tipos de metodos execute existan.
En este caso, en vez de acutalizar a partir del atributo id (metrictype:method), actualizaría el mapa global a partir
de una key metrictype:mi_clave.

Otra forma de llevar a cabo esto, sería desde la sonda. En vez de crear los nodos con un name para cada metodo, puedo englobar distintos
metodos con un mismo name. Por ejemplo, en el caso de un PreparedStatement, englobo todos los execute, executeUpdate, executeQuery en un mismo
nodo con name=execute_pstmt

En el caso de SqlProbe, se ha decidido seguir la segunda alternativa
*/

/**
 * Representa la ejecucion de un metodo que es la granularidad minima. 
 * Las clases descendientes deben sobrecargar en caso necesario los metodos:
 *    merge, combine, specificClose, createGlobalStats, isEquivalent
 *    
 * nodeId indica el ID de nodo asociado a la sonda fisica. Sirve para mostrar la informacion de forma jerarquizada
 * 
 * Si se añade un atributo debe seriealizarse en marshallintrinsic
 */
public class MethodNode implements Serializable
{
	private MethodNode parent;
	public HashMap<Integer,MethodNode> children;
	
	protected int id=-1;
	
	protected String name;
	protected int metricType;

	private long closingTs=-1;
	private int closingMode;
	private int closingNodeId;
	private long openingTs=-1;

	// Estadisticas
	private int partialExTime;
	private long iniTs=-1;
	private long partialIniTs=-1;
	private int accExTime;
	private int accTotTime;
	private int invCount;
	private int errorCount;
	private int maxTotTime;
	private int maxExTime;
	private int minTotTime=Integer.MAX_VALUE;
	private int minExTime=Integer.MAX_VALUE;
	
	// Por rendimiento no se encapsula para que
	// pueda ser accedido eficientemente desde DeltaTree
	public boolean isEntryPoint;
	
	/**
	 * Crea un nodo a raiz de una apertura
	 */
	public MethodNode(String name, int metricType, int id) {
		this.id = id;
		this.name = name;
		this.metricType = metricType;
		children = new HashMap<Integer, MethodNode>();
		isEntryPoint = false;
	}

	public MethodNode(String name, int metricType, boolean isEntryPoint, int id) {
		this(name,metricType,id);
		this.isEntryPoint=isEntryPoint;
	}

	/**
	 * Crea un nodo indefinido
	 */
	private MethodNode() {
		children = new HashMap<Integer, MethodNode>();
	}
	
	public int getMetricType() {
		return metricType;
	}
	
	public Collection<MethodNode> getChildren() {
		return children.values();
	}

	public boolean hasParent() {
		return parent!=null;
	}

	public boolean hasDefinedId() {
		return id!=-1;
	}

	public boolean hasDefinedParent() {
		return parent!=null && parent.id!=-1;
	}

	public static MethodNode createUndefinedNode() {
		return new MethodNode();
	}

	public int getId() {
		return id;
	}

	public String getName(Map methodNames) {
		if (name.equals("")) {
			return (String)methodNames.get(id);
		} else {
			return name;
		}
	}
	
	public String getDigestedName(Map methodNames) {
		return Names.getDigestedName(getName(methodNames), true);
	}
	
	public String getMethodClassAndName(Map methodNames) {
		return Names.getClassAndMethod(getName(methodNames));
	}

	public boolean hasChild(int id){
		return children.containsKey(id);
	}

	public MethodNode getChild(int id) {
		return children.get(id);
	}

	public int getChildrenCount() {
		return children.size();
	}

	/**
	 * Presupone que el nodo no tiene el hijo	 
	 * Si lo tiene, se sobreescribe
	 */
	public void addChild(MethodNode node) {
		children.put(node.getId(),node);
		node.parent = this;
	}

	public long getOpeningTs() {
		return openingTs;
	}

	/**
	 * Informa de la apertura del nodo actual.
	 * Se llama cuando el primer nodo de un arbol es de apertura
	 * y sólo para ese nodo
	 */
	public void startOpen(long ts) {
		openingTs = ts;
		partialIniTs = ts;
		iniTs = ts;	
	}

	/**
	 * Se trata de un open especial para la mezcla. Un open normal, actualiza
	 * el iniTs y el partialTs del nodo a añadir. En una mezcla, estos valores
	 * ya están correctamente definidos en el arbol2 por lo que si se llama
	 * al open normal se actualizaría al openingTs por lo que se perderian
	 * todas las actualizaciones del partial llevadas a cabo.
	 * 
	 * El parametro addChild en caso de ser true Indica que se solicita una accion 
	 * de apertura sobre el nodo actual pero sin añadir ningun hijo. Unicamente 
	 * actualiza las variables estado relacionadas con los tiempos.
	 * Se uiliza para concatenar dos arboles cuando el segundo empieza por una
	 * apertura y el hijo del nodo raiz del arbol2 ya existe como hijo del nodo
	 * actual del arbol1 por lo que no se puede llamar a open ya que este metodo
	 * añadiria y lo que necesitamos es mezclar los hijos	 * 
	 */
	public void openForCombine(MethodNode childNode, long ts,boolean addChild) {
		if (addChild) {
			addChild(childNode);
		}

		partialExTime+=(ts-partialIniTs);
	}

	public void open(MethodNode childNode, long ts) {
		addChild(childNode);
		updateOpenStats(childNode, ts);
	}

	public MethodNode open(int nodeId, long ts) {
		MethodNode node = children.get(nodeId);
		updateOpenStats(node, ts);

		return node;
	}

	private void updateOpenStats(MethodNode node, long ts) {
		// Actualizo el tiempo exclusivo del elemento padre 
		partialExTime+=(ts-partialIniTs);

		// Actualizo el elemento actual
		node.partialIniTs = ts;
		node.iniTs = ts;		
	}

	/**
	 * 2 = End
	 * 3 = Exception
	 */
	public void close(int nodeId, long ts, int mode, Map<Integer, IGlobalStats> globalStatsMap) {
		if (id!=-1) 
		{
			// Actualizo el elemento que acaba de terminar
			partialExTime+=ts-partialIniTs;
			int totalTime = (int)(ts-iniTs);
			accExTime+=partialExTime;
			accTotTime+=totalTime;
			invCount++;

			// Se trata de un nodo definido, su nodo open está definido 
			if (mode==3) {
				// Exception
				errorCount++;
			}

			if (totalTime>maxTotTime) {
				maxTotTime = totalTime;
			}

			if (partialExTime>maxExTime) {
				maxExTime = partialExTime;
			}

			if (totalTime<minTotTime) {
				minTotTime = totalTime;
			}

			if (partialExTime<minExTime) {
				minExTime = partialExTime;
			}

			specificClose(nodeId, totalTime, partialExTime, mode==3, globalStatsMap);  
			
			partialExTime = 0; 		

		} else {
			// Se trata de un nodo no definido => su nodo open no está definido
			closingTs = ts;
			closingMode = mode;
			closingNodeId= nodeId;
		}

		if (parent!=null) {
			parent.partialIniTs=ts;
		}
	}

	/**
	 * Indica si la primera acción sobre el nodo ha sido un cierre
	 */
	public boolean isClosingNode() {
		return closingTs!=-1;
	}

	/**
	 * Indica si la primera acción sobre el nodo ha sido una apertura
	 */
	public boolean isOpeningNode() {
		return openingTs!=-1;
	}

	
	/**
	 * Mezcla sin mezclar los hijos
	 * Este método debe ser sobrecargado en las clases descendientes
	 */
	public void merge(MethodNode _node) 
	{
		if (metricType!=_node.metricType) {
			throw new RuntimeException("Incompatible nodes");
		}
		
		accExTime+=_node.accExTime;
		accTotTime+=_node.accTotTime;
		invCount+=_node.invCount;		
		errorCount+=_node.errorCount;
		
		if (maxTotTime<_node.maxTotTime) {
			maxTotTime = _node.maxTotTime;
		}
		
		if (maxExTime<_node.maxExTime) {
			maxExTime = _node.maxExTime;
		}
		
		if (minTotTime>_node.minTotTime) {
			minTotTime = _node.minTotTime;
		}
		
		if (minExTime>_node.minExTime) {
			minExTime = _node.minExTime;
		}
	}
	
	
	/**
	 * Mezcla en el tiempo el nodo actual con el especificado.
	 * Devuelve si el nodo acaba de ser cerrado
	 * Este método debe ser sobrecargado en las clases descendientes
	 */
	public boolean combine(MethodNode _node, Map<Integer, IGlobalStats> globalStats) 
	{
		boolean justClosed = false;
		// El close deja el exclusiveTime a cero por eso tengo añadirlo antes de un posible startingClose
		partialExTime+=_node.partialExTime;

		if (_node.isClosingNode()) {
			// Se trata de un nodo que empieza cerrando
			// Actualizo el partialTs
			if (_node.partialIniTs!=-1) {
				// Si alguien del arbol2 ha actualizado el partialTs se
				// actualiza el del arbol1 con ese valor.
				// Si nadie lo ha actualizado, se deja el del arbol1
				// Por ejemplo: A | /A
				// En este caso, el del arbol2 partialTs estaria sin inicializar y no debe				
				// sobreescribir el valor del arbol1
				partialIniTs = _node.partialIniTs;
			}
			close (_node.closingNodeId, _node.closingTs,_node.closingMode, globalStats);
			closingTs=-1;
			closingMode=0;
			justClosed = true;
		} 

		// Actualizo la información del nodo concluida
		accExTime += _node.accExTime;
		accTotTime += _node.accTotTime;
		invCount+=_node.invCount;
		errorCount+=_node.errorCount;
		if (maxTotTime<_node.maxTotTime)
			maxTotTime=_node.maxTotTime;
		if (minTotTime>_node.minTotTime)
			minTotTime=_node.minTotTime;
		if (maxExTime<_node.maxExTime)
			maxExTime=_node.maxExTime;
		if (minExTime>_node.minExTime)
			minExTime=_node.minExTime;

		if (_node.partialIniTs!=-1) {
			partialIniTs = _node.partialIniTs;
		}

		if (_node.iniTs!=-1) {
			iniTs = _node.iniTs; 
		}

		return justClosed;
	}
	
	protected void specificClose(int categoryId, int totTime, int exTime, boolean error, Map<Integer, IGlobalStats> globalStatsMap) {
		((SimpleGlobalStats)getGlobalStats(id,globalStatsMap, categoryId)).updateBasic(totTime, exTime, error);
	}
	
	protected IGlobalStats getGlobalStats(int gsId, Map<Integer, IGlobalStats> globalStats, int categoryId) 
	{
		IGlobalStats gs = globalStats.get(gsId);
		if (gs!=null) {
			return gs;
		} else {
			IGlobalStats gb = createGlobalStats(categoryId);
			globalStats.put(gsId, gb);
			return gb;
		}
	}
	
	protected IGlobalStats createGlobalStats(int categoryId) {
		return new SimpleGlobalStats(id, "", "Method",metricType, categoryId);
	}


	public void detach() {
		if (parent!=null) {
			parent.children.remove(id);
		}
		parent=null;
	}



	public MethodNode createUndefinedParent() {
		MethodNode parent = new MethodNode();
		parent.children.put(id, this);
		this.parent = parent;
		return parent;
	}


	public MethodNode getParent() {
		return parent;
	}

	public String toString(Map methodNames) {
		return getName(methodNames);
	}

	public int getInvCount() {
		return invCount;
	}
	
	public int getErrorCount() {
		return errorCount;
	}
	
	public float getAvgExTime() {
		if (invCount!=0)
			return accExTime/invCount;
		else
			return -1;
	}
	
	public float getAvgTotTime() {
		if (invCount!=0)
			return accTotTime/invCount;
		else
			return -1;
	}
	
	
	public boolean isEquivalent(MethodNode _node) {
		
		return (metricType==_node.metricType && ( (id==-1 && _node.id==-1) || id==_node.id) && ( (name==null && _node.name==null) || name.equals(_node.name)) &&
				accExTime==_node.accExTime && accTotTime==_node.accTotTime && invCount==_node.invCount &&
				errorCount==_node.errorCount && maxTotTime==_node.maxTotTime && maxExTime==_node.maxExTime &&
				minTotTime==_node.minTotTime && minExTime==_node.minExTime);
	}
	
//	************ MARSHALLING ************

	/**
	 * Envía informacion intrinseca al nodo
	 * No envia la informacion relativa a los hijos del mismo ni de su padre.
	 */
	public void marshallIntrinsic(DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		dos.writeShort(metricType);
		dos.writeInt(id);
		SondasUtils.writeCachedString(name, dos, stringsCache);
		dos.writeLong(closingTs);
		dos.writeByte(closingMode);
		dos.writeInt(closingNodeId);
		dos.writeLong(openingTs);
		dos.writeBoolean(isEntryPoint);

		// Estadisticas
		dos.writeInt(partialExTime);
		dos.writeLong(iniTs);
		dos.writeLong(partialIniTs);
		dos.writeInt(accExTime);
		dos.writeInt(accTotTime);
		dos.writeInt(invCount);
		dos.writeInt(errorCount);
		dos.writeInt(maxTotTime);
		dos.writeInt(maxExTime);
		dos.writeInt(minTotTime);
		dos.writeInt(minExTime);
	}
	
	public static MethodNode unmarshallIntrinsic(DataInputStream dis, StringsCache stringsCache) throws IOException 
	{
		int metricType = dis.readShort();
		
		// A partir del metricType se crea a partir de su factory el tipo real de nodo
		// Desde el factory en concreto se instanciara BasicNode(dis,stringsCache)
		IDeltaNodeFactory factory = InspectorRuntime.getDeltaProbeFactory(metricType);
		MethodNode node = factory.createNode(dis, stringsCache, metricType);
		return node;
	}
	
	/**
	 * Deserializa de forma intrinseca (sin hijos ni padres un nodo)
	 * @throws IOException 
	 */
	public MethodNode(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		this();
		id = dis.readInt();
		name = SondasUtils.readCachedString(dis, stringsCache);
		closingTs = dis.readLong();
		closingMode = dis.readByte();
		closingNodeId = dis.readInt();
		openingTs = dis.readLong();
		isEntryPoint = dis.readBoolean();
		
		// Estadisticas
		partialExTime = dis.readInt();
		iniTs = dis.readLong();
		partialIniTs = dis.readLong();
		accExTime = dis.readInt();
		accTotTime = dis.readInt();
		invCount = dis.readInt();
		errorCount = dis.readInt();
		maxTotTime = dis.readInt();
		maxExTime = dis.readInt();
		minTotTime = dis.readInt();
		minExTime = dis.readInt();	
		this.metricType = metricType; 
	}

//	*************** TESTING ************

	public String getCsvStats() {
		StringBuilder sb = new StringBuilder();
		sb.append(accTotTime).append(",");
		sb.append(accTotTime/(float)invCount).append(",");
		sb.append(accExTime).append(",");
		sb.append(accExTime/(float)invCount).append(",");
		sb.append(invCount);
		return sb.toString();
	}

	public String getStats() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("-> ");
		sb.append("Total time: ").append(accTotTime);
		sb.append(" Avg total time: ").append(accTotTime/(float)invCount);
		sb.append(" Exclusive time: ").append(accExTime);
		sb.append(" Avg exclusive time: ").append(accExTime/(float)invCount);
		sb.append(" Inv Count: ").append(invCount);
		sb.append("\n");
		return sb.toString();
	}
}
