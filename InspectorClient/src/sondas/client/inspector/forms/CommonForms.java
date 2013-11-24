package sondas.client.inspector.forms;

import java.awt.Component;

import javax.swing.JOptionPane;

public class CommonForms {
	
	public static void showMessage(Component parent, String msg, String title, int msgType) {
		JOptionPane.showMessageDialog(parent, msg,title, msgType);
	}
	
	public static void showError(Component parent, String msg, Throwable e) {
		String _msg = e.getLocalizedMessage();
		if (_msg==null) {
			_msg = "Undefined error: "+e;
		}
		
		if (msg!=null) {
			msg = msg+":"+_msg;
		} else {
			msg = _msg;
		}
		
		while (e.getCause()!=null) {
			msg+=": "+e.getCause().getLocalizedMessage();
			e = e.getCause();
		}
		CommonForms.showMessage(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void showError(Component parent, Throwable e) {
		showError(parent, null, e);
	}
	
	public static void showError(Component parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg,"Error", JOptionPane.ERROR_MESSAGE);
	}
}
