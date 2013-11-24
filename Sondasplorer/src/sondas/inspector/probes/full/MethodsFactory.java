package sondas.inspector.probes.full;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

import sondas.dispatcher.inspector.IFactory;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.utils.SondasUtils;

public class MethodsFactory implements IFactory {
	private HashMap methodsCache;
	
	public MethodsFactory() {
		methodsCache = new HashMap();
	}
	
	public ITrace createTrace(DataInputStream dis) throws IOException {
		byte mode = dis.readByte();
		String fqMethod=null;
		
		if (mode==Constants.Start) {
			byte cached = dis.readByte();
			if (cached==Constants.NoCached) {
				// No esta cacheado
				fqMethod = SondasUtils.readString(dis);
				int newCachedId = dis.readInt();
				// Lo cacheo
				methodsCache.put(new Integer(newCachedId), fqMethod);
			} else {
				// Esta cacheado
				int cachedId = dis.readInt();
				fqMethod = (String) methodsCache.get(new Integer(cachedId));
			}
		}
		
		return new MethodTrace(fqMethod, mode);
	}
}
