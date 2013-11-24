package sondas.inspector.probes.delta;

import java.util.Map;

import sondas.utils.ConcurrentHashMap;

public class SqlGlobal {
	static private ConcurrentHashMap pstmQueriesCache;
	static private ConcurrentHashMap pstmParamsCache;
	
	static {
		// Relaciona pstmt con sql
		pstmQueriesCache = new ConcurrentHashMap();
		
		// Relaciona pstmt con sus parametros
		pstmParamsCache = new ConcurrentHashMap();		

	}
	
	static public void newPreparedStatement(Object pstmt, String sql) {
		if (!pstmQueriesCache.containsKey(pstmt) || !pstmQueriesCache.get(pstmt).equals(sql)) {
			//System.out.println("##########2 "+!pstmQueriesCache.get(pstmt).equals(sql));
			// Si no contiene el objeto pstmt o si contiene un objeto pstmt pero con un sql distinto, se registra
			int pos=-1;
			int count=0;
			do {
				pos = sql.indexOf('?', pos+1);
				if (pos!=-1) {
					count++;
				}
			} while (pos!=-1);
			
			pstmQueriesCache.put(pstmt, sql);
			
			Object params[] = new Object[count];
			pstmParamsCache.put(pstmt, params);
			
			//System.out.println("XXXXX-> "+sql+","+count);
			
			//System.out.println("  SqlAgent: Associated PreparedStatement object "+pstmt+" to sql "+sql);
		}
	}
	
	static public void setPreparedStatementParam(Object pstmt, int index, Object param) {
		Object[] params = (Object[]) pstmParamsCache.get(pstmt);
		if (params!=null) {
			params[index]=param;
		}
	}
	
	/**
	 * Obtiene la query asociada al PreparedStatement
	 */
	static public String getPreparedStatementQuery(Object pstmt) {
		return (String) pstmQueriesCache.get(pstmt);
	}
	
	/**
	 * Obtiene los parametros del PreparedStatement
	 */
	static public Object[] getPreparedStatementParams(Object pstmt) {
		return (Object[]) pstmParamsCache.get(pstmt);
	}
	
	static public void removePreparedStatement(Object pstmt) {
		pstmParamsCache.remove(pstmt);
		pstmQueriesCache.remove(pstmt);
	}
}
