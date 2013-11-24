package sondas.client.inspector.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import sondas.client.inspector.ClientException;
import sondas.client.inspector.forms.CommonForms;
import sondas.client.inspector.model.StatsTreeData;
import sondas.utils.Names;

class MetricBean {
	public int metricType;
	public String statName;
	public String statUnit;
	public String nodeName;
	public int id;
	public String format;
	public boolean isAcc;

	MetricBean (int metricType, String statName, String unit, String nodeName, int id, String format, boolean isAcc) {
		this.statName=statName;
		this.metricType=metricType;
		this.statUnit=unit;
		this.nodeName=nodeName;
		this.id=id;
		this.format=format;
		this.isAcc=isAcc;
	}
}

class Node {
	String name;
	boolean isFinal;

	public Node(String name, boolean isFinal) {
		this.name=name;
		this.isFinal=isFinal;
	}

	public String toString() {
		return name;
	}

	public int compare(Node node) {
		return name.compareTo(node.name);
	}
}

public class StatsTree extends JTree 
{
	final private static String rootNodeTitle="Metrics"; 
	private static Object[] hierarchy;
	private static HashMap<String,MetricBean> dictionary = new HashMap<String,MetricBean>(); 

	public StatsTree() {
		super(processHierarchy(new Object[]{rootNodeTitle}));
	}

