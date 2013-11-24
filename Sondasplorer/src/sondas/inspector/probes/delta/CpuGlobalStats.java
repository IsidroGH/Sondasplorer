package sondas.inspector.probes.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import sondas.inspector.StringsCache;
import sondas.inspector.delta.BaseGlobalStats;
import sondas.inspector.delta.IGlobalStats;
import sondas.utils.SondasUtils;

class CpuStats implements Serializable {
	float accCpu;
	int count;
	
	CpuStats(float accCpu, int count) {
		this.accCpu=accCpu;
		this.count=count;
	}
	
	void merge(CpuStats _stats) {
		accCpu+=_stats.accCpu;
		count+=_stats.count;
	}
	
	float getValue() {
		if (count!=0) {
			return accCpu/count;
		} else {
			return 0;
		}
	}
	
	void update (float value) {
		accCpu+=value;
		count++;
	}
	
	public CpuStats clone() {
		return new CpuStats(accCpu, count);
	}
	
	public CpuStats getDiferential(CpuStats _stats) {
		return new CpuStats(accCpu-_stats.accCpu, count-_stats.count);
	}
}

public class CpuGlobalStats extends BaseGlobalStats
{
	private Map<String, CpuStats> cpuPerEntry;
	
	public CpuGlobalStats(int id, int metricType, int categoryId) {
		super(id,"Servlets CPU","Cpu",metricType, categoryId);
		cpuPerEntry = new HashMap<String, CpuStats>();
	}

	private CpuGlobalStats(IGlobalStats gs) {
		super(gs);
		cpuPerEntry = new HashMap<String, CpuStats>();
	}
	
	/**
	 * Devuelve todas las entradas registradas en este global.
	 * En este caso, todos los servlets (ej: GET/MiServlet) sobre los que se está monitorizando la CPU
	 */
	public Set<String> getEntryNames() {
		return cpuPerEntry.keySet();
	}
	
	public void updateCpu(String entryId, long iniCpu, long iniTs, long endCpu, long endTs) {
		CpuStats stats = cpuPerEntry.get(entryId);
		
		float cpu;
		
		if (endCpu-iniCpu==0) {
			cpu=0;
		} else {
			cpu = (endCpu-iniCpu)/(float)((endTs-iniTs)*1000000);
			if (cpu>1) {
				cpu=1;
			}
		}
		
		
		
		if (stats!=null) {
			stats.update(cpu);
		} else {
			stats = new CpuStats(cpu, 1);
			cpuPerEntry.put(entryId, stats);
		}
	}
	
	/**
	 * statsName = totTime
	 * nodeName = GET/MiServlet
	 */
	public float getMetricValue(String key, String statsName) {
		if (cpuPerEntry.containsKey(key)) {
			CpuStats stats = cpuPerEntry.get(key);
			return stats.getValue()*100;
		} else {
			return 0;
		}
	}
	
	public void merge(IGlobalStats _gs) {
		CpuGlobalStats _cpuGs = (CpuGlobalStats) _gs;
		
		for (String key:_cpuGs.cpuPerEntry.keySet()) {
			CpuStats _val = _cpuGs.cpuPerEntry.get(key);
			
			if (!cpuPerEntry.containsKey(key)) {
				cpuPerEntry.put(key, _val);
			} else {
				CpuStats val = cpuPerEntry.get(key);
				val.merge(_val);
			}
		}
	}
	
	public CpuGlobalStats(DataInputStream dis, StringsCache stringsCache, int metricType) throws IOException 
	{
		super(dis,stringsCache, metricType);
		cpuPerEntry = new HashMap<String, CpuStats>();
		int size = dis.readInt();
		for (int i=0;i<size;i++) {
			String key = SondasUtils.readCachedString(dis, stringsCache);
			float val = dis.readFloat();
			int count = dis.readInt();
			CpuStats stats = new CpuStats(val, count);
			cpuPerEntry.put(key, stats);
		}
	}

	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException 
	{
		super.marshall(dos, stringsCache);
		dos.writeInt(cpuPerEntry.size());
		for (String key:cpuPerEntry.keySet()) {
			CpuStats val = cpuPerEntry.get(key);
			SondasUtils.writeCachedString(key, dos, stringsCache);
			dos.writeFloat(val.accCpu);
			dos.writeInt(val.count);
		}
	}
	
	public IGlobalStats getDiferential(IGlobalStats _gs) {
		// Creo la respuesta con el diferencial de la Global
		CpuGlobalStats resp = new CpuGlobalStats((BaseGlobalStats)super.getDiferential(_gs));
		
		CpuGlobalStats _cpuGs = (CpuGlobalStats) _gs;
		
		for (Iterator it=cpuPerEntry.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			CpuStats value = (CpuStats) cpuPerEntry.get(key);

			if (!_cpuGs.cpuPerEntry.containsKey(key)) {
				resp.cpuPerEntry.put(key, value.clone());
			} else {
				resp.cpuPerEntry.put(key, value.getDiferential(_cpuGs.cpuPerEntry.get(key)));
			}			
		}
		
		return resp;
	}
	
	public Object clone() {
		CpuGlobalStats resp = new CpuGlobalStats(this);
		
		for (Iterator it=cpuPerEntry.keySet().iterator();it.hasNext();) {
			String key = (String)it.next();
			CpuStats value = (CpuStats) cpuPerEntry.get(key);
			resp.cpuPerEntry.put(key,value.clone());
		}
		
		return resp;
	}
	
	public boolean isEquivalent(IGlobalStats _gs) {
		CpuGlobalStats _cpuGs = (CpuGlobalStats) _gs;
		
		if (cpuPerEntry.size()!=_cpuGs.cpuPerEntry.size()) {
			return false;
		}
		
		for (String key:cpuPerEntry.keySet()) {
			CpuStats stats = cpuPerEntry.get(key);
			
			if (!_cpuGs.cpuPerEntry.containsKey(key)) {
				return false;
			}
			
			if (_cpuGs.cpuPerEntry.get(key).accCpu!=stats.accCpu) {
				return false;
			}
			
			if (_cpuGs.cpuPerEntry.get(key).count!=stats.count) {
				return false;
			}
		}
		
		return true;
	}
}
