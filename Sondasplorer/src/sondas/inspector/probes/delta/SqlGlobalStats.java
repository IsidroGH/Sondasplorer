package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleGlobalStats;
import sondas.inspector.delta.SimpleStats;
import sondas.utils.IUnmarshaller;
import sondas.utils.SondasUtils;

/**
 * Sobreescribe: *merge, *marshall, *Ctor(dis), *isEquivalent, clone*, getDiferential*
 */
public class SqlGlobalStats extends SimpleGlobalStats
{
	// Relaciona PreparedStatements sin parametros con sus estadisticas
	private Map pstmtQueries;

	// Relaciona Statements con parametros con sus estadisticas 
	private Map topPstmtQueries; 

	// Relaciona PreparedStatements sin parametros con sus estadisticas
	private Map stmtQueries;

	// Relaciona Statements con parametros con sus estadisticas 
	private Map topStmtQueries; 

	
	public SqlGlobalStats(int id, String name, String typeName, int metricType, int categoryId) {
		super(id,name,typeName,metricType, categoryId);
		pstmtQueries = new HashMap();
		topPstmtQueries = new HashMap();
		stmtQueries = new HashMap();
		topStmtQueries = new HashMap();
	}

	private SqlGlobalStats(IGlobalStats gs) {
		super(gs);
		pstmtQueries = new HashMap();
		topPstmtQueries = new HashMap();
		stmtQueries = new HashMap();
		topStmtQueries = new HashMap();
	}

	/**
	 * statsName = totTime
	 * nodeName = PreparedStatements|select roles, access from users where userid=?
	 */
	public float getMetricValue(String statement, boolean topStatement, String statsName, int sqlType) {
		Map queries;
		Map topQueries;
		
		if (sqlType==SqlNode.PreparedStatement) {
			queries=pstmtQueries;
			topQueries=topPstmtQueries;
		} else if (sqlType==SqlNode.Statement) {
			queries=stmtQueries;
			topQueries=topStmtQueries;
		} else {
			throw new RuntimeException("Unkown sql type: "+sqlType);
		}
		
		return getMetricValue(statement, topStatement, statsName, queries, topQueries);
	}
	
	private float getMetricValue(String statement, boolean topStatement, String statsName, Map queries, Map topQueries) {
		float resp=0;
		/*
		StringTokenizer st = new StringTokenizer(nodeName,"|");
		String aux = st.nextToken(); // PreparedStatement | Statement 
		aux = st.nextToken();
		*/
		SimpleStats stats = null;

		if (topStatement) {
			stats = (SimpleStats) topQueries.get(statement);
		} else {
			stats = (SimpleStats) queries.get(statement);	
		}

		if (stats!=null) {
			if (statsName.equals("totTime")) {
				resp = stats.getAvgTotTime();
			} else if (statsName.equals("invCount")) {
				resp = stats.getInvCount();
			} else {
				throw new RuntimeException("Unkown metric name: "+statsName);
			}
		}
		
		return resp;
	}

	public Object clone() {
		SqlGlobalStats resp = new SqlGlobalStats(this);

		for (Iterator it=pstmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) pstmtQueries.get(key);
			resp.pstmtQueries.put(key,value.clone());
		}

