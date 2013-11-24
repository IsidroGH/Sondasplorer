package sondas.inspector;

import java.util.HashMap;

import sondas.Global;

/**
 * Cache finita de cadenas.
 * El tamaño de la cache viene determinado por la propiedad Sondasplorer stringscache.size
 * @author Isidro
 *
 */
public class StringsCache 
{
	public HashMap methodIdRel;
	public HashMap idMethodRel;
	public int currentId;
	private static int MaxSize;

	static {
		MaxSize=-1;
		if (Global.isConfigLoaded()) {
			MaxSize = Global.getPropertyAsInteger("Agent", "stringscache.size");
		}
	} 


	public StringsCache() {
		methodIdRel= new HashMap();
		idMethodRel= new HashMap();
		currentId=0;
	}

	public void clear() {
		methodIdRel.clear();
		idMethodRel.clear();
		currentId=0;
	}

	/**
	 * Almacena el metodo en la cache y devuelve su id asociado
	 * En caso de que la cache este llena no se almacena y devuelve -1 como id
	 */
	public int addString(String method) {
		if (currentId<MaxSize || MaxSize==-1) {
			int newId = currentId;
			methodIdRel.put(method, new Integer(currentId++));
			idMethodRel.put(newId, method);
			return newId;
		} else {
			return -1;
		}
	}

	public void addString(int id, String method) {
		idMethodRel.put(id, method);
		methodIdRel.put(method, id);
	}
}
