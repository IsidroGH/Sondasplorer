package sondas.inspector.probes.full;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.utils.IMarshable;

public class ServletTrace implements ITrace
{
	final private String request;
	final private int mode;
	
	static private String prefix; 
	static private DataOutputStream formerDos;

	static {
		prefix = String.valueOf(InspectorRuntime.getInstance().getMetricType("ServletMetric")+":");
	}
	
	/**
	 *	mode:
	 * 		1=I
	 * 		2=O
	 * 		3=E
	 */
	public ServletTrace(String request, int mode) {
		this.request=request;
		this.mode=mode;
	}

	private static final ThreadLocal requestsCache = new ThreadLocal() {
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
			StringsCache requestsCacheLocal = (StringsCache)requestsCache.get();
			requestsCacheLocal.clear();
		}
		
		dos.write(mode);
		
		if (mode==Constants.Start) {
			// Solo registro el nombre del metodo si es type I (startMethod)
			StringsCache requestsCacheLocal = (StringsCache)requestsCache.get();
			Integer id = (Integer) requestsCacheLocal.methodIdRel.get(request);
			if (id==null) {
				dos.writeByte(Constants.NoCached); // No cache
				// Envio el texto del metodo y lo meto en cache
				// Length
				dos.writeShort(request.length());
				// Request
				dos.writeBytes(request);
				
				int newId = requestsCacheLocal.addString(request);
				dos.writeInt(newId);
			} else {
				// El texto esta en cache
				dos.writeByte(Constants.Cached); // Si cache
				dos.writeInt(id.intValue());
			}
		}
	}
	
	public String getKey() {
		return prefix+request;
	}
	
	public String getName() {
		return request;
	}
	
	public int getMode() {
		return mode;
	}
}


