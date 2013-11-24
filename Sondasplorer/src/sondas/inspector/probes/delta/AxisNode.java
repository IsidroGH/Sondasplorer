package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.utils.ICachedMarshable;
import sondas.utils.SondasUtils;

public class AxisNode extends MethodNode 
{
	private Map endPoints;
	private String currentEndPoint;
	
	/**
	 * Crea un nodo a raiz de una apertura
	 */
	public AxisNode(String name, int metricType, int id) {
		super(name, metricType, false, id);
		endPoints = new HashMap();
	}
	
	public AxisNode(DataInputStream dis, StringsCache stringsCache,int metricType) throws IOException 
	{
		super(dis,stringsCache, metricType);

		currentEndPoint = SondasUtils.readCachedString(dis, stringsCache);
		endPoints = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
	}
	
	public Map getEndPoints() {
		return endPoints;
	}
	
	
	
	public void marshallIntrinsic(DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		super.marshallIntrinsic(dos,stringsCache);	

		SondasUtils.writeCachedString(currentEndPoint.toString(), dos, stringsCache);
		SondasUtils.marshallStringKeyMap(endPoints, dos, stringsCache);
	}
	
	protected IGlobalStats createGlobalStats(int categoryId) {
		return new AxisGlobalStats(id, name, "Axis",metricType, categoryId);
	}
	
	public void setCurrentEndPoint(String endPoint) {
		currentEndPoint = endPoint;
	}
	
	private void updateAxis(int totTime, int exTime, boolean error, IGlobalStats gs) {

		SimpleStats endPointsStats = (SimpleStats) endPoints.get(currentEndPoint);
		if (endPointsStats==null) {
			endPointsStats = new SimpleStats();
			endPoints.put(currentEndPoint, endPointsStats);
		}

		endPointsStats.update(totTime, exTime, error);
		
		((AxisGlobalStats)gs).updateAxis(currentEndPoint, totTime, exTime, error);
	}
	
	protected void specificClose(int categoryId, int totTime, int exTime, boolean error,  Map<Integer, IGlobalStats> globalStatsMap) {
		// En este caso, asocio la gs al id del nodo porque desde la sonda, estoy agrupando todos los executes en un mismo tipo de nodo
		updateAxis(totTime, exTime, error, getGlobalStats(id, globalStatsMap, categoryId));
		super.specificClose(categoryId, totTime, exTime, error, globalStatsMap);
	}
	
	private void internalMerge(AxisNode _node) {
		for (Iterator it = _node.endPoints.keySet().iterator();it.hasNext();) {
			Object key = it.next();
			if (endPoints.containsKey(key)) {
				// Mezclo
				((SimpleStats)endPoints.get(key)).merge((SimpleStats)_node.endPoints.get(key));
			} else {
				// Como no existe en el nodo actual lo copio aqui
				endPoints.put(key,_node.endPoints.get(key));
			}
		}
	}
	
	/**
	 * Mezcla sin mezclar los hijos
	 * Este método debe ser sobrecargado en las clases descendientes
	 */
	public void merge(MethodNode _node) 
	{
		super.merge(_node);
		internalMerge((AxisNode)_node);
	}
	
	public boolean combine(MethodNode _node, Map<Integer, IGlobalStats> globalStats) 
	{
		boolean justClosed = super.combine(_node, globalStats);

		// Es posible que el nodo sea indefinido porque se trate de un close
		// En ese caso, _node no sera de tipo SqlNode porque es indefinido sera BasicNode
		if (_node instanceof AxisNode) {
			internalMerge((AxisNode)_node);

			// Mezclo los currents
			AxisNode _axisNode = (AxisNode) _node;
			if (_axisNode.currentEndPoint != null) {
				// El nodo a combinar tiene esos parametros definidos
				currentEndPoint = _axisNode.currentEndPoint;
			} 
		}
		return justClosed;		
	}
	
	public boolean isEquivalent(MethodNode _node) {
		if (!super.isEquivalent(_node)) {
			return false;
		}

		AxisNode _axisNode = (AxisNode) _node;

		if (!areEquivalentQueries(endPoints, _axisNode.endPoints)) {
			return false;
		}

		if (currentEndPoint!=_axisNode.currentEndPoint) {
			return false;
		}
		
		return true;		
	}
	
	private boolean areEquivalentQueries(Map map, Map _map) {
		if (map.size()!=_map.size()) {
			return false;
		}
		
		for (Object key:map.keySet()) {
			SimpleStats stats = (SimpleStats) map.get(key);
			SimpleStats _stats = (SimpleStats) _map.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}
		
		return true;
	}
}
