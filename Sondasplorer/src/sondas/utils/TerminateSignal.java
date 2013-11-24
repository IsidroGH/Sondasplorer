package sondas.utils;

import java.io.DataOutputStream;
import java.io.IOException;


public class TerminateSignal implements IMarshable {
	public static TerminateSignal signal = new TerminateSignal();
	
	public void marshall(DataOutputStream dos) throws IOException {
		dos.writeShort(-1);
	}
}
