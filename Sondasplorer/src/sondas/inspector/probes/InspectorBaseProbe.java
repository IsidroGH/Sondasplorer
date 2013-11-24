package sondas.inspector.probes;

import sondas.BaseProbe;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.delta.DeltaTree;

public abstract class InspectorBaseProbe extends BaseProbe implements IInspectorProbe 
{
	final protected int fullProbeId;
	final protected int deltaProbeId;

	protected volatile boolean fullMode;
	protected volatile boolean capturing;
	protected final String deltaTreeKey;

	final protected int metricType;

	public InspectorBaseProbe(boolean traceInternalMethods, boolean traceParams, boolean traceResponse, String metricTypeName) 
	{
		super (traceInternalMethods, traceParams, traceResponse);
		metricType = InspectorRuntime.getInstance().getMetricType(metricTypeName);
		fullProbeId = InspectorRuntime.getFullProbeId();
		deltaProbeId = InspectorRuntime.getDeltaProbeId();
		deltaTreeKey=deltaProbeId+":dt";
		fullMode = false;
	}

	public void init() {
		InspectorRuntime.getInstance().observeProbe(this);		
	}

	public void startCapture() {
		// En caso de delta tengo que vaciar el deltaTree
		capturing=true;
	}

	public void stopCapture() {
		capturing=false;
	}

	public void setFullMode() {
		fullMode=true;
	}

	public void setDeltaMode() {
		fullMode=false;
	}

	/**
	 * Devuelve el DeltaTree asociado al hilo.
	 * Si no existe, crea uno y lo asigna al hilo asi como lo registra en InspectorRuntime
	 * En ambos casos obtiene el lock sobre el DT que deberá ser liberado posteriormente por
	 * el cliente mediante una llamada al metodo releaseDeltaTree
	 */
	public DeltaTree getThreadDeltaTree(int tid) throws InterruptedException
	{
		DeltaTree dt = (DeltaTree) getThreadObject(deltaTreeKey);
		if (dt==null) {
			dt = new DeltaTree(tid);
			InspectorRuntime.getInstance().registerDeltaTree(dt, getThreadStore());		
		}

		// Intenta adquirir de forma exclusiva acceso sobre el DT
		// El otro elemento que puede requerir acceso exclusivo sobre el elemento
		// es el Sender. Cuando se adquiere, puede ser que el Sender lo haya desbloqueado
		// y deregistrado (isJustSent) por lo que tenemos que crear un DT nuevo (guardando el estado)
		// e intentar adquirir un lock sobre el con la misma problemática. Por eso aparece un bucle siendo
		// la condicion de ruptura que el DT no haya sido envidado => no ha sido deregistrado
		while (true) 
		{
			dt.mutex.acquire();
			if (!dt.isRegistered()) 
			{
				InspectorRuntime.getInstance().registerDeltaTree(dt, getThreadStore());
			} else {
				break;
			}
		}

		return dt;
	}
	

	public void releaseDeltaTree(DeltaTree dt) {
		if (dt!=null) {
			long holds = dt.mutex.holds();
			dt.mutex.release(holds);
		}
	}
}
