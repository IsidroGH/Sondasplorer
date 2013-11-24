package sondas.inspector.probes.full;

import java.io.DataOutputStream;
import java.io.IOException;

import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.utils.SondasUtils;

public class SqlTrace extends MethodTrace 
{
	final private static int threshold = Integer.parseInt(InspectorRuntime.getProperty("sqlprobe.threshold"));
	final private static int maxParamLength = Integer.parseInt(InspectorRuntime.getProperty("sqlprobe.max_param_length"));
	final static private String prefix=String.valueOf(InspectorRuntime.getInstance().getMetricType("SqlMetric")+":");
	
	private String sql;
	private String sqlParams;
	
	public SqlTrace (MethodTrace mt) {
		super(mt);
		
	}
	
	public SqlTrace(String fqMethod, int mode, String sql, Object[] sqlParams) 
	{
		super(fqMethod, mode);

		this.sql = sql;
		
		StringBuilder sb = new StringBuilder();
		
		for (int i=0;i<sqlParams.length;i++) {
			String param = sqlParams[i].toString();
			if (param.length()>maxParamLength) {
				// Como máximo registro maxParamLength bytes del parametro
				param = param.substring(0, maxParamLength);
			}
			sb.append(param).append(";");
		}
		this.sqlParams = sb.toString();
	}
	
	public void marshall(DataOutputStream dos) throws IOException 
	{
		super.marshall(dos);
		StringsCache stringsCache = (StringsCache)methodsCache.get();
		SondasUtils.writeCachedString(sql, dos, stringsCache);
		SondasUtils.writeCachedString(sqlParams, dos, stringsCache);
	}
}
