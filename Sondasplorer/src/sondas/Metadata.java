package sondas;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sondas.utils.SondasUtils;

public class Metadata implements Serializable {
	public List methodNames;
	
	public Metadata() {
		
	}
	
	public Metadata(DataInputStream dis) throws IOException
	{
		readMethodNames(dis);
	}
	
	public void marshall(int pid, DataOutputStream dos) throws IOException
	{
		dos.writeShort(pid);
		writeMethodNames(dos);
	}
	
	private void writeMethodNames(DataOutputStream dos) throws IOException {
		if (methodNames!=null) {
			dos.writeInt(methodNames.size());
			for (Iterator it=methodNames.iterator();it.hasNext();) {
				String methodName = (String) it.next();
				SondasUtils.writeString(methodName, dos);
			}
		} else {
			dos.writeInt(0);
		}
	}
	
	private void readMethodNames(DataInputStream dis) throws IOException {
		methodNames = new ArrayList();
		int count = dis.readInt();
		for (int i=0;i<count;i++) {
			String methodName = SondasUtils.readString(dis);
			methodNames.add(methodName);
		}
	}
	
}


