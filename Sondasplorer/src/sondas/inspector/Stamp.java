package sondas.inspector;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.utils.IMarshable;

public class Stamp implements IMarshable {
	private int tid;
	private long ts;
	private int metricType;
	private ITrace trace;
	private int probeId;
	private int nodeId;
	
	public Stamp (int nodeId, int tid, int probeId, long ts, int metricType, ITrace data) {
		this.tid=tid;
		this.ts=ts;
		this.metricType=metricType;
		this.trace=data;
		this.probeId=probeId;
		this.nodeId=nodeId;
	}
	
	public void marshall(DataOutputStream dos) throws IOException {
		dos.writeShort(probeId);
		dos.writeInt(nodeId);
		dos.writeInt(tid);
		dos.writeLong(ts);
		dos.writeByte(metricType);
		trace.marshall(dos);
	}

	public ITrace getTrace() {
		return trace;
	}
	
	public int getNodeId() {
		return nodeId;
	}

	public int getMetricType() {
		return metricType;
	}

	public int getProbeId() {
		return probeId;
	}

	public int getTid() {
		return tid;
	}

	public long getTs() {
		return ts;
	}
	
}
