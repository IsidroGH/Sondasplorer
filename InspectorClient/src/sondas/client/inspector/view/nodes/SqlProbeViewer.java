package sondas.client.inspector.view.nodes;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import sondas.client.inspector.model.StatsTreeData;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.inspector.probes.SqlProbe;
import sondas.inspector.probes.delta.MethodNode;
import sondas.inspector.probes.delta.SqlGlobalStats;
import sondas.inspector.probes.delta.SqlNode;

public class SqlProbeViewer extends MethodProbeViewer 
{
	public float getMetricValue(String nodeName, String statsName, IGlobalStats gs) 
	{
		String statement;
		boolean topStatement;
		
		SqlGlobalStats sqlGs = (SqlGlobalStats) gs;
		StringTokenizer st = new StringTokenizer(nodeName,"|");
		String aux = st.nextToken(); // PreparedStatement | Statement 
		aux = st.nextToken();
		
		if (aux.equals("Top statements")) {
			statement = st.nextToken();
			topStatement = true;
		} else {
			statement = aux;
			topStatement = false;
		}
		
		int sqlType;
		if (nodeName.startsWith("Statement")) {
			sqlType=SqlNode.Statement;
		} else if (nodeName.startsWith("PreparedStatement")) {
			sqlType=SqlNode.PreparedStatement;
		} else {
			throw new RuntimeException("Unkown sql node: "+nodeName);
		}
		
		return sqlGs.getMetricValue(statement, topStatement, statsName, sqlType);
		
	}
	
	
	protected void addTypeName(MethodNode node, StringBuilder sb) {
		SqlNode sqlNode = (SqlNode) node;
		if (sqlNode.getSqlType()==SqlNode.PreparedStatement) {
			sb.append("Type: PreparedStatement\n");
		} else if (sqlNode.getSqlType()==SqlNode.Statement) {
			sb.append("Type: Statement\n");
		} else {
			sb.append("Type: Undefined\n");
		}
	}

	protected void addTier(StringBuilder sb) {
		sb.append("Tier: Database\n");
	}

	protected void addName(MethodNode node, StringBuilder sb, Map methodNames) {
		if (node.getId()==SqlProbe.PstmtExecuteId || node.getId()==SqlProbe.StmtExecuteId ) {
			sb.append("Execute\n");
		} else {
			sb.append(node.getDigestedName(methodNames)).append("\n");			
		}
	}

	private void generateToolTip(String name, Map queries, StringBuilder sb) {
		if (queries.size()>0) {
			sb.append("<b>").append(name).append("<br></b>");
			for (Iterator it=queries.keySet().iterator();it.hasNext();) {
				String query = (String) it.next();
				SimpleStats stats = (SimpleStats) queries.get(query);
				sb.append(query);
				sb.append(" Count:").append(stats.getInvCount());
				sb.append(" AET:").append(Math.round(stats.getAvgExTime()));
				sb.append(" ATT:").append(Math.round(stats.getAvgTotTime()));
				sb.append("<br>");
			}
		}
	}

	public String getToolTip(MethodNode node) {
		SqlNode sqlNode = (SqlNode)node;
		Map queries = sqlNode.getPstmtQueries();

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		generateToolTip("Statements", sqlNode.getStmtQueries(), sb);
		generateToolTip("PreparedStatements", sqlNode.getPstmtQueries(), sb);
		sb.append("</html>");
		return sb.toString();
	}

	/*
	public String getToolTip(BasicNode node) {
		SqlNode sqlNode = (SqlNode)node;
		Map queries = sqlNode.getPstmtQueries();

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>PreparedStatements</b>");
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			SimpleStats stats = (SimpleStats) queries.get(query);
			sb.append(query);
			sb.append(" Count:").append(node.getInvCount());
			sb.append(" Errors:").append(node.getErrorCount());
			sb.append(" AET:").append(Math.round(node.getAvgExTime()));
			sb.append(" ATT:").append(Math.round(node.getAvgTotTime()));
			sb.append("<br>");
		}

		sb.append("<b>Statements</b>");
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			SimpleStats stats = (SimpleStats) queries.get(query);
			sb.append(query);
			sb.append(" Count:").append(node.getInvCount());
			sb.append(" Errors:").append(node.getErrorCount());
			sb.append(" AET:").append(Math.round(node.getAvgExTime()));
			sb.append(" ATT:").append(Math.round(node.getAvgTotTime()));
			sb.append("<br>");
		}

		sb.append("</html>");
		return sb.toString();
	}
	 */

