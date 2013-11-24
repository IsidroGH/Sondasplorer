package sondas.client.inspector.math;

public class Interpolator 
{
	public static DataSet process(long x[], float y[], int pointsCount, boolean isAccum) 
	{
		DataSet resp = new DataSet();
		
		Spline spline = new Spline(x, y);
		
		float scale = x.length/(float)pointsCount;
		int n = x.length;

		if (n<=pointsCount) {
			resp.x = new long[n];
			resp.y = new float[n];

			for (int i=0;i<n;i++) {
				resp.x[i]=x[i];
				resp.y[i]=y[i];
			}
		} else {
			
			resp.x = new long[pointsCount];
			resp.y = new float[pointsCount];
			
			float delta = (x[x.length-1]-x[0])/(float)(pointsCount-1);

			long firstX = x[0];
			for (int i=0;i<pointsCount;i++) {
				long auxX=(long)Math.floor(firstX+(double)delta*i+0.5f);
				float val = (float) spline.spline_value(auxX);

				resp.x[i]= auxX;
				if (isAccum) {
					resp.y[i]= val*scale;
				} else {
					resp.y[i]= val;
				}
			}

		}
		
		return resp;
	}

	public static void main (String[] args) {
		long x[]=new long[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
		float y[]=new float[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};

		DataSet resp = Interpolator.process(x, y, 10, false);
		
		for (int i=0;i<resp.x.length;i++) {
			System.out.println("x'="+resp.x[i]+", y'="+resp.y[i]);
		}
	}
}
