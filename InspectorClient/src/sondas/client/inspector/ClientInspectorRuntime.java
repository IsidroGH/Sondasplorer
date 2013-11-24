package sondas.client.inspector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sondas.client.inspector.model.Model;
import sondas.client.inspector.view.nodes.IProbeViewer;
import sondas.client.inspector.view.nodes.MethodProbeViewer;
import sondas.dispatcher.inspector.IFactory;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.KeyValueBean;
import sondas.utils.MultipleRelation;
import sondas.utils.SondasUtils;

public class ClientInspectorRuntime extends InspectorRuntime 
{
	private static ClientInspectorRuntime instance = new ClientInspectorRuntime();
	private HashMap nodeViewers;
	
	public ClientInspectorRuntime() {
		super();
		loadNodeViewers();
	}
	
	public static ClientInspectorRuntime getInstance() {
		return instance;
	}
	
	private void loadNodeViewers() 
	{
		nodeViewers = new HashMap();
		MultipleRelation map = config.getCategory("NodeViewers");
		for (Iterator it=map.getKeys().iterator();it.hasNext();) {
			String key = (String)it.next();
			String value = (String)map.getReference(key);
			Object nodeViewer = SondasUtils.newInstance(value);
			nodeViewers.put(Integer.parseInt(key), nodeViewer);
		}
	}
	
	public IProbeViewer getNodeViewer(int nodeType) throws ClientException {
		IProbeViewer resp = (IProbeViewer)nodeViewers.get(new Integer(nodeType));
		if (resp==null) {
			throw new ClientException("Node viewer not defined for metric type: "+nodeType);
		}
		
		return resp; 
	}
}
