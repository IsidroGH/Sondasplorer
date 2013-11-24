package sondas.inspector.probes;

import java.util.HashSet;
import java.util.List;

import sondas.CurrentThread;
import sondas.CustomParam;
import sondas.inspector.Constants;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.MethodExecutionData;
import sondas.inspector.Stamp;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.probes.delta.MethodNode;
import sondas.inspector.probes.full.MethodTrace;
import sondas.utils.DataSender;
import sondas.utils.SondasUtils;

//No vamos a usar la modalidad globalenabled porque está más orientada a obtener datos
//en tiempo real. Inspector es una herramienta de procesado offline: captura de aquí hasta aquí
//y muestrame los resultados.	

public class MethodProbe extends InspectorBaseProbe
{
	HashSet entryPoints;
	
	public MethodProbe() 
	{
		super(false, false, false,"MethodMetric");
		/*
		setCustomParam(0, "test/draft/TestBean", "getText", false, "java/lang/String"); 
		setCustomParam(0, "test/draft/TestBean", "getText", false, "java/lang/String");
		*/ 
		
		entryPoints = new HashSet();
		if (InspectorRuntime.hasProperty("MethodProbe", "EntryPoint")) {
			List eps = InspectorRuntime.getMultipleProperty("MethodProbe", "EntryPoint");
			for (int i=0;i<eps.size();i++) {
				entryPoints.add(eps.get(i));
			}
		}
		
	}

	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference)
	{
		/*
		String cp;
		if (customParams!=null) {
			cp = (String)customParams[0]+","+(String)customParams[1];
		} else {
			cp ="NO";
		}
		System.out.println("MethodProbe: "+cp);
		*/
		
		MethodExecutionData exData = null;
		if (capturing) 
		{
			exData = new MethodExecutionData();
			
			//String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
			int tid = ((CurrentThread)currentThread.get()).tid;

			if (fullMode) 
			{
				// Full Mode
				String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
				MethodTrace trace = new MethodTrace(fqMethod, Constants.Start);
				Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt = null;

				// Compruebo si es un entrypoint
				boolean isEntryPoint =  entryPoints.contains(className+"."+methodName+"#"+methodSignature);
				exData.isEntryPoint=isEntryPoint;
				
				try {
					dt = getThreadDeltaTree(tid); 
					
					// Method
					long ts = System.currentTimeMillis();
					if (dt.hasChildNode(methodId)) {
						dt.openNode(methodId,ts);
					} else {
						MethodNode node = new MethodNode("", metricType, isEntryPoint, methodId);
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
		MethodExecutionData exData=null;
		
		if (capturing) 
		{
			exData = new MethodExecutionData();
			
			String fqMethod = SondasUtils.getFqMethod(className, methodName, methodSignature);
			
			if (fullMode) 
			{
				// Full Mode
				MethodTrace trace = new MethodTrace(fqMethod, Constants.Start);
				Stamp stamp = new Stamp(nodeId, tid, fullProbeId, System.currentTimeMillis(), metricType, trace);
				DataSender.sendBuffered(stamp);
			} else {
				// Delta Mode
				DeltaTree dt = null; 

				try {
					dt = getThreadDeltaTree(tid); 
					 
					// Method
					if (dt.hasChildNode(methodId)) {
						dt.openNode(methodId,ts);
					} else {
						MethodNode node = new MethodNode(fqMethod, metricType, methodId);
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
	
	public void endMethodDebug(int nodeId, Object executionData, Object response,long ts)
	{
		endMethodInternalDebug(nodeId, executionData, response, false, ts);
	}

	public void exceptionMethodDebug(int nodeId, Object executionData,long ts)
	{
		endMethodInternalDebug(nodeId, executionData, null, false, ts);
	}

	private void endMethodInternal(int nodeId, Object executionData, Object response, boolean withException) 
	{
		if (capturing && executionData!=null) 
		{
			long ts = System.currentTimeMillis();

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

					MethodExecutionData exData = (MethodExecutionData) executionData;
					dt.closeNode(nodeId, ts,withException?3:2,exData.isEntryPoint);
					
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
			// MethodExecutionData exData = (MethodExecutionData) getExecutionData(executionId);

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

					dt.closeNode(nodeId, ts,withException?3:2,false);
					
				} catch (InterruptedException  e) {
				} finally {
					releaseDeltaTree(dt);
				}
			}
		}
	}
}
