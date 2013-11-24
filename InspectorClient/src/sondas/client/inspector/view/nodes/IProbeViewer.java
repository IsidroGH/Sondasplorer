package sondas.client.inspector.view.nodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import sondas.client.inspector.model.StatsTreeData;
import sondas.inspector.delta.IGlobalStats;

public interface IProbeViewer {
	public void registerTimedStats(IGlobalStats gs,   Map<String, StatsTreeData> statsTreeDataMap);
	public boolean isTimeStatsVisible();
	public float getMetricValue(String nodeName, String statsName, IGlobalStats gs);
}
