package sondas.inspector.probes;

public interface IInspectorProbe 
{
	public void startCapture();
	public void stopCapture();	
	public void setFullMode();
	public void setDeltaMode();
}
