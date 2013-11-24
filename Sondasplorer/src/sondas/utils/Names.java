package sondas.utils;

public class Names {
	
	/**
	 * Convierte un metodo del formato a bajo nivel Java al de alto nivel incluyendo los parametros
	 * Los parametros se separan del metodo mediante un #.
	 * Si name contiene ese simbolo se asume que es un metodo, si no lo contiene
	 * se asume que no es un metodo y no convierte nada.
	 */
	static public  String getDigestedName(String name, boolean withPackage) {
		int i = name.indexOf('#');
		if (i==-1) {
			// No es un metodo fq
			return name;
		}
		
		String className = name.substring(0, i).replaceAll("/", ".");
		if (!withPackage) {
			// Elimino el paquete a la clase
			int pos = className.lastIndexOf('.');
			if (pos!=-1) {
				className = className.substring(pos+1);
			}
		}
		
		String params = name.substring(i+2, name.indexOf(')'));
		String digestedParams = getDigestedParams(params);
		String resp = className+digestedParams;
		return resp;
	}
	
	/**
	 * Convierte un metodo del formato a bajo nivel Java al de alto nivel sin incluir los parametros ni el paquete
	 * Los parametros se separan del metodo mediante un #.
	 * Si name contiene ese simbolo se asume que es un metodo, si no lo contiene
	 * se asume que no es un metodo y no convierte nada.
	 */
	static public  String getClassAndMethod(String fqName) {
		int i = fqName.indexOf('#');
		if (i==-1) {
			// No es un metodo fq
			return fqName;
		}
		
		// paquete+clase+metodo
		String aux1 = fqName.substring(0, i);
		char aux2[] = aux1.toCharArray();
		
		boolean methodFound=false;
		int classMarker=-1;
		
		for (i=aux2.length-1;i>=0;i--) {
			if (aux2[i]=='.') {
				if (!methodFound) {
					methodFound=true;
				} else {
					classMarker=i;
					break;
				}
			}
		}
		
		return aux1.substring(classMarker+1);
	}

	static private String getDigestedParams(String params) {
		StringBuffer resp = new StringBuffer();
		resp.append("(");
		boolean firstParam=true;
		String type=null;
		int arrayDimension=0;

		byte[] bytes = params.getBytes();
		for (int i=0;i<bytes.length;i++) {
			byte car = bytes[i];

			if (car=='[') {
				arrayDimension++;
			} else {
				if (car=='Z'){
					type="boolean";
				} else if (car=='C'){
					type="char";
				} else if (car=='B'){
					type="byte";
				} else if (car=='S'){
					type="short";
				} else if (car=='I'){
					type="int";
				} else if (car=='F'){
					type="float";
				} else if (car=='J'){
					type="long";
				} else if (car=='D'){
					type="double";
				} else if (car=='L'){
					int pos = params.indexOf(';',i+1);
					type = params.substring(i+1, pos);
					type = digestType(type);
					i=pos;
				}

				for (int j=0;j<arrayDimension;j++) {
					type+="[]";
				}

				arrayDimension=0;

				if (!firstParam) {
					resp.append(",");
				} else {
					firstParam=false;
				}

				resp.append(type);
			}                        
		}

		resp.append(")");
		return resp.toString();
	}

	static private String digestType(String type){
		// javax/servlet/http/HttpServletRequest
		int pos = type.lastIndexOf('/');
		if (pos==-1) {
			return type;
		} else {
			return type.substring(pos+1);
		}
	}
	
}
