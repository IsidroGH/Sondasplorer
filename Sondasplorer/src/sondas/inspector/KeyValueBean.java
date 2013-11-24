package sondas.inspector;

import java.util.StringTokenizer;

public class KeyValueBean {
	public String key;
	public String value;
	
	public KeyValueBean(String line) {
		StringTokenizer st = new StringTokenizer(line,"=");
		key = st.nextToken();
		value = st.nextToken();
	}
}
