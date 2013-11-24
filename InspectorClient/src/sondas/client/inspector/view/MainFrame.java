package sondas.client.inspector.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.ui.about.AboutDialog;
import sondas.client.inspector.ClientException;
import sondas.client.inspector.ClientInspectorRuntime;
import sondas.client.inspector.Preferences;
import sondas.client.inspector.forms.AboutDlg;
import sondas.client.inspector.forms.AgentSessionForm;
import sondas.client.inspector.forms.CapturesForm;
import sondas.client.inspector.forms.CommonForms;
import sondas.client.inspector.forms.NewCapture;
import sondas.client.inspector.math.DataSet;
import sondas.client.inspector.math.Interpolator;
import sondas.client.inspector.model.Model;
import sondas.client.inspector.model.StatsTreeData;
import sondas.client.inspector.view.nodes.IProbeViewer;
import sondas.inspector.InspectorRuntime;
import sondas.inspector.connectivity.DispatcherManager;
import sondas.inspector.delta.IGlobalStats;
import sondas.inspector.delta.SimpleGlobalStats;
import sondas.inspector.probes.delta.MethodNode;
import sondas.utils.Names;
import sondas.utils.SondasUtils;


public class MainFrame extends JFrame implements IClientEvents
{
	private static final long serialVersionUID = -4777469141649396711L;

	private static MainFrame mainFrame;

	private static String MenuAgentSession = "Set agent session...";
	private static String MenuLoadModelLoc = "Load capture from local..."; 
	private static String MenuLoadModelRep = "Load capture from repository...";
	private static String MenuNewCapture = "New capture...";
	private static String MenuAbout = "About...";

	// Results Tabbed Panel
	private ResultsTabbedPanel resultsTabbedPanel;
	//private TreePanel treePanel;
	private StatsTree metricsTree;
	private Model model;
	private JScrollPane treePanelScroll;
	private JSplitPane horSplitPane;
	private List<String> agents;
	private JLabel status;
	private DispatcherManager dispatcherManager;
	private JMenuItem newCaptureMenu;
	private JMenuItem loadCaptureMenu;
	private JPanel mainPanel;

	private String disHost;
	private int disPort;
	private int capPort;
	private String licensesServer;

	final private static String appName ="Sondasplorer Inspector";

	private TimeRangePanel timeRange;

	private boolean ctrlPressed;
	private boolean timeRangeChangedWhileCtrlPressed;

	private AboutDlg aboutDlg;
	
	/**
	 * Si la tecla de control esta presionada no hago nada
	 * Si la tecla de control no esta presionada regenero la vista con el nuevo time range
	 */
	public void timeRangeChanged() {
		if (ctrlPressed) {
			timeRangeChangedWhileCtrlPressed=true;
		} else {
			regenView();
		}
	}
	

	/**
	 * Regenera la vista en funcion del nuevo TimeRange
	 * @throws FileNotFoundException 
	 */
	public void regenView()  {
		try { 

			FileInputStream fis = new FileInputStream(Preferences.cacheDataFile);
			LoadModelTask task = new LoadModelTask(timeRange.getMinTs(), timeRange.getMaxTs(), timeRange.getIniTs(), timeRange.getEndTs(), true, fis, Preferences.RecursivityLevel, this);
			task.execute();
			this.setEnabled(false);
		} catch (FileNotFoundException e) {
			CommonForms.showError(this, "Error reading cache");
		}
	}

