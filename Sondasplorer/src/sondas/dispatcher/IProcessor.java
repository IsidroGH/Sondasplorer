package sondas.dispatcher;

import java.io.DataInputStream;
import java.util.List;
import java.util.Properties;

import sondas.Config;
import sondas.Metadata;

public interface IProcessor {
	public void execute(DataInputStream dis);
	public void setWorkers(List workers);
}
