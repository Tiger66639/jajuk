/*
 *  Jajuk
 *  Copyright (C) 2003 bflorat
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
 * $Log$
 * Revision 1.4  2003/10/12 21:08:11  bflorat
 * 12/10/2003
 *
 * Revision 1.3  2003/10/10 15:53:30  bflorat
 * Clean up, tests are now in tests/bflorat
 *
 * Revision 1.2  2003/10/09 21:15:29  bflorat
 * *** empty log message ***
 *
 * Revision 1.1  2003/10/07 21:02:23  bflorat
 * Initial commit
 *
 */
package org.jajuk;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;

import org.jajuk.base.FIFO;
import org.jajuk.base.TechnicalStrings;
import org.jajuk.base.Type;
import org.jajuk.base.TypesManager;
import org.jajuk.i18n.Messages;
import org.jajuk.ui.CommandJPanel;
import org.jajuk.ui.InformationJPanel;
import org.jajuk.ui.JajukJMenuBar;
import org.jajuk.ui.PerspectiveBarJPanel;
import org.jajuk.ui.perspectives.IPerspectiveManager;
import org.jajuk.ui.perspectives.PerspectiveManagerFactory;
import org.jajuk.util.log.Log;

/**
 * Jajuk lauching class
 *
 * @author     bflorat
 * @created    3 oct. 2003
 */
public class Main implements TechnicalStrings{

	public static JFrame jframe;
	public static CommandJPanel command;
	public static PerspectiveBarJPanel perspectiveBar;
	public static InformationJPanel information;
	

	public static void main(String[] args) {
		try {
			//perform initial checkups
			initialCheckups();

			//log startup
			Log.getInstance();
			Log.setVerbosity(Log.DEBUG);

			//Display user configuration
			Log.debug(System.getProperties().toString());

			//starts ui
			jframe = new JFrame("Jajuk : Just Another Jukebox"); //$NON-NLS-1$
			Dimension dScreenSize=Toolkit.getDefaultToolkit().getScreenSize();
			jframe.setSize((int)(0.9*dScreenSize.getWidth()),(int)(0.9*dScreenSize.getHeight()));
			//TODO see for automatic maximalize
			jframe.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					exit(0);
				}
			});
			Container container = jframe.getContentPane();
			// Create the perspective manager
			IPerspectiveManager perspectiveManager =
				PerspectiveManagerFactory.getPerspectiveManager();
			perspectiveManager.setParentContainer(container);

			//Creates the command panel
			command = new CommandJPanel();

			// Create the perspective tool bar panel
			perspectiveBar = new PerspectiveBarJPanel();

			// Create the information bar panel
			information = new InformationJPanel();
			//****temp
			information.setMessage("Now playing foo track...",InformationJPanel.INFORMATIVE); //temp //$NON-NLS-1$
			information.setSelection("124 items : 4.5Mo"); //temp //$NON-NLS-1$
			information.setCurrentStatusMessage("00:01:02/00:05:12"); //$NON-NLS-1$
			information.setTotalStatus(50);
			information.setTotalStatusMessage("00:23:23/01:34:56"); //$NON-NLS-1$
			information.setCurrentStatus(76);
			//**************************
			//registers supported types
			try {
				TypesManager.registerType(new Type(Messages.getString("Main.Mpeg_layer_3_5"),EXT_MP3,PLAYER_IMPL_JAVALAYER)); //$NON-NLS-1$ //$NON-NLS-2$
				TypesManager.registerType(new Type(Messages.getString("Main.Playlist_7"),EXT_PLAYLIST,PLAYER_IMPL_JAVALAYER)); //$NON-NLS-1$ //$NON-NLS-2$
				TypesManager.registerType(new Type(Messages.getString("Main.Ogg_vorbis_9"),EXT_OGG,PLAYER_IMPL_JAVALAYER)); //$NON-NLS-1$ //$NON-NLS-2$
				
			} catch (Exception e1) {
				Log.error(Messages.getString("Main.Error_registering_players_11"),e1); //$NON-NLS-1$
			}
			//Starts the FIFO
			new FIFO().start();
			
			//Add static panels
			container.add(command, BorderLayout.NORTH);
			container.add(perspectiveBar, BorderLayout.WEST);
			container.add(information, BorderLayout.SOUTH);

			//Set menu bar to the frame
			jframe.setJMenuBar(JajukJMenuBar.getInstance());
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			jframe.show();
		} catch (Exception e) { //last chance to catch any error for logging purpose
			Log.error(Messages.getString("Main.uncatched_exception_2"), e); //$NON-NLS-1$
			exit(1);
		}
	}

	/**
	 * Performs some basic startup tests
	 * @throws Exception
	 */
	private static void initialCheckups() throws Exception {
		//check for jajuk home directory presence
		File fJajukDir = new File(System.getProperty("user.home") + "/.jajuk"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!fJajukDir.exists() || !fJajukDir.isDirectory()) {
			fJajukDir.mkdir(); //create the directory if it doesn't exist
		}
	}
	
	/**
	 * Exit code, used to perform saves...
	 * @param iExitCode exit code 
	 * <p>0 : normal exit
	 * <p>1: unexpected error
	 */
	public static void exit(int iExitCode){
			System.exit(iExitCode);
			
	}
	
}
