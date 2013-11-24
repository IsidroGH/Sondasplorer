package sondas.client.inspector.view;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;

import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.IRemote;
import sondas.inspector.probes.delta.MethodNode;

class RemoteElement {
	String text;
	String toolTip;

	public RemoteElement(String text, String toolTip) {
		this.text=text;
		this.toolTip=toolTip;
	}
}

public class MapPanel extends JPanel 
{
	private mxGraph graph;
	private Container container;
	private HashMap styles;
	private Map<Object, String> toolTips;
	private Object graphParent;

	public MapPanel(Container container) {
		this.container=container;
		styles = new HashMap();
		styles.put("#1", "fillColor=#EE4444;fontColor=black");
	}

	public void generateMap(Collection<IGlobalStats> gss) {
		removeAll();

		graph = new mxGraph();

		// Configuro el estilo
		Map<String, Object> style = graph.getStylesheet().getDefaultEdgeStyle();
		style.put(mxConstants.STYLE_EDGE, mxEdgeStyle.SideToSide);
		style.put(mxConstants.STYLE_ROUNDED, Boolean.TRUE);

		style = graph.getStylesheet().getDefaultVertexStyle();

		//style.put(mxConstants.STYLE_SHAPE,"treenode");
		style.put(mxConstants.STYLE_GRADIENTCOLOR,"white");
		style.put(mxConstants.STYLE_SHADOW, true);
		style.put(mxConstants.STYLE_SPACING, 10);

		graphParent = graph.getDefaultParent();

		drawMap(gss);
	}

	private void drawElements(Collection<IGlobalStats> gss) {
		Object thisAgent = graph.insertVertex(graphParent, null, "Agent", 1, 1,0, 0, "fillColor=yellow");
		graph.updateCellSize(thisAgent);

		// gs contiene normalmente listas con Urls. 
		// El procedimiento es obtener nodos asociados a las distintas maquinas donde en cada nodo
		// ira incluida la distnta informacion de los disntos tipos de nodos
		List<RemoteElement> remoteElements = getRemoteElements(gss);
		for (RemoteElement remoteElement:remoteElements) {
			Object element = graph.insertVertex(graphParent, null, remoteElement.text, 1, 1,0, 0, "");
			graph.insertEdge(graphParent, null, null, thisAgent, element);
			graph.updateCellSize(element);
		}
	}

	/**
	 * El procedimiento es obtener nodos asociados a las distintas maquinas donde en cada nodo
	 * ira incluida la distnta informacion de los disntos tipos de nodos
	 */
	private List<RemoteElement> getRemoteElements(Collection<IGlobalStats> gss) {
		ArrayList<RemoteElement> resp = new ArrayList<RemoteElement>();
		
		for (IGlobalStats gs:gss) {
			if (gs instanceof IRemote) {
				IRemote remote = (IRemote) gs;
				// TODO xxx
			}
		}
		
		return resp;
	}

	private void drawMap(Collection<IGlobalStats> gss) 
	{
		graph.setAutoSizeCells(true);
		graph.getModel().beginUpdate();

		try
		{  
			drawElements(gss);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			graph.getModel().endUpdate();
		}

		mxIGraphLayout layout = new mxCircleLayout(graph);
		layout.execute(graphParent);

		final mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.setConnectable(false);
		graphComponent.setBorder(null);
		graphComponent.setToolTips(true);
		graphComponent.setDragEnabled(false);

		graphComponent.addMouseWheelListener(new MouseWheelListener() 
		{
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getWheelRotation() < 0) {
					graphComponent.zoomIn();
				}
				else {
					graphComponent.zoomOut();
				}
			}
		});

		add(graphComponent);
	}
}
