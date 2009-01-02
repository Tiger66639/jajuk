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
 *  $Revision$
 *  
 *  Comes initially from aTunes 1.8
 */

package org.jajuk.services.lyrics.providers;

import java.net.MalformedURLException;

import ext.XMLUtils;
import ext.services.network.NetworkUtils;

import org.jajuk.util.Const;
import org.jajuk.util.log.Log;
import org.w3c.dom.Document;

/**
 * Lyrics Provider extracting lyrics from lyricwiki.org
 * 
 */
public class LyricWikiProvider extends GenericProvider {

  /** URL pattern used by jajuk to retrieve lyrics */
  private static final String URL = "http://lyricwiki.org/api.php?func=getSong&artist=%artist&song=%title&fmt=xml";

  /** URL pattern to web page (see ILyricsProvider interface for details) */
  private static final String WEB_URL = "http://lyricwiki.org/%artist:%title";

  /**
   * Charset to use
   */
  private static final String CHARSET = "UTF-8";

  public LyricWikiProvider() {
    super(URL);
  }

  /*
   * (non-Javadoc)
   * 
   * @see ext.services.lyrics.providers.GenericProvider#getLyrics(java.lang.String,
   *      java.lang.String)
   */
  public String getLyrics(final String artist, final String title) {
    try {
      String xml = callProvider(artist, title);
      Document document = XMLUtils.getDocument(xml);
      String lyrics = XMLUtils.getChildElementContent(document.getDocumentElement(), "lyrics");
      if (lyrics == null || lyrics.trim().equalsIgnoreCase("Not found")) {
        return null;
      }
      return lyrics;
    } catch (Exception e) {
      Log.error(e);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.services.lyrics.providers.ILyricsProvider#getResponseEncoding()
   */
  public String getResponseEncoding() {
    return "UTF-8";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.services.lyrics.providers.ILyricsProvider#getWebURL(java.lang.String,
   *      java.lang.String)
   */
  public java.net.URL getWebURL(final String pArtist, final String pTitle) {
    String queryString = WEB_URL;
    // Replace spaces by _
    String artist = pArtist.replaceAll(" ", "_");
    String title = pTitle.replaceAll(" ", "_");
    
    queryString = queryString.replace(Const.PATTERN_AUTHOR, (artist != null) ? NetworkUtils
        .encodeString(artist) : "");
    queryString = queryString.replace(Const.PATTERN_TRACKNAME, (title != null) ? NetworkUtils
        .encodeString(title) : "");

    java.net.URL out = null;
    try {
      out = new java.net.URL(queryString);
    } catch (MalformedURLException e) {
      Log.error(e);
    }
    return out;
  }

}