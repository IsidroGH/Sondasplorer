package sondas;

import java.io.Writer;

import sondas.inspector.InspectorRuntime;

public class MPInspector extends BaseMessagesProcessorModule 
{
	static {
		InspectorRuntime.getInstance().init(true);
	}
	public String getName() {
		return "inspector";
	}
	
	public void execute(String command, Writer out) throws Exception {
		if (command.equals("set delta")) {
			InspectorRuntime.getInstance().setDeltaMode();
			ConversationManager.sendResponse(ConversationManager.Ok, out,"delta mode on");
		} else if (command.equals("set full")) {
			InspectorRuntime.getInstance().setFullMode();
			ConversationManager.sendResponse(ConversationManager.Ok, out,"full mode off");
		} else if (command.equals("start")) {
			InspectorRuntime.getInstance().startCapture();
			ConversationManager.sendResponse(ConversationManager.Ok, out,"capture on");
		} else if (command.equals("stop")) {
			InspectorRuntime.getInstance().stopCapture();
			ConversationManager.sendResponse(ConversationManager.Ok, out,"capture off");
		} else if (command.startsWith("set slice ")) {
			int slice = Integer.parseInt(command.substring(10));
			InspectorRuntime.getInstance().setSlice(slice);
			ConversationManager.sendResponse(ConversationManager.Ok, out,"slice set");
		} else {
			ConversationManager.sendResponse(ConversationManager.Error, out,"Invalid command");
		}
	}
	
	public void abort() {
		InspectorRuntime.getInstance().stopCapture();
	}
}
