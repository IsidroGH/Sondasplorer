package sondas.client.inspector.view;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class TimeGraphicPanel extends ChartPanel {

	private TimeSeries series[];
	private String title;
	private String xTitle;
	private String yTitle;
	private String format;
	
	private static Font titleFont = new Font("Arial", Font.PLAIN, 14);

	public TimeGraphicPanel(String title, String xTitle, String yTitle,String[] seriesNames, String format) {
		super(null);
		series = new TimeSeries[seriesNames.length];
		this.title=title;
		this.xTitle=xTitle;
		this.yTitle=yTitle;
		this.format=format;

		for (int i=0;i<seriesNames.length;i++) {
			series[i] = new TimeSeries(seriesNames[i], FixedMillisecond.class);
		}

		generate();
	}

	public void setPoint(int index, long ts, float y) {
		series[index].add(new FixedMillisecond(ts), y);
	}

	public void setSerie(int index, long ts[], float y[]){
		int count = ts.length;
		for (int i=0;i<count;i++){
			series[index].add(new FixedMillisecond(ts[i]), y[i]);
		}
	}

	private void generate() {
		TimeSeriesCollection dataSet= new TimeSeriesCollection();
		for (int i=0;i<series.length;i++) {
			dataSet.addSeries(series[i]);
		}

		JFreeChart chart = 
			ChartFactory.createTimeSeriesChart(title,  
					xTitle,yTitle,dataSet,
					true,
					true, 
					true               
			);

		chart.setBackgroundPaint(Color.white);
		chart.getTitle().setPaint(Color.black);
		chart.getTitle().setFont(titleFont);

		XYPlot plot = (XYPlot) chart.getPlot();
		XYLineAndShapeRenderer renderer= (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setShapesVisible(true);
		renderer.setShapesFilled(true);
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);
		plot.setDomainGridlinePaint(Color.gray);

		renderer.setPaint(Color.blue);
		
		if (format!=null) {
			Axis yAxis = plot.getRangeAxis();
			if (yAxis != null && yAxis instanceof NumberAxis) {
				NumberAxis na = (NumberAxis) yAxis;

				DecimalFormat df = new DecimalFormat(format);
				na.setNumberFormatOverride(df);
			}
		}

		DateAxis axis = (DateAxis) plot.getDomainAxis();
		//axis.setDateFormatOverride(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"));
		axis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));

		setChart(chart);
	}

	public void clear() {
		for (int i=0;i<series.length;i++) {
			series[i].clear();
		}
	}

	public static void main(String args[]) 
	{
		TimeGraphicPanel gp = new TimeGraphicPanel("Gráficas","XTitle","YTitle",new String[]{"Serie1","Serie2"},"");

		gp.setPoint(0, 1, 23);
		/*
		gp.setPoint(0, 2, 43);
		gp.setPoint(0, 3, 73);
		gp.setPoint(0, 4, 13);
		gp.setPoint(0, 5, 23);
		gp.setPoint(0, 6, 53);
		gp.setPoint(0, 7, 33);

		gp.setPoint(1, 1, 93);
		gp.setPoint(1, 2, 73);
		gp.setPoint(1, 3, 53);
		gp.setPoint(1, 4, 73);
		gp.setPoint(1, 5, 83);
		gp.setPoint(1, 6, 43);
		gp.setPoint(1, 7, 23);
		 */
		/*
		gp.clear();
		gp.setPoint(0, 1, 23);
		gp.setPoint(1, 7, 23);
		 */

		JFrame frame = new JFrame("Test");
		frame.add(gp);
		frame.pack();
		frame.setVisible(true);
	}

}


