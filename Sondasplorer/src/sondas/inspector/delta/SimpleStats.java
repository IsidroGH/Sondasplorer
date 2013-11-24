package sondas.inspector.delta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import sondas.inspector.StringsCache;
import sondas.utils.ICachedMarshable;
import sondas.utils.IMarshable;
import sondas.utils.IUnmarshaller;
import sondas.utils.SondasUtils;

class SimpleStatsUnmarshaller implements IUnmarshaller {
	public Object unmarshall(DataInputStream dis, StringsCache stringsCache) throws IOException {
		return new SimpleStats(dis, stringsCache);
	}
}

public class SimpleStats implements ICachedMarshable, Cloneable, Serializable
{
	final private static IUnmarshaller unmarshaller = new SimpleStatsUnmarshaller();
	
	// Estadisticas
	private int accExTime;
	private int accTotTime;
	private int invCount;
	private int errorCount;
	private int maxTotTime;
	private int maxExTime;
	private int minTotTime=Integer.MAX_VALUE;
	private int minExTime=Integer.MAX_VALUE;

	public SimpleStats() {
		
	}
	
	public float getAvgTotTime() {
		if (invCount!=0)
			return accTotTime/invCount;
		else
			return -1;
	}
	
	public float getAvgExTime() {
		if (invCount!=0)
			return accExTime/invCount;
		else 
			return -1;
	}
	
	public int getInvCount() {
		return invCount;
	}
	
	public int getErrorCount() {
		return errorCount;
	}
	
	public int getMinTotTime() {
		return minTotTime;
	}
	
	public int getMinExTime() {
		return minExTime;
	}
	
	public int getMaxTotTime() {
		return maxTotTime;
	}
	
	public int getMaxExTime() {
		return maxExTime;
	}
	
	public static IUnmarshaller getUnmarshaller() {
		return unmarshaller;
	}
	
	public Object clone() {
		SimpleStats resp = new SimpleStats();
		resp.accExTime=accExTime;
		resp.accTotTime=accTotTime;
		resp.invCount=invCount;
		resp.errorCount=errorCount;
		resp.maxTotTime=maxTotTime;
		resp.minTotTime=minTotTime;
		resp.minExTime=minExTime;
		return resp;
	}
	
	public void update(int totalTime, int exTime, boolean error) {
		accExTime+=exTime;
		accTotTime+=totalTime;
		invCount++;
		if (error) {
			errorCount++;
		}
		
		if (totalTime>maxTotTime) {
			maxTotTime = totalTime;
		}

		if (exTime>maxExTime) {
			maxExTime = exTime;
		}

		if (totalTime<minTotTime) {
			minTotTime = totalTime;
		}

		if (exTime<minExTime) {
			minExTime = exTime;
		}
	}
	
	public void merge(SimpleStats gs) {
		accExTime+=gs.accExTime;
		accTotTime+=gs.accTotTime;
		invCount+=gs.invCount;
		errorCount+=gs.errorCount;
		
		if (gs.maxTotTime>maxTotTime) {
			maxTotTime = gs.maxTotTime;
		}

		if (gs.maxExTime>maxExTime) {
			maxExTime = gs.maxExTime;
		}

		if (gs.minTotTime<minTotTime) {
			minTotTime = gs.minTotTime;
		}

		if (gs.minExTime<minExTime) {
			minExTime = gs.minExTime;
		}
	}
	
	public boolean isEquivalent(SimpleStats _st) {
		return 	(accExTime==_st.accExTime && accTotTime==_st.accTotTime && invCount==_st.invCount &&
				errorCount==_st.errorCount && maxTotTime==_st.maxTotTime && maxExTime==_st.maxExTime &&
				minTotTime==_st.minTotTime && minExTime==_st.minExTime);	
	}
	
	public void marshall(DataOutputStream dos, StringsCache stringsCache) throws IOException {
		// Estadisticas
		dos.writeInt(accExTime);
		dos.writeInt(accTotTime);
		dos.writeInt(invCount);
		dos.writeInt(errorCount);
		dos.writeInt(maxTotTime);
		dos.writeInt(maxExTime);
		dos.writeInt(minTotTime);
		dos.writeInt(minExTime);
	}
	
	public SimpleStats(DataInputStream dis, StringsCache stringsCache) throws IOException {
		// Estadisticas
		accExTime = dis.readInt();
		accTotTime = dis.readInt();
		invCount = dis.readInt();
		errorCount = dis.readInt();
		maxTotTime = dis.readInt();
		maxExTime = dis.readInt();
		minTotTime = dis.readInt();
		minExTime = dis.readInt();
	}
	
	/**
	 * Devuelve un globalStat con la diferencia entre el mismo y el especificado como parametro
	 */
	public SimpleStats getDiferential(SimpleStats _st) {
		SimpleStats resp = new SimpleStats();
		resp.accExTime = accExTime-_st.accExTime;
		resp.accTotTime = accTotTime-_st.accTotTime;
		resp.invCount = invCount-_st.invCount;
		resp.errorCount = errorCount-_st.errorCount;
		// Los maximos y minimos seran los del objeto actual porque si han cambiado
		// se deben exclusivamente a lo nuevo
		resp.maxTotTime = maxTotTime;
		resp.maxExTime = maxExTime;
		resp.minTotTime = minTotTime;
		resp.minExTime = minExTime;
	
		return resp;
	}
}
