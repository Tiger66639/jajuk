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
 *  $Revision$
 */

package org.jajuk.base;

import java.awt.MediaTracker;
import java.net.URL;

import javax.swing.ImageIcon;

import org.jajuk.util.ConfigurationManager;
import org.jajuk.util.DownloadManager;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;


/**
 *  A cover, encapsulates URL, files and manages cover priority to display
 *
 * @author     bflorat
 * @created    22 août 2004
 */
public class Cover implements Comparable,ITechnicalStrings {

    public static final int LOCAL_COVER = 0;
    public static final int REMOTE_COVER = 1;
    public static final int DEFAULT_COVER = 2;
    public static final int ABSOLUTE_DEFAULT_COVER = 3;
    
    /**Cover URL**/
    private URL url;
    
    /**Cover Type*/
    private int iType;
    
    /**Image*/
    private ImageIcon image;
    
    /**Image data*/
    private byte[] bData;
     
   /**
   * Constructor
    * @param sUrl cover url : absolute path for a local file, http url for a remote file
    * @param iType
    */
    public Cover(URL url, int iType){
        long l = System.currentTimeMillis();
        this.url = url;
        this.iType = iType;
        //if Pre-load option is enabled, load this cover
        if (ConfigurationManager.getBoolean(CONF_COVERS_PRELOAD)){
            try{
                image = getImage();
            }
            catch(Exception e){ //means download failed
                image = null;
            }
        }
    }
    
       
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Cover cOther = (Cover)o;
        //check if the 2 covers are identical
       if (cOther.equals(this)){
            return 0;
        }
        //check absolute covers
        if (getType() == ABSOLUTE_DEFAULT_COVER){
            return 1;
        }
        else if (cOther.getType() == ABSOLUTE_DEFAULT_COVER){
            return -1;
        }
        //Default cover is the less prioritory
        if (getType() == DEFAULT_COVER){
            if (cOther.getType() == DEFAULT_COVER){
                return 0; //i'm a default cover and ther other too
            }
            else{
                return -1; //i'm a defautl cover and the other not
            }
        }
         //local covers are prioritary upon remote ones :
        else if ( getType() == LOCAL_COVER ){
            if (cOther.getType()!=LOCAL_COVER){ //the other is not a local cover
                return 1; //i'm a local cover and the other not
            }
            else{ //both are local covers, analyse name
               String sFile = Util.getOnlyFile(getURL().getFile());
               String sOtherFile = Util.getOnlyFile(cOther.getURL().getFile());
                //     files named "cover" or "front" are prioritary upon others : 
                if ( Util.isStandardCover(sFile)){
                    if ( !Util.isStandardCover(sOtherFile)){
                        return 1; //i'm a local standard cover and the other is only a local non-standard cover
                    }
                    else{
                        return 0; //both are local-standard covers
                    }
                }
                else{
                    if ( Util.isStandardCover(sOtherFile)){
                        return -1;//i'm a local cover and the other is local standard cover 
                    }
                    else{
                        return 0; //both are local non-standard covers
                    }
                }        
            }
        }
        return 0; //any other case
    }

    /**
     * @return Returns the iType.
     */
    public int getType() {
        return iType;
    }
    /**
     * @return Returns the sURL.
     */
    public URL getURL() {
        return url;
    }
    
    
    /**
     * @return Returns the image.
     */
    public ImageIcon getImage() throws Exception {
        if (image == null){ //do nothing if the image is already loaded
            long l = System.currentTimeMillis();
            if ( iType == LOCAL_COVER || iType == DEFAULT_COVER  || iType == ABSOLUTE_DEFAULT_COVER){
                this.image = new ImageIcon(url);
            }
            else if (iType == REMOTE_COVER){
                bData = DownloadManager.download(url);
                this.image = new ImageIcon(bData); 
                if ( image.getImageLoadStatus() != MediaTracker.COMPLETE){
                    throw new JajukException("129"); //$NON-NLS-1$
                }
            }
            Log.debug("Loaded "+url.toString()+" in  "+(System.currentTimeMillis()-l)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        return image;
    }
    
    /**
     * toString method
     */
    public String toString(){
        return "Type="+iType +" URL="+url; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Equals needed for consitency for sorting
     */
    public boolean equals(Object o){
       boolean bOut = false;
        Cover cOther = (Cover)o;
       if (getType() == Cover.ABSOLUTE_DEFAULT_COVER || cOther.getType()==Cover.ABSOLUTE_DEFAULT_COVER){
           return  (cOther.getType() == getType()); //either both are default cover, either one is not and so, they are unequal
       }
       //here, all url are not null
       //for local covers, we concidere that 2 covers with the same file name are identical even if they are in different directories
       if (getType() != Cover.REMOTE_COVER){
           return Util.getOnlyFile(url.getFile()).equals(Util.getOnlyFile(cOther.getURL().getFile()));
       }
       //Remote cover
       bOut = url.getFile().equals(cOther.getURL().getFile());
       return bOut;
    }
    
    public byte[] getData() {
        return bData;
    }
}
