package sondas.client.inspector.view;

//Example from http://www.crionics.com/products/opensource/faq/swing_ex/SwingExamples.html
/* (swing1.1) */


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

public class SortableTable extends JPanel 
{

	private SortableTableModel dm;
	private String[] headerStr;
	private JTable table;

	public SortableTable(Object[][] data, String[] headerStr, Class columType[], int[] columnWidth) 
	{
		this.headerStr=headerStr;

		setLayout(new BorderLayout());

		dm = new SortableTableModel(columType);

		table = new JTable(dm);
		table.setRowSelectionAllowed(true);
		table.clearSelection();

		//table.setShowGrid(false);
		table.setShowVerticalLines(true);
		table.setShowHorizontalLines(false);
		SortButtonRenderer renderer = new SortButtonRenderer();
		TableColumnModel model = table.getColumnModel();
		int n = headerStr.length;

		dm.setDataVector(data,headerStr);
		
		for (int i = 0; i < n; i++) {
			model.getColumn(i).setHeaderRenderer(renderer);
			model.getColumn(i).setPreferredWidth(columnWidth[i]);
			if (columType[i]==Float.class) {
				model.getColumn(i).setCellRenderer(new DecimalFormatRenderer());
			}
		}

		JTableHeader header = table.getTableHeader();
		header.addMouseListener(new HeaderListener(header, renderer));
		JScrollPane pane = new JScrollPane(table);
		add(pane, BorderLayout.CENTER);
	}

	static class DecimalFormatRenderer extends DefaultTableCellRenderer {
		private static final DecimalFormat formatter = new DecimalFormat( "0.00" );
		public Component getTableCellRendererComponent(
				JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			// First format the cell value as required
			value = formatter.format(value);
			// And pass it on to parent class
			return super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, column );
		}
	}

	public String[] getHeader() {
		return headerStr;
	}
	
	
	public JTable getTable() {
		return table;
	}

	class HeaderListener extends MouseAdapter {
		JTableHeader header;

		SortButtonRenderer renderer;

		HeaderListener(JTableHeader header, SortButtonRenderer renderer) {
			this.header = header;
			this.renderer = renderer;
		}

		public void mousePressed(MouseEvent e) {
			int col = header.columnAtPoint(e.getPoint());
			int sortCol = header.getTable().convertColumnIndexToModel(col);
			renderer.setPressedColumn(col);
			renderer.setSelectedColumn(col);
			header.repaint();

			if (header.getTable().isEditing()) {
				header.getTable().getCellEditor().stopCellEditing();
			}

			boolean isAscent;
			if (SortButtonRenderer.DOWN == renderer.getState(col)) {
				isAscent = true;
			} else {
				isAscent = false;
			}
			((SortableTableModel) header.getTable().getModel()).sortByColumn(
					sortCol, isAscent);
		}

		public void mouseReleased(MouseEvent e) {
			int col = header.columnAtPoint(e.getPoint());
			renderer.setPressedColumn(-1); // clear
			header.repaint();
		}
	}

}

class SortableTableModel extends DefaultTableModel 
{
	private int[] indexes;
	private TableSorter sorter;
	private Class columTypes[];

	public SortableTableModel(Class columTypes[]) {
		this.columTypes=columTypes;		
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public Class getColumnClass(int col) {
		return columTypes[col];
	}

	public Object getValueAt(int row, int col) {
		int rowIndex = row;
		if (indexes != null) {
			rowIndex = indexes[row];
		}
		return super.getValueAt(rowIndex, col);
	}


	public void setValueAt(Object value, int row, int col) {
		int rowIndex = row;
		if (indexes != null) {
			rowIndex = indexes[row];
		}
		super.setValueAt(value, rowIndex, col);
	}

	public void sortByColumn(int column, boolean isAscent) {
		if (sorter == null) {
			sorter = new TableSorter(this);
		}
		sorter.sort(column, isAscent);
		fireTableDataChanged();
	}

	public int[] getIndexes() {
		int n = getRowCount();
		if (indexes != null) {
			if (indexes.length == n) {
				return indexes;
			}
		}
		indexes = new int[n];
		for (int i = 0; i < n; i++) {
			indexes[i] = i;
		}
		return indexes;
	}
}

class TableSorter {
	SortableTableModel model;

	public TableSorter(SortableTableModel model) {
		this.model = model;
	}

