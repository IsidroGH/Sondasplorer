package sondas;

public class BaseExecutionData implements IExecutionData {
	private int id;
	
	public BaseExecutionData() {
		id = this.hashCode();
	}
	
	public int getId() {
		return id;
	}
}
