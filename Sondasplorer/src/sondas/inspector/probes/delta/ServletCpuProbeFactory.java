package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.inspector.ITrace;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;

public class ServletCpuProbeFactory implements IDeltaNodeFactory
{
	public MethodNode createNode(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException
	{
		return null; 
	}
	
	public MethodNode createNode(ITrace trace, StringsCache stringsCache, int metricType, int id) {
		return null;
	}
	
	public IGlobalStats createGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException {
		return new CpuGlobalStats(dis, stringsCache, metricType);
	}
}
