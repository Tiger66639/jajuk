/*
 *  Jajuk
 *  Copyright (C) 2007 The Jajuk Team
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $$Revision$$
 */

package org.jajuk.ui.wizard;

import org.jajuk.i18n.Messages;
import org.jajuk.ui.JajukJDialog;
import org.jajuk.ui.OKCancelPanel;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;
import org.jajuk.util.log.Log;
import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.Message;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * 
 * Quality Feedback agent
 */
public class QualityFeedbackWizard extends JajukJDialog implements KeyListener,
		ActionListener, ITechnicalStrings {
	private static final long serialVersionUID = 1L;

	JPanel jpMain;

	JTextArea jtaNotice;

	JLabel jlType;

	JComboBox jcbType;

	JLabel jlFrom;

	JTextField jtfFrom;

	JLabel jlDesc;

	JTextField jtfDesc;

	JLabel jlDetail;

	JTextArea jtaDetail;

	OKCancelPanel okp;

	/**
	 * Constructor
	 */
	public QualityFeedbackWizard() {
		setTitle(Messages.getString("JajukJMenuBar.19")); 
		getContentPane().setPreferredSize(new Dimension(800, 600));
		// Notice
		jtaNotice = new JTextArea(Messages.getString("QualityFeedbackWizard.7")); 
		jtaNotice.setLineWrap(true);
		jtaNotice.setWrapStyleWord(true);
		jtaNotice.setEditable(false);
		jtaNotice.setMargin(new Insets(10, 10, 10, 10));
		jtaNotice.setOpaque(true);
		jtaNotice.setBackground(Color.ORANGE);
		jtaNotice.setForeground(Color.DARK_GRAY);
		jtaNotice.setFont(new Font("Dialog", Font.BOLD, 12)); 
		// From
		jlFrom = new JLabel(Messages.getString("QualityFeedbackWizard.1")); 
		jlFrom.setToolTipText(Messages.getString("QualityFeedbackWizard.2")); 
		jtfFrom = new JTextField();
		jtfFrom.setToolTipText(Messages.getString("QualityFeedbackWizard.2")); 
		// Description
		jlDesc = new JLabel(Messages.getString("QualityFeedbackWizard.3")); 
		jlDesc.setToolTipText(Messages.getString("QualityFeedbackWizard.4")); 
		jtfDesc = new JTextField();
		jtfDesc.setToolTipText(Messages.getString("QualityFeedbackWizard.4")); 
		jtfDesc.addKeyListener(this);
		// Type
		jlType = new JLabel(Messages.getString("QualityFeedbackWizard.12")); 
		jlType.setToolTipText(Messages.getString("QualityFeedbackWizard.12")); 
		jcbType = new JComboBox();
		jcbType.addItem(Messages.getString("QualityFeedbackWizard.8")); 
		jcbType.addItem(Messages.getString("QualityFeedbackWizard.9")); 
		jcbType.addItem(Messages.getString("QualityFeedbackWizard.10")); 
		jcbType.addItem(Messages.getString("QualityFeedbackWizard.11")); 
		jcbType.setToolTipText(Messages.getString("QualityFeedbackWizard.12")); 
		// Details
		jlDetail = new JLabel(Messages.getString("QualityFeedbackWizard.5")); 
		jlDetail.setToolTipText(Messages.getString("QualityFeedbackWizard.6")); 
		jtaDetail = new JTextArea();
		jtaDetail.setToolTipText(Messages.getString("QualityFeedbackWizard.6")); 
		okp = new OKCancelPanel(this);
		okp.getOKButton().setEnabled(false);
		jpMain = new JPanel();
		int iXSeparator = 10;
		int iYSeparator = 20;
		double[][] dSize = {
				{ iXSeparator, 0.3, iXSeparator, 0.7, iXSeparator },
				{ iYSeparator, TableLayout.PREFERRED, iYSeparator,
						TableLayout.PREFERRED, iYSeparator, 20, iYSeparator,
						20, iYSeparator, 0.99, iYSeparator, 20, iYSeparator } };
		jpMain.setLayout(new TableLayout(dSize));
		jpMain.add(new JLabel(Util.getImage(IMAGE_WRITE)), "1,1"); 
		jpMain.add(jtaNotice, "3,1"); 
		jpMain.add(jlType, "1,3"); 
		jpMain.add(jcbType, "3,3"); 
		jpMain.add(jlFrom, "1,5"); 
		jpMain.add(jtfFrom, "3,5"); 
		jpMain.add(jlDesc, "1,7"); 
		jpMain.add(jtfDesc, "3,7"); 
		jpMain.add(jlDetail, "1,9"); 
		jpMain.add(new JScrollPane(jtaDetail), "3,9"); 
		jpMain.add(okp, "3,11"); 
		getContentPane().add(jpMain);
		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				jtfFrom.requestFocusInWindow();
			}
		});
		getRootPane().setDefaultButton(okp.getOKButton());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource().equals(this.okp.getOKButton())) {
			final Message message = new Message();
			message.setSubject(jtfDesc.getText());
			ArrayList<String> alTo = new ArrayList<String>();
			alTo.add(FEEDBACK_EMAIL);
			message.setToAddrs(alTo);
			String sBody = ""; 
			sBody += "Type: " + jcbType.getSelectedItem() + '\n'; 
			sBody += "From: " + jtfFrom.getText() + '\n'; 
			sBody += "Subject: " + jtfDesc.getText() + '\n'; 
			sBody += "Details: " + jtaDetail.getText() + '\n'; 
			sBody += "Version: " + JAJUK_VERSION + '\n'; 
			if (jcbType.getSelectedIndex() == 0) { // bug
				sBody += Util.getAnonymizedSystemProperties().toString() + '\n';
				sBody += Util.getAnonymizedJajukProperties().toString() + '\n';
				Iterator it = Log.getSpool();
				while (it.hasNext()) {
					sBody += it.next().toString() + '\n';
				}
			}
			message.setBody(sBody);
			new Thread() {
				public void run() {
					try {
						Desktop.mail(message);
					} catch (Exception e) {
						Messages.showErrorMessage("136"); 
						Log.error(e);
					}
				}
			}.start();
			dispose();
		} else if (ae.getSource().equals(this.okp.getCancelButton())) {
			dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		if (!jtfDesc.getText().trim().equals("")) { 
			okp.getOKButton().setEnabled(true);
		} else {
			okp.getOKButton().setEnabled(false);
		}
	}
}