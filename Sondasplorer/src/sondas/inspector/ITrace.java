package sondas.inspector;

import sondas.utils.IMarshable;

public interface ITrace extends IMarshable {
	public String getKey();
	public String getName();
	public int getMode();
}
