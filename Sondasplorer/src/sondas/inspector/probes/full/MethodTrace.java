package sondas.inspector.probes.full;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.utils.IMarshable;

public class MethodTrace implements ITrace
{
	final private String fqMethod;
	final private int mode;
	
	static private String prefix=String.valueOf(InspectorRuntime.getInstance().getMetricType("MethodMetric")+":");
	static private DataOutputStream formerDos;

	/**
	 *	mode:
	 * 		1=I
	 * 		2=O
	 * 		3=E
	 */
	public MethodTrace(String fqMethod, int mode) {
		this.fqMethod=fqMethod;
		this.mode=mode;
	}
	
	public MethodTrace(MethodTrace mt) {
		this.fqMethod = mt.fqMethod;
		this.mode = mt.mode;
	}

	protected static final ThreadLocal methodsCache = new ThreadLocal() {
		protected Object initialValue()
		{
			return new StringsCache();
		}
	};
	
	public void marshall(DataOutputStream dos) throws IOException 
	{
		if (dos!=formerDos) {
			// Si el canal es diferente al ultimo borro la cache de textos
			formerDos=dos;
			StringsCache methodsCacheLocal = (StringsCache)methodsCache.get();
			methodsCacheLocal.clear();
		}
		
		dos.writeByte(mode);
		
		if (mode==Constants.Start) {
			// Solo registro el nombre del metodo si es type I (startMethod)
			StringsCache methodsCacheLocal = (StringsCache)methodsCache.get();
			Integer id = (Integer) methodsCacheLocal.methodIdRel.get(fqMethod);
			if (id==null) {
				dos.writeByte(Constants.NoCached); // No cache
				// Envio el texto del metodo y lo meto en cache
				// Length
				dos.writeShort(fqMethod.length());
				// Request
				dos.writeBytes(fqMethod);
				
				int newId = methodsCacheLocal.addString(fqMethod);
				dos.writeInt(newId);
			} else {
				// El texto esta en cache
				dos.writeByte(Constants.Cached); // Si cache
				dos.writeInt(id.intValue());
			}
		}
	}
	
	public String getKey() {
		return prefix+fqMethod;
	}
	
	public String getName() {
		return fqMethod;
	}
	
	public int getMode() {
		return mode;
	}
}

