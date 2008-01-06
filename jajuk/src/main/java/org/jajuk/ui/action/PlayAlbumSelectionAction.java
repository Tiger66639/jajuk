/*
 *  Jajuk
 *  Copyright (C) 2003-2008 The Jajuk Team
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
 *  $Revision: 3132 $
 */
package org.jajuk.ui.action;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jajuk.base.Album;
import org.jajuk.base.File;
import org.jajuk.base.Track;
import org.jajuk.services.players.FIFO;
import org.jajuk.util.ConfigurationManager;
import org.jajuk.util.IconLoader;
import org.jajuk.util.Messages;
import org.jajuk.util.Util;

/**
 * Play albums for a selection. We expect the selection to be tracks and we play
 * only the first found album
 * <p>
 * Action emitter is responsible to ensure all items provided share the same
 * type
 * </p>
 * <p>
 * Selection data is provided using the swing properties DETAIL_SELECTION
 * </p>
 */
public class PlayAlbumSelectionAction extends SelectionAction {

  private static final long serialVersionUID = -8078402652430413821L;

  PlayAlbumSelectionAction() {
    super(Messages.getString("TracksTableView.11"), IconLoader.ICON_ALBUM, true);
    setShortDescription(Messages.getString("TracksTableView.11"));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.action.ActionBase#perform(java.awt.event.ActionEvent)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void perform(ActionEvent e) throws Exception {
    super.perform(e);
    if (selection.size() == 0 || !(selection.get(0) instanceof Track)) {
      return;
    }
    // Select all files from the first found album
    Album album = ((Track) selection.get(0)).getAlbum();
    List<File> files = Util.getPlayableFiles(album);
    FIFO.getInstance().push(
        Util.createStackItems(Util.applyPlayOption(files), ConfigurationManager
            .getBoolean(CONF_STATE_REPEAT), true), false);
  }

}