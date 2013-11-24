package sondas.inspector.delta;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import sondas.inspector.StringsCache;

public interface IGlobalStats extends Cloneable {
	public void merge(IGlobalStats gs);
	public boolean isEquivalent(IGlobalStats _gs);
	
	// Indica el id del nodo. Cada metodo instrumentalizado tendra un id unico
	public int getId();
	
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException;
	public String getName(Map methodNames);
	public String getTypeName();
	public int getMetricType();
	public int getCategoryId();
	public IGlobalStats getDiferential(IGlobalStats _gs);
	public Object clone();
}
