package sondas.client.inspector.view.nodes;

import sondas.inspector.probes.delta.MethodNode;

public class ServletProbeViewer extends MethodProbeViewer {
	protected void addTypeName(MethodNode node, StringBuilder sb) {
		sb.append("Type: Servlet\n");
	}
	
	protected void addTier(StringBuilder sb) {
		sb.append("Tier: Application");
	}
	
	public boolean isTimeStatsVisible() {
		return true;
	}
}
