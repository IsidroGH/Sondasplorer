package sondas;

public class NodeIndexBean {
	int index;
	int nodeId;
	IProbe probe;
	
	public NodeIndexBean (int nodeId, Integer index, IProbe probe) {
		this.index = index.intValue();
		this.nodeId = nodeId;
		this.probe=probe;
	}
}
