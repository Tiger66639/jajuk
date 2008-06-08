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
 *  $$Revision$$
 */
package org.jajuk.ui.actions;

import java.awt.event.ActionEvent;
import java.util.Properties;

import org.jajuk.base.Directory;
import org.jajuk.events.Event;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.players.FIFO;
import org.jajuk.services.players.StackItem;
import org.jajuk.util.IconLoader;
import org.jajuk.util.Messages; 
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.error.JajukException;

public class FinishAlbumAction extends ActionBase {

  private static final long serialVersionUID = 1L;

  FinishAlbumAction() {
    super(Messages.getString("JajukWindow.16"), IconLoader.ICON_FINISH_ALBUM, !FIFO.isStopped());
    setShortDescription(Messages.getString("JajukWindow.32"));
  }

  @Override
  public void perform(ActionEvent evt) throws JajukException {
    StackItem item = FIFO.getInstance().getCurrentItem();// stores
    // current item
    FIFO.getInstance().clear(); // clear fifo
    Directory dir = item.getFile().getDirectory();
    // then re-add current item
    FIFO.getInstance().push(
        UtilFeatures.createStackItems(dir.getFilesFromFile(item.getFile()), item.isRepeat(), item
            .isUserLaunch()), true);
    FIFO.getInstance().computesPlanned(true); // update planned list
    Properties properties = new Properties();
    properties.put(DETAIL_ORIGIN, DETAIL_SPECIAL_MODE_NORMAL);
    ObservationManager.notify(new Event(JajukEvents.EVENT_SPECIAL_MODE, properties));
  }
}
