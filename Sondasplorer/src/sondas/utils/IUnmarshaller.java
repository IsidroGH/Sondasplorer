package sondas.utils;

import java.io.DataInputStream;
import java.io.IOException;

import sondas.inspector.StringsCache;

public interface IUnmarshaller {
	public Object unmarshall(DataInputStream dis, StringsCache stringsCache) throws IOException;
}
