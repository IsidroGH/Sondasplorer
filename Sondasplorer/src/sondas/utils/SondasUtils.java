package sondas.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import sondas.Global;
import sondas.MethodInstrumentalizer;
import sondas.inspector.Constants;
import sondas.inspector.StringsCache;

public class SondasUtils 
{
	private static int readStringBuffersSize=1024;

	private static final ThreadLocal readStringBuffers = new ThreadLocal() {
		protected Object initialValue()
		{
			return new byte[readStringBuffersSize];
		}
	};

	static public void marshallStringKeyMap(Map map, DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeInt(map.size());
		Set keys = map.keySet();

		for (Iterator it=keys.iterator();it.hasNext();) {
			String key = (String) it.next();
			ICachedMarshable value = (ICachedMarshable) map.get(key);
			writeCachedString(key, dos, stringsCache);
			value.marshall(dos, stringsCache);
		}
	}

	static public Map unmarshallStringKeyMap(DataInputStream dis, IUnmarshaller vu, StringsCache stringsCache) throws IOException 
	{
		HashMap map = new HashMap();

		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			String key = readCachedString(dis, stringsCache);
			Object value = vu.unmarshall(dis, stringsCache);
			map.put(key,value);
		}

		return map;
	}

	static public String readString(DataInputStream dis) throws IOException 
	{
		int lng = dis.readShort();

		byte buffer[];

		if (lng>readStringBuffersSize) {
			if (lng>100000) {
				throw new IllegalStateException("Excessive string size: "+lng);
			} else {
				buffer = new byte[lng]; 
			}
		} else {
			buffer = (byte[]) readStringBuffers.get();
		}

		int readed=0;		
		do {
			readed+=dis.read(buffer, readed, lng-readed);
		} while(readed<lng);

		return new String(buffer,0,lng);
	}

	static public void writeString(String text, DataOutputStream dos) throws IOException {
		int lng = text.length();

		dos.writeShort(lng);
		// Request
		dos.writeBytes(text);
	}

	/**
	 * Si no esta cacheado lo cachea
	 */
	static public String readCachedString(DataInputStream dis, StringsCache stringsCache) throws IOException
	{
		byte aux = dis.readByte();
		if (aux==Constants.Cached) {
			// Cacheado
			int id = dis.readInt();
			return (String)stringsCache.idMethodRel.get(id);
		} else if (aux==Constants.Null) {
			return null;
		} else {
			// No cacheado
			String str = readString(dis);
			int newId = dis.readInt();
			if (newId!=-1) {
				stringsCache.addString(newId, str);
			}
			return str;
		}
	}

	/*
	static public void writeCachedString(String text, DataOutputStream dos, StringsCache stringsCache) throws IOException {
		System.out.println("debug profiler quitar");
		int lng = text.length();

		dos.writeShort(lng);
		// Request
		dos.writeBytes(text);
	}*/

	/*
	static public void writeCachedString(String text, DataOutputStream dos, StringsCache stringsCache) throws IOException 
	{
			if (text==null) {
				dos.writeByte(Constants.Null);
				return;
			}


			dos.writeShort(text.length());
			// Request
			dos.writeBytes(text);
	}*/


	static public void writeCachedString(String text, DataOutputStream dos, StringsCache stringsCache) throws IOException 
	{
		if (text==null) {
			dos.writeByte(Constants.Null);
			return;
		}

		Integer id = (Integer) stringsCache.methodIdRel.get(text);
		if (id==null) {
			dos.writeByte(Constants.NoCached); // No cache
			// Envio el texto del metodo y lo meto en cache
			// Length
			dos.writeShort(text.length());
			// Request
			dos.writeBytes(text);

			int newId = stringsCache.addString(text);
			dos.writeInt(newId);
		} else {
			// El texto esta en cache
			dos.writeByte(Constants.Cached); // Si cache
			dos.writeInt(id.intValue());
		}
	}

	static public Object newInstance(String className) {
		try {
			Class clazz = Class.forName(className);
			return  clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Error instanciating class "+className, e);
		}
	}

	public static String getFqMethod(String clazz, String method, String sign) {
		return clazz+"."+method+"#"+sign;
	}

	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}

	public static void writeHeader(Map categories,  DataOutputStream dos) throws IOException {
		writeCategories(categories, dos);
	}

	public static void readHeader(Map categories, DataInputStream dis) throws IOException {
		readCategories(categories, dis);
	}

	/*
	private static void writeMethodNames(Map methodsMap, DataOutputStream dos) throws IOException {
		dos.writeInt(methodsMap.size());
		for (Iterator it=methodsMap.keySet().iterator();it.hasNext();) {
			int methodId = (Integer) it.next();
			String name =(String)methodsMap.get(methodId);
			dos.writeShort(methodId);
			writeString(name, dos);
		}
	}
	
	private static void readMethodNames(Map methodsMap, DataInputStream dis) throws IOException {
		int count = dis.readInt();
		for (int i=0;i<count;i++) {
			int id = dis.readShort();
			String name = readString(dis);
			methodsMap.put(id,name);
		}
	}*/

	/**
	 * Lee Name,Id 
	 * Escribe Name, Id
	 */
	private static void writeCategories(Map map, DataOutputStream dos) throws IOException {
		dos.writeInt(map.size());
		for (Iterator it=map.keySet().iterator();it.hasNext();) {
			String catName = (String) it.next();
			int catId = Integer.valueOf((Integer)map.get(catName));
			writeString(catName, dos);
			dos.writeShort(catId);
		}
	}
	
	/**
	 * Lee Name,Id 
	 * Devuelve Name, Id
	 */
	private static void readCategories(Map categories, DataInputStream dis) throws IOException {
		int count = dis.readInt();
		for (int i=0;i<count;i++) {
			String catName = readString(dis);
			int catId = dis.readShort();
			categories.put(catName,catId);
		}
	}

	public static List parseCvsString(String csv) {
		ArrayList resp = new ArrayList();
		StringTokenizer st = new StringTokenizer(csv,",");
		while (st.hasMoreTokens()) {
			resp.add(st.nextToken());
		}
		return resp;
	}

	public static void marshallStringList(List<String> list, DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeInt(list.size());
		for (String item:list) {
			writeCachedString(item, dos,stringsCache);
		}
	}

	public static List<String> unmarshallStringList(DataInputStream dis, StringsCache stringsCache) throws IOException {
		ArrayList<String> resp = new ArrayList<String>();
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			String item = readCachedString(dis, stringsCache);
			resp.add(item);
		}

		return resp;
	}

	public static void marshallBooleanList(List<Boolean> list, DataOutputStream dos) throws IOException {
		dos.writeInt(list.size());
		for (boolean item:list) {
			dos.writeBoolean(item);
		}
	}

	public static List<Boolean> unmarshallBooleanList(DataInputStream dis) throws IOException {
		ArrayList<Boolean> resp = new ArrayList<Boolean>();
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			boolean item = dis.readBoolean();
			resp.add(item);
		}

		return resp;
	}


	public static void marshallIntegerList(List<Integer> list, DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeInt(list.size());
		for (int item:list) {
			dos.writeInt(item);
		}
	}

	public static List<Integer> unmarshallIntegerList(DataInputStream dis, StringsCache stringsCache) throws IOException {
		ArrayList<Integer> resp = new ArrayList<Integer>();
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			int item = dis.readInt();
			resp.add(item);
		}

		return resp;
	}

}
