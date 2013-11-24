package sondas.dispatcher.inspector;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import sondas.Config;
import sondas.ConversationManager;
import sondas.Metadata;
import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.IProcessor;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.Stamp;
import sondas.inspector.probes.full.MethodTrace;
import sondas.utils.SondasUtils;

/**
 * WebTrace: probeId(2)threadId(4)0(1)length(2)text(N)
 * MethodTrace: probeId(2)threadId(4)1(1)ts(8)type(1){type=1 => length(2)text(N) *
 */
public class FullModeInspectorProcessor implements IProcessor 
{
	private List workers;
	private int fullProbePid;

	public FullModeInspectorProcessor() {
		fullProbePid = InspectorRuntime.getFullProbeId();
	}

	public void setWorkers(List workers) {
		this.workers = workers;
	}

	public void execute(DataInputStream dis)
	{
		ITrace trace;
		Stamp stamp;

		try {
			int nodeId = dis.readInt();
			int tid = dis.readInt();
			long ts = dis.readLong();
			byte metricType = dis.readByte();

			trace = getTrace(metricType, dis);

			stamp = new Stamp(nodeId, tid, fullProbePid, ts, metricType, trace);
		}catch (IOException e) {
			throw new RuntimeException("Error marshalling Inspector",e);
		}

		// Ejecuto todos los workers
		for (Iterator it=workers.iterator();it.hasNext();) {
			IFullWorker worker = (IFullWorker) it.next();
			worker.execute(stamp);
		}
	}

	private ITrace getTrace(int metricType, DataInputStream dis) throws IOException {
		// Obtengo la factoria a partir de metricType
		IFactory factory = InspectorRuntime.getTraceFactory(metricType);

		// deserializo la traza y la devuelvo
		return factory.createTrace(dis);
	}
	
	public int sendCommand(String cmd) {
		return ConversationManager.Error;
	}
}

