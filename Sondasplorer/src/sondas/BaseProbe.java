package sondas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public abstract class BaseProbe implements IProbe
{
	private boolean traceInternalMethods;
	private boolean traceParams;
	private boolean traceResponse;
	private HashSet methodsToInstrumentalize;
	private boolean selectiveInstrumentalization;
	private List customParams;
	
	protected BaseProbe(boolean traceInternalMethods, boolean traceParams, boolean traceResponse) {
		methodsToInstrumentalize = new HashSet();
		this.traceInternalMethods=traceInternalMethods;
		this.traceParams=traceParams;
		this.traceResponse=traceResponse;
	}
	

	
	protected void setCustomParam(int paramPos, String className, String method, boolean isInterface, String returnType) {
		if (customParams==null) {
			customParams=new ArrayList();
		}
		
		CustomParam param = new CustomParam(paramPos,className,method,isInterface,returnType);
		customParams.add(param);
	}
	
	protected void setThreadObject(Object id, Object obj) {
		Map store = (Map) threadStore.get();
		store.put(id, obj);
	}
	
	protected void removeThreadObject(Object id) {
		Map store = (Map) threadStore.get();
		store.remove(id);
	}
	
	protected Object  getThreadObject(Object id) {
		Map store = (Map) threadStore.get();
		return store.get(id);
	}
	
	protected Map  getThreadStore() {
		return (Map) threadStore.get();
	}

	
	/*
	private static final ThreadLocal executionsMap = new ThreadLocal() {
        protected Object initialValue()
        {
            return new ExecutionMap();
        }
    };*/
    
    private static final ThreadLocal threadStore = new ThreadLocal() {
        protected Object initialValue()
        {
            return new HashMap();
        }
    };
	
	public boolean traceInternalMethods() {
		return traceInternalMethods;
	}
	
	public boolean traceParams() {
		return traceParams;
	}
	
	public boolean traceResponse() {
		return traceResponse;
	}
	
	/**
	 * Si es llamado solo se instrumentalizan aquellos definidos
	 * Si no es llamado, se instrumentalizan todos los metodos
	 */
	protected void setMethodToInstrumentalize(String methodName, String signature) {
		selectiveInstrumentalization = true;
		methodsToInstrumentalize.add(methodName+":"+signature);
	}
	
	/**
	 * Si es llamado solo se instrumentalizan aquellos definidos
	 * Si no es llamado, se instrumentalizan todos los metodos
	 * En este caso se instrumentalizan todos los metodos independientemente de la firma
	 */
	protected void setMethodToInstrumentalize(String methodName) {
		selectiveInstrumentalization = true;
		methodsToInstrumentalize.add(methodName);
	}
	
	public boolean hasToInstrumentalize(String methodName, String signature) {
		if (selectiveInstrumentalization) {
			if (methodsToInstrumentalize.contains(methodName)) {
				return true;
			} else {
				if (signature==null) {
					signature="";
				}
				return methodsToInstrumentalize.contains(methodName+":"+signature);		
			}
		} else {
			return true;
		}
	}
	
	protected static final ThreadLocal currentThread = new ThreadLocal() {
        protected Object initialValue()
        {
            return new CurrentThread();
        }
    };
    
    public List getCustomParams() {
		return customParams;
	}
    
    public boolean useCustomParams() {
    	return customParams!=null;
    }
	
	
	public void leaveMethod(Object executionData){} 
	
	public void resumeMethod(Object executionData){} 
	
	//public void endMethod(int executionId, Object response) {}
	
	//public void exceptionMethod(int executionId){}
}
