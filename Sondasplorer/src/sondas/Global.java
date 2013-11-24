package sondas;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import sondas.inspector.KeyValueBean;

public class Global 
{
	static private int registeredNodes;
	volatile static public boolean enabledNodes[];
	static public HashMap nodesNamesIdRel = new HashMap();
	static private Config config;
	
	static public boolean isConfigLoaded() {
		return config!=null;
	}
	
	static public void loadConfiguration(String cfgFile) throws IOException 
	{
		config = new Config(cfgFile);
	}
	
	static public int getPropertyAsInteger(String category, String propertyName) {
		return config.getPropertyAsInteger(category, propertyName);
	}
	
	static public boolean getPropertyAsBoolean(String category, String propertyName) {
		return config.getPropertyAsBoolean(category, propertyName);
	}
	
	static public String getProperty(String category, String propertyName) {
		return config.getProperty(category, propertyName);
	}
	
	/**
	 * Registra el nodo y devuelve su Id. 
	 * Si ya esta registrado no hace nada y devuelve su Id
	 */
	static public int registerNode(String node) 
	{
		if (!nodesNamesIdRel.containsKey(node)) {
			nodesNamesIdRel.put(node,Integer.valueOf(registeredNodes));
			return registeredNodes++;
		} else {
			return ((Integer)nodesNamesIdRel.get(node)).intValue();
		}
	}
	
	/**
	 * Devuelve un mapa<String,Id> con todos los nodos registrados
	 */
	static public Map getNodesIds() {
		return nodesNamesIdRel;
	}
	
	/**
	 * Si el nodo no esta registrado devuelve -1
	 */
	static int getNodeId(String nodeName) {
		if (nodesNamesIdRel.containsKey(nodeName)) {
			return ((Integer)nodesNamesIdRel.get(nodeName)).intValue();
		} else {
			return -1;
		}
	}
	
	/**
	 * Al tratarse de un tipo primitivo de 32bits no es necesaria la sincronizacion
	 * ya que la operacion de lectura y escritura es atomica.
	 * Otra cosa es si dos hilos intentan activar o desactivar un nodo de forma simultanea. 
	 * En este caso el orden de alteracion es indeterminado. 
	 */
	static void enableNode(int nodeId) {
		enabledNodes[nodeId]=true;
		
		// enabledNodes es volatile, es necesario hacer autoasignacion de enabledNodes para
		// que otros hilos detecten el cambio sin que exista sincronizacion
		// http://www.javamex.com/tutorials/volatile_arrays.shtml
		enabledNodes=enabledNodes;
	}

	/**
	 * Al tratarse de un tipo primitivo de 32bits no es necesaria la sincronizacion
	 * ya que la operacion de lectura y escritura es atomica.
	 * Otra cosa es si dos hilos intentan activar o desactivar un nodo de forma simultanea. 
	 * En este caso el orden de alteracion es indeterminado. 

	 */
	static void disableNode(int nodeId) {
		enabledNodes[nodeId]=false;
		
		// enabledNodes es volatile, es necesario hacer autoasignacion de enabledNodes para
		// que otros hilos detecten el cambio sin que exista sincronizacion
		// http://www.javamex.com/tutorials/volatile_arrays.shtml
		enabledNodes=enabledNodes;
	}
	
	static public void finishNodeRegistration(){
		enabledNodes = new boolean[registeredNodes];
		// Activo todos los nodos
		for (int i=0;i<registeredNodes;i++) {
			enabledNodes[i]=true;
		}
	}
}
