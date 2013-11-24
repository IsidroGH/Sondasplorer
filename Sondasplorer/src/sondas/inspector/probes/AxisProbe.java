package sondas.inspector.probes;

import java.net.URL;
import java.util.HashMap;
import java.util.WeakHashMap;
import sondas.BaseExecutionData;
import sondas.CurrentThread;
import sondas.SondasLoader;
import sondas.inspector.Constants;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.delta.AxisNode;
import sondas.inspector.probes.delta.MethodNode;
import sondas.inspector.probes.delta.SqlNode;
import sondas.utils.ConcurrentHashMap;
import sondas.utils.DataSender;

class AxisExData extends BaseExecutionData 
{
	public boolean isInvocation;

	public AxisExData(boolean isInvocation) {
		this.isInvocation = isInvocation;
	}
}

/*
 * Instrumentalizo el metodo finalize de forma que cuando es llamado
 * se elimina el objeto Call de la cache
 */
public class AxisProbe extends InspectorBaseProbe 
{
	final public static int AxisId = SondasLoader.getNewId(); 
	
	private static WeakHashMap callEndPointRel = new WeakHashMap(); 

	public AxisProbe() 
	{
		super(false, true, false,"AxisMetric");
		setMethodToInstrumentalize("setTargetEndpointAddress","Ljava/net/URL;");
		setMethodToInstrumentalize("invoke","");
		setMethodToInstrumentalize("finalize","");
	}

	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference)
	{
		AxisExData exData = null;

		if (methodName.equals("setTargetEndpointAddress")) {
			// Asocio el objeto Call con este endpoint
			String url = getCanonicalUrl((URL)params[0]);
			
			synchronized(callEndPointRel) {
				callEndPointRel.put(thisReference, url);
			}
		} else if (methodName.equals("invoke")) {
			int tid = ((CurrentThread)currentThread.get()).tid;

			DeltaTree dt = null;
			exData = new AxisExData(true);

			// Obtengo el endpoint de la invocacion
			String endPoint; 
			synchronized (thisReference) {
				endPoint = (String)callEndPointRel.remove(thisReference);
			}

			try {
				
				dt = getThreadDeltaTree(tid);
				// Method
				if (dt.hasChildNode(AxisId)) {
					long ts = System.currentTimeMillis();
					AxisNode node = (AxisNode) dt.openNode(AxisId,ts);
					node.setCurrentEndPoint(endPoint);
				} else {
					AxisNode node = new AxisNode("axis_call", metricType, AxisId);
					long ts = System.currentTimeMillis();
					dt.openNode(node, ts);
					node.setCurrentEndPoint(endPoint);
				}

			} catch (InterruptedException e) {
			} finally {
				releaseDeltaTree(dt);
			}
		} else if (methodName.equals("finalize")) {
			synchronized (thisReference) {
				callEndPointRel.remove(this);
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

	private void endMethodInternal(int nodeId, Object executionData, Object response, boolean withException)
	{
		if (capturing){
			if (executionData!=null)
			{ 
				long ts = System.currentTimeMillis();
				AxisExData exData = (AxisExData) executionData;

				if (exData.isInvocation) {
					// Invocation

					int tid = ((CurrentThread)currentThread.get()).tid;

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

	/**
	 * Del una Url devuelve un String con el protocolo+host+puerto+path+query
	 * Si no tiene puerto no lo incluye.
	 */
	private String getCanonicalUrl(URL url) {
		StringBuilder sb = new StringBuilder();
		sb.append(url.getProtocol()).append("://").append(url.getHost());
		
		if (url.getPort()!=-1) {
			sb.append(":").append(url.getPort());
		}
		
		if (url.getPath()!=null && !url.getPath().equals("")) {
			sb.append(url.getPath());
		}
		
		if (url.getQuery()!=null && !url.getQuery().equals("")) {
			sb.append("?").append(url.getQuery());
		}
		
		return sb.toString();
	}
}
