package sondas.client.inspector.view.nodes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import sondas.client.inspector.model.StatsTreeData;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.inspector.probes.delta.AxisGlobalStats;
import sondas.inspector.probes.delta.AxisNode;
import sondas.inspector.probes.delta.MethodNode;

public class AxisProbeViewer extends MethodProbeViewer  
{
	protected void addTypeName(MethodNode node, StringBuilder sb) {
		sb.append("Type: WebService Call\n");
	}
	
	protected void addTier(StringBuilder sb) {
		sb.append("Tier: Backend\n");
	}

	protected void addName(MethodNode node, StringBuilder sb) {
		sb.append("Invocation\n");
	}
	
	public String getToolTip(MethodNode node) {
		AxisNode axisNode = (AxisNode)node;
		Map endPoints = axisNode.getEndPoints();

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		
		if (endPoints.size()>0) {
			for (Iterator it=endPoints.keySet().iterator();it.hasNext();) {
				String endPoint = (String) it.next();
				SimpleStats stats = (SimpleStats) endPoints.get(endPoint);
				sb.append(endPoint.toString());
				sb.append(" Count:").append(stats.getInvCount());
				sb.append(" ATT:").append(Math.round(stats.getAvgTotTime()));
				sb.append(" Err:").append(Math.round(stats.getErrorCount()));
				sb.append("<br>");
			}
		}
		
		sb.append("</html>");
		return sb.toString();
	}
	
	public boolean isTimeStatsVisible() {
		return true; 
	}
	
	
	public float getMetricValue(String nodeName, String statsName, IGlobalStats gs) 
	{
		StringTokenizer st = new StringTokenizer(nodeName,"|");
		st.nextToken(); // Ignoro el item Web Service 
		st.nextToken(); // Ignoro el item Axis
		String endPoint = st.nextToken();
		if (st.hasMoreTokens()) {
			endPoint+=st.nextToken();
		}
		
		AxisGlobalStats axisGs = (AxisGlobalStats) gs;
		return axisGs.getMetricValue(endPoint, statsName);
	}
	
	public void registerTimedStats(IGlobalStats gs,  Map<String, StatsTreeData> statsTreeDataMap) 
	{
		int gsId = gs.getId();
		AxisGlobalStats axisGs = (AxisGlobalStats) gs;

		for (Iterator it=axisGs.getEndPoints().keySet().iterator();it.hasNext();) {
			URL url;
			try {
				url = new URL((String) it.next());
			} catch (MalformedURLException e) {
				// Ignoro la entrada
				continue;
			}
			
			String nodeName;
			StringBuilder sb = new StringBuilder("Web Services|Axis|");
			sb.append(url.getProtocol()).append("://").append(url.getHost());
			
			if (url.getPort()!=-1) {
				sb.append(":").append(url.getPort());
			}
			
			if (url.getPath()!=null && !url.getPath().equals("")) {
				sb.append("|").append(url.getPath());
			}
			
			if (url.getQuery()!=null && !url.getQuery().equals("")) {
				sb.append("?").append(url.getQuery());
			}
			
			nodeName = sb.toString();
			
			String nodeKey = gsId+":"+nodeName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), nodeName, gsId);
				std.registerStat("totTime","Total time","mseg");
				std.registerStat("invCount","Invocation count","count");
				std.registerStat("errCount","Error count","count");
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}
	}
}
