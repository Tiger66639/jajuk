/*
 *  Jajuk
 *  Copyright (C) 2003-2009 The Jajuk Team
 *  http://jajuk.info
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
package org.jajuk.base;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares albums
 * 
 * @TODO Convert criteria from int to an enum
 */
public class AlbumComparator implements Comparator<Album>, Serializable {
  private static final long serialVersionUID = -5927167916548652076L;

  /*
   * This needs to be kept in-sync with what we use in
   * CatalogView.initMetaInformation()!
   * 
   * 0 .. style 1 .. author 2 .. album 3 .. year 4 .. discovery date 5 .. rate 6 ..
   * hits
   */
  private int criteria = 0;

  public AlbumComparator(int criteria) {
    this.criteria = criteria;
  }

  public int compare(Album album1, Album album2) {
    // for albums, perform a fast compare
    if (criteria == 2) {
      return album1.compareTo(album2);
    }
    // get a track for each album
    Track track1 = album1.getAnyTrack();
    Track track2 = album2.getAnyTrack();

    // check tracks (normally useless)
    if (track1 == null || track2 == null) {
      return 0;
    }

    String albumArtist1 = null;
    String albumArtist2 = null;

    // @TODO
    // beware, this code is not consistent with equals. This should be ok as
    // result is used by a List but it could be a drama if we used a Set
    // See : http: // java.sun.com/j2se/1.4.2/docs/api/java/lang/Comparable.html
    switch (criteria) {
    case 0: // style
      // Cache this, time consuming
      albumArtist1 = album1.getAlbumArtistOrArtist();
      albumArtist2 = album2.getAlbumArtistOrArtist();

      // Sort on Style/Author/Year/Title
      if (track1.getStyle() == track2.getStyle()) {
        // [Perf] We can make this '==' comparison because all these strings are
        // internalized
        if (albumArtist1 == albumArtist2) {
          if (track1.getYear() == track2.getYear()) {
            return album1.compareTo(album2);
          } else {
            return track1.getYear().compareTo(track2.getYear());
          }
        } else {
          return albumArtist1.compareTo(albumArtist2);
        }
      } else {
        return track1.getStyle().compareTo(track2.getStyle());
      }
    case 1: // author
      // Cache this, time consuming
      albumArtist1 = album1.getAlbumArtistOrArtist();
      albumArtist2 = album2.getAlbumArtistOrArtist();

      // Sort on Author/Year/Title
      // we use now the album artist
      if (albumArtist1 == albumArtist2) {
        if (track1.getYear() == track2.getYear()) {
          return album1.compareTo(album2);
        } else {
          return track1.getYear().compareTo(track2.getYear());
        }
      } else {
        return albumArtist1.compareTo(albumArtist2);
      }
    case 3: // year
      // Cache this, time consuming
      albumArtist1 = album1.getAlbumArtistOrArtist();
      albumArtist2 = album2.getAlbumArtistOrArtist();

      // Sort on: Year/Author/Title
      if (track1.getYear() == track2.getYear()) {
        if (albumArtist1 == albumArtist2) {
          return album1.compareTo(album2);
        } else {
          return albumArtist1.compareTo(albumArtist2);
        }
      } else {
        return track1.getYear().compareTo(track2.getYear());
      }
    case 4: // Discovery date
      // Sort on: Discovery date/title
      if (track1.getDiscoveryDate().equals(track2.getDiscoveryDate())) {
        return album1.compareTo(album2);
      } else {
        return track2.getDiscoveryDate().compareTo(track1.getDiscoveryDate());
      }
    case 5: // Rate
      // Sort on: Rate/title
      if (album1.getRate() == album2.getRate()) {
        return album1.compareTo(album2);
      } else if (album1.getRate() < album2.getRate()) {
        return -1;
      } else {
        return 1;
      }
    case 6: // Hits
      if (album1.getHits() == album2.getHits()) {
        return album1.compareTo(album2);
      } else if (album1.getHits() < album2.getHits()) {
        return -1;
      } else {
        return 1;
      }
    }
    return 0;
  }
}
