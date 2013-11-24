package sondas.inspector.probes;

import sondas.BaseExecutionData;
import sondas.CurrentThread;
import sondas.SondasLoader;
import sondas.inspector.Constants;
import sondas.inspector.Stamp;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.delta.MethodNode;
import sondas.inspector.probes.delta.SqlGlobal;
import sondas.inspector.probes.delta.SqlNode;
import sondas.inspector.probes.full.SqlTrace;
import sondas.utils.DataSender;
import sondas.utils.SondasUtils;

/**
 * Type
 * 	1 = PreparedStatement
 */
class ExData extends BaseExecutionData 
{
	String sql;
	int type;
	boolean isConnection;

	/**
	 * Llamado desde Connection
	 */
	public ExData(int type, String sql) {
		this.sql = sql;
		this.type = type;
		isConnection=true;
	}
	
	public ExData() {
	}

}

public class SqlProbe extends InspectorBaseProbe
{
	final public static int PstmtExecuteId = SondasLoader.getNewId(); 
	final public static int StmtExecuteId = SondasLoader.getNewId();
	
	public SqlProbe() 
	{
		super(false, true, false,"SqlMetric");
		// Connection
		setMethodToInstrumentalize("prepareStatement");

		// PreparedStatement
		setMethodToInstrumentalize("setString");
		setMethodToInstrumentalize("setInt");
		setMethodToInstrumentalize("setDate");
		setMethodToInstrumentalize("executeQuery");
		setMethodToInstrumentalize("close");
	}

