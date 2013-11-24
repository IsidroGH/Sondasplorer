package sondas.inspector.delta;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.StringsCache;

public interface IDeltaMarshable {
	public void marshall(int pid, DataOutputStream dos, StringsCache stringsCache) throws IOException;
}
