package sondas.rt;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;

import sondas.SondasLoader;

public class AutoInstrument 
{
	public static void premain(String options, Instrumentation ins) {
		SondasLoader.startup();

		ins.addTransformer(new SondasClassLoader() );
		System.out.println("AutoInstrument loaded!");
	}
	
	public static class SondasClassLoader implements ClassFileTransformer {
		
		public byte[] transform(java.lang.ClassLoader loader, java.lang.String className, java.lang.Class classBeingRedefined, java.security.ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException
		{
			if (className.startsWith("inst/") || className.startsWith("java/") || className.startsWith("sun/")) {
				return null;
			}
			
			byte[] instClazz = SondasLoader.load(className, classfileBuffer); 
			return instClazz;
		}
	}
}
