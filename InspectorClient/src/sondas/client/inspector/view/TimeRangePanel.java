package sondas.client.inspector.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.jidesoft.swing.RangeSlider;
import com.jidesoft.swing.SelectAllUtils;

class TimeRangePanel extends JPanel {
	protected RangeSlider _rangeSlider;
	private long minTs;
	private long maxTs;
	private long iniTs;
	private long endTs;
	private SimpleDateFormat sdf;
	final JTextField minField;
    final JTextField maxField;
    private int amplitude;
    private IClientEvents client;
    private boolean isEnabled;
	
    public long getIniTs() {
    	return iniTs;
    }
    
    public long getEndTs() {
    	return endTs;
    }
    
    public long getMinTs() {
    	return minTs;
    }
    
    public long getMaxTs() {
    	return maxTs;
    }
    
	private String getDate(long ts) {
		return sdf.format(new Date(ts));
	}
	
	private void regenLimits(int ini, int end) {
		_rangeSlider.setHighValue(end);
		_rangeSlider.setLowValue(ini);
    	iniTs=minTs+(long)(amplitude*(ini/(float)100));
    	endTs=minTs+(long)(amplitude*(end/(float)100));
    	
        minField.setText(getDate(iniTs));
        maxField.setText(getDate(endTs));
	}
	
	public void enable() {
		_rangeSlider.setEnabled(true);
		isEnabled=true;
	}
	
	public void disable() {
		_rangeSlider.setEnabled(false);
		isEnabled=false;
	}
	
	public TimeRangePanel(IClientEvents client, long minTs, long maxTs) 
	{
		this.minTs = minTs;
		this.maxTs = maxTs;
		this.iniTs = maxTs;
		this.endTs = maxTs;
		this.client=client;
		amplitude=(int)(maxTs-minTs);
		minField = new JTextField();
        maxField = new JTextField();
        SelectAllUtils.install(minField);
        SelectAllUtils.install(maxField);

        sdf = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
        
        _rangeSlider = new RangeSlider(0, 100, 0, 100);
        
        _rangeSlider.setPaintTicks(true);
        _rangeSlider.setMajorTickSpacing(10);
        
        _rangeSlider.addMouseListener(new MouseAdapter() {
			
			public void mouseReleased(MouseEvent e) {
				if (isEnabled) {
					TimeRangePanel.this.client.timeRangeChanged();
				}
			}
		});
        
        _rangeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
            	if (isEnabled) {
            		regenLimits(_rangeSlider.getLowValue(), _rangeSlider.getHighValue());
            	}
            }
        });

        minField.setText(getDate(iniTs));
        maxField.setText(getDate(endTs));

        
        JPanel rangePanel = new JPanel(new GridLayout());
        rangePanel.add(_rangeSlider);
        //rangePanel.setPreferredSize(new Dimension(400,30));
        //rangePanel.setMaximumSize( new Dimension( 100, 30 ) );
        
        JPanel minPanel = new JPanel(new BorderLayout());
        minField.setEditable(false);
        minPanel.add(minField);

        JPanel maxPanel = new JPanel(new BorderLayout());
        maxField.setEditable(false);
        maxPanel.add(maxField);

        JPanel textFieldPanel = new JPanel(new GridLayout(1, 3));
        textFieldPanel.add(minPanel);
        textFieldPanel.add(new JPanel());
        textFieldPanel.add(new JPanel());
        textFieldPanel.add(new JPanel());
        textFieldPanel.add(maxPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(rangePanel, BorderLayout.CENTER);
        panel.add(textFieldPanel, BorderLayout.AFTER_LAST_LINE);
		
        add(panel);
        
	}
	
	public void setLimits(long minTs, long maxTs) {
		this.minTs = minTs;
		this.maxTs = maxTs;
		amplitude=(int)(maxTs-minTs);
		regenLimits(0,100);
	}

}

