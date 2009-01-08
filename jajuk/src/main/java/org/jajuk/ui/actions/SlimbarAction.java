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
 *  $$Revision: 3425 $$
 */

package org.jajuk.ui.actions;

import java.awt.event.ActionEvent;

import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.ui.widgets.JajukSlimbar;
import org.jajuk.ui.widgets.JajukWindow;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;

/**
 * Action to hide slim bar.
 */
public class SlimbarAction extends JajukAction {

  private static final long serialVersionUID = 1L;

  SlimbarAction() {
    super(Messages.getString("JajukSlimWindow.0"), IconLoader.getIcon(JajukIcons.FULL_WINDOW), true);
    setShortDescription(Messages.getString("JajukSlimWindow.0"));
  }

  @Override
  public void perform(ActionEvent evt) throws Exception {
    JajukSlimbar slimbar = JajukSlimbar.getInstance();
    /*
     * If slimbar is visible, hide it and show the main window. Note that both
     * main window and slimbar can be displayed at the same time: If the slimbar
     * is visible and user display main window by right clicking on the tray,
     * the main window is displayed, this is a normal behavior
     */
    if (slimbar.isVisible()) {
      JajukSlimbar.getInstance().setVisible(false);
      JajukWindow.getInstance().display(true);
    } else {
      if (!slimbar.isInitialized()) {
        slimbar.initUI();
      }
      slimbar.setVisible(true);
      JajukWindow.getInstance().display(false);
      // Need focus for keystrokes
      slimbar.requestFocus();
    }
    // Notify that slimbar visibility change (menu bar is interested in it)
    ObservationManager.notify(new JajukEvent(JajukEvents.PARAMETERS_CHANGE));
  }
}