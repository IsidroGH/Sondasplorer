package sondas.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Implementa una relacion 1:n
 */
public class MultipleRelation {
	
	// item:List<items>
	private HashMap map;
	
	public MultipleRelation() {
		map = new HashMap();
	}
	
	public Set getKeys() {
		return map.keySet();
	}
	
	public boolean containsRelation(Object item, Object reference) {
		List list = (List)map.get(item);
		if (list==null) {
			return false;
		} else {
			return list.contains(reference);
		}
	}
	
	public void addRelation(Object item, Object reference) {
		List list = (List)map.get(item);
		if (list==null) {
			list = new ArrayList();
			map.put(item, list);
		} 
		
		list.add(reference);
	}
	
	/**
	 * Devuelve todas las referencias
	 */
	public List getReferences(Object item) {
		List list = (List)map.get(item);
		if (list==null) {
			list = new ArrayList();
			map.put(item, list);
		}
		
		return list;
	}
	
	/**
	 * Devuelve la primera referencia
	 */
	public Object getReference(Object item) {
		List list = (List)map.get(item);
		if (list==null) {
			return null;
		} else {
			return list.get(0);
		}
	}
}
