/*
 * Jajuk Copyright (C) 2003 bflorat
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307,
 * USA. $Log$
 * USA. Revision 1.5  2003/10/28 21:34:37  bflorat
 * USA. 28/10/2003
 * USA.
 * USA. Revision 1.4  2003/10/26 21:28:49  bflorat
 * USA. 26/10/2003
 * USA. Place - Suite 330, Boston, MA 02111-1307, USA. Revision 1.3 2003/10/24 15:44:25 bflorat Place - Suite 330, Boston, MA 02111-1307, USA. 24/10/2003 Place - Suite 330,
 * Boston, MA 02111-1307, USA. Revision 1.2 2003/10/23 22:07:40 bflorat 23/10/2003
 * 
 * Revision 1.1 2003/10/21 17:51:43 bflorat 21/10/2003
 *  
 */
package org.jajuk.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.jajuk.util.JajukFileFilter;
import org.jajuk.util.MD5Processor;
import org.jajuk.util.Util;
import org.jajuk.util.log.Log;

/**
 * A physical directory
 * <p>
 * Physical item
 * 
 * @Author bflorat @created 17 oct. 2003
 */
public class Directory extends PropertyAdapter {

	/** ID. Ex:1,2,3... */
	private String sId;
	/** directory name. Ex: rock */
	private String sName;
	/** Parent directory ID* */
	private Directory dParent;
	/** Directory device */
	private Device device;
	/** Child directories */
	private ArrayList alDirectories = new ArrayList(20);
	/** Child files */
	private ArrayList alFiles = new ArrayList(20);
	/** IO file for optimizations* */
	private java.io.File fio;

	/**
	 * Direcotry constructor
	 * 
	 * @param id
	 * @param sName
	 * @param style
	 * @param author
	 */
	public Directory(String sId, String sName, Directory dParent, Device device) {
		this.sId = sId;
		this.sName = sName;
		this.dParent = dParent;
		this.device = device;
		this.fio = new File(device.getUrl() + getAbsolutePath());
	}

	/**
	 * toString method
	 */
	public String toString() {
		return "Directory[ID=" + sId + " Name=" + getAbsolutePath() + " Parent ID=" + (dParent == null ? "null" : dParent.getId()) + " Device=" + device.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$
	}

	/**
	 * Return an XML representation of this item
	 * 
	 * @return
	 */
	public String toXml() {
		StringBuffer sb = new StringBuffer("\t\t<directory id='" + sId);
		sb.append("' name='");
		sb.append(Util.formatXML(sName));
		sb.append("' parent='");
		String sParent = "-1";
		if (dParent!=null){
			sParent = dParent.getId();
		}
		sb.append(sParent);
		sb.append("' device='");
		sb.append(device.getId()).append("' ");
		sb.append(getPropertiesXml());
		sb.append("/>\n");
		return sb.toString();
	}

	/**
	 * Equal method to check two directories are identical
	 * 
	 * @param otherDirectory
	 * @return
	 */
	public boolean equals(Directory otherDirectory) {
		return this.getId().equals(otherDirectory.getId() );
	}

	/**
	 * @return
	 */
	public Device getDevice() {
		return device;
	}

	/**
	 * @return
	 */
	public String getId() {
		return sId;
	}

	/**
	 * @return
	 */
	public String getName() {
		return sName;
	}

	/**
	 * @return
	 */
	public Directory getParentDirectory() {
		return dParent;
	}

	/**
	 * @return
	 */
	public ArrayList getDirectories() {
		return alDirectories;
	}

	/**
	 * @param directory
	 */
	public void addDirectory(Directory directory) {
		alDirectories.add(directory);
	}
	
	/**
		 * @return
		 */
		public ArrayList getFiles() {
			return alFiles;
		}

		/**
		 * @param directory
		 */
		public void addFile(File file) {
			alFiles.add(file);
		}


	/**
	 * Scan all files in a directory
	 * 
	 * @param
	 */
	public void scan() {
		java.io.File[] files = getFio().listFiles(JajukFileFilter.getInstance(false, true));
		for (int i = 0; i < files.length; i++) {
			if (TypeManager.getTypeByExtension(Util.getExtension(files[i])).isMusic()) {
				
				/*org.jajuk.base.File fCompare = new org.jajuk.base.File("0",fio.getName(),this,null,0,"");
				ArrayList alFiles = FileManager.getFiles();
				int index = alFiles.indexOf(fCompare);
				if (index != -1){  //file already exists, we don't rescan it 
					org.jajuk.base.File file = (org.jajuk.base.File)alFiles.get(index);
					FileManager.registerFile(file.getName(), this, file.getTrack(), file.getSize(), file.getQuality());
					return;
				}*/
				Tag tag = new Tag(files[i]);
				String sTrackName = tag.getTrackName();
				String sAlbumName = tag.getAlbumName();
				String sAuthorName = tag.getAuthorName();
				String sStyle = tag.getStyleName();
				long length = tag.getLength(); //length in sec
				String sYear = tag.getYear();
				String sQuality = tag.getQuality();
				
				Album album = AlbumManager.registerAlbum(sAlbumName);
				Style style = StyleManager.registerStyle(sStyle);
				Author author = AuthorManager.registerAuthor(sAuthorName);
				Type type = TypeManager.getTypeByExtension(Util.getExtension(files[i]));
				Track track = TrackManager.registerTrack(sTrackName, album, style, author, length, sYear, type);
				org.jajuk.base.File newFile = FileManager.registerFile(files[i].getName(), this, track, files[i].length(), sQuality);
				Log.debug("Found File: "+ newFile);
				TrackManager.getTrack(track.getId()).addFile(newFile);
			}
			else{  //playlist file
				try{
					String sName = files[i].getName();
					BufferedReader br = new BufferedReader(new FileReader(files[i]));
					StringBuffer sbContent = new StringBuffer();
					String sTemp;
					do{
						sTemp = br.readLine();
						sbContent.append(sTemp);
					}
					while ( sTemp != null);
					String sHashcode =MD5Processor.hash(sbContent.toString()); 
					PlaylistFile plFile = PlaylistFileManager.registerPlaylistFile(sName,sHashcode,this);
					PlaylistManager.registerPlaylist(plFile);
					Log.debug("Found Playlist file: "+ plFile);
				}
				catch(Exception e){
					Log.error(e);
				}
				
			}
		}
	}

	/**
	 * Return full directory path name
	 * 
	 * @return String
	 */
	public String getAbsolutePath() {
		StringBuffer sbOut = new StringBuffer().append(java.io.File.separatorChar).append(getName());
		boolean bTop = false;
		Directory dCurrent = this;
		while (!bTop) {
			dCurrent = dCurrent.getParentDirectory();
			if (dCurrent != null) { //if it is the root directory, no
				// parent
				sbOut.insert(0, java.io.File.separatorChar).insert(1, dCurrent.getName());
			} else {
				bTop = true;
			}
		}
		return sbOut.toString();
	}

	/**
	 * @return Returns the IO file reference to this directory.
	 */
	public File getFio() {
		return fio;
	}

}
