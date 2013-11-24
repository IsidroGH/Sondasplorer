package sondas.inspector;

import java.io.Serializable;
import java.util.ArrayList;

import sondas.Metadata;

public class InspectorPacket implements Serializable {
	public ArrayList treesList;
	public Metadata metadata;

	public InspectorPacket() {
		treesList = new ArrayList();
	}
}