		for (Iterator it=topPstmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) topPstmtQueries.get(key);
			resp.topPstmtQueries.put(key,value.clone());
		}
		
		for (Iterator it=stmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) stmtQueries.get(key);
			resp.stmtQueries.put(key,value.clone());
		}

		for (Iterator it=topStmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) topStmtQueries.get(key);
			resp.topStmtQueries.put(key,value.clone());
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
		SqlGlobalStats resp = new SqlGlobalStats((SimpleGlobalStats)super.getDiferential(_gs));

		SqlGlobalStats _sqlGs = (SqlGlobalStats) _gs;
		// Añado el diferencial de la SqlGlobal
		// Queries
		getDiferential(pstmtQueries, _sqlGs.pstmtQueries, resp.pstmtQueries);
		getDiferential(topPstmtQueries, _sqlGs.topPstmtQueries, resp.topPstmtQueries);
		getDiferential(stmtQueries, _sqlGs.stmtQueries, resp.stmtQueries);
		getDiferential(topStmtQueries, _sqlGs.topStmtQueries, resp.topStmtQueries);

		return resp;
	}
	
	/*
	public GlobalStats getDiferential(GlobalStats _gs) {
		// Creo la respuesta con el diferencial de la Global
		SqlGlobalStats resp = new SqlGlobalStats(super.getDiferential(_gs));

		SqlGlobalStats _sqlGs = (SqlGlobalStats) _gs;
		// Añado el diferencial de la SqlGlobal
		// Queries
		for (Iterator it=pstmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) pstmtQueries.get(key);

			if (!_sqlGs.pstmtQueries.containsKey(key)) {
				resp.pstmtQueries.put(key, value.clone());
			} else {
				resp.pstmtQueries.put(key, value.getDiferential((SimpleStats)_sqlGs.pstmtQueries.get(key)));
			}			
		}
		// SlowQueries
		for (Iterator it=topPstmtQueries.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			SimpleStats value = (SimpleStats) topPstmtQueries.get(key);

			if (!_sqlGs.topPstmtQueries.containsKey(key)) {
				resp.topPstmtQueries.put(key, value.clone());
			} else {
				resp.topPstmtQueries.put(key, value.getDiferential((SimpleStats)_sqlGs.topPstmtQueries.get(key)));
			}			
		}

		return resp;
	}
	*/

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
		return topStmtQueries;
	}

	public void updateSql(String sql, String fullSql, int totTime, int exTime, boolean error, int sqlType) 
	{
		Map queries;
		Map topQueries;
		
		if (sqlType==SqlNode.PreparedStatement) {
			queries = pstmtQueries;
			topQueries = topPstmtQueries;
		} else if (sqlType==SqlNode.Statement) {
			queries = stmtQueries;
			topQueries = topStmtQueries;
		} else {
			System.err.println("Invalid sql type: "+sqlType);
			return;
		}
		
		
		// Actualizo las estadisticas de las queries sin parametrizar
		SimpleStats pstmtStats;

		pstmtStats = (SimpleStats) queries.get(sql);
		if (pstmtStats==null) {
			pstmtStats = new SimpleStats();
			queries.put(sql, pstmtStats);
		}

		pstmtStats.update(totTime, exTime, error);

		// Actualizo las estadisticas de las queries parametrizadas se ha superado el umbral
		if (fullSql!=null) {

			pstmtStats = (SimpleStats) topQueries.get(fullSql);
			if (pstmtStats==null) {
				pstmtStats = new SimpleStats();
				topQueries.put(fullSql, pstmtStats);
			}

			pstmtStats.update(totTime, exTime, error);
		}
	}
	
	private void mergeQueries(Map queries, Map _queries) {
		for (Object key:_queries.keySet()) {
			if (queries.containsKey(key)) {
				SimpleStats stats = (SimpleStats) queries.get(key);
				SimpleStats _stats = (SimpleStats) _queries.get(key);
				stats.merge(_stats);
			} else {
				queries.put(key, _queries.get(key));
			}
		}
	}
	
	public void merge(IGlobalStats _gs) 
	{
		super.merge(_gs);

		// Mezclo las queries sin parametros
		SqlGlobalStats _sqlGs = (SqlGlobalStats) _gs;

		mergeQueries(pstmtQueries, _sqlGs.pstmtQueries);
		mergeQueries(topPstmtQueries, _sqlGs.topPstmtQueries);
		mergeQueries(stmtQueries, _sqlGs.stmtQueries);
		mergeQueries(topStmtQueries, _sqlGs.topStmtQueries);
	}
	
	/*
	public void merge(GlobalStats _gs) 
	{
		super.merge(_gs);

		// Mezclo las queries sin parametros
		SqlGlobalStats _sqlGs = (SqlGlobalStats) _gs;

		for (Object key:_sqlGs.pstmtQueries.keySet()) {
			if (pstmtQueries.containsKey(key)) {
				SimpleStats stats = (SimpleStats) pstmtQueries.get(key);
				SimpleStats _stats = (SimpleStats) _sqlGs.pstmtQueries.get(key);
				stats.merge(_stats);
			} else {
				pstmtQueries.put(key, _sqlGs.pstmtQueries.get(key));
			}
		}

		// Mezclo las queries con parametros
		for (Object key:_sqlGs.topPstmtQueries.keySet()) {
			if (topPstmtQueries.containsKey(key)) {
				SimpleStats stats = (SimpleStats) topPstmtQueries.get(key);
				SimpleStats _stats = (SimpleStats) _sqlGs.topPstmtQueries.get(key);
				stats.merge(_stats);
			} else {
				topPstmtQueries.put(key, _sqlGs.topPstmtQueries.get(key));
			}
		}
	}
	*/

	public boolean isEquivalent(IGlobalStats _gs) {
		SqlGlobalStats _sqlGs = (SqlGlobalStats) _gs;

		if (!super.isEquivalent(_gs)) {
			return false;
		}

		if (pstmtQueries.size()!=_sqlGs.pstmtQueries.size()) {
			return false;
		}

		if (topPstmtQueries.size()!=_sqlGs.topPstmtQueries.size()) {
			return false;
		}


		for (Object key:pstmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) pstmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlGs.pstmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		for (Object key:topPstmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) topPstmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlGs.topPstmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}
		
		for (Object key:stmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) stmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlGs.stmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		for (Object key:topStmtQueries.keySet()) {
			SimpleStats stats = (SimpleStats) topStmtQueries.get(key);
			SimpleStats _stats = (SimpleStats) _sqlGs.topStmtQueries.get(key);
			if (!stats.isEquivalent(_stats)) {
				return false;
			}
		}

		return true;
	}

	public SqlGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		super(dis,stringsCache, metricType);
		pstmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		topPstmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		stmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
		topStmtQueries = SondasUtils.unmarshallStringKeyMap(dis, SimpleStats.getUnmarshaller(), stringsCache);
	}

	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		super.marshall(dos, stringsCache);
		SondasUtils.marshallStringKeyMap(pstmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(topPstmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(stmtQueries, dos, stringsCache);
		SondasUtils.marshallStringKeyMap(topStmtQueries, dos, stringsCache);
	}
}
