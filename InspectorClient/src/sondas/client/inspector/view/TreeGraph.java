package sondas.client.inspector.view;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxLayoutManager;

public class TreeGraph extends mxGraph 
{
	private mxIGraphLayout layout;
	private Map<Object, String> toolTips = new HashMap<Object, String>();
	private Container container;
	
	public TreeGraph(boolean horizontal, Container container) 
	{
		this.container=container;
		layout = new mxCompactTreeLayout(this,horizontal) {
			public int getLevelDistance() {
				return 30;
			}

			public int getNodeDistance() {
				return 10;
			}

			public boolean isUseBoundingBox() {
				return false;
			}
		};

		new mxLayoutManager(this){
			public mxIGraphLayout getLayout(Object parent)
			{
				if (graph.getModel().getChildCount(parent) > 0) {
					return TreeGraph.this.layout;
				}
				return null;
			}
		};

		setCellsSelectable(false);
	}

	public Map<Object, String> getToolTipsMap() {
		return toolTips;
	}
	
	public boolean isCellFoldable(Object cell, boolean collapse) {
		return this.getOutgoingEdges(cell).length > 0;
	}

	public Object[] foldCells(boolean collapse, boolean recurse, Object[] cells) {
		Object resp[]=null;

		this.model.beginUpdate();
		try
		{
			resp = toggleSubtree(this, cells[0], !collapse);
			this.model.setCollapsed(cells[0], collapse);			
		}
		finally
		{
			this.model.endUpdate();
		}

		// Si pongo esto empieza todo a parpadear cuando colapso un nodo y hago pequeño el panel
		//container.validate();
		return resp;
	}
	
	public Object[] foldCellsWithoutRepaint(boolean collapse, boolean recurse, Object[] cells) {
		Object resp[]=null;

		this.model.beginUpdate();
		try
		{
			resp = toggleSubtree(this, cells[0], !collapse);
			this.model.setCollapsed(cells[0], collapse);
			return resp;
		}
		finally
		{
			this.model.endUpdate();
		}
	}

	private Object[] toggleSubtree(mxGraph graph, Object cell, boolean show)
	{
		ArrayList cells = new ArrayList();

		CellVisitor cellVisitor = new CellVisitor(cells, cell, graph);

		graph.traverse(cell, true, cellVisitor);

		Object[] resp = cells.toArray();

		graph.toggleCells(show, resp, true);

		return resp;
	};

	/*
	public mxRectangle getPreferredSizeForCell(Object cell) {
		mxRectangle rect = super.getPreferredSizeForCell(cell);
		rect.setHeight(rect.getHeight()+40);
		rect.setWidth(rect.getWidth()+20);
		return rect;
	}
	*/
	
	public String getToolTipForCell(Object cell) {
		String tip = toolTips.get(cell);
		
		return tip!=null?tip:"";
	}

	class CellVisitor implements mxGraph.mxICellVisitor 
	{
		List cells;
		Object cell;
		mxGraph graph;

		public CellVisitor(List cells, Object cell, mxGraph graph) {
			this.cells=cells;
			this.cell=cell;
			this.graph = graph;
		}
		public boolean visit(Object vertex, Object edge) {
			if (vertex != cell)
			{
				cells.add(vertex);
			}

			// Stops recursion if a collapsed cell is seen
			return vertex == cell || !graph.isCellCollapsed(vertex);
		}
	}
}


