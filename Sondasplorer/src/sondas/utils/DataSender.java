package sondas.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import sondas.Global;
import sondas.MPCommon;
import sondas.MessagesProcessor;
import sondas.Metadata;
import sondas.SondasLoader;
import sondas.inspector.InspectorRuntime;

public class DataSender extends Thread
{
	final private static int ListenerThreadAction = 1;
	final private static int DataPumperAction = 2;

	static private ServerSocket serverSocket;
	static private Socket clientSocket;
	static private DataOutputStream dos;
	private static int interval;
	private static int bufferSize;
	volatile private static boolean active;
	volatile private static boolean shutdown;
	static private Thread listenerThread;
	static private Thread pumperThread;
	static private DataOutputStream debugFileOut;
	public static Mutex mutex = new Mutex();
	private int threadAction;

	private static LinkedList listOfPacketsToSend = new LinkedList();
	private static LinkedList listOfPidsToSend = new LinkedList();

	private DataSender(int threadAction) {
		this.threadAction=threadAction;
	}

	static {
		/*
		// TODO esto se usaba antes para full trace. Estudiar si sigue siendo necesario
		interval = Global.getPropertyAsInteger("DataSender", "datasender.interval");
		bufferSize = Global.getPropertyAsInteger("DataSender", "datasender.buffer.size");
		*/

		listenerThread = new DataSender(ListenerThreadAction);
		pumperThread = new DataSender(DataPumperAction);

		// Debug
		try {
			//debugFileOut =  new DataOutputStream(new FileOutputStream("C:/Users/Isidro/Documents/test1.ins"));
			//SondasUtils.writeString("SondasPlorer 0.1",debugFileOut);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void startUp() {
		listenerThread.setDaemon(true);
		listenerThread.start();
		pumperThread.setDaemon(true);
		pumperThread.start();
	}

	private static final ThreadLocal dataSenderBean = new ThreadLocal() {
		protected Object initialValue()
		{
			return new DataSenderBean(interval);
		}
	};

	private static final ThreadLocal signaledBuffer = new ThreadLocal() {
		protected Object initialValue()
		{
			SignaledBuffer localSignaledBuffer = new SignaledBuffer();

			// Envio el localSignaledBuffer al MessagesProcesor (Observer pattern)
			((MPCommon)(MessagesProcessor.getBaseModule())).registerSignaledBuffer(localSignaledBuffer);

			return localSignaledBuffer;
		}
	};

	static public boolean sendPeriodic(IMarshable data) {
		if (active) {
			long ts = System.currentTimeMillis();

			DataSenderBean dsb = (DataSenderBean)dataSenderBean.get();

			if (ts>=dsb.deadline) {
				dsb.updateDeadline(ts);

				// Envio informacion
				synchronized (dos) {
					try {
						data.marshall(dos);
						dos.flush();
					} catch (IOException e) {
						System.out.println("DataSender: Error sending");
						e.printStackTrace();   
						active=false;
					}
				}

				return true;
			}
		}

		return false;
	}

	static public void sendImmediate(IMarshable data) throws IOException {
		if (active) {
			// Envio informacion
			synchronized (dos) {
				try {
					data.marshall(dos);
					dos.flush();
				} catch (IOException e) {
					active=false;
					closeChannel();
					throw e;
				}

			}
		}
	}

	static public void flushChannel() {
		try {
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	/*
	static public void sendImmediate(int pid, IDeltaMarshable data) throws IOException {
		// Debug
		if (active) {
			// Envio informacion
			synchronized (dos) {
				try {
					long a = System.currentTimeMillis();

					dos.writeShort(pid);
					ObjectOutputStream oos = new ObjectOutputStream(dos);
					oos.writeObject(data);

					System.out.println("DT sent time: "+ (System.currentTimeMillis()-a));

					dos.flush();
				} catch (IOException e) {
					active=false;
					closeChannel();
					throw e;
				}

			}
		}
	}
	*/

	/**
	 * Envia una lista de arboles. Normalmente estarán asociados a un mismo timestamp
	 */
	static public void sendImmediate(int pid, Object data) throws IOException {
		if (active) {
			// Envio informacion
			synchronized (dos) {
				try {
					dos.writeShort(pid);
					ObjectOutputStream oos = new ObjectOutputStream(dos);
					oos.writeObject(data);

					dos.flush();
				} catch (IOException e) {
					active=false;
					closeChannel();
					throw e;
				}

			}
		}

	}

	static public void sendBuffer(List buffer) {
		// Envio informacion
		synchronized (dos) {
			try {
				for (int i=0;i<buffer.size();i++) {
					IMarshable item = (IMarshable) buffer.get(i);
					item.marshall(dos);
				}
				dos.flush();
			} catch (IOException e) {
				System.out.println("DataSender: Error sending");
				e.printStackTrace();    					
				active=false;
			}
		}
	}

	static public void sendBuffered(IMarshable data) 
	{
		// Debug
		if (1==1) {
			//System.out.println("sendBuffered debug on");
			try {
				synchronized(debugFileOut) {
					data.marshall(debugFileOut);
					debugFileOut.flush();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ;
		}

		if (!shutdown) {
			SignaledBuffer signaledBufferLocal = (SignaledBuffer) signaledBuffer.get();
			signaledBufferLocal.sendRequest=true;

			if (active) {
				List localBuffer = (List) signaledBufferLocal.buffer;
				localBuffer.add(data);
				if (localBuffer.size()==bufferSize) {
					// Envio informacion
					synchronized (dos) {
						try {
							for (int i=0;i<bufferSize;i++) {
								IMarshable item = (IMarshable) localBuffer.get(i);
								item.marshall(dos);
							}
							dos.flush();
						} catch (IOException e) {
							System.out.println("DataSender: Error sending");
							e.printStackTrace();    					
							active=false;
						}
					}

					// Limpio el buffer
					localBuffer.clear();
				}

			}

			signaledBufferLocal.sendRequest=false;
		}
	}

	/**
	 * Indica que se quiere hacer shutdown. Permite que las sondas que estén enviando datos terminen
	 * pero nuevos datos no son enviados
	 */
	static public void signalShutdown() {
		shutdown=true;
		active=false;
		try {
			serverSocket.close();
		} catch (IOException e) {}
	}

	/**
	 * Cierra el canal hacia el cliente
	 */
	static public void closeChannel() {
		if (clientSocket!=null) {
			dos=null;
			try {clientSocket.close();} catch (IOException ex){}
		}
	}


	private void listenerThread() {
		int port = Global.getPropertyAsInteger("DataSender","datasender.port");

		while (!shutdown) {
			try {
				serverSocket = new ServerSocket(port);
			} catch (IOException e) {
				System.out.println("DataSender: Could not listen on port "+port+ " ("+e.getMessage()+")");
				return;
			}

			System.out.println("DataSender listening on port "+port);

			while(!shutdown) 
			{
				try {
					Socket _clientSocket = serverSocket.accept();

					if (clientSocket!=null && !clientSocket.isClosed()) {

						try {
							InputStream is = clientSocket.getInputStream();
							is.read();
							// Rechazo la nueva peticion de conexion
							System.err.println("DataSender currently connected. Rejecting new connection");
							try {_clientSocket.close();} catch (IOException e) {}
							continue;
						} catch (SocketException e) {
							System.err.println("Connection closed by peer");
							try {clientSocket.close();} catch (IOException e2) {}
						}

					} 

					clientSocket=_clientSocket;
					dos = new DataOutputStream(clientSocket.getOutputStream());
					SondasUtils.writeString("SondasPlorer 0.1",dos);

					// Escribo la cabecera
					// Envio el nombre del agente
					String agentName = Global.getProperty("Agent", "agent.name");
					SondasUtils.writeString(agentName, dos);
					
					// Envio la cabecera
					SondasUtils.writeHeader(Global.getNodesIds(),dos);
					
					active=true;
				} catch (Throwable e) {
					e.printStackTrace();
					active=false;
					try {serverSocket.close();} catch (IOException ex){}
					break;
				}
			}
		}
	}

	// Consumer
	
	private void dataPumperThread() {
		while (!shutdown) {
			synchronized(listOfPacketsToSend) {
				while (listOfPacketsToSend.size() == 0) {
					try {
						listOfPacketsToSend.wait();
					} catch (InterruptedException ex) {
						return;
					}
				}
				Object packtedToSend = (Object) listOfPacketsToSend.removeLast();
				int pid = (Integer) listOfPidsToSend.removeLast();

				// NUEVO
				try {
					// Envio el paquete
					System.out.println("Sending packet from pumper");
					sendImmediate(pid, packtedToSend);
				} catch (Throwable e) {
					System.err.println("Exception while sending packet from pumper");
					e.printStackTrace();
					InspectorRuntime.getInstance().stopCapture();
				}
			}
		}
	}

	synchronized static public void sendToPumper(int pid, Object packetToSend) {
		synchronized(listOfPacketsToSend) {
			listOfPacketsToSend.addFirst(packetToSend);
			listOfPidsToSend.add(pid);
			listOfPacketsToSend.notifyAll();
		}
	}

	public void run() 
	{
		if (threadAction==ListenerThreadAction) {
			listenerThread();
		} else if (threadAction==DataPumperAction) {
			dataPumperThread();
		}
	}
}
