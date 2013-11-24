package sondas.client.inspector;

public class ClientException extends Exception 
{
	public ClientException(String msg, Throwable e) {
		super (msg,e);
	}
	
	public ClientException(String msg) {
		super (msg);
	}
}
