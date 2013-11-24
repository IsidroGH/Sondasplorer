package sondas;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseMessagesProcessorModule 
{
	/**
	 * Devuelve true en caso de ser invocado exit
	 */
	public abstract void execute(String command, Writer out) throws Exception;
	public abstract String getName();
	public abstract void abort();
}
