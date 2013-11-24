package sondas.inspector.probes;

import sondas.CurrentThread;
import sondas.CustomParam;
import sondas.inspector.Constants;
import sondas.inspector.MethodExecutionData;
import sondas.inspector.Stamp;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.delta.ServletNode;
import sondas.inspector.probes.full.MethodTrace;
import sondas.inspector.probes.full.ServletTrace;
import sondas.utils.DataSender;

//No vamos a usar la modalidad globalenabled porque está más orientada a obtener datos
//en tiempo real. Inspector es una herramienta de procesado offline: captura de aquí hasta aquí
//y muestrame los resultados.	

public class ServletProbe extends InspectorBaseProbe
{	
	public ServletProbe() 
	{
		super(false, false, false,"ServletMetric");
		
		setCustomParam(0, "javax/servlet/http/HttpServletRequest", "getMethod", true, "java/lang/String");
		setCustomParam(0, "javax/servlet/http/HttpServletRequest", "getRequestURI", true, "java/lang/String");
	}

	/**
	 * Si no esta en modo capturing exId=-1 y no se ejecutará nada para maximizar el rendimiento
	 */
	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference)
	{
		MethodExecutionData exData=null;

		if (capturing) 
		{
			exData = new MethodExecutionData();

			// HttpServletRequest request = (HttpServletRequest) params[0];
			// String servletRequest = request.getMethod()+request.getRequestURI();
			String servletRequest = (String)customParams[0]+(String)customParams[1];
			
			/*
			if (System.currentTimeMillis()%1==0) {
				servletRequest="GET/GeDocumController";
			} else {
				servletRequest="GET/GeDocumSecController";
			}*/
			
			int tid = ((CurrentThread)currentThread.get()).tid;
			
			if (fullMode) 
			{
				// Full Mode
				ServletTrace trace = new ServletTrace(servletRequest, Constants.Start);
				Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt=null;
				
				try {
					dt = getThreadDeltaTree(tid);
					
					long ts = System.currentTimeMillis();
					if (dt.hasChildNode(methodId)) {
						dt.openNode(methodId,ts);
					} else {
						ServletNode node = new ServletNode(servletRequest, metricType, methodId);
						dt.openNode(node, ts);
					}
				} catch (InterruptedException e) {
				} finally {
					releaseDeltaTree(dt);
				}
			}
		}

		return exData;		
	}

	public Object startMethodDebug(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference, long ts, int tid)
	{
		MethodExecutionData exData = null;

		if (capturing) 
		{
			exData = new MethodExecutionData();

			// HttpServletRequest request = (HttpServletRequest) params[0];
			// String servletRequest = request.getMethod()+request.getRequestURI();
			String servletRequest;
			if (System.currentTimeMillis()%1==0) {
				servletRequest="GET/GeDocumController";
			} else {
				servletRequest="GET/GeDocumSecController";
			}
			
			if (fullMode) 
			{
				// Full Mode
				ServletTrace trace = new ServletTrace(servletRequest, Constants.Start);
				Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt = null; 

				try {
					dt = getThreadDeltaTree(tid);
					
					if (dt.hasChildNode(methodId)) {
						dt.openNode(methodId,ts);
					} else {
						ServletNode node = new ServletNode(servletRequest, metricType, methodId);
						dt.openNode(node, ts);
					}
				} catch (InterruptedException e) {
				} finally {
					releaseDeltaTree(dt);
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
		endMethodInternal(nodeId, executionData, null, false);
	}
	
	public void endMethodDebug(int nodeId, Object executionData, Object response, long ts)
	{
		endMethodInternalDebug(nodeId, executionData, response, false,ts);
	}

	public void exceptionMethodDebug(int nodeId, Object executionData, long ts)
	{
		endMethodInternalDebug(nodeId, executionData, null, false,ts);
	}

	/**
	 * Si no esta en modo capturing exId=-1 y no se ejecutará nada para maximizar el rendimiento
	 */
	private void endMethodInternal(int nodeId, Object executionData, Object response, boolean withException) 
	{
		if (capturing && executionData!=null) 
		{
			long ts = System.currentTimeMillis();
			
			// Mientras no nos sea util no lo invoco:
			// MethodExecutionData exData = (MethodExecutionData) getExecutionData(exId);
			
			int tid = ((CurrentThread)currentThread.get()).tid;
			
			if (fullMode) 
			{
				// Full Mode
				Stamp stamp;

				if (withException) {
					MethodTrace trace = new MethodTrace(null, Constants.Exception);
					stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
				} else {
					MethodTrace trace = new MethodTrace(null, Constants.End);
					stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
				}

				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt = null; 
				
				try {
					dt = getThreadDeltaTree(tid); 

					dt.closeNode(nodeId, ts,withException?3:2,true);
					
				} catch (InterruptedException  e) {
				} finally {
					releaseDeltaTree(dt);
				}
			}
		}
	}
	
	private void endMethodInternalDebug(int nodeId, Object executionData, Object response, boolean withException, long ts) 
	{
		if (capturing && executionData!=null) 
		{
			// Mientras no nos sea util no lo invoco:
			// MethodExecutionData exData = (MethodExecutionData) getExecutionData(exId);
			
			int tid = ((CurrentThread)currentThread.get()).tid;
			
			if (fullMode) 
			{
				// Full Mode
				Stamp stamp;

				if (withException) {
					MethodTrace trace = new MethodTrace(null, Constants.Exception);
					stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
				} else {
					MethodTrace trace = new MethodTrace(null, Constants.End);
					stamp = new Stamp(nodeId, tid, fullProbeId, ts, metricType, trace);
				}

				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt = null; 
				
				try {
					dt = getThreadDeltaTree(tid); 

					dt.closeNode(nodeId, ts,withException?3:2,true);
					
				} catch (InterruptedException  e) {
				} finally {
					releaseDeltaTree(dt);
				}
			}
		}
	}
}
