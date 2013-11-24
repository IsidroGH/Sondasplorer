package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.inspector.ITrace;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;

public class AxisProbeFactory implements IDeltaNodeFactory
{
	public MethodNode createNode(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException
	{
		return new AxisNode(dis,stringsCache, metricType);
	}
	
	public MethodNode createNode(ITrace trace, StringsCache stringsCache, int metricType, int id) {
		return new AxisNode(trace.getName(), metricType, id);
	}
	
	public IGlobalStats createGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		return new AxisGlobalStats(dis, stringsCache, metricType);
	}
}
