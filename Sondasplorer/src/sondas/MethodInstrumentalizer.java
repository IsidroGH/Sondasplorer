package sondas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import sondas.inspector.InspectorRuntime;
import sondas.utils.SondasUtils;

/*
 * TODO:
 * BIPUSH solo almacena 1 byte con signo. Migrar a SIPUSH para que los ids (de probe por ejemplo) puedan ser mayores de 128.
 */
public class MethodInstrumentalizer implements MethodVisitor, Opcodes
{
	private String methodName;
	private String className;
	private boolean isCtor;
	private boolean superCalled;
	private String methodSignature;
	private int originalFirstVbleId;
	private int paramsCount;
	private int firstParamId;
	private int endParamId;
	private boolean traceResponse;
	private char respChar;
	
	private SondasLoader sondasLoader = SondasLoader.getInstance();

	// Variables globales al metodo
	static private int exIdsVbles[] = new int[32]; // Máximo 32 sondas permitidas 
	static private int probesVblesIds[] = new int[32]; // Máximo 32 sondas permitidas

	// Array global
	static private byte paramTypes[] = new byte[128];

	// Esta variable contiene el numero de variables añadidas por las sondas mas una variable que contiene la excepcion del finally
	private int newVblesCount;
	private String fqName;
	private MethodVisitor mv;
	private Label l0 = new Label();
	private Label l1 = new Label();
	private int access;
	private int respVbleId=-1;

	// NodeIndexBean
	private List probesNodesAndIndexes;

	private boolean tryInserted;
	
	// Cada método instrumentalizado con una determinada sonda tendrá un id único
	// que es el que se usará para buscar nodos en los mapas del DeltaTree
	static int id=1;

	public MethodInstrumentalizer(String className, String classSignature, 
			int access, String methodName, String methodSignature, 
			String[] exceptions, List probesNodesAndIndexes, MethodVisitor origMv) 	
	{
		tryInserted = false;
		this.probesNodesAndIndexes = probesNodesAndIndexes;
		this.mv=origMv;
		this.access = access;
		this.methodName = methodName;
		this.className = className.replaceAll("/", ".");
		this.methodSignature = methodSignature;
		this.fqName = SondasUtils.getFqMethod(className,methodName,methodSignature);
		
		sondasLoader.setMethodName(id, SondasUtils.getFqMethod(this.className, methodName, methodSignature));
		
		if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
			isCtor = true;
		} else {
			isCtor = false;
		}

		superCalled = false;

		// Obtengo el id de la primera variable
		originalFirstVbleId = parseSignature(methodSignature);

		firstParamId = isStatic()?0:1;
		endParamId = originalFirstVbleId-1;

