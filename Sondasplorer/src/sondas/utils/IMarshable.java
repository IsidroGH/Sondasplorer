package sondas.utils;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.StringsCache;

public interface IMarshable {
	public void marshall(DataOutputStream dos) throws IOException;
}
