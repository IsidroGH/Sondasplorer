package sondas.client.inspector.view.nodes;

import java.util.Map;
import java.util.Set;
import sondas.client.inspector.model.StatsTreeData;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.probes.delta.CpuGlobalStats;


public class ServletCpuProbeViewer implements IProbeViewer 
{
	public float getMetricValue(String nodeName, String statsName, IGlobalStats gs) 
	{
		CpuGlobalStats cpuGs = (CpuGlobalStats) gs;
		return cpuGs.getMetricValue(nodeName, statsName);
	}
	
	public void registerTimedStats(IGlobalStats gs,  Map<String, StatsTreeData> statsTreeDataMap) {
		int gsId = gs.getId();
		CpuGlobalStats cpuGs = (CpuGlobalStats) gs;
		
		Set<String> entryNames = cpuGs.getEntryNames();
		
		for (String entryName:entryNames) {
			String nodeKey = gsId+":"+entryName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), entryName, gsId);
				std.registerStat("cpu","Cpu usage","%","##0", false);
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}
	
	}
	
	public boolean isTimeStatsVisible() {
		return true;
	}
}
