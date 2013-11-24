package sondas.dispatcher.inspector;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sondas.ConversationManager;
import sondas.Metadata;
import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.AgentClientConnection;
import sondas.dispatcher.IProcessor;
import sondas.dispatcher.RespBean;
import sondas.inspector.ITrace;
import sondas.inspector.InspectorPacket;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.Stamp;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.DeltaTree;

public class DeltaModeInspectorProcessor implements IProcessor, IMessagesReceiver
{
	private List workers;
	private int deltaProbePid;
	private StringsCache stringsCache;
	private LinkedBlockingQueue packetsQueue;

	public DeltaModeInspectorProcessor() throws IOException {
		deltaProbePid = InspectorRuntime.getFullProbeId();
		stringsCache = new StringsCache();
		packetsQueue = new LinkedBlockingQueue();
		CapturesServer capServer = new CapturesServer(); 
		capServer.start();

		// Consumidor de DT
		/*
		new Thread(){
			public void run() 
			{
				long currentTs=-1;
				DeltaTree currentDt=null;

				while(true) {
					try {
						Thread.sleep(5000);
						ArrayList trees = (ArrayList) treesQueue.take();

						for (Iterator treeIt=trees.iterator();treeIt.hasNext();) 
						{
							DeltaTree partialDt=(DeltaTree) treeIt.next();
							long partialTs = partialDt.getTimeStamp();

							if (partialTs!=currentTs) {

								if (currentTs!=-1) {
									// Envio porque ha llegado un ts nuevo que no es el primero
									for (Iterator it=workers.iterator();it.hasNext();) {
										IDeltaWorker worker = (IDeltaWorker) it.next();
										worker.execute(currentDt);
									}
								} 

								currentDt=partialDt;
								currentTs=partialTs;
							} else {
								currentDt.merge(partialDt);
							}
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}.start();*/

		new Thread(){
			public void run() 
			{

				while(true) {
					try {
						// NUEVO TODO: el take debe devolver un bean con los trees y la metadata

						InspectorPacket packet = (InspectorPacket)packetsQueue.take();

						for (Iterator it=workers.iterator();it.hasNext();) 
						{
							IDeltaWorker worker = (IDeltaWorker) it.next();

							if (packet.metadata!=null) {
								worker.executeMetadata(packet.metadata);
							}
							// Se trata de lista de trees
							System.out.println("Dispatcher trees size "+packet.treesList.size());

							for (Iterator treeIt=packet.treesList.iterator();treeIt.hasNext();) 
							{
								DeltaTree currentDt=(DeltaTree) treeIt.next();
								worker.execute(currentDt);
							}
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		}.start();
	}

	public void setWorkers(List workers) {
		this.workers = workers;
	}

	public void execute(DataInputStream dis)
	{
		InspectorPacket packet;

		try {
			//currentDt = new DeltaTree(dis,stringsCache);

			ObjectInputStream ois = new ObjectInputStream(dis);

			packet = (InspectorPacket) ois.readObject();
			packetsQueue.add(packet);

			/*
			// Ejecuto todos los workers
			for (Iterator treeIt=trees.iterator();treeIt.hasNext();) 
			{
				DeltaTree currentDt=(DeltaTree) treeIt.next();
				for (Iterator it=workers.iterator();it.hasNext();) {
					IDeltaWorker worker = (IDeltaWorker) it.next();
					worker.execute(currentDt);
				}
			}
			 */
		} catch (Throwable e) {
			throw new RuntimeException("Error unmarshalling data",e);
		}
	}

	/**
	 * Devuelve error si algun worker falla.
	 * Solo devuelve el mensaje de error del primer worker que ha fallado.
	 * 
	 * Si va bien, devuelve en msg las respuestas de todos los workers concatenadas
	 */
	public RespBean sendCommand(String cmd, BufferedWriter outText, OutputStream outBin) {
		RespBean resp = new RespBean();
		resp.code=ConversationManager.Ok;

		StringBuilder sb = new StringBuilder();

		for (int i=0;i<workers.size();i++) {
			IMessagesReceiver worker = (IMessagesReceiver) workers.get(i);
			RespBean aux = worker.sendCommand(cmd, outText, outBin);
			if (aux.code==ConversationManager.Error) {
				resp.code=ConversationManager.Error;
				resp.msg=aux.msg;
				return resp;
			}

			if (aux.code==ConversationManager.Ok) {
				sb.append(aux.msg).append(",");
			}
		}

		resp.msg=sb.toString();

		return resp;
	}
}
