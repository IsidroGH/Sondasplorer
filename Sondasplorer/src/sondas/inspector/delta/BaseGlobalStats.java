package sondas.inspector.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import sondas.inspector.StringsCache;
import sondas.utils.SondasUtils;

/**
 * Las clases que hereden deben sobreescribir:
 *    merge, marshall, Ctor(dis), isEquivalent, clone, getDiferential
 */
public class BaseGlobalStats implements IGlobalStats, Serializable
{
	private String name;
	private String typeName;
	private int id;
	private int metricType;
	
	// Representa la categoria asociada a la GlobalStats
	// Se usa a la hora de colocar el nodo en el arbol de estadisticas
	private int categoryId=-1; 
	
	/**
	 * TypeName indica el tipo de la metrica con fines de presentacion
	 */
	public BaseGlobalStats(int id, String name, String typeName, int metricType, int categoryId) {
		this.name=name;
		this.typeName=typeName;
		this.id=id;
		this.metricType = metricType;
		this.categoryId=categoryId;
	}
	
	private BaseGlobalStats() {
		
	}
	
	/**
	 * Devuelve un globalStat con la diferencia entre el mismo y el especificado como parametro
	 */
	public IGlobalStats getDiferential(IGlobalStats _gs) {
		BaseGlobalStats _sgs = (BaseGlobalStats) _gs;
		
		BaseGlobalStats resp = new BaseGlobalStats();
		resp.name = name;
		resp.typeName = typeName;
		resp.id = id;
		resp.metricType = metricType;
		resp.categoryId=categoryId;
	
		return resp;
	}
	
	public Object clone() {
		BaseGlobalStats resp = new BaseGlobalStats();
		resp.name=name;
		resp.typeName=typeName;
		resp.id=id;
		resp.metricType=metricType;
		resp.categoryId=categoryId;
		return resp;
	}
	
	public BaseGlobalStats(IGlobalStats gs) {
		BaseGlobalStats sgs = (BaseGlobalStats) gs;
		name=sgs.name;
		typeName=sgs.typeName;
		id=sgs.id;
		metricType=sgs.metricType;
		categoryId=sgs.categoryId;	
	}
	
	/**
	 * Representa la categoria asociada a la GlobalStats
	 * Se usa a la hora de colocar el nodo en el arbol de estadisticas
	 */
	public int getCategoryId() {
		return categoryId;
	}
	
	/**
	 * Devuelve el valor especificado.
	 * En caso de una metrica promedio (exTime, totTime, etc) devolvera -1 en caso 
	 * de que el número de invocaciones sea 0
	 */
	public float getMetricValue(String nodeName, String statsName) {
		return 0;
	}
	
	public int getMetricType(){
		return metricType;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName(Map methodNames) {
		if (name.equals("")) {
			return (String) methodNames.get(id);
		} else {
			return name;
		}
	}
	
	public String getTypeName() {
		return typeName;
	}
	

	/**
	 * Combina la actual Global con la pasada por parametro
	 * No modifica la GlobalStat pasada como parametro.
	 */
	public void merge(IGlobalStats gs) {
	}
	
	public BaseGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		typeName = SondasUtils.readCachedString(dis, stringsCache);
		id = dis.readInt();
		name = SondasUtils.readCachedString(dis, stringsCache);
		this.metricType=metricType;
		categoryId = dis.readInt();
	}
	 
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeShort(metricType);
		
		SondasUtils.writeCachedString(typeName, dos, stringsCache);
		dos.writeInt(id);
		SondasUtils.writeCachedString(name, dos, stringsCache);
		dos.writeInt(categoryId);
	}
	
	public boolean isEquivalent(IGlobalStats _gs) {
		BaseGlobalStats _sgs = (BaseGlobalStats) _gs;
		return (typeName.equals(_sgs.typeName) && id==_sgs.id && name.equals(_sgs.name) && categoryId==_sgs.categoryId);
	}
	
}

