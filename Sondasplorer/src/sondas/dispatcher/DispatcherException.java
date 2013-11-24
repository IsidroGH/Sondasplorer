package sondas.dispatcher;

public class DispatcherException extends Exception {
	public DispatcherException(String msg, Throwable e) {
		super (msg,e);
	}
	
	public DispatcherException(String msg) {
		super (msg);
	}
}
