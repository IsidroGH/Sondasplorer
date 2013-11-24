package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleGlobalStats;

public class ServletNode extends MethodNode {
	/**
	 * Crea un nodo a raiz de una apertura
	 */
	public ServletNode(String name, int metricType, int id) {
		super(name, metricType, true, id);
	}
	
	public ServletNode(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		super(dis,stringsCache, metricType);
		
		// Aqui se deserializara cualquier otra cosa necesaria
	}
	
	public void marshallIntrinsic(DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		super.marshallIntrinsic(dos,stringsCache);		
		// Aqui se serializara cualquier otra cosa necesaria
	}
	
	/**
	 * En este caso creo un GlobalStats y no un ServletGlobalStats porque por ahora no
	 * contemplo en un nodo servlet mas informacion que la de un nodo basico
	 */
	protected IGlobalStats createGlobalStats(int categoryId) {
		return new SimpleGlobalStats(id, name, "Servlet", metricType, categoryId);
	}
	
}
