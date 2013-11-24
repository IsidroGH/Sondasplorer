package sondas.client.inspector.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import sondas.client.inspector.ClientException;
import sondas.dispatcher.inspector.IDeltaNodeFactory;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.StringsCache;
import sondas.inspector.delta.DeltaTree;
import sondas.inspector.delta.IGlobalStats;
import sondas.utils.SondasUtils;

public class ClientDeltaComposer {
	public static void unmarshallGlobalStatsMap(DataInputStream dis, StringsCache stringsCache, Map<Integer,IGlobalStats> globalStatsMap) throws IOException {
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			int key = dis.readInt();
			int metricType = dis.readShort();
			IDeltaNodeFactory factory = InspectorRuntime.getDeltaProbeFactory(metricType);
			IGlobalStats value = factory.createGlobalStats(dis, stringsCache, metricType);
			//GlobalStats value = new GlobalStats(dis,stringsCache,metricType);
			globalStatsMap.put(key,value);
		}
	}
	
	public static void unmarshallTimedGlobalStatsMaps(DataInputStream dis, StringsCache stringsCache, Map<Long/*TimeStamp*/, Map<Integer,IGlobalStats> > timedGlobalStatsMaps) throws IOException {
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			long key = dis.readLong();
			Map<Integer,IGlobalStats> value = new HashMap<Integer,IGlobalStats>();
			unmarshallGlobalStatsMap(dis, stringsCache, value);
			timedGlobalStatsMaps.put(key,value);
		}
	}
	
	public static void unmarshallStatsTreeDataMap(DataInputStream dis, StringsCache stringsCache, Map<String, StatsTreeData> statsTreeDataMap) throws IOException {
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			String key = SondasUtils.readCachedString(dis, stringsCache);
			StatsTreeData value = new StatsTreeData(dis,stringsCache);
			statsTreeDataMap.put(key,value);
		}
	}
	
	public static DeltaTree doProcessLicensed(DataInputStream dis, long iniTs, long endTs, Map< Long, Map<Integer,IGlobalStats> > timedGlobalMaps, Map<String, StatsTreeData> statsTreeDataMap, String licensesServer) throws IOException 
	{	
		DeltaTree resultTree = new DeltaTree();
		
		StringsCache stringsCache = new StringsCache();
		byte buffer[]=new byte[16384];

		URL url;
		HttpURLConnection connection = null;  
		try {
			//Create connection
			url = new URL(licensesServer);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches (false);
			connection.setDefaultUseCaches(false);			
			connection.setRequestProperty("Content-Type", "application/octet-stream");		

			//Send request
			DataOutputStream dos = new DataOutputStream (connection.getOutputStream ());
			SondasUtils.writeCachedString("12345678", dos, stringsCache); // License code
			SondasUtils.writeCachedString("combine", dos, stringsCache); // Action

			int len;

			while ((len=dis.read(buffer))!=-1) {
				dos.write(buffer,0,len);
			}

			dos.flush ();
			dos.close ();

			stringsCache.clear();
			
			//Get Response
			try {
				InputStream is = connection.getInputStream();
				DataInputStream licensedDis = new DataInputStream(is);
				
				// Recibo los parametros calculados
				ClientDeltaComposer.unmarshallTimedGlobalStatsMaps(licensedDis, stringsCache, timedGlobalMaps);
				ClientDeltaComposer.unmarshallStatsTreeDataMap(licensedDis, stringsCache, statsTreeDataMap);
						
				// Recibo el deltaTree resultado
				resultTree = DeltaTree.unmarshallComposed(licensedDis, stringsCache);
				
				return resultTree;
			} finally {
				dis.close();				
			}
		} finally {
			if(connection != null) {
				connection.disconnect(); 
			}
		}
	}
}
