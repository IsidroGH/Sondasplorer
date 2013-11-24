package sondas.utils;

public class DataSenderBean {
	long deadline; 
	int interval; // Intervalo en mseg
	
	DataSenderBean(int interval) {
		this.interval=interval*1000;
		long ts = System.currentTimeMillis();
		deadline = (ts/this.interval)*this.interval+this.interval;
	}
	
	void updateDeadline(long ts) {;
		deadline = (ts/interval)*interval+interval;
		System.out.println("Update: Ts actual :"+ts+" newDealine: "+deadline);
	}
}
