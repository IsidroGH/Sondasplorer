package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.IRemote;
import sondas.inspector.delta.SimpleGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.utils.ICachedMarshable;
import sondas.utils.SondasUtils;

public class AxisGlobalStats extends SimpleGlobalStats implements IRemote
{
	private Map endPoints;
	
	public AxisGlobalStats(int id, String name, String typeName, int metricType, int categoryId) {
		super(id,name,typeName,metricType, categoryId);
		endPoints = new HashMap();
	}

	private AxisGlobalStats(IGlobalStats gs) {
		super(gs);
		endPoints = new HashMap();
	}

	
	/**
	 * nodeKey representa el id en el contexto de esta global.
	 * Para el caso de AxisGLobalStats es protocolo+host+puerto+servicio+query
	 */
	public float getMetricValue(String endPoint, String statsName) 
	{
		float resp=0;
		
		SimpleStats stats = (SimpleStats) endPoints.get(endPoint);

		if (stats!=null) {
			if (statsName.equals("totTime")) {
				resp = stats.getAvgTotTime();
			} else if (statsName.equals("invCount")) {
				resp = stats.getInvCount();
			} else if (statsName.equals("errCount")) {
				resp = stats.getErrorCount();
			} else {
				throw new RuntimeException("Unkown metric name: "+statsName);
			}
		}
		
		return resp;	
	}
	
	public Object clone() {
		AxisGlobalStats resp = new AxisGlobalStats(this);

		for (Iterator it=endPoints.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) endPoints.get(key);
			resp.endPoints.put(key,value.clone());
		}
		
		return resp;
	}
	
	private void getDiferential(Map queries, Map _queries, Map resp) {
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) queries.get(key);

			if (!_queries.containsKey(key)) {
				resp.put(key, value.clone());
			} else {
				resp.put(key, value.getDiferential((SimpleStats)_queries.get(key)));
			}			
		}
	}
	
	/**
	 * Devuelve un globalStat con la diferencia entre el mismo y el especificado como parametro
	 */
	public IGlobalStats getDiferential(IGlobalStats _gs) {
		// Creo la respuesta con el diferencial de la Global
		AxisGlobalStats resp = new AxisGlobalStats((SimpleGlobalStats)super.getDiferential(_gs));

		AxisGlobalStats _axisGs = (AxisGlobalStats) _gs;
		// Añado el diferencial de la SqlGlobal
		// Queries
		getDiferential(endPoints, _axisGs.endPoints, resp.endPoints);

		return resp;
	}
	
	public Map getEndPoints() {
		return endPoints;
	}
	
	public void updateAxis(String endPoint, int totTime, int exTime, boolean error) 
	{
		SimpleStats stats = (SimpleStats) endPoints.get(endPoint);
		if (stats==null) {
			stats = new SimpleStats();
			endPoints.put(endPoint, stats);
		}

		stats.update(totTime, exTime, error);
	}
	
	public void merge(IGlobalStats _gs) 
	{
		super.merge(_gs);

		AxisGlobalStats _axisGs = (AxisGlobalStats) _gs;

		Map _endPoints =  _axisGs.endPoints;
		
		for (Object key:_endPoints.keySet()) {
			if (endPoints.containsKey(key)) {
				SimpleStats stats = (SimpleStats) endPoints.get(key);
				SimpleStats _stats = (SimpleStats) _endPoints.get(key);
				stats.merge(_stats);
			} else {
				endPoints.put(key, _endPoints.get(key));
			}
		}
	}
	
	public boolean isEquivalent(IGlobalStats _gs) {
		AxisGlobalStats _axisGs = (AxisGlobalStats) _gs;

		if (!super.isEquivalent(_gs)) {
			return false;
		}

		if (endPoints.size()!=_axisGs.endPoints.size()) {
			return false;
		}

		for (Object key:endPoints.keySet()) {
			SimpleStats stats = (SimpleStats) endPoints.get(key);
			SimpleStats _stats = (SimpleStats) _axisGs.endPoints.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		return true;
	}
	
	public AxisGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		super(dis,stringsCache, metricType);
		endPoints = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
	}

	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		super.marshall(dos, stringsCache);
		
		SondasUtils.marshallStringKeyMap(endPoints, dos, stringsCache);
	}
}
