package sondas;

class NodeProbeBean {
	String probeName;
	String node;
	IProbe probe;
	int nodeId;
	
	public NodeProbeBean(String probeName, String node, int nodeId, IProbe probe) {
		this.probeName = probeName;
		this.node = node;
		this.probe=probe;
		this.nodeId=nodeId;
	}
}
