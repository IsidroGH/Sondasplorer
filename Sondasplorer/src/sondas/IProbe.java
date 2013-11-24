package sondas;

import java.util.List;

public interface IProbe {
	public Object startMethod(int nodeId, String className, String methodName, String methodSignature, int methodId, Object params[], Object customParams[], Object thisReference);
	public void endMethod(int nodeId, Object executionData, Object response); 
	public void leaveMethod(Object executionData); 
	public void resumeMethod(Object executionData); 
	public void exceptionMethod(int nodeId, Object executionData);
	
	/**
	 * Indica si se deben incluir sondas antes y despues de las llamadas métodos desde el método a instrumentalizar
	 * En la practica se corresponde a ejecutar los eventos leaveMethod y resumeMethod antes y despues de cada 
	 * metodo interno 
	 */
	public boolean traceInternalMethods();
	public boolean traceParams();
	public boolean traceResponse();
	
	public boolean hasToInstrumentalize(String methodName, String signature);
	
	/**
	 * Metodo llamado por el framework justo se ha creado el objeto
	 * Se utilizara por ejemplo para añadir la sonda a un observer ya que
	 * no puedo hacerlo desde el constructor porque se escaparía el this
	 */
	public void init();
	
	public List getCustomParams();
    public boolean useCustomParams();
}
