package sondas;

import java.io.FileOutputStream;
import java.util.List;
import org.objectweb.asm.*;

import sondas.utils.SondasUtils;

public class ClassInstrumentalizer implements ClassVisitor
{
	private ClassWriter cw;
	private String generatedClass;
	private String className;
	private String classSignature;
	private String superClassName;
	private boolean fileOut;
	private byte[] out;

	/**
	 * Constructor que genera un fichero con la clase instrumentalizada
	 * Para pruebas
	 */
	public ClassInstrumentalizer (String generatedClass) {
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		this.generatedClass = generatedClass;
		fileOut = true;
	}

	/**
	 * Constructor de Producción
	 */
	public ClassInstrumentalizer () {
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		fileOut = false;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) 
	{
		
		// No instrumentalizo los constructores
		if (!name.equals("<init>")&&!name.equals("<clinit>")) 
		{
			List probesNodesAndIndexes = SondasLoader.getInstance().getMethodProbesIndexes(className, name, desc, superClassName);

			if (probesNodesAndIndexes.size()>0) {
				try {
					//System.out.println("  Añadiendo sonda a "+name);
					MethodVisitor origMv = cw.visitMethod(access, name, desc, signature, exceptions);

					MethodInstrumentalizer methodInstrumentalizer = new MethodInstrumentalizer(className, classSignature, access, name, desc, exceptions, probesNodesAndIndexes, origMv);

					// Devuelvo el metodo instrumentalizado
					return methodInstrumentalizer;
				} catch (Exception e) {
					System.out.println("Unable to add probe to "+SondasUtils.getFqMethod(className,name,desc));
					e.printStackTrace();
				}
			}
		} 
		
		// Dejamos el metodo sin instrumentalizar
		return cw.visitMethod(access, name, desc, signature, exceptions);
	}



	public byte[] getInstrumentalizedClass() {
		return out;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) 
	{
		//System.out.println("-> Class: "+name+", superClass: "+superName);
		
		this.className = name.replaceAll("/", ".");
		this.classSignature = signature;
		this.superClassName = superName.replaceAll("/", ".");
		cw.visit(version, access, name, signature, superName, interfaces);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return cw.visitAnnotation(desc, visible);
	}

	public void visitAttribute(Attribute attr)
	{
		cw.visitAttribute(attr);
	}

	public void visitEnd() {
		cw.visitEnd();
		if (fileOut) {
			try {
				FileOutputStream fos = new FileOutputStream(generatedClass);
				fos.write(cw.toByteArray());
				fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			out = cw.toByteArray();
		}
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return cw.visitField(access, name, desc, signature, value);
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		cw.visitInnerClass(name, outerName, innerName, access);
	}

	public void visitOuterClass(String owner, String name, String desc) {
		cw.visitOuterClass(owner, name, desc);
	}

	public void visitSource(String source, String debug) {
		cw.visitSource(source, debug);
	}
}
