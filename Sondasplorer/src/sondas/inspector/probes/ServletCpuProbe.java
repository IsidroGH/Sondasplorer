package sondas.inspector.probes;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import sondas.BaseExecutionData;
import sondas.CurrentThread;
import sondas.CustomParam;
import sondas.Global;
import sondas.SondasLoader;
import sondas.inspector.Constants;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.delta.CpuGlobalStats;
import sondas.inspector.probes.delta.MethodNode;
import sondas.inspector.probes.delta.ServletNode;
import sondas.utils.DataSender;

class ExecutionData extends BaseExecutionData 
{
	long iniTs;
	long iniCpu;
	String request;
	long threadId;
}

public class ServletCpuProbe extends InspectorBaseProbe
{	
	final public static int CpuId = SondasLoader.getNewId(); 
	
	final private int MaxParams=5;
	private boolean enabled;
	private ThreadMXBean mxBean;
	private int paramsCount;
	private String[] paramsList;
	
	public ServletCpuProbe() 
	{
		super(false, false, false,"ServletCpuMetric");
		
		setCustomParam(0, "javax/servlet/http/HttpServletRequest", "getMethod", true, "java/lang/String");
		setCustomParam(0, "javax/servlet/http/HttpServletRequest", "getRequestURI", true, "java/lang/String");
		
		paramsList = new String[MaxParams];
		if (InspectorRuntime.hasProperty("ServletCpuProbe", "parameters")) {
			String params = InspectorRuntime.getProperty("ServletCpuProbe", "parameters");
			StringTokenizer st = new StringTokenizer(params,",");
			for (int i=0;i<MaxParams;i++) {
				if (st.hasMoreTokens()) {
					String param = st.nextToken();
					paramsList[paramsCount++]=param;
				} else {
					break;
				}
			}
		}
		
		mxBean = ManagementFactory.getThreadMXBean();
		
		boolean timeMonitoring = mxBean.isThreadCpuTimeSupported();
		if (timeMonitoring) {
			enabled=true;
		} else {
			System.err.println("ServletCpuProbe not supported by JVM");
			enabled=false;
		}
		
		System.err.println("ServletCPUProbe en modo Debug");
	}

	/**
	 * Si no esta en modo capturing exId=-1 y no se ejecutará nada para maximizar el rendimiento
	 */
	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference)
	{
		ExecutionData exData = null;

		if (enabled && capturing && !fullMode) 
		{
			exData = new ExecutionData();
			
			/*
			StringBuilder servletRequest = new StringBuilder();
			HttpServletRequest request = (HttpServletRequest) params[0];
			servletRequest.append(request.getRequestURI());
			
			for (int i=0;i<paramsCount;i++) {
				servletRequest.append(".");
				servletRequest.append(request.getParameter(paramsList[i]));
			}
						
			// Debug
			
			if (System.currentTimeMillis()%1==0) {
				servletRequest.append("GeDocumController.execute");
			} else {
				servletRequest.append("GeDocumSecController.execute");
			}*/
			
			String servletRequest = (String)customParams[0]+(String)customParams[1];
			
			exData.request=servletRequest.toString();
			long threadId = Thread.currentThread().getId();
			exData.threadId = threadId;
			
			exData.iniTs=System.currentTimeMillis();
			exData.iniCpu= mxBean.getThreadCpuTime(threadId);
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

	/**
	 * Si no esta en modo capturing exId=-1 y no se ejecutará nada para maximizar el rendimiento
	 */
	private void endMethodInternal(int nodeId, Object executionData, Object response, boolean withException) 
	{
		if (enabled && capturing && executionData!=null) 
		{
			ExecutionData exData = (ExecutionData) executionData;
			
			long endCpu = mxBean.getThreadCpuTime(exData.threadId);
			long endTs = System.currentTimeMillis();
			
			int tid = ((CurrentThread)currentThread.get()).tid;
			
			// Delta Mode
			DeltaTree dt = null;
			
			
			try {
				dt = getThreadDeltaTree(tid); 
				
				CpuGlobalStats gs = (CpuGlobalStats) dt.getGlobalStats(CpuId);
				if (gs==null) {
					gs = new CpuGlobalStats(CpuId, metricType, nodeId);
					dt.addGlobalStats(CpuId, gs);
				}
				gs.updateCpu(exData.request, exData.iniCpu, exData.iniTs, endCpu, endTs);
			} catch (InterruptedException  e) {
			} finally {
				releaseDeltaTree(dt);
			}

		}
		
	}
}

