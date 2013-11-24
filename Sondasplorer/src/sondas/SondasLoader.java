package sondas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifierClassVisitor;

import sondas.inspector.InspectorRuntime;
import sondas.utils.DataSender;
import sondas.utils.Logger;
import sondas.utils.MultipleRelation;
import sondas.utils.SondasUtils;

public class SondasLoader 
{
	private static SondasLoader instance;
	// private ArrayList instClassPatterns;

	private int registeredProbes;

	// Pattern - NodosSondas
	private MultipleRelation regexNodeProbesRel; 

	// Inherited - NodosSondas
	private MultipleRelation inheritedNodeProbesRel;

	// Relaciona la instancia de una sonda con su indice y viceversa 
	static private HashMap probeIndexRel;

	// Relaciona el nombre de la sonda con su clase 
	static private HashMap probeClassNameProbeRel;	

	static private Set registeredProbesNames;

	static private HashMap replacedClasses;
	
	private static List methodNames = new ArrayList<String>();
	
	static {
		instance = new SondasLoader();
	}
	
	/**
	 * Inicializador estatico para cargar el singleton
	 */
	public static void startup() {
		
	}
	
	private SondasLoader() {
		loadCfg();
	}

	public static SondasLoader getInstance() {
		return instance;
	}
	
	synchronized public static int getNewId() {
		return ++MethodInstrumentalizer.id;
	}

	private void loadCfg() {
		BufferedReader buffer = null;
		
		try {
			System.out.println("Loading Sondasplorer...");

			String cfgFile = System.getProperty("agent.cfg");		
			if (cfgFile==null) {
				throw new RuntimeException("Undefined agent.cfg");
			}
			
			System.out.println("Sondasplorer config: "+cfgFile);
			//Global.cfg = new Config(cfgFile);
			Global.loadConfiguration(cfgFile);
			
			// instClassPatterns = new ArrayList();
			regexNodeProbesRel = new MultipleRelation();
			inheritedNodeProbesRel = new MultipleRelation();
			registeredProbesNames = new HashSet();
			probeIndexRel = new HashMap();
			probeClassNameProbeRel = new HashMap();
			replacedClasses = new HashMap();

			// Leo el fichero de sondas
			String probesCfg = Global.getProperty("Agent", "probes");
			buffer = new BufferedReader(new FileReader(probesCfg));
			String line;
			while (( line = buffer.readLine()) != null){
				if (!line.startsWith("#")) {
					parseLine(line);
				}
			}
			int messagesProcessorPort = Integer.parseInt(Global.getProperty("Agent", "messages.processor.port"));
			Thread thread = new MessagesProcessor(messagesProcessorPort);
			thread.setDaemon(true);
			thread.start(); 

			Global.finishNodeRegistration();
			
			DataSender.startUp();
			
			System.out.println("Sondasplorer loaded!");
		} catch (Throwable e) {
			e.printStackTrace();
			System.out.println("Sondasplorer not loaded!");
		} finally {
			try {if (buffer!=null) {buffer.close();}} catch (Throwable e){};
		}
	}