	//n2 selection
	public void sort(int column, boolean isAscent) {
		int n = model.getRowCount();
		int[] indexes = model.getIndexes();

		for (int i = 0; i < n - 1; i++) {
			int k = i;
			for (int j = i + 1; j < n; j++) {
				if (isAscent) {
					if (compare(column, j, k) < 0) {
						k = j;
					}
				} else {
					if (compare(column, j, k) > 0) {
						k = j;
					}
				}
			}
			int tmp = indexes[i];
			indexes[i] = indexes[k];
			indexes[k] = tmp;
		}
	}

	// comparaters

	public int compare(int column, int row1, int row2) {
		Object o1 = model.getValueAt(row1, column);
		Object o2 = model.getValueAt(row2, column);
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		} else {
			Class type = model.getColumnClass(column);
			if (type.getSuperclass() == Number.class) {
				return compare((Number) o1, (Number) o2);
			} else if (type == String.class) {
				return ((String) o1).compareTo((String) o2);
			} else if (type == Date.class) {
				return compare((Date) o1, (Date) o2);
			} else if (type == Boolean.class) {
				return compare((Boolean) o1, (Boolean) o2);
			} else {
				return ((String) o1).compareTo((String) o2);
			}
		}
	}

	public int compare(Number o1, Number o2) {
		double n1 = o1.doubleValue();
		double n2 = o2.doubleValue();
		if (n1 < n2) {
			return -1;
		} else if (n1 > n2) {
			return 1;
		} else {
			return 0;
		}
	}

	public int compare(Date o1, Date o2) {
		long n1 = o1.getTime();
		long n2 = o2.getTime();
		if (n1 < n2) {
			return -1;
		} else if (n1 > n2) {
			return 1;
		} else {
			return 0;
		}
	}

	public int compare(Boolean o1, Boolean o2) {
		boolean b1 = o1.booleanValue();
		boolean b2 = o2.booleanValue();
		if (b1 == b2) {
			return 0;
		} else if (b1) {
			return 1;
		} else {
			return -1;
		}
	}

}

class SortButtonRenderer extends JButton implements TableCellRenderer {
	public static final int NONE = 0;

	public static final int DOWN = 1;

	public static final int UP = 2;

	int pushedColumn;

	Hashtable state;

	JButton downButton, upButton;

	public SortButtonRenderer() {
		pushedColumn = -1;
		state = new Hashtable();

		setMargin(new Insets(0, 0, 0, 0));
		setHorizontalTextPosition(LEFT);
		setIcon(new BlankIcon());

		// perplexed
		// ArrowIcon(SwingConstants.SOUTH, true)
		// BevelArrowIcon (int direction, boolean isRaisedView, boolean
		// isPressedView)

		downButton = new JButton();
		downButton.setMargin(new Insets(0, 0, 0, 0));
		downButton.setHorizontalTextPosition(LEFT);
		downButton
		.setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));
		downButton.setPressedIcon(new BevelArrowIcon(BevelArrowIcon.DOWN,
				false, true));

		upButton = new JButton();
		upButton.setMargin(new Insets(0, 0, 0, 0));
		upButton.setHorizontalTextPosition(LEFT);
		upButton.setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
		upButton.setPressedIcon(new BevelArrowIcon(BevelArrowIcon.UP, false,
				true));

	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		JButton button = this;
		Object obj = state.get(new Integer(column));
		if (obj != null) {
			if (((Integer) obj).intValue() == DOWN) {
				button = downButton;
			} else {
				button = upButton;
			}
		}
		button.setText((value == null) ? "" : value.toString());
		boolean isPressed = (column == pushedColumn);
		button.getModel().setPressed(isPressed);
		button.getModel().setArmed(isPressed);
		return button;
	}

	public void setPressedColumn(int col) {
		pushedColumn = col;
	}

	public void setSelectedColumn(int col) {
		if (col < 0)
			return;
		Integer value = null;
		Object obj = state.get(new Integer(col));
		if (obj == null) {
			value = new Integer(DOWN);
		} else {
			if (((Integer) obj).intValue() == DOWN) {
				value = new Integer(UP);
			} else {
				value = new Integer(DOWN);
			}
		}
		state.clear();
		state.put(new Integer(col), value);
	}

	public int getState(int col) {
		int retValue;
		Object obj = state.get(new Integer(col));
		if (obj == null) {
			retValue = NONE;
		} else {
			if (((Integer) obj).intValue() == DOWN) {
				retValue = DOWN;
			} else {
				retValue = UP;
			}
		}
		return retValue;
	}
}

class BevelArrowIcon implements Icon {
	public static final int UP = 0; // direction

	public static final int DOWN = 1;

	private static final int DEFAULT_SIZE = 11;

	private Color edge1;

	private Color edge2;

	private Color fill;

	private int size;

	private int direction;

