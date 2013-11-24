package sondas.client.inspector.view;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import sondas.client.inspector.ClientException;
import sondas.client.inspector.ClientInspectorRuntime;
import sondas.client.inspector.view.nodes.MethodProbeViewer;
import sondas.inspector.probes.delta.MethodNode;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraphView;

public class TreePanel extends JPanel 
{
	private TreeGraph graph;
	private Object graphParent;
	private HashMap nodeVertexs;
	private DecimalFormat dec;
	private int TimeThreshold=100;
	private HashMap globalStats;
	private HashMap styles;
	private Map<Object, String> toolTips;
	private ArrayList foldedNodes;
	private int foldTH; // Fold threshold (Total Time)
	private Container container;
	
	public TreePanel(Container container) {
		nodeVertexs = new HashMap();
		globalStats = new HashMap();
		styles = new HashMap();
		foldedNodes = new ArrayList();
		this.container=container;

		//setPreferredSize(new Dimension(640,400));

		// Estilos
		styles.put("#1", "fillColor=#EE4444;fontColor=black");

		dec = new DecimalFormat();
		dec.setMinimumFractionDigits(2);
		dec.setMaximumFractionDigits(2);

		foldTH=200;
	}

	public void generateTree(Set<MethodNode> nodes) {
		removeAll();

		graph = new TreeGraph(true, container);
		
		
		toolTips = graph.getToolTipsMap();

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

		drawGraph(nodes);
	}

	private void drawGraph(Set<MethodNode> nodes) 
	{
		graph.setAutoSizeCells(true);
		graph.getModel().beginUpdate();

		try
		{
			Object agentNode = graph.insertVertex(graphParent, null, "Agent", 1, 1, 0, 0, "fillColor=#00EE22;fontColor=black"); 
			graph.updateCellSize(agentNode);
			
			for (MethodNode node:nodes) {
				Object entryNode = drawNode(node);
				graph.insertEdge(graphParent, null, null, agentNode, entryNode);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			graph.getModel().endUpdate();
		}

		// Colapso todos los nodes folded
		if (foldedNodes.size()>0)
			graph.foldCellsWithoutRepaint(true, true, foldedNodes.toArray());

		mxIGraphLayout layout = new mxCompactTreeLayout(graph, true);
		layout.execute(graphParent);

		final mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.setConnectable(false);
		graphComponent.setBorder(null);
		graphComponent.setToolTips(true);
		graphComponent.setDragEnabled(false);
		graphComponent.getViewport().setOpaque(false);
		graphComponent.setBackground(Color.white);
		
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

		setLayout(new GridLayout());
		add(graphComponent);
	}

	private Object drawNode(MethodNode node) throws IOException, ClientException 
	{
		boolean folded = node.getAvgTotTime()<200;

		MethodProbeViewer nodeViewer = (MethodProbeViewer) ClientInspectorRuntime.getInstance().getNodeViewer(node.getMetricType());
		String text = nodeViewer.getView(node);
		String toolTip = nodeViewer.getToolTip(node);
		
		// Style
		String style=null;
		if (node.getAvgExTime()>100) {
			// Rojo
			style = (String)styles.get("#1");
		} 

		Object nodeVertex;
		if (style!=null) {
			nodeVertex = graph.insertVertex(graphParent, null, text, 1, 1, 0, 0, style); 
		} else {
			nodeVertex = graph.insertVertex(graphParent, null, text, 1, 1, 0, 0); 
		}
		
		if (toolTip!=null) {
			toolTips.put(nodeVertex,toolTip);
		}			

		if (folded) {
			foldedNodes.add(nodeVertex);
		}

		graph.updateCellSize(nodeVertex);

		int childrenCount = node.getChildrenCount();

		// Dibujo los hijos
		for (MethodNode child:node.getChildren()) {
			Object childVertex = drawNode(child);

			// Creo el edge
			graph.insertEdge(graphParent, null, null, nodeVertex, childVertex);
		}

		return nodeVertex;
	}

}



