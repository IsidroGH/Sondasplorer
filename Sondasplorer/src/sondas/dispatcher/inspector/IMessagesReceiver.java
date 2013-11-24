package sondas.dispatcher.inspector;

import java.io.BufferedWriter;
import java.io.OutputStream;

import sondas.dispatcher.RespBean;

public interface IMessagesReceiver {
	public RespBean sendCommand(String cmd, BufferedWriter outText, OutputStream outBin);
}