	public BevelArrowIcon(int direction, boolean isRaisedView,
			boolean isPressedView) {
		if (isRaisedView) {
			if (isPressedView) {
				init(UIManager.getColor("controlLtHighlight"), UIManager
						.getColor("controlDkShadow"), UIManager
						.getColor("controlShadow"), DEFAULT_SIZE, direction);
			} else {
				init(UIManager.getColor("controlHighlight"), UIManager
						.getColor("controlShadow"), UIManager
						.getColor("control"), DEFAULT_SIZE, direction);
			}
		} else {
			if (isPressedView) {
				init(UIManager.getColor("controlDkShadow"), UIManager
						.getColor("controlLtHighlight"), UIManager
						.getColor("controlShadow"), DEFAULT_SIZE, direction);
			} else {
				init(UIManager.getColor("controlShadow"), UIManager
						.getColor("controlHighlight"), UIManager
						.getColor("control"), DEFAULT_SIZE, direction);
			}
		}
	}

	public BevelArrowIcon(Color edge1, Color edge2, Color fill, int size, int direction) {
		init(edge1, edge2, fill, size, direction);
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		switch (direction) {
		case DOWN:
			drawUpArrow(g, x, y);
			break;
		case UP:
			drawDownArrow(g, x, y);			
			break;
		}
	}

	public int getIconWidth() {
		return size;
	}

	public int getIconHeight() {
		return size;
	}

	private void init(Color edge1, Color edge2, Color fill, int size,
			int direction) {
		this.edge1 = edge1;
		this.edge2 = edge2;
		this.fill = fill;
		this.size = size;
		this.direction = direction;
	}

	private void drawDownArrow(Graphics g, int xo, int yo) {
		g.setColor(edge1);
		g.drawLine(xo, yo, xo + size - 1, yo);
		g.drawLine(xo, yo + 1, xo + size - 3, yo + 1);
		g.setColor(edge2);
		g.drawLine(xo + size - 2, yo + 1, xo + size - 1, yo + 1);
		int x = xo + 1;
		int y = yo + 2;
		int dx = size - 6;
		while (y + 1 < yo + size) {
			g.setColor(edge1);
			g.drawLine(x, y, x + 1, y);
			g.drawLine(x, y + 1, x + 1, y + 1);
			if (0 < dx) {
				g.setColor(fill);
				g.drawLine(x + 2, y, x + 1 + dx, y);
				g.drawLine(x + 2, y + 1, x + 1 + dx, y + 1);
			}
			g.setColor(edge2);
			g.drawLine(x + dx + 2, y, x + dx + 3, y);
			g.drawLine(x + dx + 2, y + 1, x + dx + 3, y + 1);
			x += 1;
			y += 2;
			dx -= 2;
		}
		g.setColor(edge1);
		g.drawLine(xo + (size / 2), yo + size - 1, xo + (size / 2), yo + size - 1);
	}
	
	private void drawUpArrow(Graphics g, int xo, int yo) {
		g.setColor(edge1);
		int x = xo + (size / 2);
		g.drawLine(x, yo, x, yo);
		x--;
		int y = yo + 1;
		int dx = 0;
		while (y + 3 < yo + size) {
			g.setColor(edge1);
			g.drawLine(x, y, x + 1, y);
			g.drawLine(x, y + 1, x + 1, y + 1);
			if (0 < dx) {
				g.setColor(fill);
				g.drawLine(x + 2, y, x + 1 + dx, y);
				g.drawLine(x + 2, y + 1, x + 1 + dx, y + 1);
			}
			g.setColor(edge2);
			g.drawLine(x + dx + 2, y, x + dx + 3, y);
			g.drawLine(x + dx + 2, y + 1, x + dx + 3, y + 1);
			x -= 1;
			y += 2;
			dx += 2;
		}
		g.setColor(edge1);
		g.drawLine(xo, yo + size - 3, xo + 1, yo + size - 3);
		g.setColor(edge2);
		g.drawLine(xo + 2, yo + size - 2, xo + size - 1, yo + size - 2);
		g.drawLine(xo, yo + size - 1, xo + size, yo + size - 1);
	}
	

}

class BlankIcon implements Icon {
	private Color fillColor;

	private int size;

	public BlankIcon() {
		this(null, 11);
	}

	public BlankIcon(Color color, int size) {
		//UIManager.getColor("control")
		//UIManager.getColor("controlShadow")
		fillColor = color;

		this.size = size;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		if (fillColor != null) {
			g.setColor(fillColor);
			g.drawRect(x, y, size - 1, size - 1);
		}
	}

	public int getIconWidth() {
		return size;
	}

	public int getIconHeight() {
		return size;
	}
}
