package sondas.client.inspector.view.nodes;

import java.util.Map;
import sondas.client.inspector.model.Model;
import sondas.client.inspector.model.StatsTreeData;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleGlobalStats;
import sondas.inspector.probes.delta.MethodNode;

public class MethodProbeViewer implements IProbeViewer
{
	static Map<Integer,String> methodNames;
	
	static {
		methodNames = Model.getMethodNames();
	}
	
	public String getView(MethodNode node) {
		StringBuilder sb = new StringBuilder();
		addName(node, sb);
		addTypeName(node, sb);
		addCommonStatistics(node, sb);
		addTier(sb);
		return sb.toString();
	}

	public float getMetricValue(String nodeName, String statsName, IGlobalStats gs) 
	{
		SimpleGlobalStats methodGs = (SimpleGlobalStats) gs;
		return methodGs.getMetricValue(statsName);
	}
	
	protected void addCommonStatistics(MethodNode node, StringBuilder sb) {
		sb.append("Count:").append(node.getInvCount());
		sb.append(" Errors:").append(node.getErrorCount()).append("\n");
		sb.append("AET:").append(Math.round(node.getAvgExTime()));
		sb.append(" ATT:").append(Math.round(node.getAvgTotTime())).append("\n");
	}

	protected void addName(MethodNode node, StringBuilder sb) {
		//sb.append(node.getDigestedName()).append("\n");
		sb.append(node.getMethodClassAndName(methodNames)).append("\n");
	}

	protected void addTypeName(MethodNode node, StringBuilder sb) {
		sb.append("Type: Method\n");
	}

	protected void addTier(StringBuilder sb) {
		sb.append("Tier: Application");
	}

	public String getToolTip(MethodNode node) {
		return node.getDigestedName(methodNames);
	}
	
	public boolean isTimeStatsVisible() {
		return false;
	}

	/**
	 * Crea el StatsTreeData con los StatsNames del tipo de nodo
	 * El valor se obtendra de la global (GlobalStats)
	 */
	/*
	public StatsTreeData createTimedStats(int metricType, String name) 
	{
		StatsTreeData std = new StatsTreeData(metricType, name);
		std.registerStat("totTime","Total time","mseg");
		std.registerStat("exTime","Exclusive time","mseg");
		std.registerStat("invCount","Invocation count","count");
		std.registerStat("errCount","Error count","count");
		
		return std;
	}*/
	
	public void registerTimedStats(IGlobalStats gs,   Map<String, StatsTreeData> statsTreeDataMap) 
	{
		int gsId = gs.getId();
		String key = String.valueOf(gsId);
		
		if (!statsTreeDataMap.containsKey(key)) {
			StatsTreeData std = new StatsTreeData(gs.getMetricType(), gs.getName(methodNames),gsId);
			std.registerStat("totTime","Total time","mseg");
			std.registerStat("exTime","Exclusive time","mseg");
			std.registerStat("invCount","Invocation count","count");
			std.registerStat("errCount","Error count","count");
			statsTreeDataMap.put(key, std);
			
			// Registra las metricas globales creando la entrada correspondiente asociada a MetricsTree en el mapa metricsTreeDataMap
			int categoryId = gs.getCategoryId();		
			std.registerNode(categoryId);
		} 
	}

}
