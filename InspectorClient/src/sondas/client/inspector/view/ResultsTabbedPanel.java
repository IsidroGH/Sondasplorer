package sondas.client.inspector.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

public class ResultsTabbedPanel extends JTabbedPane
{
	private JPanel statisticsTab;
	private JPanel entryPointsTab;

	private String[] statisticsColumName = { "Name", "Type", "Count", "Avg. Total Time", "Min. Total Time", "Max. Total Time", "Avg. Ex. Time", "Min. Ex. Time", "Max. Ex. Time" };
	private Class[] statisticsColumType = {String.class, String.class, Integer.class, Float.class, Float.class, Float.class, Float.class, Float.class, Float.class};
	private int[] statisticsColumnWidth={400,20,10,40,40,40,40,40,40};

	private String[] entryPointsColumName = { "Entry Point", "Count"};
	private Class[] entryPointsColumType = {String.class, Integer.class};
	private int[] entryPointsColumnWidth={200,40};

	private List<Integer> statisticsCellData;

	public ResultsTabbedPanel() 
	{
		setPreferredSize(new Dimension(640,200));

		// Tab Entry points
		/*
		entryPointsTab = new JPanel();
		entryPointsTab.setOpaque(false);
		entryPointsTab.add(new JLabel("Entry Points"), BorderLayout.WEST);		
		SortableTable entryPointsTable = new SortableTable(null, entryPointsColumName, entryPointsColumType,entryPointsColumnWidth);
		insertTab(null, null, createEntryPointsPanel(entryPointsTable), null, 0);
		setTabComponentAt(0, entryPointsTab);
		 */

		// Tab statistics
		statisticsTab = new JPanel();
		statisticsTab.setOpaque(false);
		statisticsTab.add(new JLabel("Statistics"), BorderLayout.WEST);
		SortableTable statisticsTable = new SortableTable(null, statisticsColumName, statisticsColumType,statisticsColumnWidth);
		//insertTab(null, null, new JScrollPane(new JScrollPane(statisticsTable)), null, 1);
		insertTab(null, null, createStatisticsPanel(statisticsTable), null, 0);
		setTabComponentAt(0, statisticsTab);	
	}

	public void setStatisticsData(TableData statisticsCellData) {
		this.statisticsCellData = statisticsCellData.getRowKeys();
		SortableTable statisticsTable = new SortableTable(statisticsCellData.getData(), statisticsColumName, statisticsColumType,statisticsColumnWidth);
		remove(0);
		//insertTab(null, null, new JScrollPane(new JScrollPane(statisticsTable)), null, 1);
		insertTab(null, null, createStatisticsPanel(statisticsTable), null, 0);
		setTabComponentAt(0, statisticsTab);
	}

	/*
	public void setEntryPointsData(TableData entryPointsCellData) {
		this.entryPointsRowKeys = entryPointsCellData.getRowKeys();
		SortableTable entryPointsTable = new SortableTable(entryPointsCellData.getData(), entryPointsColumName, entryPointsColumType,entryPointsColumnWidth);
		remove(0);
		insertTab(null, null, createEntryPointsPanel(entryPointsTable), null, 0);
		setTabComponentAt(0, entryPointsTab);
	}
	 */

	private Component createStatisticsPanel(final SortableTable sortableTable) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));

		Box buttons = Box.createHorizontalBox();

		JButton generateTreeButton = new JButton("Generate Tree");
		buttons.add(generateTreeButton);

		JButton exportDataButton = new JButton("Export Data");
		buttons.add(exportDataButton);

		panel.add(buttons);

		generateTreeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int row = sortableTable.getTable().getSelectedRow();
				if (row!=-1) {
					SortableTableModel stb = (SortableTableModel) sortableTable.getTable().getModel();
					int index = stb.getIndexes()[row];

					//String entryPoint = (String) sortableTable.getTable().getValueAt(row, 0);
					int id = statisticsCellData.get(index); 
					MainFrame.getInstance().generateTree(id);
				}
			}
		});

		exportDataButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				SortableTableModel stb = (SortableTableModel) sortableTable.getTable().getModel();
				Vector rows = stb.getDataVector();
				MainFrame.getInstance().exportData(sortableTable.getHeader(), rows);
			}
		});

		//JScrollPane scrollPanel = new JScrollPane(sortableTable);
		panel.add(sortableTable); 

		return panel;
	}
}