	/**
	 * class=pgai\.prosa\..*;method=.*;sign=.*;probe=TimeMethodSonder;node=Node1
	 * Pattern: class.method#sign
	 * 
	 * Devuelve el nombre del nodo
	 * 
	 */
	private void parseLine(String line) throws InstantiationException, IllegalAccessException, ClassNotFoundException 
	{
		//class=pgai\.prosa\..*;method=.*;sign=.*;probe=TimeMethodSonder;node=Node1
		
		String aux = line.trim();		
		if (aux.equals("")|| aux.startsWith("#")) {
			return;
		}
		
		HashMap parameters = new HashMap();
		StringTokenizer st = new StringTokenizer(line,",");

		while(st.hasMoreTokens()) {
			String token=st.nextToken();
			StringTokenizer stAux = new StringTokenizer(token,"=");
			String key = stAux.nextToken();
			String value = stAux.nextToken();
			
			parameters.put(key, value);
		}
		
		String probe = (String) parameters.get("probe");
		String node = (String) parameters.get("node");
		String type = (String) parameters.get("type");

		if (type.equals("regex")) {
			// Patron: class.method#sign
			IProbe iProbe = registerProbe(probe);
			int nodeId = Global.registerNode(node);
			NodeProbeBean nodeProbe = new NodeProbeBean(probe,node,nodeId, iProbe);
			
			String clazz = (String) parameters.get("class");
			String method = (String) parameters.get("method");
			String sign = (String) parameters.get("sign");
			
			MatchingRule matchingRule = new MatchingRule();
			matchingRule.regexPattern = SondasUtils.getFqMethod(clazz,method,sign); 

			// Añado la relacion entre el patron y la sonda-nodo
			regexNodeProbesRel.addRelation(matchingRule, nodeProbe);

			// Incluyo el patrón de la clase en la lista de patrones de clases
			// Esto es útil para saber si una clase debe ser instrumentalizada o no
			/*
			if (!instClassPatterns.contains(clazz)) {
				instClassPatterns.add(clazz);
			}
			 */
		} else if (type.equals("inherited")) {
			IProbe iProbe = registerProbe(probe);
			int nodeId = Global.registerNode(node);
			NodeProbeBean nodeProbe = new NodeProbeBean(probe,node,nodeId, iProbe);
			
			String clazz = (String) parameters.get("class");
			String methods = (String) parameters.get("methods");
			String signs = (String) parameters.get("signs");
			
			MatchingRule matchingRule = new MatchingRule();
			matchingRule.clazz = clazz;
			matchingRule.loadMethods(methods);
			matchingRule.loadSigns(signs);
			
			inheritedNodeProbesRel.addRelation(matchingRule, nodeProbe);
		} else if (type.equals("replace")) {
			String clazz = (String) parameters.get("class");
			String newClazz = (String) parameters.get("new-class");
			
			replacedClasses.put(clazz, newClazz);
		}



	}

