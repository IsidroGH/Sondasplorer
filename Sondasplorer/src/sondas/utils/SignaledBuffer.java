package sondas.utils;

import java.util.ArrayList;

public class SignaledBuffer {
	public ArrayList buffer;
	public volatile boolean sendRequest;
	
	public SignaledBuffer() {
		buffer = new ArrayList();
		sendRequest=false;
	}
}
