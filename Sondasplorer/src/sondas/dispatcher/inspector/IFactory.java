package sondas.dispatcher.inspector;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.inspector.ITrace;

public interface IFactory {
	public ITrace createTrace(DataInputStream dis) throws IOException;
}
