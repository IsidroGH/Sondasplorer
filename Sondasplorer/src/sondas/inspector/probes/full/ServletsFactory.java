package sondas.inspector.probes.full;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;

import sondas.dispatcher.inspector.IFactory;
import sondas.inspector.Constants;
import sondas.inspector.ITrace;
import sondas.utils.SondasUtils;

public class ServletsFactory implements IFactory {
	private HashMap requestsCache;
	
	public ServletsFactory() {
		requestsCache = new HashMap();
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
				requestsCache.put(new Integer(newCachedId), fqMethod);
			} else {
				// Esta cacheado
				int cachedId = dis.readInt();
				fqMethod = (String) requestsCache.get(new Integer(cachedId));
			}
		}
		
		return new ServletTrace(fqMethod, mode);
	}
}
