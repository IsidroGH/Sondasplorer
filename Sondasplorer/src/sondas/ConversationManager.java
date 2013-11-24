package sondas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.DispatcherException;
import sondas.dispatcher.RespBean;

public class ConversationManager 
{
	final public static int Ok=0;
	final public static int Error=1;
	final public static int Ignored=2;

	// Comandos

	public static RespBean sendCommand(BufferedReader in, Writer out, String cmd) throws IOException {
		RespBean resp = new RespBean();
		write(out, cmd+"\r\n");
		String aux = readln(in);
		StringTokenizer st = new StringTokenizer(aux,";");
		resp.code = Integer.parseInt(st.nextToken());
		resp.msg = st.nextToken();
		return resp;
	}
	
	/**
	 * Envia un comando que forma parte de un conjunto de comandos. 
	 * Lanza una excepcion en caso de que algo vaya mal
	 * @throws ConversationException 
	 */
	public static RespBean sendPartialCommand(BufferedReader in, Writer out, String cmd) throws IOException, ConversationException 
	{
		RespBean resp = new RespBean();

		write(out, cmd+"\r\n");
		String aux = readln(in);
		StringTokenizer st = new StringTokenizer(aux,";");
		resp.code = Integer.parseInt(st.nextToken());
		resp.msg = st.nextToken();


		if (resp.code==ConversationManager.Error) {
			throw new ConversationException(resp.msg);
		}
		return resp;
	}
	
	public static void sendResponse(int code, Writer out, String text) throws IOException {
		write(out, code+";"+text+"\r\n");
	}

	// Private
	
	private static String readln(BufferedReader in) throws IOException {
		return in.readLine();
	}

	private static void write(Writer out, String text) throws IOException {
		out.write(text);
		out.flush();
	}
}
