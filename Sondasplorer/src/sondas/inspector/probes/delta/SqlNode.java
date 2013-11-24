package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.utils.SondasUtils;

/**
 * Sobrecarga  *merge, *combine, *specificClose, *createGlobalStats, *marshall, *Ctor(dis)
 */
public class SqlNode extends MethodNode 
{
	// Constantes globales
	final public static int PreparedStatement=1;
	final public static int Statement=2;
	
	private String currentSql;
	private String currentFullSql;
	final private static int threshold = InspectorRuntime.getPropertyAsInteger("sqlprobe.threshold",1000);
	final private static int maxParamLength = InspectorRuntime.getPropertyAsInteger("sqlprobe.max_param_length",10);
	final private static int maxQueriesCount = InspectorRuntime.getPropertyAsInteger("sqlprobe.max_queries_count",50);
	
	private int sqlType;
	
	// Relaciona preparedStatemtns sin parametros con sus estadisticas
	private Map pstmtQueries;

	// Relaciona preparedStatemtns con parametros con sus estadisticas 
	private Map topPstmtQueries; 

	// Relaciona Statements sin parametros con sus estadisticas
	private Map stmtQueries;

	// Relaciona Statements con parametros con sus estadisticas 
	private Map topStmtQueries; 

	
	/**
	 * Crea un nodo a raiz de una apertura
	 */
	public SqlNode(String name, int metricType, int id) {
		super(name, metricType, false, id);
		pstmtQueries = new HashMap();
		topPstmtQueries = new HashMap();
		stmtQueries = new HashMap();
		topStmtQueries = new HashMap();
	}

	public SqlNode(DataInputStream dis, StringsCache stringsCache,int metricType) throws IOException 
	{
		super(dis,stringsCache, metricType);

		currentSql = SondasUtils.readCachedString(dis, stringsCache);
		currentFullSql = SondasUtils.readCachedString(dis, stringsCache);
		pstmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		topPstmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		stmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		topStmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		sqlType = dis.readShort();
	}
	
	public int getSqlType() {
		return sqlType;
	}
	
	public Map getPstmtQueries() {
		return pstmtQueries;
	}

	public Map getTopPstmtQueries() {
		return topPstmtQueries;
	}
	
	public Map getStmtQueries() {
		return stmtQueries;
	}

	public Map getTopStmtQueries() {
		return topPstmtQueries;
	}

