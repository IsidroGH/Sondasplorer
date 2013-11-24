package sondas;

public class CustomParam {
	
	final public static int INVOKEVIRTUAL=1;
	final public static int INVOKEINTERFACE=2;
	
	int paramPos;
	String method;
	String className;
	boolean isInterface; 
	String returnType;
	
	public CustomParam(int paramPos, String className, String method, boolean isInterface, String returnType) {
		this.paramPos = paramPos;
		this.method = method;
		this.className = className;
		this.isInterface = isInterface;
		this.returnType = returnType;
	}
}