	/**
	 * categoriesDictionary relaciona un id de categoria con el nodo: Metrics|Servlets por ejemplo
	 * @param items
	 * @param categoriesDictionary
	 */
	public StatsTree(StatsTreeData[] items, Map<Integer,String> categoriesDictionary) 
	{
		super(processHierarchy(items, categoriesDictionary));

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				doMouseClicked(me);
			}
		});
	}

	private void doMouseClicked(MouseEvent me) {
		TreePath tp = getPathForLocation(me.getX(), me.getY());
		if (tp!=null) {
			int count = tp.getPathCount();
			if (count>3) 
			{

				//System.out.println(tp.getPathCount()+","+tp);
				String nodeName =  tp.getPathComponent(count-2).toString();
				String statTitle = tp.getPathComponent(count-1).toString();

				StringBuilder sb = new StringBuilder();
				for (int i=0;i<count;i++) {
					if (i>0) {
						sb.append("|");
					}
					sb.append(tp.getPathComponent(i).toString());
				}

				//MetricBean mb = dictionary.get(tp.getPathComponent(0).toString()+"|"+tp.getPathComponent(1).toString()+"|"+nodeName+"|"+statTitle);
				MetricBean mb = dictionary.get(sb.toString());
				if (mb==null) {
					// No hago nada
					return;
				}


				//MainFrame.getInstance().showTimeMetrics(mb.metricType,nodeName,mb.statName, statTitle, mb.statUnit);
				try {
					MainFrame.getInstance().showTimeMetrics(mb, nodeName, statTitle);
				} catch (ClientException e) {
					CommonForms.showError(this,e);
				}
			}
		}

	}

	/*
	static Object[] hierarchy =
	{ 	"Metrics",
		new Object[] { "Servlets", new Object[]{"Prueba1", "Response time","Invocation count"}, new Object[]{"Prueba2", "Response time","Invocation count"}},
		"EJB",
		"Database",
		new Object[] { "Pool", "Oracle", "Shadow", "EntireX"},
	};
	 */

	/*
	private static Object[] getHierarchy(StatsTreeData[] items,  Map<Integer,String> categoriesDictionary) {
		Object[] resp = new Object[items.length+1];
		resp[0]="Metrics";

		sortStatsTree(items);

		for (int i=0;i<items.length;i++) {
			int nodesCount = items[i].getCategoriesCount();
			int statsCount = items[i].getStatsCount();
			String typeName = items[i].getTypeName(); // Servlets

			Object[] branch = new Object[nodesCount+1];
			resp[i+1]=branch;
			branch[0]= typeName;


			List<Integer> nodes = items[i].getCategories();
			String[] sortedNodes = new String[nodes.size()];
			int j=0;
			for (int categoryId:nodes) {
				// Obtengo el nombre de la categoria: Metrics|Servlets a partir de su Id
				sortedNodes[j++]=categoriesDictionary.get(categoryId);
			}
			sortStrings(sortedNodes);

			List<String> statNames = items[i].getStatNames();
			List<String> statTitles = items[i].getStatTitles();
			List<String> statUnits = items[i].getStatUnits();

			for (int k=0;k<statTitles.size();k++) {
				String statTitle = statTitles.get(k);

				MetricBean mb = new MetricBean(items[i].getMetricType(), statNames.get(k), statUnits.get(k));
				dictionary.put(typeName+":"+statTitle,mb);
			}

			j=1;
			for (String node:sortedNodes) {
				Object[] subBranch = new Object[statsCount+1]; //new Object[]{"Prueba1", "Response time","Invocation count"}
				branch[j++]=subBranch;
				subBranch[0]=node;
				int k=1;
				for (String statTitle:statTitles) {
					subBranch[k++]=statTitle;
				}


			}
		}

		return resp;
	}*/

	/**
	 * Ordena el array de statstree alfabeticamente
	 * @param array
	 * @param len
	 */
	/*
	public static void sortStatsTree(StatsTreeData[] items)
	{
		int a,b;
		StatsTreeData temp;
		int sortTheStrings = items.length - 1;
		for (a = 0; a < sortTheStrings; ++a)
			for (b = 0; b < sortTheStrings; ++b) {
				if(items[b].getTypeName().compareTo(items[b + 1].getTypeName()) >0)
				{
					temp = items[b];
					items[b] = items[b + 1];
					items[b + 1] = temp;	
				}
			}
	}*/

	/*
	public static void sortStrings(String[] items)
	{
		int a,b;
		String temp;
		int sortTheStrings = items.length - 1;
		for (a = 0; a < sortTheStrings; ++a)
			for (b = 0; b < sortTheStrings; ++b) {
				if(items[b].compareTo(items[b + 1]) >0)
				{
					temp = items[b];
					items[b] = items[b + 1];
					items[b + 1] = temp;	
				}
			}
	}*/

	private static DefaultMutableTreeNode processHierarchy(StatsTreeData[] stats, Map<Integer,String> categoriesDictionary)
	{
		HashMap<String, DefaultMutableTreeNode> vertexs = new HashMap<String, DefaultMutableTreeNode>();
		HashMap<String, DefaultMutableTreeNode> catVertexs = new HashMap<String, DefaultMutableTreeNode>();

		// Creo el nodo raiz
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new Node(rootNodeTitle,false));

		for (StatsTreeData std:stats) {
			// Añado cada uno de los std asociados a cada tipo de metrica
			// Tendran el formato pej:
			//  method	(1 Nivel)
			//	pstmt|query (2 Niveles)
			String name = Names.getDigestedName(std.getName(),true);
			StringBuilder sb = new StringBuilder();
			String[] items=tokenize(name);
			// Creo todos los vertex en caso de que no existan
			String vertexKey=null;
			DefaultMutableTreeNode firstNode=null;
			DefaultMutableTreeNode parentNode = null;
			for (int j=0;j<items.length;j++) {
				if (j>0) {
					parentNode = vertexs.get(sb.toString());
					sb.append("|");
				} 
				sb.append(items[j]);
				vertexKey = sb.toString();
				if (!vertexs.containsKey(vertexKey)) {
					// Creo el vertex
					Node aux;
					if (j==items.length-1) {
						aux = new Node(items[j],true);
					} else {
						aux = new Node(items[j],false);
					}
					DefaultMutableTreeNode vertex = new DefaultMutableTreeNode(aux);
					vertexs.put(vertexKey, vertex);
					if (parentNode!=null) {
						addOrderedChild(parentNode,vertex);
					} else {
						firstNode = vertex;
					}
				}
			}
			// Añado las estadisticas al nodo
			List<String> statNames = std.getStatNames();
			List<String> statTitles = std.getStatTitles();
			List<String> statUnits = std.getStatUnits();
			List<String> statFormats = std.getStatFormats();
			List<Boolean> isAccList = std.getIsAccList();
			
			DefaultMutableTreeNode node = vertexs.get(name);
			
			if (node==null) {
				throw new RuntimeException("Error trying to show node "+node+" in time stats tree.");
			}
			
			for (int i=0;i<std.getStatsCount();i++) {
				String statTitle = statTitles.get(i);
				DefaultMutableTreeNode statItem = new DefaultMutableTreeNode(new Node(statTitle,false));

				addOrderedChild(node,statItem);

				MetricBean mb = new MetricBean(std.getMetricType(), statNames.get(i), statUnits.get(i), name, std.getMetricKey(), statFormats.get(i), isAccList.get(i));
				for (int catId:std.getCategories()) {
					String fqName = rootNodeTitle+"|"+categoriesDictionary.get(catId)+"|"+name+"|"+statTitle;
					dictionary.put(fqName,mb);	
				}

			}
			// Añado cada resultado a su categorias. Normalmentesolo tendra una categoria donde poner el resultado
			for (int catId:std.getCategories()) {
				// Coloco cada una de las categorias
				String fqName = categoriesDictionary.get(catId);

				sb = new StringBuilder();
				
				items=tokenize(fqName);
				// Creo todos los vertex en caso de que no existan
				vertexKey=null;
				for (int j=0;j<items.length;j++) {
					if (j>0) {
						sb.append("|");
					}
					sb.append(items[j]);
					vertexKey = sb.toString();
					if (!catVertexs.containsKey(vertexKey)) {
						// Creo el vertex
						DefaultMutableTreeNode vertex = new DefaultMutableTreeNode(new Node(items[j],false));
						catVertexs.put(vertexKey, vertex);

						if (j==0) {
							// primer nivel no existe => lo añado al root
							addOrderedChild(rootNode,vertex);
						}
					}
				}

				DefaultMutableTreeNode catNode = catVertexs.get(fqName);

				if (firstNode!=null) {
					addOrderedChild(catNode,firstNode);
				}
			}
		}

		return rootNode;

	}

	private static void addOrderedChild(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
		
		int childCount = parent.getChildCount();
		Node childNode = (Node)child.getUserObject();
		
		boolean finalChild;
		if (childNode.isFinal) {
			finalChild = true;
		} else {
			finalChild = false;
		}

		if (childCount==0) {
			parent.add(child);
			return;
		}
		
		// Primero van los nodos finales y despues los nodos no finales
		for (int i=0;i<childCount;i++) {
			Node auxNode = (Node)((DefaultMutableTreeNode)parent.getChildAt(i)).getUserObject();
			if (finalChild) {
				// Final child
				if (auxNode.isFinal) {
					// Final
					// Si auxNode es mayor que node, añado node antes 
					//if (auxNode.compare(childNode)>0) {
					if (childNode.compare(auxNode)<0) {
						parent.insert(child, i); ///////////////////
						break;
					} else if (i==childCount-1) {
						// Es el ultimo
						parent.add(child);
						break;
					}
				} else {
					// No final
					// Inserto el nodo antes del no final
						parent.insert(child, i);
						break;
				}
			} else {
				// No final child, van abajo del todo
				if (auxNode.isFinal) {
					if (i==childCount-1) {
						// Solo hay nodos finales=> lo añado
						parent.add(child);
						break;
					}
					// En caso contrario no hago nada
				} else {
					if (auxNode.compare(childNode)>0) {
						parent.insert(child, i);
						break;
					} else if (i==childCount-1) {
						// Es el ultimo
						parent.add(child);
						break;
					}
				}
			}
		}
		
		//parent.add(child);
	}

	private static String[] tokenize(String object) {
		StringTokenizer st = new StringTokenizer(object,"|");
		int count = st.countTokens();
		String[] resp = new String[count];
		for (int i=0;i<count;i++) {
			resp[i]=st.nextToken();
		}
		return resp;
	}

	private static DefaultMutableTreeNode processHierarchy(Object[] hierarchy) 
	{

		DefaultMutableTreeNode node = new DefaultMutableTreeNode(hierarchy[0]);
		DefaultMutableTreeNode child;
		for(int i=1; i<hierarchy.length; i++) {
			Object nodeSpecifier = hierarchy[i];
			if (nodeSpecifier instanceof Object[])  // Ie node with children
				child = processHierarchy((Object[])nodeSpecifier);
			else
				child = new DefaultMutableTreeNode(nodeSpecifier); // Ie Leaf
			node.add(child);
		}
		return(node);
	}
}
