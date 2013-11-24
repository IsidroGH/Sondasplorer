package sondas.dispatcher.inspector;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.inspector.ITrace;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.probes.delta.MethodNode;

public interface IDeltaNodeFactory {
	public MethodNode createNode(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException;
	public MethodNode createNode(ITrace trace, StringsCache stringsCache, int metricType, int id);
	public IGlobalStats createGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException;
}
