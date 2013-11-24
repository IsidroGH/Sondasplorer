package sondas;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import sondas.utils.MultipleRelation;

public class Config 
{
	private HashMap<String, MultipleRelation> cfgCats = new HashMap<String, MultipleRelation>();
	
	public Config(String cfgFile) throws IOException {
		loadConfiguration(cfgFile);
	}
	
	private void loadConfiguration(String cfgFile) throws IOException 
	{
		String category="";

		BufferedReader input =  new BufferedReader(new FileReader(cfgFile));
		try {
			String line = null; //not declared within while loop

			while (( line = input.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#")) {
					continue;
				}
				
				if (line.equals("")) {
					continue;
				}

				if (line.startsWith("[")) {
					category=line.substring(1, line.length()-1);
					cfgCats.put(category, new MultipleRelation());
				} else {
					StringTokenizer st = new StringTokenizer(line,"=");
					((MultipleRelation)cfgCats.get(category)).addRelation(st.nextToken(), st.nextToken());
				}
			}
		} finally {
			input.close();
		}
	}
	
	public MultipleRelation getCategory(String category) {
		return (MultipleRelation)cfgCats.get(category);
	}
	
	/**
	 * Devuelve el primer elemento de los que puedan haber definidos
	 * Este metodo sirve para obtener una propiedad no multiple.
	 * Para obtener multiples propiedades se debe usar el metodo getMultipleProperty
	 */
	public String getProperty(String category, String propertyName) {
		MultipleRelation mr = cfgCats.get(category);
		if (mr==null) {
			throw new RuntimeException("Category no defined: Category:"+category);
		}
		
		List ref = mr.getReferences(propertyName);
		
		if (ref==null || ref.size()==0 || ref.get(0)==null) {
			throw new RuntimeException("Property not defined. Category:"+category+", Name:"+propertyName);
		} 
		
		return (String)ref.get(0);
	}
	
	public String getNullableProperty(String category, String propertyName) {
		MultipleRelation mr = cfgCats.get(category);
		if (mr==null) {
			return null;
		}
		
		List ref = mr.getReferences(propertyName);
		
		if (ref==null || ref.size()==0 || ref.get(0)==null) {
			return null;
		} 
		
		return (String)ref.get(0);
	}
	
	public boolean hasProperty(String category, String propertyName) {
		MultipleRelation mr = cfgCats.get(category);
		if (mr==null) {
			return false;
		}
		
		List ref = mr.getReferences(propertyName);
		
		if (ref==null || ref.size()==0 || ref.get(0)==null) {
			return false;
		} 
		
		return true;
	}
	
	public List getMultipleProperty(String category, String propertyName) 
	{
		return cfgCats.get(category).getReferences(propertyName);
	}
	
	public int getPropertyAsInteger(String category, String propertyName) {
		String aux = getProperty(category, propertyName);
		return Integer.parseInt(aux);
	}
	
	public int getPropertyAsInteger(String category, String propertyName, int defaultValue) {
		String aux = getNullableProperty(category, propertyName);
		if (aux==null) {
			return defaultValue;
		} else {
			return Integer.parseInt(aux);
		}
	}
	
	public boolean getPropertyAsBoolean(String category, String propertyName) {
		return getProperty(category, propertyName).equals("1");
	}
}
