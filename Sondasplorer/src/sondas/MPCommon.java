package sondas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.StringTokenizer;

import sondas.dispatcher.Dispatcher;
import sondas.dispatcher.DispatcherException;
import sondas.dispatcher.RespBean;
import sondas.utils.DataSender;
import sondas.utils.SignaledBuffer;

public class MPCommon extends BaseMessagesProcessorModule 
{
	private ArrayList signaledBuffers=new ArrayList();
	
	public String getName() {
		return "";
	}
	
	synchronized public void registerSignaledBuffer(SignaledBuffer signaledBuffer)
	{
		signaledBuffers.add(signaledBuffer);
	}
	
	public void execute(String command, Writer out) throws Exception {
		if (command.startsWith("enable ")) {
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			String nodeName = st.nextToken();
			int nodeId = Global.getNodeId(nodeName);
			if (nodeId!=-1) {
				Global.enableNode(nodeId);
				ConversationManager.sendResponse(ConversationManager.Ok, out, "Node "+nodeName+" enabled");
			} else {
				ConversationManager.sendResponse(ConversationManager.Error, out,"Node "+nodeName+" not registered");
			}
		} else if (command.startsWith("disable ")) {
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			String nodeName = st.nextToken();
			int nodeId = Global.getNodeId(nodeName);
			if (nodeId!=-1) {
				Global.disableNode(nodeId);
				ConversationManager.sendResponse(ConversationManager.Ok, out,"Node "+nodeName+" disabled");
			} else {
				ConversationManager.sendResponse(ConversationManager.Error, out,"Node "+nodeName+" not registered");
			}
		} else if (command.startsWith("shutdown")) {
			ConversationManager.sendResponse(ConversationManager.Ok, out,"Shutting down");
			DataSender.signalShutdown();
			// Recorro los signaledbuffers y los voy vaciando (flush)
			for (int i=0;i<signaledBuffers.size();i++) {
				SignaledBuffer signaledBuffer = (SignaledBuffer) signaledBuffers.get(i);
				
				while (signaledBuffer.sendRequest==true) {
					// Espero
					ConversationManager.sendResponse(ConversationManager.Ok, out,"Waiting for application threads to finish");
					Thread.sleep(5000);									
				}
				
				// Envio el contenido del buffer
				DataSender.sendBuffer(signaledBuffer.buffer);
			}
			DataSender.closeChannel();
			ConversationManager.sendResponse(ConversationManager.Ok, out,"Shutted down");
		} else {
			ConversationManager.sendResponse(ConversationManager.Error, out,"Invalid command");
		}
	}
	
	public void abort() {
		
	}
	
}
