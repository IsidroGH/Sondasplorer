package sondas.client.inspector.view;

import java.util.ArrayList;
import java.util.List;

public class TableData {
	private int cols;
	private ArrayList<Object[]> rows;
	private ArrayList<Integer> rowKeys;
	
	public TableData(int cols) {
		this.cols = cols;
		rows = new ArrayList<Object[]>();
		rowKeys = new ArrayList<Integer>();
	}
	
	public List<Integer> getRowKeys() {
		return rowKeys;
	}
	
	public void addRow(int key, Object...rowVals) {
		
		if (rowVals.length!=cols) {
			throw new RuntimeException("Row size mismatch: "+rowVals.length);
		}
		rowKeys.add(key);
		rows.add(rowVals);
	}
	
	/*
	public void addRow(Object...rowVals) {
		
		if (rowVals.length!=cols) {
			throw new RuntimeException("Row size mismatch: "+rowVals.length);
		}
		rows.add(rowVals);
	}
	*/
	
	public Object[][] getData() {
		Object[][] resp = new Object[rows.size()][cols];
		
		int i=0;
		for (Object[] row:rows) {
			resp[i++]=row;
		}
		
		return resp;
	}
}