		newVblesCount=1; // Al menos la excepcion del finally añadido
		
		
	}

	private void insertTryFinallyIfFirstIns() {
		if (!tryInserted) {
			mv.visitTryCatchBlock(l0, l1, l1, null);
			mv.visitLabel(l0);
			tryInserted=true;
		}
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc)
	{
		insertTryFinallyIfFirstIns();

		for (int i=0;i<probesNodesAndIndexes.size();i++) {
			NodeIndexBean nib = (NodeIndexBean)probesNodesAndIndexes.get(i);

			if (nib.probe.traceInternalMethods()) {
				mv.visitVarInsn(ALOAD, probesVblesIds[i]);
				// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
				mv.visitVarInsn(ALOAD, exIdsVbles[i]);
				mv.visitMethodInsn(INVOKEINTERFACE, "sondas/IProbe", "leaveMethod", "(Ljava/lang/Object;)V");
			}
		}

		mv.visitMethodInsn(opcode, owner, name, desc);

		for (int i=0;i<probesNodesAndIndexes.size();i++) {
			NodeIndexBean nib = (NodeIndexBean)probesNodesAndIndexes.get(i);

			if (nib.probe.traceInternalMethods()) {
				mv.visitVarInsn(ALOAD, probesVblesIds[i]);
				// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
				mv.visitVarInsn(ALOAD, exIdsVbles[i]);
				mv.visitMethodInsn(INVOKEINTERFACE, "sondas/IProbe", "resumeMethod", "(Ljava/lang/Object;)V");
			}
		}
	}

	/**
	 * Devuelve el Id de la nueva variable a añadir
	 */
	private int newVble() {
		int resp = originalFirstVbleId+newVblesCount;
		newVblesCount++;
		return resp;
	}

	
	public void visitCode() 
	{
		mv.visitCode();

		boolean traceParams=false;

		for (int i=0;i<probesNodesAndIndexes.size();i++) {
			NodeIndexBean nib = (NodeIndexBean)probesNodesAndIndexes.get(i);
			if (nib.probe.traceParams()) {
				traceParams=true;
				break;
			}
		}

		for (int i=0;i<probesNodesAndIndexes.size();i++) {
			NodeIndexBean nib = (NodeIndexBean)probesNodesAndIndexes.get(i);
			if (nib.probe.traceResponse()) {
				traceResponse=true;
				break;
			}
		}

		int paramsVbleId = newVble();

		if (traceParams) 
		{
			mv.visitIntInsn(BIPUSH, paramsCount);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitVarInsn(ASTORE, paramsVbleId);

			// Añado los parametros al array de objetos
			for (int offset=0,param=0;param<paramsCount;offset++,param++) {
				byte paramType = paramTypes[offset];

				mv.visitVarInsn(ALOAD, paramsVbleId);
				mv.visitIntInsn(BIPUSH, param);

				if (paramType=='B') {
					mv.visitTypeInsn(NEW, "java/lang/Byte");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ILOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");
				} else if (paramType=='S') {
					mv.visitTypeInsn(NEW, "java/lang/Short");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ILOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
				}  else if (paramType=='I') {
					mv.visitTypeInsn(NEW, "java/lang/Integer");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ILOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
				}  else if (paramType=='J') {
					mv.visitTypeInsn(NEW, "java/lang/Long");
					mv.visitInsn(DUP);
					mv.visitVarInsn(LLOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V");
					// Este tipo primitivo ocupa dos words de 4 bytes
					offset++;
				} else if (paramType=='F') {
					mv.visitTypeInsn(NEW, "java/lang/Float");
					mv.visitInsn(DUP);
					mv.visitVarInsn(FLOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
				} else if (paramType=='D') {
					mv.visitTypeInsn(NEW, "java/lang/Double");
					mv.visitInsn(DUP);
					mv.visitVarInsn(DLOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
					// Este tipo primitivo ocupa dos words de 4 bytes
					offset++;
				} else if (paramType=='Z') {
					mv.visitTypeInsn(NEW, "java/lang/Boolean");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ILOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");
				} else if (paramType=='C') {
					mv.visitTypeInsn(NEW, "java/lang/Character");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ILOAD, firstParamId+offset);
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
				} else {
					// Se trata de un objeto o un array
					mv.visitVarInsn(ALOAD, firstParamId+offset);
				}

				mv.visitInsn(AASTORE);
			} 
		} else {
			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ASTORE, paramsVbleId);
		}

		int customParamsVbleId = newVble();
		
		// Bucle de invocacion a las sondas
		for (int i=0;i<probesNodesAndIndexes.size();i++) {
			NodeIndexBean nib = (NodeIndexBean)probesNodesAndIndexes.get(i);

			exIdsVbles[i] = newVble();
			probesVblesIds[i] = newVble();

			mv.visitIntInsn(BIPUSH, nib.index);
			mv.visitMethodInsn(INVOKESTATIC, "sondas/SondasLoader", "getProbe", "(I)Lsondas/IProbe;");
			mv.visitVarInsn(ASTORE, probesVblesIds[i]);
			mv.visitVarInsn(ALOAD, probesVblesIds[i]);
			mv.visitIntInsn(BIPUSH, ((NodeIndexBean)probesNodesAndIndexes.get(i)).nodeId);
			//mv.visitLdcInsn(fqName);
			mv.visitLdcInsn(className);
			mv.visitLdcInsn(methodName);
			mv.visitLdcInsn(methodSignature);
			mv.visitIntInsn(SIPUSH, id);

			// Parametros
			mv.visitVarInsn(ALOAD, paramsVbleId);
			
			// Custom params			
			if(nib.probe.useCustomParams()) {
				List customParams = nib.probe.getCustomParams();
				int count = customParams.size();
				
				mv.visitIntInsn(BIPUSH, count);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
				mv.visitVarInsn(ASTORE, customParamsVbleId);
				
				for (int j=0;j<customParams.size();j++) {
					mv.visitVarInsn(ALOAD, customParamsVbleId);
					mv.visitIntInsn(BIPUSH, j);
					CustomParam customParam = (CustomParam)customParams.get(j);
					
					// Llamo al accessMethod y el resultado lo meto dentro del array
					// Por ahora solo permito accessMethods que devuelvan un objeto
					if (customParam.returnType.length()>1) {
						int accessInvokationType = customParam.isInterface?INVOKEINTERFACE:INVOKEVIRTUAL;
						
						if (accessInvokationType!=-1) {
							mv.visitVarInsn(ALOAD, firstParamId+customParam.paramPos);
							mv.visitMethodInsn(accessInvokationType, customParam.className, customParam.method, "()L"+customParam.returnType+";");	
						} else {
							// Si no es interface ni virtual lo ignoramos
							mv.visitInsn(ACONST_NULL);	
						}
					} else {
						mv.visitInsn(ACONST_NULL);	
					}
					
					mv.visitInsn(AASTORE);
				}
				
				mv.visitVarInsn(ALOAD, customParamsVbleId);
			} else {
				mv.visitInsn(ACONST_NULL);
			}
			
			
			if (isStatic()) {
				// Si es estatico se pasa null como objeto this
				mv.visitInsn(ACONST_NULL);
			} else {
				// Referencia a this
				mv.visitVarInsn(ALOAD, 0);
			}
			// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
			mv.visitMethodInsn(INVOKEINTERFACE, "sondas/IProbe", "startMethod", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I[Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
			mv.visitVarInsn(ASTORE, exIdsVbles[i]);
		}
		
		// Variable de retorno
		// Si es void no reservo espacio
		// Si es long o double reservo 8 bytes
		// En otro caso reservo 4 bytes
		if (!isCtor) {
			if (traceResponse) {
				if (respChar!='V') {
					respVbleId = newVble();
					if (respChar=='J' || respChar=='D') {
						newVble();
					}
				}
			}
		}
		
		id++;
	}

	public void visitInsn(int opcode)
	{
		insertTryFinallyIfFirstIns();

		if (opcode==RETURN || opcode==IRETURN || opcode==LRETURN || opcode==FRETURN || opcode==DRETURN || opcode==ARETURN) {
			if (!isCtor) {
				
				if (traceResponse) {
					if (opcode==ARETURN) 
					{
						// Si el retorno es un objeto
						mv.visitVarInsn(ASTORE, respVbleId);
					} else if (opcode==IRETURN) {
						mv.visitVarInsn(ISTORE, respVbleId);
					}  else if (opcode==LRETURN) {
						mv.visitVarInsn(LSTORE, respVbleId);
					} else if (opcode==FRETURN) {
						mv.visitVarInsn(FSTORE, respVbleId);
					} else if (opcode==DRETURN) {
						mv.visitVarInsn(DSTORE, respVbleId);
					} 
				}
				
				// Bucle de invocacion a las sondas. 
				// Invoco a las sondas en orden inverso a como fueron llamadas
				for (int i=probesNodesAndIndexes.size()-1;i>=0;i--) {
					mv.visitVarInsn(ALOAD, probesVblesIds[i]);
					
					// 20100529 - Añado el id de nodo en la invocacion a endMethod
					mv.visitIntInsn(BIPUSH, ((NodeIndexBean)probesNodesAndIndexes.get(i)).nodeId);
					
					// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
					mv.visitVarInsn(ALOAD, exIdsVbles[i]);

					if (traceResponse) {
						if (opcode==RETURN) {
							// Si el retorno es void, paso null como parametro de respuesta
							mv.visitInsn(ACONST_NULL);
						} else if (opcode==ARETURN) {
							// Si el retorno es un objeto o un array
							mv.visitVarInsn(ALOAD, respVbleId);
						} else if (opcode==IRETURN) {
							// Como los tipos ZCBSI son almacenados en I, tengo que parsear la firma del metodo previamente
							// para saber el tipo real de respuesta que viene almacenado en respChar
							if (respChar=='I') {
								mv.visitTypeInsn(NEW, "java/lang/Integer");
								mv.visitInsn(DUP);
								mv.visitVarInsn(ILOAD, respVbleId);
								mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
							} else if (respChar=='Z') {
								mv.visitTypeInsn(NEW, "java/lang/Boolean");
								mv.visitInsn(DUP);
								mv.visitVarInsn(ILOAD, respVbleId);
								mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");
							} else if (respChar=='C') {
								mv.visitTypeInsn(NEW, "java/lang/Character");
								mv.visitInsn(DUP);
								mv.visitVarInsn(ILOAD, respVbleId);
								mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
							} else if (respChar=='B') {
								mv.visitTypeInsn(NEW, "java/lang/Byte");
								mv.visitInsn(DUP);
								mv.visitVarInsn(ILOAD, respVbleId);
								mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(C)V");
							} else if (respChar=='S') {
								mv.visitTypeInsn(NEW, "java/lang/Short");
								mv.visitInsn(DUP);
								mv.visitVarInsn(ILOAD, respVbleId);
								mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
							}
						}  else if (opcode==LRETURN) {
							mv.visitTypeInsn(NEW, "java/lang/Long");
							mv.visitInsn(DUP);
							mv.visitVarInsn(LLOAD, respVbleId);
							mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V");
						} else if (opcode==FRETURN) {
							mv.visitTypeInsn(NEW, "java/lang/Float");
							mv.visitInsn(DUP);
							mv.visitVarInsn(FLOAD, respVbleId);
							mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
						} else if (opcode==DRETURN) {
							mv.visitTypeInsn(NEW, "java/lang/Double");
							mv.visitInsn(DUP);
							mv.visitVarInsn(DLOAD, respVbleId);
							mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
						} 
					} else {
						// Si no hay que tracear la respuesta paso null
						mv.visitInsn(ACONST_NULL);
					}

					// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
					mv.visitMethodInsn(INVOKEINTERFACE, "sondas/IProbe", "endMethod", "(ILjava/lang/Object;Ljava/lang/Object;)V");
				}
			}
		}

		if (traceResponse) {
			// Respuesta
			if (opcode==ARETURN) 
			{
				// Si el retorno es un objeto
				mv.visitVarInsn(ALOAD, respVbleId);
			} else if (opcode==IRETURN) {
				mv.visitVarInsn(ILOAD, respVbleId);
			}  else if (opcode==LRETURN) {
				mv.visitVarInsn(LLOAD, respVbleId);
			} else if (opcode==FRETURN) {
				mv.visitVarInsn(FLOAD, respVbleId);
			} else if (opcode==DRETURN) {
				mv.visitVarInsn(DLOAD, respVbleId);
			}
		}

		mv.visitInsn(opcode);
	}

	public void visitMaxs(int arg0, int arg1) 
	{
		insertTryFinallyIfFirstIns();

		mv.visitLabel(l1);
		mv.visitVarInsn(ASTORE, originalFirstVbleId);

		if (!isCtor) {
			// Bucle de invocacion a las sondas

			for (int i=0;i<probesNodesAndIndexes.size();i++) {
				mv.visitVarInsn(ALOAD, probesVblesIds[i]);
				
				// 20100529 - Añado el id de nodo en la invocacion a endMethod
				mv.visitIntInsn(BIPUSH, ((NodeIndexBean)probesNodesAndIndexes.get(i)).nodeId);
				
				// 20100718 ahora en vez de devolver un Id desde startMethod devuelvo el objeto
				mv.visitVarInsn(ALOAD, exIdsVbles[i]);				
				mv.visitMethodInsn(INVOKEINTERFACE, "sondas/IProbe", "exceptionMethod", "(ILjava/lang/Object;)V");
			}

		}

		mv.visitVarInsn(ALOAD, originalFirstVbleId);
		mv.visitInsn(ATHROW);

		mv.visitMaxs(arg0, arg1);
	}

	/**
	 * Parsea los parametros del metodo devolviendo el Id de la primera variable (no parametro)
	 * 
	 * TODO: Comprobar el correcto funcionamiento con arrays multimensionales
	 */
	protected int parseSignature(String signature) {
		if (signature==null) {
			return 0;
		}

		int open = signature.indexOf("(");
		int end = signature.indexOf(")");
		respChar = signature.charAt(end+1);
		
		signature =  signature.substring(open+1, end);
		
		int pos=0;
		int len = signature.length();
		int resp = 0;
		byte[] bytes = signature.getBytes();

		boolean array=false;
		
		while (pos<len) {
			byte code=bytes[pos];
			if (code=='Z' || code=='V' || code=='C' || code=='B' || code=='S' || code=='I' || code=='F' || code=='J' || code=='D'){
				
				if (array) {
					paramTypes[resp]='[';
					array =false;
					
					resp++;
					paramsCount++;
				} else {
					paramTypes[resp]=code;
					
					if (code=='J' || code=='D') {
						// long y double ocupan 2 words de 4 bytes
						resp+=2;
						paramsCount++;
					} else {
						resp++;
						paramsCount++;
					}
				}
				
				pos++;
				continue;
			} else if (code=='L'){
				// Object
				if (array) {
					paramTypes[resp]='[';
					array =false;
				} else {
					paramTypes[resp]=code;
				}
				pos = signature.indexOf(";", pos+1)+1;
				resp++;
				paramsCount++;
				continue;
			}  else if (code=='[') {
				array=true;
				pos++;
				continue;
			} else {
				throw new RuntimeException("Parameter not supported: "+code);
			}
		}

		if ((access&ACC_STATIC)!=ACC_STATIC) {
			resp++;
		} 
		return resp;
	}

	public boolean isStatic() {
		return (access&ACC_STATIC)==ACC_STATIC;
	}

	public void visitVarInsn(int opcode, int var) 
	{
		insertTryFinallyIfFirstIns();

		if (!isCtor) {
			if (var>=originalFirstVbleId) {
				var+=newVblesCount;
			}
		}
			
		mv.visitVarInsn(opcode, var);
	}

	public void visitIincInsn(int var, int increment)
	{
		insertTryFinallyIfFirstIns();

		if (!isCtor) {
			if (var>=originalFirstVbleId) {
				var+=newVblesCount;
			}
		}
		mv.visitIincInsn(var, increment);
	}

//	Visitors

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		insertTryFinallyIfFirstIns();
		return mv.visitAnnotation(desc, visible);
	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible){
		insertTryFinallyIfFirstIns();
		return mv.visitParameterAnnotation(parameter, desc, visible);
	}

	public AnnotationVisitor visitAnnotationDefault() {
		insertTryFinallyIfFirstIns();
		return mv.visitAnnotationDefault();
	}

	public void visitAttribute(Attribute attr){
		insertTryFinallyIfFirstIns();
		mv.visitAttribute(attr);
	}

	public void	visitEnd(){
		insertTryFinallyIfFirstIns();
		mv.visitEnd();
	}

	public void visitFieldInsn(int opcode, String owner, String name, String desc){
		insertTryFinallyIfFirstIns();
		mv.visitFieldInsn(opcode, owner, name, desc);
	}

	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack){
		insertTryFinallyIfFirstIns();
		mv.visitFrame(type, nLocal, local, nStack, stack);
	}

	public void visitIntInsn(int opcode, int operand){
		insertTryFinallyIfFirstIns();
		mv.visitIntInsn(opcode, operand);
	}

	public void visitJumpInsn(int opcode, Label label){
		insertTryFinallyIfFirstIns();
		mv.visitJumpInsn(opcode, label);
	}

	public void visitLabel(Label label){
		insertTryFinallyIfFirstIns();
		mv.visitLabel(label);
	}

	public void visitLdcInsn(Object cst){
		insertTryFinallyIfFirstIns();
		mv.visitLdcInsn(cst);
	}

	public void visitLineNumber(int line, Label start){
		insertTryFinallyIfFirstIns();
		mv.visitLineNumber(line, start);
	}

	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index){
		insertTryFinallyIfFirstIns();
		mv.visitLocalVariable(name, desc, signature, start, end, index);
	}

	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels){
		insertTryFinallyIfFirstIns();
		mv.visitLookupSwitchInsn(dflt, keys, labels);
	}

	public void visitMultiANewArrayInsn(String desc, int dims){
		insertTryFinallyIfFirstIns();
		mv.visitMultiANewArrayInsn(desc, dims);
	}

	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels){
		insertTryFinallyIfFirstIns();
		mv.visitTableSwitchInsn(min, max, dflt, labels);
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type){
		mv.visitTryCatchBlock(start, end, handler, type);
	}

	public void visitTypeInsn(int opcode, String type){
		insertTryFinallyIfFirstIns();
		mv.visitTypeInsn(opcode, type);
	}
}
