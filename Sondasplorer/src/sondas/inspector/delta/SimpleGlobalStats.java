package sondas.inspector.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;

import sondas.inspector.StringsCache;
import sondas.inspector.probes.delta.MethodNode;
import sondas.utils.SondasUtils;

/**
 * Las clases que hereden deben sobreescribir:
 *    merge, marshall, Ctor(dis), isEquivalent, clone, getDiferential
 *    
 * Esta clase podría heredar de BaseGlobalStats pero es tan frecuente su uso que
 * se ha preferido evitar herencia e implementarla completamente aqui
 */
public class SimpleGlobalStats implements IGlobalStats, Serializable
{
	// Estadisticas
	private int accExTime;
	private int accTotTime;
	private int invCount;
	private int errorCount;
	private int maxTotTime;
	private int maxExTime;
	private int minTotTime=Integer.MAX_VALUE;
	private int minExTime=Integer.MAX_VALUE;
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
	public SimpleGlobalStats(int id, String name, String typeName, int metricType, int categoryId) {
		this.name=name;
		this.typeName=typeName;
		this.id=id;
		this.metricType = metricType;
		this.categoryId=categoryId;
	}
	
	private SimpleGlobalStats() {
		
	}
	
	public void addSubtree(MethodNode node) {
		
	}
	
	/**
	 * Devuelve un globalStat con la diferencia entre el mismo y el especificado como parametro
	 */
	public IGlobalStats getDiferential(IGlobalStats _gs) {
		SimpleGlobalStats _sgs = (SimpleGlobalStats) _gs;
		
		SimpleGlobalStats resp = new SimpleGlobalStats();
		resp.accExTime = accExTime-_sgs.accExTime;
		resp.accTotTime = accTotTime-_sgs.accTotTime;
		resp.invCount = invCount-_sgs.invCount;
		resp.errorCount = errorCount-_sgs.errorCount;
		resp.name = name;
		resp.typeName = typeName;
		resp.id = id;
		resp.metricType = metricType;
		resp.categoryId=categoryId;
		// Los maximos y minimos seran los del objeto actual porque si han cambiado
		// se deben exclusivamente a lo nuevo
		resp.maxTotTime = maxTotTime;
		resp.maxExTime = maxExTime;
		resp.minTotTime = minTotTime;
		resp.minExTime = minExTime;
	
		return resp;
	}
	
	public Object clone() {
		SimpleGlobalStats resp = new SimpleGlobalStats();
		resp.accExTime=accExTime;
		resp.accTotTime=accTotTime;
		resp.invCount=invCount;
		resp.errorCount=errorCount;
		resp.maxTotTime=maxTotTime;
		resp.minTotTime=minTotTime;
		resp.minExTime=minExTime;
		resp.name=name;
		resp.typeName=typeName;
		resp.id=id;
		resp.metricType=metricType;
		resp.categoryId=categoryId;
		return resp;
	}
	
	public SimpleGlobalStats(IGlobalStats gs) {
		SimpleGlobalStats sgs = (SimpleGlobalStats) gs;
		accExTime=sgs.accExTime;
		accTotTime=sgs.accTotTime;
		invCount=sgs.invCount;
		errorCount=sgs.errorCount;
		maxTotTime=sgs.maxTotTime;
		minTotTime=sgs.minTotTime;
		minExTime=sgs.minExTime;
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
	public float getMetricValue(String statsName) {
		// Ignoro name porque la metrica es la GlobalStat mismo
		if (statsName.equals("totTime")) {
			return getAvgTotTime();
		} else if (statsName.equals("exTime")) {
			return getAvgExTime();
		} else if (statsName.equals("invCount")) {
			return getInvCount();
		} else if (statsName.equals("errCount")) {
			return getErrorCount();
		} else {
			throw new RuntimeException("Unkown metric name");
		}
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
	
	public float getAvgTotTime() {
		if (invCount!=0) 
			return accTotTime/invCount;
		else 
			return -1;
		
	}
	
	public float getAvgExTime() {
		if (invCount!=0) 
			return accExTime/invCount;
		else
			return -1;
	}
	
	public int getInvCount() {
		return invCount;
	}
	
	public int getErrorCount() {
		return errorCount;
	}
	
	public int getMinTotTime() {
		return minTotTime;
	}
	
	public int getMinExTime() {
		return minExTime;
	}
	
	public int getMaxTotTime() {
		return maxTotTime;
	}
	
	public int getMaxExTime() {
		return maxExTime;
	}
	
	/**
	 * Combina la actual Global con la pasada por parametro
	 * No modifica la GlobalStat pasada como parametro.
	 */
	public void merge(IGlobalStats gs) {
		SimpleGlobalStats sgs = (SimpleGlobalStats) gs;
		accExTime+=sgs.accExTime;
		accTotTime+=sgs.accTotTime;
		invCount+=sgs.invCount;
		errorCount+=sgs.errorCount;
		
		if (sgs.maxTotTime>maxTotTime) {
			maxTotTime = sgs.maxTotTime;
		}

		if (sgs.maxExTime>maxExTime) {
			maxExTime = sgs.maxExTime;
		}

		if (sgs.minTotTime<minTotTime) {
			minTotTime = sgs.minTotTime;
		}

		if (sgs.minExTime<minExTime) {
			minExTime = sgs.minExTime;
		}
	}
	
	public void updateBasic(int totalTime, int exTime, boolean error) 
	{
		accExTime+=exTime;
		accTotTime+=totalTime;
		invCount++;
		if(error) {
			errorCount++;
		}
		
		if (totalTime>maxTotTime) {
			maxTotTime = totalTime;
		}

		if (exTime>maxExTime) {
			maxExTime = exTime;
		}

		if (totalTime<minTotTime) {
			minTotTime = totalTime;
		}

		if (exTime<minExTime) {
			minExTime = exTime;
		}
	}
	
	public SimpleGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		typeName = SondasUtils.readCachedString(dis, stringsCache);
		id = dis.readInt();
		name = SondasUtils.readCachedString(dis, stringsCache);
		this.metricType=metricType;
		
		// Estadisticas
		accExTime = dis.readInt();
		accTotTime = dis.readInt();
		invCount = dis.readInt();
		errorCount = dis.readInt();
		maxTotTime = dis.readInt();
		maxExTime = dis.readInt();
		minTotTime = dis.readInt();
		minExTime = dis.readInt();
		categoryId = dis.readInt();
	}
	 
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeShort(metricType);
		
		SondasUtils.writeCachedString(typeName, dos, stringsCache);
		dos.writeInt(id);
		SondasUtils.writeCachedString(name, dos, stringsCache);

		// Estadisticas
		dos.writeInt(accExTime);
		dos.writeInt(accTotTime);
		dos.writeInt(invCount);
		dos.writeInt(errorCount);
		dos.writeInt(maxTotTime);
		dos.writeInt(maxExTime);
		dos.writeInt(minTotTime);
		dos.writeInt(minExTime);
		dos.writeInt(categoryId);
	}
	
	public boolean isEquivalent(IGlobalStats _gs) {
		SimpleGlobalStats _sgs = (SimpleGlobalStats) _gs;
		return (typeName.equals(_sgs.typeName) && id==_sgs.id && name.equals(_sgs.name) &&
				accExTime==_sgs.accExTime && accTotTime==_sgs.accTotTime && invCount==_sgs.invCount &&
				errorCount==_sgs.errorCount && maxTotTime==_sgs.maxTotTime && maxExTime==_sgs.maxExTime &&
				minTotTime==_sgs.minTotTime && minExTime==_sgs.minExTime && categoryId==_sgs.categoryId);	
	}
	
}
