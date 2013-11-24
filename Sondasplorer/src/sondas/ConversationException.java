package sondas;

public class ConversationException extends Exception {
	public ConversationException(String msg, Throwable e) {
		super (msg,e);
	}
	
	public ConversationException(String msg) {
		super (msg);
	}
}
