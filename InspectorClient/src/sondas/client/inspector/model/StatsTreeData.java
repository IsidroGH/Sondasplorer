package sondas.client.inspector.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sondas.inspector.StringsCache;
import sondas.utils.SondasUtils;

/**
 * Clase que encapsula todos los hijos de un nodo del arbol StatsTree
 */
public class StatsTreeData {
	private int metricType;
	private List<String> statsNames=new ArrayList<String>();
	private List<String> statsTitles=new ArrayList<String>();
	private List<String> statsUnits=new ArrayList<String>();
	private List<String> statsFormats=new ArrayList<String>();
	private List<Boolean> isAccList=new ArrayList<Boolean>();
	private List<Integer> categories=new ArrayList<Integer>();
	private String name;
	private int id;
	
	
	
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		dos.writeInt(metricType);
		SondasUtils.marshallStringList(statsNames, dos, stringsCache);
		SondasUtils.marshallStringList(statsTitles, dos, stringsCache);
		SondasUtils.marshallStringList(statsUnits, dos, stringsCache);
		SondasUtils.marshallStringList(statsFormats, dos, stringsCache);
		SondasUtils.marshallBooleanList(isAccList, dos);
		SondasUtils.marshallIntegerList(categories, dos, stringsCache);
		SondasUtils.writeCachedString(name, dos, stringsCache);
		dos.writeInt(id);
	}
	
	public StatsTreeData(DataInputStream dis, StringsCache stringsCache) throws IOException {
		metricType = dis.readInt();
		statsNames = SondasUtils.unmarshallStringList(dis, stringsCache);
		statsTitles = SondasUtils.unmarshallStringList(dis, stringsCache);
		statsUnits = SondasUtils.unmarshallStringList(dis, stringsCache);
		statsFormats = SondasUtils.unmarshallStringList(dis, stringsCache);
		isAccList = SondasUtils.unmarshallBooleanList(dis);
		categories = SondasUtils.unmarshallIntegerList(dis, stringsCache);
		name = SondasUtils.readCachedString(dis, stringsCache);
		id = dis.readInt();
	}
	
	
	public int getMetricType() {
		return metricType;
	}
	
	public String getName() {
		return name;
	}
	
	public int getMetricKey() {
		return id;
	}
	
	public int getCategoriesCount() {
		return categories.size();
	}
	
	/**
	 * Name contiene el nombre del nodo (leaf)
	 * pej:
	 * 	methodname
	 *  pstmt|query
	 * @param name
	 */
	public StatsTreeData(int metricType, String name, int id) {
		this.metricType=metricType;
		this.name=name;
		this.id=id;
	}
	
	
	
	/**
	 * Devuelve todos los identificadores de estadisticas que tiene definido
	 * pej: totTime, exTime, etc
	 * @return
	 */
	public List<String> getStatNames() {
		return statsNames;
	}
	
	public List<String> getStatTitles() {
		return statsTitles;
	}
	
	public List<String> getStatUnits() {
		return statsUnits;
	}
	
	public List<String> getStatFormats() {
		return statsFormats;
	}
	
	public List<Boolean> getIsAccList() {
		return isAccList;
	}
	
	
	public List<Integer> getCategories() {
		return categories;
	}
	
	public int getStatsCount() {
		return statsNames.size();
	}
	
	public void registerStat(String statName,  String title, String unit) {
		registerStat(statName, title, unit,null, false);
	}
	
	public void registerStat(String statName,  String title, String unit, boolean isAcc) {
		registerStat(statName, title, unit, null, isAcc);
	}
	
	public void registerStat(String statName,  String title, String unit, String format, boolean isAcc) {
		statsNames.add(statName);
		statsTitles.add(title);
		statsUnits.add(unit);
		statsFormats.add(format);
		isAccList.add(isAcc);
	}
	
	public void registerNode(int categoryId) {
		categories.add(categoryId);
	}
	
	public boolean hasNode(int categoryId) {
		return categories.contains(categoryId);
	}
}
