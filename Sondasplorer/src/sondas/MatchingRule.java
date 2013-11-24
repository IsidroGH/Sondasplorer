package sondas;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class MatchingRule {
	String regexPattern;
	String clazz;
	Set methods;
	String sign;
	Set signs;

	/**
	 * Recibe como parametro una lista de metodos separados por :
	 */
	void loadMethods(String commaMethods) {
		methods = new HashSet();

		StringTokenizer st = new StringTokenizer(commaMethods,":");
		while (st.hasMoreTokens()) {
			String method = st.nextToken();
			methods.add(method);
		}
	}

	/**
	 * Recibe como parametro una lista de metodos separados por coma
	 */
	void loadSigns(String commaSigns) {
		if (commaSigns!=null) {
			signs = new HashSet();

			StringTokenizer st = new StringTokenizer(commaSigns,":");
			while (st.hasMoreTokens()) {
				String sign = st.nextToken();
				signs.add(sign);
			}
		}
	}
}