	/**
	 * Solo reporto metricas del metodo executeQuery
	 */
	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference)
	{
		// DEBUG
		//System.out.println("SQL Probe: Debug mode");
		
		ExData exData = null;

		if (capturing) 
		{
			if ( (/*DEBUG*/className!=null && className.equals("java.sql.Connection")) || thisReference instanceof java.sql.Connection) {
				if (methodName.equals("prepareStatement")) {
					exData = new ExData(1,(String) params[0]);
				}
			} else if ( (/*DEBUG*/className!=null && className.equals("java.sql.PreparedStatement")) ||thisReference instanceof java.sql.PreparedStatement) {
				if (methodName.startsWith("set")) {
					SqlGlobal.setPreparedStatementParam(thisReference, ((Integer)params[0]).intValue()-1, params[1]);
				} else if (methodName.startsWith("execute")) {
					// Englobo todos los execute, executeQuery, executeUpdate dentro de un mismo de nodo llamado pstmt_execute
					int tid = ((CurrentThread)currentThread.get()).tid;
					String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);

					// Obtengo la informacion de la query
					String sql = (String) SqlGlobal.getPreparedStatementQuery(thisReference);

					// Obtengo los parametros de la query
					Object[] sqlParams = SqlGlobal.getPreparedStatementParams(thisReference);

					if (fullMode) 
					{
						// Full Mode
						SqlTrace trace = new SqlTrace(fqMethod, Constants.Start, sql, sqlParams);
						Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
						DataSender.sendBuffered(stamp);
					} else {
						// Delta Mode
						DeltaTree dt = null;
						exData = new ExData();

						try {
							dt = getThreadDeltaTree(tid);
							// Method
							if (dt.hasChildNode(PstmtExecuteId)) {
								long ts = System.currentTimeMillis();
								SqlNode node = (SqlNode) dt.openNode(PstmtExecuteId,ts);
								node.setCurrentSql(sql, sqlParams, SqlNode.PreparedStatement);
							} else {
								SqlNode node = new SqlNode("pstmt_execute", metricType, PstmtExecuteId);
								long ts = System.currentTimeMillis();
								dt.openNode(node, ts);
								node.setCurrentSql(sql, sqlParams, SqlNode.PreparedStatement);
							}

						} catch (InterruptedException e) {
						} finally {
							releaseDeltaTree(dt);
						}
					}
				} else if (methodName.startsWith("close")) {
					SqlGlobal.removePreparedStatement(thisReference);
				}
			} else if ( (/*DEBUG*/className!=null && className.equals("java.sql.Statement")) ||thisReference instanceof java.sql.Statement) {
				// Englobo todos los execute, executeQuery, executeUpdate dentro de un mismo de nodo llamado pstmt_execute
				int tid = ((CurrentThread)currentThread.get()).tid;

				// Obtengo la informacion de la query
				String sql = (String)params[0];

				if (fullMode) 
				{
					// Full Mode
					String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
					SqlTrace trace = new SqlTrace(fqMethod, Constants.Start, sql, null);
					Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
					DataSender.sendBuffered(stamp);
				} else {
					// Delta Mode
					DeltaTree dt = null;
					exData = new ExData();
					
					try {
						dt = getThreadDeltaTree(tid);
						// Method
						if (dt.hasChildNode(StmtExecuteId)) {
							long ts = System.currentTimeMillis();
							SqlNode node = (SqlNode) dt.openNode(StmtExecuteId,ts);
							node.setCurrentSql(sql, null, SqlNode.Statement);
						} else {
							SqlNode node = new SqlNode("stmt_execute", metricType, StmtExecuteId);
							long ts = System.currentTimeMillis();
							dt.openNode(node, ts);
							node.setCurrentSql(sql, null, SqlNode.Statement);
						}

					} catch (InterruptedException e) {
					} finally {
						releaseDeltaTree(dt);
					}
				}
			} else if (methodName.startsWith("close")) {
				SqlGlobal.removePreparedStatement(thisReference);
			}
		}
		return exData;
	}

	public Object startMethodDebug(int nodeId, String className, String methodName, String methodSignature, Object params[], Object customParams[], Object thisReference, long ts)
	{
		// DEBUG
		//System.out.println("SQL Probe: Debug mode");
		
		ExData exData = null;

		if (capturing) 
		{
			if ( (/*DEBUG*/className!=null && className.equals("java.sql.Connection")) || thisReference instanceof java.sql.Connection) {
				if (methodName.equals("prepareStatement")) {
					exData = new ExData(1,(String) params[0]);
				}
			} else if ( (/*DEBUG*/className!=null && className.equals("java.sql.PreparedStatement")) ||thisReference instanceof java.sql.PreparedStatement) {
				if (methodName.startsWith("set")) {
					SqlGlobal.setPreparedStatementParam(thisReference, ((Integer)params[0]).intValue()-1, params[1]);
				} else if (methodName.startsWith("execute")) {
					// Englobo todos los execute, executeQuery, executeUpdate dentro de un mismo de nodo llamado pstmt_execute
					int tid = ((CurrentThread)currentThread.get()).tid;

					// Obtengo la informacion de la query
					String sql = (String) SqlGlobal.getPreparedStatementQuery(thisReference);

					// Obtengo los parametros de la query
					Object[] sqlParams = SqlGlobal.getPreparedStatementParams(thisReference);

					if (fullMode) 
					{
						// Full Mode
						String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
						SqlTrace trace = new SqlTrace(fqMethod, Constants.Start, sql, sqlParams);
						Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
						DataSender.sendBuffered(stamp);
					} else {
						// Delta Mode
						DeltaTree dt = null;
						exData = new ExData();
						
						try {
							dt = getThreadDeltaTree(tid);

							// Method
							if (dt.hasChildNode(PstmtExecuteId)) {
								SqlNode node = (SqlNode) dt.openNode(PstmtExecuteId,ts);
								node.setCurrentSql(sql, sqlParams, SqlNode.PreparedStatement);
							} else {
								SqlNode node = new SqlNode("pstmt_execute", metricType, PstmtExecuteId);
								dt.openNode(node, ts);
								node.setCurrentSql(sql, sqlParams, SqlNode.PreparedStatement);
							}

						} catch (InterruptedException e) {
						} finally {
							releaseDeltaTree(dt);
						}
					}
				}
			} else if ( (/*DEBUG*/className!=null && className.equals("java.sql.Statement")) ||thisReference instanceof java.sql.Statement) {
				if (methodName.startsWith("execute")) {
					// Englobo todos los execute, executeQuery, executeUpdate dentro de un mismo de nodo llamado pstmt_execute	

					int tid = ((CurrentThread)currentThread.get()).tid;

					// Obtengo la informacion de la query
					String sql = (String)params[0];
					
					if (fullMode) 
					{
						// Full Mode
						String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
						SqlTrace trace = new SqlTrace(fqMethod, Constants.Start, sql, null);
						Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
						DataSender.sendBuffered(stamp);
					} else {
						// Delta Mode
						DeltaTree dt = null;
						exData = new ExData();
						
						try {
							dt = getThreadDeltaTree(tid);
							// Method
							if (dt.hasChildNode(StmtExecuteId)) {
								SqlNode node = (SqlNode) dt.openNode(StmtExecuteId,ts);
								node.setCurrentSql(sql, null, SqlNode.Statement);
							} else {
								SqlNode node = new SqlNode("stmt_execute", metricType, StmtExecuteId);
								dt.openNode(node, ts);
								node.setCurrentSql(sql, null, SqlNode.Statement);
							}

						} catch (InterruptedException e) {
						} finally {
							releaseDeltaTree(dt);
						}
					}
				}
			}
		}
		return exData;
	}

	
	public void endMethod(int nodeId, Object executionData, Object response)
	{
		endMethodInternal(nodeId, executionData, response, false);
	}

	public void exceptionMethod(int nodeId, Object executionData)
	{
		endMethodInternal(nodeId, executionData, null, true);
	}
	
	public void endMethodDebug(int nodeId, Object executionData, Object response, long ts)
	{
		endMethodInternalDebug(nodeId, executionData, response, false,ts);
	}

	public void exceptionMethodDebug(int nodeId, Object executionData, long ts)
	{
		endMethodInternalDebug(nodeId, executionData, null, true, ts);
	}

	private void endMethodInternal(int nodeId, Object executionData, Object response, boolean withException) 
	{
		if (capturing){
			if (executionData!=null)
			{ 
				long ts = System.currentTimeMillis();
				ExData exData = (ExData) executionData;
				
				if (exData.isConnection) {
					// Connection
					if (exData.type==1) {
						// PreparedStatement
						SqlGlobal.newPreparedStatement(response, exData.sql);
					}
				} else {
					// Execution

					int tid = ((CurrentThread)currentThread.get()).tid;

					if (fullMode) 
					{
						// Full Mode
						Stamp stamp;			

						if (withException) {
							SqlTrace trace = new SqlTrace(null, Constants.Exception, null, null);
							stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);

						} else {
							SqlTrace trace = new SqlTrace(null, Constants.End, null, null);
							stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
						}

						DataSender.sendBuffered(stamp);
					} else {
						// Delta Mode
						DeltaTree dt = null;

						try {
							dt = getThreadDeltaTree(tid); 

							dt.closeNode(nodeId, ts,withException?3:2,false);

						} catch (InterruptedException  e) {
						} finally {
							releaseDeltaTree(dt);
						}
					}
				}
			}
		}
	}
	
	private void endMethodInternalDebug(int nodeId, Object executionData, Object response, boolean withException, long ts) 
	{
		if (capturing){
			if (executionData!=null)
			{ 
				ExData exData = (ExData) executionData;
				
				if (exData.isConnection) {
					// Connection
					if (exData.type==1) {
						// PreparedStatement
						SqlGlobal.newPreparedStatement(response, exData.sql);
					}
				} else {
					// Execution

					int tid = ((CurrentThread)currentThread.get()).tid;

					if (fullMode) 
					{
						// Full Mode
						Stamp stamp;			

						if (withException) {
							SqlTrace trace = new SqlTrace(null, Constants.Exception, null, null);
							stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);

						} else {
							SqlTrace trace = new SqlTrace(null, Constants.End, null, null);
							stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
						}

						DataSender.sendBuffered(stamp);
					} else {
						// Delta Mode
						DeltaTree dt = null;

						try {
							dt = getThreadDeltaTree(tid); 

							dt.closeNode(nodeId, ts,withException?3:2,false);

						} catch (InterruptedException  e) {
						} finally {
							releaseDeltaTree(dt);
						}
					}
				}
			}
		}
	}

}
