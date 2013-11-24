package sondas.inspector.probes.full;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.inspector.ITrace;

public class SqlsFactory extends MethodsFactory {

	public SqlsFactory() {
		super();
	}
	
	public ITrace createTrace(DataInputStream dis) throws IOException {
		MethodTrace mt = (MethodTrace) super.createTrace(dis);
		SqlTrace sqlTrace = new SqlTrace(mt);
		return sqlTrace;
	}
}
