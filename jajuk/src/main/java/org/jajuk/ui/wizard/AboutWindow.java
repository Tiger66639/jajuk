/*
 *  Jajuk
 *  Copyright (C) 2005 The Jajuk Team
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
 *  $$Revision: 2644 $$
 */

package org.jajuk.ui.wizard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jajuk.ui.widgets.JajukJDialog;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.log.Log;
import org.jfree.ui.about.AboutPanel;
import org.jfree.ui.about.Licences;
import org.jfree.ui.about.SystemPropertiesPanel;

/**
 * View used to show the Jajuk about and contributors.
 * <p>
 * Help perspective *
 */
public class AboutWindow extends JajukJDialog {

  private static final long serialVersionUID = 1L;

  /** Licence panel */
  private JPanel jpLicence;

  /** General informations panel */
  private AboutPanel ap;

  /** JVM properties panel */
  private SystemPropertiesPanel spp;

  /** Tabbed pane with previous panels */
  private JTabbedPane jtp;

  /** Additional informations */
  private static final String INFOS = "http://jajuk.info";

  /**
   * Constructor
   */
  public AboutWindow() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setTitle(Messages.getString("JajukJMenuBar.16"));
        initUI();
        setLocationByPlatform(true);
        setSize(new Dimension(600, 300));
        setVisible(true);
      }

    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.IView#display()
   */
  public void initUI() {
    // license panel
    jpLicence = new JPanel(new BorderLayout());
    JTextArea jta = new JTextArea(Licences.getInstance().getGPL());
    jta.setLineWrap(true);
    jta.setWrapStyleWord(true);
    jta.setCaretPosition(0);
    jta.setEditable(false);
    jta.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent me) {
        if (me.getClickCount() == 1
            && ((me.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK)
            && ((me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK)) {
          try {
            UtilGUI.showPictureDialog("http://www.jajuk.info/images/flbf.jpg");
          } catch (Exception e) {
            Log.debug("Ignoring exception in AboutWindow: ", e);
          }
        }
      }
    });

    jpLicence.add(new JScrollPane(jta));
    jtp = new JTabbedPane();
    JPanel jpAbout = new JPanel();
    jpAbout.setLayout(new BoxLayout(jpAbout, BoxLayout.Y_AXIS));
    ap = new AboutPanel("Jajuk", Const.JAJUK_VERSION + " <" + Const.JAJUK_CODENAME + ">" + " "
        + Const.JAJUK_VERSION_DATE, Messages.getString("AboutView.11"), INFOS, IconLoader.getIcon(
        JajukIcons.LOGO).getImage());
    jpAbout.add(ap);
    jpAbout.add(Box.createVerticalGlue());
    spp = new SystemPropertiesPanel();
    jtp.addTab(Messages.getString("AboutView.7"), jpAbout);
    jtp.addTab(Messages.getString("AboutView.8"), jpLicence);
    jtp.addTab(Messages.getString("AboutView.9"), spp);
    add(jtp);

    // Add key listener to enable Escape key to close the window
    this.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        // allow to close the dialog with Escape
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          dispose();
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.IView#getDesc()
   */
  public String getDesc() {
    return Messages.getString("AboutView.10");
  }
}
