/*
 * NewCapture.java
 *
 * Created on __DATE__, __TIME__
 */

package sondas.client.inspector.forms;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JOptionPane;

import sondas.client.inspector.ClientException;
import sondas.client.inspector.view.MainFrame;
import sondas.inspector.connectivity.DispatcherManager;

/**
 *
 * @author  __USER__
 */
public class NewCapture extends javax.swing.JDialog {
	private DispatcherManager dm;
	private boolean started;
	private MainFrame mainFrame;

	/** Creates new form NewCapture */
	public NewCapture(MainFrame parent, boolean modal, DispatcherManager dm) {
		super(parent, modal);
		initComponents();
		this.mainFrame=parent;
		bStop.setEnabled(false);
		setLocationRelativeTo(parent);
		this.dm = dm;

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent arg0) {
				if (started) {
					try {
						NewCapture.this.dm.stopCapture();
					} catch (ClientException e) {
						e.printStackTrace();
					}
				}
			}
		});

		setVisible(true);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	//GEN-BEGIN:initComponents
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {

		jLabel1 = new javax.swing.JLabel();
		captureName = new javax.swing.JTextField();
		bStart = new javax.swing.JButton();
		bStop = new javax.swing.JButton();
		bExit = new javax.swing.JButton();
		jLabel2 = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();
		jLabel3 = new javax.swing.JLabel();
		timeSlice = new javax.swing.JTextField();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		jLabel1.setText("Capture name");

		bStart.setText("Start");
		bStart.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				bStartActionPerformed(evt);
			}
		});

		bStop.setText("Stop");
		bStop.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				bStopActionPerformed(evt);
			}
		});

		bExit.setText("Exit");
		bExit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				bExitActionPerformed(evt);
			}
		});

		jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14));
		jLabel2.setForeground(java.awt.Color.red);
		jLabel2.setText("New Capture");

		jLabel3.setText("Time slice");

		timeSlice.setText("30");

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				95,
																				95,
																				95)
																		.add(
																				jLabel2))
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				36,
																				36,
																				36)
																		.add(
																				jLabel1)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				captureName,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																				181,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
										.addContainerGap())
						.add(
								org.jdesktop.layout.GroupLayout.TRAILING,
								layout
										.createSequentialGroup()
										.addContainerGap(129, Short.MAX_VALUE)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING,
																false)
														.add(
																org.jdesktop.layout.GroupLayout.TRAILING,
																progressBar,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																Short.MAX_VALUE)
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				bStart)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED,
																				org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																				Short.MAX_VALUE)
																		.add(
																				bStop)))
										.add(106, 106, 106))
						.add(
								layout
										.createSequentialGroup()
										.add(59, 59, 59)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																org.jdesktop.layout.GroupLayout.TRAILING,
																layout
																		.createSequentialGroup()
																		.add(
																				bExit)
																		.add(
																				31,
																				31,
																				31))
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				jLabel3)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				timeSlice,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																				40,
																				org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																		.addContainerGap(
																				224,
																				Short.MAX_VALUE)))));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(jLabel2)
										.add(35, 35, 35)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(
																captureName,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(jLabel1))
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel3)
														.add(
																timeSlice,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
										.add(27, 27, 27)
										.add(
												progressBar,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(bStart).add(bStop))
										.add(28, 28, 28).add(bExit).add(29, 29,
												29)));

		pack();
	}// </editor-fold>
	//GEN-END:initComponents

	private void bExitActionPerformed(java.awt.event.ActionEvent evt) {
		setVisible(false);

		try {
			if (started) {
				dm.stopCapture();
			}
		} catch (ClientException e) {
			e.printStackTrace();
			CommonForms.showError(this, e);
		} finally {
			dispose();
		}
	}

	private void bStopActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			dm.stopCapture();
			bExit.setEnabled(true);
			bStop.setEnabled(false);
			started = false;
			progressBar.setIndeterminate(false);
			bStart.setEnabled(true);
		} catch (ClientException e) {
			e.printStackTrace();
			CommonForms.showError(this, e);
		}
	}

	private void bStartActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			if (dm.existsCapture(captureName.getText())) {

				Object[] options = { "Yes", "No" };
				int n = JOptionPane.showOptionDialog(this, "Capture "
						+ captureName.getText()
						+ " already exists.\nDo you want to replace it?",
						"Capture exists", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[1]);

				if (n != 0) {
					return;
				}
			}

			int slice;

			try {
				slice = Integer.parseInt(timeSlice.getText());
			} catch (NumberFormatException e) {
				CommonForms.showError(this,
						"Time slice must be an integer greater than 0 seconds");
				return;
			}
			if (slice < 0) {
				CommonForms.showError(this,
						"Time slice must be an integer greater than 0 seconds");
				return;
			}

			dm.startCapture(captureName.getText(), slice);
			bExit.setEnabled(false);
			bStart.setEnabled(false);
			bStop.setEnabled(true);
			started = true;
			progressBar.setIndeterminate(true);

		} catch (ClientException e) {
			e.printStackTrace();
			CommonForms.showError(this, e);
			this.dispose();
			mainFrame.processError();
		}
	}

	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JButton bExit;
	private javax.swing.JButton bStart;
	private javax.swing.JButton bStop;
	private javax.swing.JTextField captureName;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JTextField timeSlice;
	// End of variables declaration//GEN-END:variables

}