	private IProbe registerProbe(String probeClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException 
	{
		IProbe resp = null;

		if (!registeredProbesNames.contains(probeClassName)) {

			// Construyo por reflexion
			Class clazz=null;
			try {
				clazz = Class.forName(probeClassName);
			} catch (ClassNotFoundException e) {
				System.out.println("Class not found: "+probeClassName);
				throw e;
			}
			IProbe probe = (IProbe) clazz.newInstance();
			probe.init();
			
			Integer index = new Integer(registeredProbes++); 
			probeIndexRel.put(index, probe);
			probeIndexRel.put(probe, index);
			probeClassNameProbeRel.put(probeClassName,probe);
			resp = probe;
			registeredProbesNames.add(probeClassName);
		} else {
			resp = (IProbe) probeClassNameProbeRel.get(probeClassName);
		}

		return resp;
	}

	/*
	private boolean isClassInstrumentalized(String className) {
		for (int i=0;i<instClassPatterns.size();i++) {
			String classPattern = (String) instClassPatterns.get(i);

			Pattern p = Pattern.compile(classPattern);
			Matcher m = p.matcher(className);
			if (m.matches()) {
				return true;
			}
		}		
		return false;
	}
	 */


	public static byte[] load(String name, byte[] b) 
	{
		if (name.startsWith("sondas.")) {
			return b;
		}
		
		if (replacedClasses.containsKey(name)) {
			try {
				String replacingClass = (String)replacedClasses.get(name);
				Logger.write("Replacing class "+name+" by "+replacingClass);
				File file=new File(replacingClass);
				int len = (int)file.length();
				byte buffer[] = new byte[len];
				
				FileInputStream fis = new FileInputStream(file);
				fis.read(buffer);
				fis.close();
				b = buffer;
			} catch (Exception e) {
				Logger.write("Couldn't replace class "+name);
				e.printStackTrace();
			}
		}
		
		ClassReader cr = new ClassReader(b);
		ClassInstrumentalizer scv = new ClassInstrumentalizer();

		cr.accept(scv,ASMifierClassVisitor.getDefaultAttributes(), 0 /*ClassReader.SKIP_DEBUG*/);
		b = scv.getInstrumentalizedClass();

		return b;
	}

	/**
	 * Obtiene todos los indices de sonda asociados con el metodo
	 * Si un metodo esta sondeado por la misma sonda varias veces, solo aparerecera en la lista de respuesta una vez
	 */
	public List getMethodProbesIndexes(String className, String methodName, String signature, String superClassName) 
	{
		HashSet aux = new HashSet();
		String fqMethod = SondasUtils.getFqMethod(className,methodName,signature);

		// NodeIndexBean
		List resp = new ArrayList();

		Set matchingRules;

		// REGEX
		matchingRules = (Set) regexNodeProbesRel.getKeys();
		for (Iterator it=matchingRules.iterator();it.hasNext();) {
			MatchingRule matchingRule = (MatchingRule) it.next();
			String pattern = matchingRule.regexPattern;
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(fqMethod);
			if (m.matches()) {
				List nodesProbes = regexNodeProbesRel.getReferences(matchingRule);  
				for (int i=0;i<nodesProbes.size();i++) {
					NodeProbeBean npb = (NodeProbeBean) nodesProbes.get(i);
					if (npb.probe.hasToInstrumentalize(methodName, signature)) {
						Logger.write("Instrumentalizing by Regex "+fqMethod+" with probe "+npb.probeName);
						Integer index = (Integer) probeIndexRel.get(npb.probe);
						if (!aux.contains(index)) {
							aux.add(index);
							resp.add(new NodeIndexBean(npb.nodeId, index, npb.probe));
						}
					}
				}
			}
		}

		// INHERITED
		matchingRules = (Set) inheritedNodeProbesRel.getKeys();
		for (Iterator it=matchingRules.iterator();it.hasNext();) {
			MatchingRule matchingRule = (MatchingRule) it.next();
			String parentCandidateClass = matchingRule.clazz;

			if (superClassName.equals(parentCandidateClass) 
					&& (matchingRule.methods.size()==0 || matchingRule.methods.contains(methodName))
					&& (matchingRule.signs== null || matchingRule.signs.contains(signature))
				) {
				// El metodo actual pertenece a una clase que hereda directamente de una de las clases INHERITED definidas en el PRB
				// y el metodo esta definido dentro de la clausula methods o no existe clausula methods
				List nodesProbes = inheritedNodeProbesRel.getReferences(matchingRule);
				for (int i=0;i<nodesProbes.size();i++) {
					NodeProbeBean npb = (NodeProbeBean) nodesProbes.get(i);
					if (npb.probe.hasToInstrumentalize(methodName, signature)) {
						Logger.write("Instrumentalizing by Inherited "+fqMethod+" with probe "+npb.probeName);
						Integer index = (Integer) probeIndexRel.get(npb.probe);
						if (!aux.contains(index)) {
							aux.add(index);
							resp.add(new NodeIndexBean(npb.nodeId, index, npb.probe));
						}
					}
				}
			}

		}

		return resp;
	}

	static public IProbe getProbe(int probeIndex) {
		IProbe probe = (IProbe) probeIndexRel.get(new Integer(probeIndex));
		return probe;	
	}
	

	public static void main(String[] args) {
		SondasLoader sl = new SondasLoader();
	}
	
	static public List getMethodNames(int startIndex) {
		synchronized (methodNames) {
			if (startIndex<methodNames.size()) {
				ArrayList copyList = new ArrayList();
				for (int i=startIndex;i<methodNames.size();i++) {
					copyList.add(methodNames.get(i));
				}
				return copyList;
			} else {
				return null;
			}
		}
	}
	
	static public void setMethodName(int id, String name) {
		synchronized (methodNames) {
			methodNames.add(name);
		}
	}
	
	static public void setMethodNames(List methodNames) {
		synchronized (methodNames) {
			SondasLoader.methodNames=methodNames;
		}
	}
	
	
}