	public boolean isTimeStatsVisible() {
		return true; 
	}

	/**
	 * Crea el StatsTreeData con los StatsNames del tipo de nodo
	 * El valor se obtendra de la global (SqlGlobalStats)
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

	private void registerTimedStats (IGlobalStats gs,  Map<String, StatsTreeData> statsTreeDataMap, int sqlType) {
		int gsId = gs.getId();
		SqlGlobalStats sqlGs = (SqlGlobalStats) gs;

		Map queries;
		Map topQueries;
		String sqlTypeName;

		if (sqlType==SqlNode.PreparedStatement) {
			queries = sqlGs.getPstmtQueries();
			topQueries = sqlGs.getTopPstmtQueries();
			sqlTypeName = "PreparedStatements";
		} else if (sqlType==SqlNode.Statement) {
			queries = sqlGs.getStmtQueries();
			topQueries = sqlGs.getTopStmtQueries();
			sqlTypeName = "Statements";
		} else {
			System.err.println("Sql type invalid: "+sqlType);
			return;
		}

		// queries
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			String nodeName = sqlTypeName+"|"+query;
			String nodeKey = gsId+":"+nodeName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), nodeName, gsId);
				std.registerStat("totTime","Total time","mseg");
				std.registerStat("invCount","Invocation count","count");
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}

		// top queries
		for (Iterator it=topQueries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			String nodeName = sqlTypeName+"|Top statements|"+query;
			String nodeKey = gsId+":"+nodeName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), nodeName, gsId);
				std.registerStat("totTime","Total time","mseg");
				std.registerStat("invCount","Invocation count","count");
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}
	}

	public void registerTimedStats(IGlobalStats gs,  Map<String, StatsTreeData> statsTreeDataMap, Map<Integer,String> methodNames) 
	{
		int gsId = gs.getId();
		SqlGlobalStats sqlGs = (SqlGlobalStats) gs;

		registerTimedStats(gs, statsTreeDataMap, SqlNode.PreparedStatement);
		registerTimedStats(gs, statsTreeDataMap, SqlNode.Statement);
	}


	/*
	public void registerTimedStats(GlobalStats gs,  Map<String, StatsTreeData> statsTreeDataMap) 
	{
		String gsId = gs.getId();
		SqlGlobalStats sqlGs = (SqlGlobalStats) gs;

		// PreparedStatements
		Map queries = sqlGs.getPstmtQueries();
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			String nodeName = "PreparedStatements|"+query;
			String nodeKey = gsId+":"+nodeName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), nodeName, gsId);
				std.registerStat("totTime","Total time","mseg");
				std.registerStat("invCount","Invocation count","count");
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}

		// Slow queries
		queries = sqlGs.getTopPstmtQueries();
		for (Iterator it=queries.keySet().iterator();it.hasNext();) {
			String query = (String) it.next();
			String nodeName = "PreparedStatements|Top statements|"+query;
			String nodeKey = gsId+":"+nodeName;

			if (!statsTreeDataMap.containsKey(nodeKey)) {
				StatsTreeData std = new StatsTreeData(gs.getMetricType(), nodeName, gsId);
				std.registerStat("totTime","Total time","mseg");
				std.registerStat("invCount","Invocation count","count");
				statsTreeDataMap.put(nodeKey, std);
				std.registerNode(gs.getCategoryId());
			}
		}

	}
	 */

	private String composeFullSql(String fullSql) {
		StringTokenizer st = new StringTokenizer(fullSql,";");
		String sql = st.nextToken();
		while (st.hasMoreTokens()) {
			sql = sql.replaceFirst("?", st.nextToken());
		}

		return sql;
	}

}