	public MainFrame() 
	{
		super(appName);

		loadConfiguration();

		Preferences.tmpPath = InspectorRuntime.getProperty("tmp.path")+"/";
		Preferences.cacheDataFile = Preferences.tmpPath+"dataCache.dt";
		Preferences.cacheIndexFile = Preferences.tmpPath+"dataIndex.dt";

		disHost = ClientInspectorRuntime.getProperty("Dispatcher","Host");
		disPort = ClientInspectorRuntime.getPropertyAsInteger("Dispatcher","Port");
		capPort = ClientInspectorRuntime.getPropertyAsInteger("Dispatcher","Captures.Port");

		dispatcherManager = new DispatcherManager(disHost, disPort, capPort);

		setFocusable(true);
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==17) {
					if (!ctrlPressed) {
						timeRangeChangedWhileCtrlPressed=false;
					}
					ctrlPressed=true;
				}
			}

			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode()==17) {
					ctrlPressed=false;
					if (timeRangeChangedWhileCtrlPressed) {
						regenView();
					}
				}
			}
		});

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			throw new RuntimeException("Error loading frame",e);
		}

		// Establezco el icono de la aplicacion
		try {
			// / => Root app folder. Si no lo pongo, lo buscaria en la misma carpeta que la clase
			// Hay que copiarlo al bin
			BufferedImage image = ImageIO.read(getClass().getResource("/icon.png"));
			setIconImage(image);

			// Conecto al dispatcher
			connectToDispatcher();
		} catch (IOException e) {
			e.printStackTrace();
		}

		resultsTabbedPanel = new ResultsTabbedPanel();
		//add(resultsTabbedPanel, BorderLayout.SOUTH);

		// TreePanel
		//treePanel = new TreePanel(this);
		treePanelScroll = new JScrollPane();

		// MetricsTree
		metricsTree = new StatsTree();
		JScrollPane metricsTreeScroll = new JScrollPane(metricsTree);

		horSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		horSplitPane.setLeftComponent(metricsTreeScroll);

		long aux = System.currentTimeMillis();
		timeRange = new TimeRangePanel(this, aux, aux);
		timeRange.disable();

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		timeRange.setAlignmentX(Component.CENTER_ALIGNMENT);
		treePanelScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
		mainPanel.add(timeRange);
		//timeRange.setMaximumSize( new Dimension( 550, 30 ) );
		mainPanel.add(treePanelScroll); 


		horSplitPane.setRightComponent(mainPanel);  
		horSplitPane.setOneTouchExpandable(true);
		horSplitPane.setDividerLocation(300);

		JSplitPane vertSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		vertSplitPane.setTopComponent(horSplitPane);
		vertSplitPane.setBottomComponent(resultsTabbedPanel);
		vertSplitPane.setDividerLocation(500);

		/*
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		timeRange.setAlignmentX(Component.CENTER_ALIGNMENT);
		mainPanel.add(timeRange);*/

		//vertSplitPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		//mainPanel.add(vertSplitPane);

		//add(mainPanel);
		add(vertSplitPane);

		aboutDlg = new AboutDlg(this, true);

		createMenu();

		// Salimos cuando la ventana se cierra.
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		licensesServer = ClientInspectorRuntime.getProperty("Dispatcher","Licenses.Server");

		mainFrame = this; 

		// Status bar
		Container c = super.getContentPane();
		status = new JLabel();
		status.setBorder(new EtchedBorder());
		c.add(status, BorderLayout.SOUTH);

		status.setText("No agent set");

		setSize(900, 800);

		// Mostramos el Frame.
		//pack();
		setVisible(true);
	}

	private boolean connectToDispatcher() {
		try {
			dispatcherManager.connect();
			agents = dispatcherManager.getAgents();
			return true;
		} catch (ClientException e) {
			e.printStackTrace();
			CommonForms.showError(this, e);
			return false;
		} 
	}

	public synchronized static void start() {
		if (mainFrame==null)
			mainFrame = new MainFrame();
	}

	private void loadConfiguration() {
		// TODO
		//System.setProperty("inspector.cfg","D:/proyectos_ws/Sondasplorer/cfg/inspector.cfg");
	}

	public void exportData(String header[], Vector rows) 
	{
		FileFilter csv = new FileFilter() {

			@Override
			public String getDescription() {
				return "Commas separated values (csv)";
			}

			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return false;
				}

				String extension = SondasUtils.getExtension(f);
				if (extension != null) {
					if (extension.equals("csv")) {
						return true;
					} else {
						return false;
					}
				}

				return false;
			}
		};


		final JFileChooser fc = new JFileChooser();

		fc.addChoosableFileFilter(csv);
		int returnVal = fc.showSaveDialog(MainFrame.this);

		try {
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				if (fc.getFileFilter()==csv) {
					File file = fc.getSelectedFile();
					if (SondasUtils.getExtension(file)==null) {
						file = new File(file.getAbsolutePath()+".csv");
					}
					writeCsvData(file, header, rows);
				}
			}
		} catch (IOException e) {
			CommonForms.showError(MainFrame.this, "Error exporting data", e); 
		}
	}

	private void writeCsvData(File file, String header[], Vector rows) throws IOException 
	{
		FileWriter fw = new FileWriter(file);
		StringBuilder sb = new StringBuilder();

		for (int i=0;i<header.length;i++) {
			Object value = (Object) header[i];
			sb.append(value);
			if (i<(header.length-1)) {
				sb.append(";");
			}
		}

		sb.append("\n");

		fw.write(sb.toString());

		for (int i=0;i<rows.size();i++) 
		{
			sb = new StringBuilder();

			Vector row = (Vector) rows.get(i);
			int length = row.size();

			for (int j=0;j<length;j++) {
				Object value = (Object) row.get(j);
				sb.append(value);
				if (j<(row.size()-1)) {
					sb.append(";");
				}
			}

			sb.append("\n");
			fw.write(sb.toString());
		}

		fw.close();
	}

	private void loadModelLocAction() throws IOException {
		final JFileChooser fc = new JFileChooser();
		FileFilter ff = new FileFilter() {

			@Override
			public String getDescription() {
				return "Inspector files";
			}
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return false;
				}

				String extension = SondasUtils.getExtension(f);
				if (extension != null) {
					if (extension.equals("ins")) {
						return true;
					} else {
						return false;
					}
				}

				return false;
			}
		};

		fc.setFileFilter(ff);

		int returnVal = fc.showOpenDialog(MainFrame.this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			loadModel(new FileInputStream(file), Preferences.RecursivityLevel);
			//treePanel.loadModel();
			//MainFrame.this.validateTree();
		}
	}

	private void agentSessionAction() {
		if (!dispatcherManager.isConnected()) {
			if (!connectToDispatcher()) {
				return;
			}
		}
		AgentSessionForm dialog = new AgentSessionForm(this, true, agents);
		String agent = dialog.getAgent();

		if (agent!=null) {
			dispatcherManager.setAgent(agent);
			status.setText("Agent "+agent+" defined");

			// Activo el menu de capturas
			newCaptureMenu.setEnabled(true);
			loadCaptureMenu.setEnabled(true);
		}
	}

	private void newCapture() {
		NewCapture dialog = new NewCapture(this, true, dispatcherManager);
	}

	private void loadModelRepAction() {
		try {
			CapturesForm dialog = new CapturesForm(this, true, dispatcherManager);
			String captureName = dialog.getCapture();
			InputStream is=null;

			if (captureName!=null) {
				is = dispatcherManager.getCapture(captureName);
				loadModel(is, Preferences.RecursivityLevel);
			}
		} catch (Exception e) {
			processError();
			e.printStackTrace();
			CommonForms.showError(this, e);
		}
	}

	private void createMenu() 
	{
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (e.getActionCommand().equals(MenuLoadModelLoc)) 
					{
						loadModelLocAction();
					} else if (e.getActionCommand().equals(MenuAgentSession)) {
						agentSessionAction();
					} else if (e.getActionCommand().equals(MenuLoadModelRep)) {
						loadModelRepAction();
					} else if (e.getActionCommand().equals(MenuNewCapture)) {
						newCapture();
					} else if (e.getActionCommand().equals(MenuAbout)) {
						aboutDlg.setLocationRelativeTo(MainFrame.this); // center on screen
						aboutDlg.setVisible(true);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					CommonForms.showError(MainFrame.this, ex); 
				}
			}
		};

		// Construyo el menu
		JMenuBar menuBar = new JMenuBar();

		JMenuItem menuItem;

		// Menu File
		JMenu menu = new JMenu("Session");

		menuItem = new JMenuItem(MenuAgentSession);
		menuItem.addActionListener(actionListener);
		menu.add(menuItem);

		menuBar.add(menu);

		// Menu Capture
		menu = new JMenu("Capture");

		newCaptureMenu = new JMenuItem(MenuNewCapture);
		newCaptureMenu.addActionListener(actionListener);
		newCaptureMenu.setEnabled(false);
		menu.add(newCaptureMenu);

		loadCaptureMenu = new JMenuItem(MenuLoadModelRep);
		loadCaptureMenu.addActionListener(actionListener);
		loadCaptureMenu.setEnabled(false);
		menu.add(loadCaptureMenu);

		menuItem = new JMenuItem(MenuLoadModelLoc);
		menuItem.addActionListener(actionListener);
		menu.add(menuItem);
		menuBar.add(menu);

		// Menu Capture
		menu = new JMenu("Help");
		JMenuItem aboutMenu = new JMenuItem(MenuAbout);
		aboutMenu.addActionListener(actionListener);
		menu.add(aboutMenu);
		menuBar.add(menu);

		setJMenuBar(menuBar);		
	}
	
	public void processError() {
		if (!dispatcherManager.isConnected()) {
			newCaptureMenu.setEnabled(false);
			loadCaptureMenu.setEnabled(false);
			status.setText("No agent set");
		}
	}

	class LoadModelTask extends SwingWorker<Void, Void> 
	{
		private int recursivityLevel;
		private Component parent;
		private InputStream is;
		private TableData entryPointsTable;
		private JDialog dialog;
		private boolean isCached;
		private long minTs;
		private long maxTs;
		private long iniTs;
		private long endTs;		
		private boolean ok;


		public LoadModelTask(InputStream is, int recursivityLevel, Component parent) {
			this(-1,-1,-1,-1,false, is, recursivityLevel, parent);
		}

		public LoadModelTask(long minTs, long maxTs, long iniTs, long endTs, boolean isCached, InputStream is, int recursivityLevel, Component parent) {
			this.recursivityLevel=recursivityLevel;
			this.parent=parent;
			//this.is=new CountingInputStream(is);
			this.is=is;
			this.isCached=isCached;
			this.minTs=minTs;
			this.maxTs=maxTs;
			this.iniTs=iniTs;
			this.endTs=endTs;

			JProgressBar pb = new JProgressBar(0,100);
			pb.setIndeterminate(true);
			pb.setPreferredSize(new Dimension(175,20));
			pb.setString("Working");
			pb.setStringPainted(true);
			pb.setValue(0); 

			JLabel label = new JLabel("Progress: ");
			JPanel center_panel = new JPanel();
			center_panel.add(label);
			center_panel.add(pb);

			dialog = new JDialog((JFrame)null, "Processing data ...");
			dialog.getContentPane().add(center_panel, BorderLayout.CENTER);
			dialog.pack();
			dialog.setVisible(true);
			setProgress(0);
			dialog.setLocationRelativeTo(MainFrame.this); // center on screen
			dialog.toFront();
		}

		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			DataInputStream dis = null; 

			try {
				//ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(parent,"Processing data...",is);
				//dis = new DataInputStream(pmis);

				dis = new DataInputStream(is);

				model = new Model(dis, Preferences.cacheDataFile, Preferences.cacheIndexFile, isCached, recursivityLevel, minTs, maxTs, iniTs, endTs, licensesServer);

				minTs = model.getMinTs();
				maxTs = model.getMaxTs();

				// Actualizo los entry points
				/*
				Collection<MethodNode> entryPoints = model.getEntryPoints();
				entryPointsTable = new TableData(2);
				ArrayList<String> entryPointsRowKeys = new ArrayList<String>();


				for (Iterator<MethodNode> it = entryPoints.iterator();it.hasNext();) {
					MethodNode entryNode = it.next();
					entryPointsTable.addRow(entryNode.getId(), Names.getDigestedName(entryNode.getName(),true),String.valueOf(entryNode.getInvCount()));
				}
				 */
				ok=true;
			} catch (Exception e) {
				e.printStackTrace();
				CommonForms.showError(parent, e);
				dialog.dispose();
				parent.setEnabled(true);
			}  
			return null;
		}

		/*
		 * Executed in event dispatch thread
		 */
		public void done() 
		{
			try {
				if (ok) {
					//resultsTabbedPanel.setEntryPointsData(entryPointsTable);
					// Actualizo la tabla de estadisticas globales
					createGlobalStatsTable(0); // XXX TODO

					regenStatsTree();

					validateTree();


					timeRange.enable();

					if (!isCached) {
						timeRange.setLimits(minTs, maxTs);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
				CommonForms.showError(parent, "Error showing data", e);
			} finally {
				dialog.dispose();
				parent.setEnabled(true);
				mainPanel.remove(1); 
				JScrollPane treePanelScroll = new JScrollPane();
				mainPanel.add(treePanelScroll);
				MainFrame.this.toFront();
			}
		}
	}

	private void regenStatsTree() {
		metricsTree = new StatsTree(model.getStatsTreeDatas(), model.getCategoriesDictionary());
		JScrollPane metricsTreeScroll = new JScrollPane(metricsTree);
		horSplitPane.setLeftComponent(metricsTreeScroll);
	}


	/**
	 * Carga el modelo desde cero => lo cachea
	 */
	private void loadModel(InputStream is, int recursivityLevel) throws IOException 
	{
		LoadModelTask task = new LoadModelTask(is, recursivityLevel, this);
		task.execute();
		//timeRange.setLimits(minTs, maxTs);
		this.setEnabled(false);
	}

	/**
	 * La global de los nodos tiene que heredar de SimpleGlobalStats para que se muestre
	 */
	private void createGlobalStatsTable(int threshold) 
	{
		TableData statsTable = new TableData(9);

		Map<Integer,IGlobalStats> globalStatsMap = model.getGlobalStats();

		for (int key:globalStatsMap.keySet()) 
		{
			// La global de todos los nodos tiene que heredar de SimpleGlobalStats para que se muestre
			IGlobalStats aux = globalStatsMap.get(key);

			if (!(aux instanceof SimpleGlobalStats)) {
				continue;
			}
			SimpleGlobalStats stats = (SimpleGlobalStats)aux;

			String obj = stats.getName(model.getMethodNames());
			if (obj==null) {
				int h=1;
			}
			
			String nodeName = Names.getDigestedName(stats.getName(model.getMethodNames()), true);
			String nodeType = stats.getTypeName();

			float referenceTime = stats.getAvgTotTime();

			if (referenceTime>=threshold) {
				// "Name", "Type", "Count", "Avg. Total Time", "Min. Total Time", "Max. Total Time", "Avg. Ex. Time", "Min. Ex. Time", "Max. Ex. Time" 
				DecimalFormat dec = new DecimalFormat();
				dec.setMinimumFractionDigits(2);
				dec.setMaximumFractionDigits(2);

				statsTable.addRow(stats.getId(), nodeName, nodeType, stats.getInvCount(),stats.getAvgTotTime(), stats.getMinTotTime(), stats.getMaxTotTime(), stats.getAvgExTime(),stats.getMinExTime(), stats.getMaxExTime());
			}
		}

		resultsTabbedPanel.setStatisticsData(statsTable);
	}

	public static MainFrame getInstance() {
		return mainFrame;
	}

	public void generateTree(int entryPointId) 
	{
		GenerateTreeTask task = new GenerateTreeTask(entryPointId, this);
		task.execute();
		this.setEnabled(false);
	}

	public void generateMap() 
	{
		GenerateMapTask task = new GenerateMapTask(this);
		task.execute();
		this.setEnabled(false);
	}

	class GenerateMapTask extends SwingWorker<Void, Void>
	{
		private String entryPointId;
		private JDialog dialog;
		private MapPanel mapPanel;

		public GenerateMapTask(Container parent) {
			mapPanel = new MapPanel(parent);

			JProgressBar pb = new JProgressBar(0,100);
			pb.setIndeterminate(true);
			pb.setPreferredSize(new Dimension(175,20));
			pb.setString("Working");
			pb.setStringPainted(true);
			pb.setValue(0); 

			JLabel label = new JLabel("Progress: ");
			JPanel center_panel = new JPanel();
			center_panel.add(label);
			center_panel.add(pb);

			dialog = new JDialog((JFrame)null, "Generating Map ...");
			dialog.getContentPane().add(center_panel, BorderLayout.CENTER);
			dialog.pack();
			dialog.setVisible(true);
			setProgress(0);
			dialog.setLocationRelativeTo(MainFrame.this); // center on screen
			dialog.toFront();
		}

		@Override
		public Void doInBackground() {
			mapPanel.generateMap(model.getGlobalStats().values());
			return null;
		}

		public void done() {
			dialog.dispose();

			mainPanel.remove(1);
			mainPanel.add(mapPanel); // ISIDRO

			JScrollBar sb = treePanelScroll.getVerticalScrollBar();
			validateTree();
			sb.setValue(mapPanel.getHeight()/2);			

			MainFrame.this.setEnabled(true);
			MainFrame.this.setVisible(true);
		}
	}


	class GenerateTreeTask extends SwingWorker<Void, Void> 
	{
		private int entryNodeId;
		private JDialog dialog;
		private TreePanel treePanel;

		public GenerateTreeTask(int entryPointId, Container parent) {
			treePanel = new TreePanel(parent);
			this.entryNodeId=entryPointId;

			JProgressBar pb = new JProgressBar(0,100);
			pb.setIndeterminate(true);
			pb.setPreferredSize(new Dimension(175,20));
			pb.setString("Working");
			pb.setStringPainted(true);
			pb.setValue(0); 

			JLabel label = new JLabel("Progress: ");
			JPanel center_panel = new JPanel();
			center_panel.add(label);
			center_panel.add(pb);

			dialog = new JDialog((JFrame)null, "Generating Tree ...");
			dialog.getContentPane().add(center_panel, BorderLayout.CENTER);
			dialog.pack();
			dialog.setVisible(true);
			setProgress(0);
			dialog.setLocationRelativeTo(MainFrame.this); // center on screen
			dialog.toFront();
		}

		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			Set<MethodNode> entryNodes = model.getSubtrees(entryNodeId);
			treePanel.generateTree(entryNodes);
			return null;
		}

		/*
		 * Executed in event dispatch thread
		 */
		public void done() {
			dialog.dispose();
			JScrollPane treePanelScroll = new JScrollPane(treePanel);
			//horSplitPane.setRightComponent(treePanelScroll);
			mainPanel.remove(1);
			//mainPanel.add(treePanelScroll);
			mainPanel.add(treePanel); // ISIDRO
			//horSplitPane.setDividerLocation(300); // TODO parametrizar
			//horSplitPane.setRightComponent(treePanelScroll);
			JScrollBar sb = treePanelScroll.getVerticalScrollBar();
			validateTree();
			sb.setValue(treePanel.getHeight()/2);			

			MainFrame.this.setEnabled(true);
			MainFrame.this.setVisible(true);
		}
	}

	public void showTimeMetrics(MetricBean mb, String name, String statTitle) throws ClientException 
	{
		int interpoledPoints=20;
		TimeGraphicPanel gp = new TimeGraphicPanel(statTitle,"time",mb.statUnit ,new String[]{name}, mb.format);

		Map< Long, Map<Integer,IGlobalStats> > map = model.getTimedGlobals();
		int count = map.size();

		long x[]=new long[count];
		float y[]=new float[count];

		int i=0;
		for (long ts:map.keySet()) {
			Map<Integer,IGlobalStats> tsGlobals = map.get(ts);
			//String metricKey = BasicNode.getId(mb.nodeName, mb.metricType);
			IGlobalStats tsGlobal = tsGlobals.get(mb.id); 

			float value;

			/*
			if (tsGlobal==null) {
				// No hay valor para esa metrica en ese timestamp
				value=0;
			} else {
				value = tsGlobal.getMetricValue(mb.nodeName, mb.statName);
			}

			x[i]=ts;
			y[i]=value;
			i++;
			 */

			// Si no existe dato, tsGlobal==null o el valor=-1
			if (tsGlobal!=null) {
				int metricType = tsGlobal.getMetricType();
				IProbeViewer probeViewer = ClientInspectorRuntime.getInstance().getNodeViewer(metricType);
				value = probeViewer.getMetricValue(mb.nodeName, mb.statName, tsGlobal);

				//value = tsGlobal.getMetricValue(metricValueKey, mb.statName);
				//value = tsGlobal.getMetricValue(mb.nodeName, mb.statName);
				if (value!=-1) {
					// Si no existe valor, value=-1
					x[i]=ts;
					y[i]=value;

					i++;
				}
			}
		}

		count = i;

		long xp[]=new long[count];
		float yp[]=new float[count];

		// TODO CRITICO Hay que ordenar esta serie en funcion de xp
		for (int j=0;j<count;j++) {
			xp[j]=x[j];
			yp[j]=y[j];
		}
		sortBiArray(xp,yp);

		DataSet ds = Interpolator.process(xp, yp, interpoledPoints, mb.isAcc);
		gp.setSerie(0, ds.x, ds.y);

		//horSplitPane.setRightComponent(gp);
		//horSplitPane.setDividerLocation(300); // TODO parametrizar

		mainPanel.remove(1);
		mainPanel.add(gp);

		validateTree();
	}

	private void sortBiArray(long[] x, float[] y) {
		int n = x.length;
		for (int pass=1; pass < n; pass++) {  // count how many times
			// This next loop becomes shorter and shorter
			for (int i=0; i < n-pass; i++) {
				if (x[i] > x[i+1]) {
					// exchange elements
					long tempX = x[i];  x[i] = x[i+1];  x[i+1] = tempX;
					float tempY = y[i];  y[i] = y[i+1]; y[i+1] = tempY;
				}
			}
		}
	}

	/**
	 * key = metricType:name
	 * metricName = respTime, etc
	 */
	/*
	public void showTimeMetrics(int metricType, String name, String statName, String statTitle, String unit) 
	//public void showTimeMetrics(MetricBean mb)
	{
		int interpoledPoints=20;
		TimeGraphicPanel gp = new TimeGraphicPanel(statTitle,"time",unit ,new String[]{name});

		Map< Long, Map<String,GlobalStats> > map = model.getTimedGlobals();
		int count = map.size();

		long x[]=new long[count];
		float y[]=new float[count];

		int i=0;
		for (long ts:map.keySet()) {
			Map<String,GlobalStats> tsGlobals = map.get(ts);
			String metricKey = BasicNode.getId(name, metricType);
			GlobalStats tsGlobal = tsGlobals.get(metricKey); 

			float value;

			if (tsGlobal==null) {
				// No hay valor para esa metrica en ese timestamp
				value=0;
			} else {
				value = tsGlobal.getMetricValue(statName);
			}

			x[i]=ts;
			y[i]=value;
			i++;
			//gp.setPoint(0, ts, value);
		}

		DataSet ds = Interpolator.process(x, y, interpoledPoints, true);
		gp.setSerie(0, ds.x, ds.y);

		horSplitPane.setRightComponent(gp);
		horSplitPane.setDividerLocation(300); // TODO parametrizar
		validateTree();
	}*/

}

