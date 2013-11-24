package sondas.utils;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.StringsCache;

public interface ICachedMarshable {
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException;
}