	public void marshallIntrinsic(DataOutputStream dos, StringsCache stringsCache) throws IOException
	{
		super.marshallIntrinsic(dos,stringsCache);	

		SondasUtils.writeCachedString(currentSql, dos, stringsCache);
		SondasUtils.writeCachedString(currentFullSql, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(pstmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(topPstmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(stmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(topStmtQueries, dos, stringsCache);
		dos.writeShort(sqlType);
	}

	protected IGlobalStats createGlobalStats(int categoryId) {
		return new SqlGlobalStats(id, name, "Sql",metricType, categoryId);
	}

	/**
	 * Establece la informacion de la operacion SQL.
	 * Es invocado desde SqlProbe
	 */
	public void setCurrentSql(String sql, Object[] sqlParams, int sqlType) 
	{
		currentSql = sql;
		this.sqlType = sqlType;
		
		if (sqlParams!=null) {
			StringBuilder sb = new StringBuilder();
			sb.append(currentSql).append(";");

			for (int i=0;i<sqlParams.length;i++) {
				String param = sqlParams[i].toString();
				if (param.length()>maxParamLength) {
					// Como máximo registro maxParamLength bytes del parametro
					param = param.substring(0, maxParamLength);
				}
				sb.append(param).append(";");
			}
			currentFullSql = sb.toString();
		} else {
			currentFullSql = sql;
		}
	}

	private void updateSql(int totTime, int exTime, boolean error, IGlobalStats gs) 
	{
		// Actualizo las estadisticas de la query sin parametrizar
		SimpleStats queryStats;
		
		Map queries;
		Map topQueries;
		
		if (sqlType==PreparedStatement) {
			queries=pstmtQueries;
			topQueries=topPstmtQueries;
		} else if (sqlType==Statement) {
			queries=stmtQueries;
			topQueries=topStmtQueries;
		} else {
			System.err.println("Invalid sql type: "+sqlType);
			return;
		}
		
		queryStats = (SimpleStats) queries.get(currentSql);
		if (queryStats==null && queries.size()<maxQueriesCount) {
			queryStats = new SimpleStats();
			queries.put(currentSql, queryStats);
		}

		queryStats.update(totTime, exTime, error);

		// Actualizo las estadisticas de la query parametrizadas si se ha superado el umbral
		if (exTime>threshold) {
			queryStats = (SimpleStats) topQueries.get(currentFullSql);
			if (queryStats==null && topQueries.size()<maxQueriesCount) {
				queryStats = new SimpleStats();
				topQueries.put(currentFullSql, queryStats);
			}

			queryStats.update(totTime, exTime, error);

			((SqlGlobalStats)gs).updateSql(currentSql, currentFullSql, totTime, exTime, error, sqlType);
		} else {		
			((SqlGlobalStats)gs).updateSql(currentSql, null, totTime, exTime, error, sqlType);
		}
	}

	protected void specificClose(int categoryId, int totTime, int exTime, boolean error,  Map<Integer, IGlobalStats> globalStatsMap) {
		// En este caso, asocio la gs al id del nodo porque desde la sonda, estoy agrupando todos los executes en un mismo tipo de nodo
		updateSql(totTime, exTime, error, getGlobalStats(id, globalStatsMap, categoryId));
		super.specificClose(categoryId, totTime, exTime, error, globalStatsMap);
	}

	private void mergeQueries(Map queries, Map _queries) {
		for (Iterator it = _queries.keySet().iterator();it.hasNext();) {
			Object key = it.next();
			if (queries.containsKey(key)) {
				// Mezclo
				((SimpleStats)queries.get(key)).merge((SimpleStats)_queries.get(key));
			} else {
				// Como no existe en el nodo actual lo copio aqui
				queries.put(key,_queries.get(key));
			}
		}
	}
	
	private void internalMerge(SqlNode _node) 
	{
		// PreparedStatements sin paremtrizar
		mergeQueries(pstmtQueries, _node.pstmtQueries);

		// PreparedStatements parametrizadas
		mergeQueries(topPstmtQueries, _node.topPstmtQueries);
		
		// Statements sin paremtrizar
		mergeQueries(stmtQueries, _node.stmtQueries);

		// Statements parametrizadas
		mergeQueries(topStmtQueries, _node.topStmtQueries);
	}
	
	/*
	private void internalMerge(SqlNode _node) 
	{
		// PreparedStatements sin paremtrizar
		for (Iterator it = _node.pstmtQueries.keySet().iterator();it.hasNext();) {
			Object key = it.next();
			if (pstmtQueries.containsKey(key)) {
				// Mezclo
				((SimpleStats)pstmtQueries.get(key)).merge((SimpleStats)_node.pstmtQueries.get(key));
			} else {
				// Como no existe en el nodo actual lo copio aqui
				pstmtQueries.put(key,_node.pstmtQueries.get(key));
			}
		}

		// Statements parametrizadas
		for (Iterator it = _node.topPstmtQueries.keySet().iterator();it.hasNext();) {
			Object key = it.next();
			if (topPstmtQueries.containsKey(key)) {
				// Mezclo
				((SimpleStats)topPstmtQueries.get(key)).merge((SimpleStats)_node.topPstmtQueries.get(key));
			} else {
				// Como no existe en el nodo actual lo copio aqui
				topPstmtQueries.put(key,_node.topPstmtQueries.get(key));
			}
		}

	}
	*/

	/**
	 * Mezcla sin mezclar los hijos
	 * Este método debe ser sobrecargado en las clases descendientes
	 */
	public void merge(MethodNode _node) 
	{
		super.merge(_node);
		internalMerge((SqlNode)_node);

	}

	public boolean combine(MethodNode _node, Map<Integer, IGlobalStats> globalStats) 
	{
		boolean justClosed = super.combine(_node, globalStats);

		// Es posible que el nodo sea indefinido porque se trate de un close
		// En ese caso, _node no sera de tipo SqlNode porque es indefinido sera BasicNode
		if (_node instanceof SqlNode) {
			internalMerge((SqlNode)_node);

			// Mezclo los currents
			SqlNode _sqlNode = (SqlNode) _node;
			if (_sqlNode.currentSql != null) {
				// El nodo a combinar tiene esos parametros definidos
				currentSql = _sqlNode.currentSql;
				currentFullSql = _sqlNode.currentFullSql;
				sqlType = _sqlNode.sqlType;
			} 
		}
		return justClosed;		
	}

	/*
	public boolean isEquivalent(BasicNode _node) {
		if (!super.isEquivalent(_node)) {
			return false;
		}

		SqlNode _sqlNode = (SqlNode) _node;

		if (pstmtQueries.size()!=_sqlNode.pstmtQueries.size()) {
			return false;
		}

		if (topPstmtQueries.size()!=_sqlNode.topPstmtQueries.size()) {
			return false;
		}
		
		if (stmtQueries.size()!=_sqlNode.stmtQueries.size()) {
			return false;
		}

		if (topStmtQueries.size()!=_sqlNode.topStmtQueries.size()) {
			return false;
		}


		for (Object key:pstmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) pstmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlNode.pstmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		for (Object key:topPstmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) topPstmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlNode.topPstmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		if (sqlType!=_sqlNode.sqlType) {
			return false;
		}
		
		return true;		
	}
	*/
	
	public boolean isEquivalent(MethodNode _node) {
		if (!super.isEquivalent(_node)) {
			return false;
		}

		SqlNode _sqlNode = (SqlNode) _node;

		if (!areEquivalentQueries(pstmtQueries, _sqlNode.pstmtQueries)) {
			return false;
		}
		
		if (!areEquivalentQueries(topPstmtQueries, _sqlNode.topPstmtQueries)) {
			return false;
		}

		if (!areEquivalentQueries(stmtQueries, _sqlNode.stmtQueries)) {
			return false;
		}

		if (!areEquivalentQueries(topStmtQueries, _sqlNode.topStmtQueries)) {
			return false;
		}

		if (sqlType!=_sqlNode.sqlType) {
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
