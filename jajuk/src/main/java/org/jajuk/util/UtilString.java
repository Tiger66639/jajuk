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
package org.jajuk.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.jajuk.base.AlbumManager;
import org.jajuk.base.AuthorManager;
import org.jajuk.base.PropertyMetaInformation;
import org.jajuk.base.StyleManager;
import org.jajuk.base.Track;
import org.jajuk.util.error.JajukException;

/**
 * Set of convenient classes for string manipulation
 */
public final class UtilString implements ITechnicalStrings {
  
  /**
   * The list of characters that we need to escape in strings
   */
  private final static String ESCAPE_CHARACTERS = "\\[](){}.*+?$^|-";
  
  /**
   * private constructor to avoid instantiating utility class
   */
  private UtilString() {
  }

  /**
   * Apply the Album pattern.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @param normalize
   * @param out
   * @param track
   * @return
   * @throws JajukException
   */
  static String applyAlbumPattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final boolean normalize, final String out, final Track track)
      throws JajukException {
    String ret = out;
    String sValue;
    if (sPattern.contains(ITechnicalStrings.PATTERN_ALBUM)) {
      sValue = track.getAlbum().getName();
      if (normalize) {
        sValue = UtilSystem.getNormalizedFilename(sValue);
      }
      if (!sValue.equals(ITechnicalStrings.UNKNOWN_ALBUM)) {
        ret = ret.replace(ITechnicalStrings.PATTERN_ALBUM, AlbumManager.format(sValue));
      } else {
        if (bMandatory) {
          throw new JajukException(149, file.getAbsolutePath());
        } else {
          ret = ret.replace(ITechnicalStrings.PATTERN_ALBUM, Messages
              .getString(ITechnicalStrings.UNKNOWN_ALBUM));
        }
      }
    }
    return ret;
  }

  /**
   * Apply the Year pattern.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @param out
   * @param track
   * @return
   * @throws JajukException
   */
  static String applyYearPattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final String out, final Track track) throws JajukException {
    String ret = out;
    if (sPattern.contains(ITechnicalStrings.PATTERN_YEAR)) {
      if (track.getYear().getValue() != 0) {
        ret = ret.replace(ITechnicalStrings.PATTERN_YEAR, track.getYear().getValue() + "");
      } else {
        if (bMandatory) {
          throw new JajukException(148, file.getAbsolutePath());
        } else {
          ret = ret.replace(ITechnicalStrings.PATTERN_YEAR, "?");
        }
      }
    }
    return ret;
  }

  /**
   * Apply the Track pattern.
   * 
   * @param sPattern
   * @param normalize
   * @param out
   * @param track
   * @return
   */
  static String applyTrackPattern(final String sPattern, final boolean normalize, final String out,
      final Track track) {
    String ret = out;
    String sValue;
    if (sPattern.contains(ITechnicalStrings.PATTERN_TRACKNAME)) {
      sValue = track.getName();
      if (normalize) {
        sValue = UtilSystem.getNormalizedFilename(sValue);
      }
      ret = ret.replace(ITechnicalStrings.PATTERN_TRACKNAME, sValue);
    }
    return ret;
  }

  /**
   * Apply the Track Order pattern.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @param out
   * @param track
   * @return
   * @throws JajukException
   */
  static String applyTrackOrderPattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final String out, final Track track) throws JajukException {
    if (sPattern.contains(ITechnicalStrings.PATTERN_TRACKORDER)) {
      // override Order from filename if not set explicitly
      long lOrder = track.getOrder();
      if (lOrder == 0) {
        final String sFilename = file.getName();
        if (Character.isDigit(sFilename.charAt(0))) {
          final String sTo = sFilename.substring(0, 3).trim().replaceAll("[^0-9]", "");
          for (final char c : sTo.toCharArray()) {
            if (!Character.isDigit(c)) {
              throw new JajukException(152, file.getAbsolutePath());
            }
          }
          lOrder = Long.parseLong(sTo);
        } else {
          if (bMandatory) {
            throw new JajukException(152, file.getAbsolutePath());
          } else {
            lOrder = 0;
          }
        }
      } 

      // prepend one digit numbers with "0"
      if (lOrder < 10) {
        return out.replace(ITechnicalStrings.PATTERN_TRACKORDER, "0" + lOrder);
      } else {
        return out.replace(ITechnicalStrings.PATTERN_TRACKORDER, lOrder + "");
      }
    }

    return out;
  }

  /**
   * Apply the Style pattern.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @param normalize
   * @param out
   * @param track
   * @return
   * @throws JajukException
   */
  static String applyStylePattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final boolean normalize, final String out, final Track track)
      throws JajukException {
    String ret = out;
    String sValue;
    if (sPattern.contains(ITechnicalStrings.PATTERN_STYLE)) {
      sValue = track.getStyle().getName();
      if (normalize) {
        sValue = UtilSystem.getNormalizedFilename(sValue);
      }
      if (!sValue.equals(ITechnicalStrings.UNKNOWN_STYLE)) {
        ret = ret.replace(ITechnicalStrings.PATTERN_STYLE, StyleManager.format(sValue));
      } else {
        if (bMandatory) {
          throw new JajukException(153, file.getAbsolutePath());
        } else {
          ret = ret.replace(ITechnicalStrings.PATTERN_STYLE, Messages
              .getString(ITechnicalStrings.UNKNOWN_STYLE));
        }
      }
    }
    return ret;
  }

  /**
   * Apply the Author pattern.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @param normalize
   * @param out
   * @param track
   * @return
   * @throws JajukException
   */
  private static String applyAuthorPattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final boolean normalize, final String out, final Track track)
      throws JajukException {
    String ret = out;
    String sValue;
    if (sPattern.contains(ITechnicalStrings.PATTERN_AUTHOR)) {
      sValue = track.getAuthor().getName();
      if (normalize) {
        sValue = UtilSystem.getNormalizedFilename(sValue);
      }
      if (!sValue.equals(ITechnicalStrings.UNKNOWN_AUTHOR)) {
        ret = ret.replaceAll(ITechnicalStrings.PATTERN_AUTHOR, AuthorManager.format(sValue));
      } else {
        if (bMandatory) {
          throw new JajukException(150, file.getAbsolutePath());
        } else {
          ret = ret.replaceAll(ITechnicalStrings.PATTERN_AUTHOR, Messages
              .getString(ITechnicalStrings.UNKNOWN_AUTHOR));
        }
      }
    }
    return ret;
  }

  /**
   * Apply a pattern. This replaces certain patterns in the provided Pattern
   * with information from the file and returns the result.
   * 
   * @param file
   *          file to apply pattern to
   * @param sPattern
   * @param bMandatory
   *          are all needed tags mandatory ?
   * @return computed string
   * @return make sure the created string can be used as file name on target
   *         file system
   * @throws JajukException
   *           if some tags are missing
   */
  public static String applyPattern(final org.jajuk.base.File file, final String sPattern,
      final boolean bMandatory, final boolean normalize) throws JajukException {
    String out = sPattern;
    final Track track = file.getTrack();

    // Check Author name
    out = applyAuthorPattern(file, sPattern, bMandatory, normalize, out, track);

    // Check Style name
    out = UtilString.applyStylePattern(file, sPattern, bMandatory, normalize, out, track);

    // Check Album Name
    out = UtilString.applyAlbumPattern(file, sPattern, bMandatory, normalize, out, track);

    // Check Track Order
    out = UtilString.applyTrackOrderPattern(file, sPattern, bMandatory, out, track);

    // Check Track name
    out = UtilString.applyTrackPattern(sPattern, normalize, out, track);

    // Check Year Value
    out = UtilString.applyYearPattern(file, sPattern, bMandatory, out, track);

    return out;
  }

  /**
   * @param s
   *          String to analyse
   * @return whether the given string contains non digit or letter characters
   */
  public static boolean containsNonDigitOrLetters(final String s) {
    boolean bOK = false;
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isLetterOrDigit(s.charAt(i))) {
        bOK = true;
        break;
      }
    }
    return bOK;
  }

  /**
   * Encode URLS
   * 
   * @param s
   * @return
   */
  public static String encodeString(final String s) {
    return s.replaceAll(" +", "+");
  }

  /**
   * Build the frame title from user option
   * 
   * @param file
   *          played file
   * @return built frame title
   */
  public static String buildTitle(final org.jajuk.base.File file) {
    // We use trailing pattern to allow scripting like MSN plugins to
    // detect jajuk frames and extract current track
    String title = ConfigurationManager.getProperty(ITechnicalStrings.CONF_FRAME_TITLE_PATTERN);
    title = title.replaceAll(PATTERN_TRACKNAME, file.getTrack().getName());
    title = title.replaceAll(PATTERN_ALBUM, file.getTrack().getAlbum().getName2());
    title = title.replaceAll(PATTERN_AUTHOR, file.getTrack().getAuthor().getName2());
    title = title.replaceAll(PATTERN_STYLE, file.getTrack().getStyle().getName2());
    title = title.replaceAll(PATTERN_TRACKORDER, Long.toString(file.getTrack().getOrder()));
    title = title.replaceAll(PATTERN_YEAR, file.getTrack().getYear().getName2());
    return title;
  }

  /*
   * Escape (in the regexp sense) a string Source Search reserved: $ ( ) * + - . ? [ \ ] ^ { | }
   * http://mindprod.com/jgloss/regex.html
   */
  public static String escapeString(String s) {
    int length = s.length();
    StringBuffer buffer = new StringBuffer(2 * length);
    for (int i = 0; i != length; i++) {
      char c = s.charAt(i);
      
      // if we have a character that needs to be escaped, we prepend backslash before it
      if(ESCAPE_CHARACTERS.indexOf(c) != -1) {
        buffer.append('\\');
      }

      // now append the actual character
      buffer.append(c);
    }
    return buffer.toString();
  }

  /**
   * Format an object to a string.
   * 
   * @param sValue
   * @param cType
   * @param bHuman
   *          is this string intended to be human-readable ?
   * @return
   * @throws Exception
   */
  public static String format(final Object oValue, final PropertyMetaInformation meta,
      final boolean bHuman) throws Exception {
    final Class<?> cType = meta.getType();
    // default (works for strings, long and double)
    String sValue = oValue.toString();
    if (cType.equals(Date.class)) {
      if (bHuman) {
        sValue = getLocaleDateFormatter().format((Date) oValue);
      } else {
        sValue = UtilString.getAdditionDateFormatter().format((Date) oValue);
      }
    } else if (cType.equals(Class.class)) {
      sValue = oValue.getClass().getName();
    }
    return sValue;
  }

  /**
   * @return locale date formatter instance
   */
  public static DateFormat getLocaleDateFormatter() {
    return DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
  }

  /**
   * Formatter for properties dialog window
   * 
   * @param sDesc
   * @return
   */
  public static String formatPropertyDesc(final String sDesc) {
    return "<HTML><center><b><font size=+0 color=#000000>" + sDesc + "</font></b><HTML>";
  }

  /**
   * format style: first letter uppercase and others lowercase
   * 
   * @param style
   * @return
   */
  public static String formatStyle(final String style) {
    if (style.length() == 0) {
      return "";
    }
    if (style.length() == 1) {
      return style.substring(0, 1).toUpperCase();
    }
    String sOut = style.toLowerCase().substring(1);
    sOut = style.substring(0, 1).toUpperCase() + sOut;
    return sOut;
  }

  /**
   * Performs some cleanups for strings comming from tag libs
   * 
   * @param s
   * @return
   */
  public static String formatTag(final String s) {
    // we delete all non char characters to avoid parsing errors
    char c;
    final StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      c = s.charAt(i);
      if (UtilString.isChar(c)) {
        sb.append(c);
      }
    }
    return sb.toString().trim();
  }

  /** Format a time from secs to a human readable format */
  public static String formatTimeBySec(final long lTime) {
    // Convert time to int for performance reasons
    int l = (int) lTime;
    if (l == -1) { // means we are in repeat mode
      return "--:--";
    } else if (l < 0) {
      // make sure to to get negative values
      l = 0;
    }
    final int hours = l / 3600;
    final int mins = l / 60 - (hours * 60);
    final int secs = l - (hours * 3600) - (mins * 60);
    final StringBuilder sbResult = new StringBuilder(8);
    if (hours > 0) {
      sbResult.append(UtilString.padNumber(hours, 2)).append(":");
    }
    return sbResult.append(UtilString.padNumber(mins, 2)).append(":").append(
        UtilString.padNumber(secs, 2)).toString();
  }

  /**
   * Format a string before XML write
   * <p>
   * see http://www.w3.org/TR/2000/REC-xml-20001006
   * <p>
   * substrings
   * <p>' to &apos;
   * <p>" to &quot;
   * <p>< to &lt;
   * <p>> to &gt;
   * <p>& to &amp;
   * 
   * @param s
   * @return
   */
  public static String formatXML(final String s) {
    String sOut = replaceReservedXMLChars(s);
    final StringBuilder sbOut = new StringBuilder(sOut.length());
    /*
     * Transform String to XML-valid characters. XML 1.0 specs ; Character Range
     * [2] Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
     * [#x10000-#x10FFFF] any Unicode character, excluding the surrogate blocks,
     * FFFE, and FFFF.
     */
    for (int i = 0; i < sOut.length(); i++) {
      final char c = sOut.charAt(i);
      if (UtilString.isChar(c)) {
        sbOut.append(c);
      }
    }
    return sbOut.toString();
  }

  /**
   * @param s
   * @return
   */
  private static String replaceReservedXMLChars(final String s) {
    String sOut = s;
    if (s.contains("&")) {
      sOut = sOut.replaceAll("&", "&amp;");
    }
    if (s.contains("\'")) {
      sOut = sOut.replaceAll("\'", "&apos;");
    }
    if (s.contains("\"")) {
      sOut = sOut.replaceAll("\"", "&quot;");
    }
    if (s.contains("<")) {
      sOut = sOut.replaceAll("<", "&lt;");
    }
    if (s.contains(">")) {
      sOut = sOut.replaceAll(">", "&gt;");
    }
    return sOut;
  }

  /**
   * @return Addition date simple format instance
   */
  public static DateFormat getAdditionDateFormatter() {
    return new SimpleDateFormat(ITechnicalStrings.ADDITION_DATE_FORMAT);
  }

  /**
   * @return Anonymized Jajuk properties (for log or quality agent)
   */
  public static Properties getAnonymizedJajukProperties() {
    final Properties properties = (Properties) ConfigurationManager.getProperties().clone();
    // We remove sensible data from logs
    properties.remove("jajuk.network.proxy_login");
    properties.remove("jajuk.network.proxy_port");
    properties.remove("jajuk.network.proxy_hostname");
    properties.remove("jajuk.options.p2p.password");
    return properties;
  }

  /**
   * @return Anonymized System properties (for log or quality agent)
   */
  public static Properties getAnonymizedSystemProperties() {
    final Properties properties = (Properties) System.getProperties().clone();
    // We remove sensible data from logs
    /*
     * can contain external program paths
     */
    properties.remove("java.library.path");
    properties.remove("java.class.path");
    // user name is private
    properties.remove("user.name");
    properties.remove("java.ext.dirs");
    properties.remove("sun.boot.class.path");
    properties.remove("deployment.user.security.trusted.certs");
    properties.remove("deployment.user.security.trusted.clientauthcerts");
    properties.remove("jajuk.log");

    return properties;
  }

  /**
   * Make sure to reduce a string to the given size
   * 
   * @param sIn
   *          Input string, exemple: blabla
   * @param iSize
   *          max size, exemple: 3
   * @return bla...
   */
  public static String getLimitedString(final String sIn, final int iSize) {
    String sOut = sIn;
    if (sIn.length() > iSize) {
      sOut = sIn.substring(0, iSize) + "...";
    }
    return sOut;
  }

  /**
   * @param ucs4char
   *          char to test
   * @return whether the char is valid, code taken from Apache sax
   *         implementation
   */
  public static boolean isChar(final int ucs4char) {
    return ((ucs4char >= 32) && (ucs4char <= 55295)) || (ucs4char == 10) || (ucs4char == 9)
        || (ucs4char == 13) || ((ucs4char >= 57344) && (ucs4char <= 65533))
        || ((ucs4char >= 0x10000) && (ucs4char <= 0x10ffff));
  }

  /**
   * 
   * @param s
   *          String to test
   * @return whether the string is void or not
   */
  public static boolean isVoid(final String s) {
    return (s == null) || s.trim().equals("");
  }

  /**
   * @param s
   * @return whether given string is XML-valid
   */
  public static boolean isXMLValid(final String s) {
    // check reserved chars
    if (s.contains("&") || s.contains("\'") || s.contains("\"") || s.contains("<")
        || s.contains(">")) {
      return false;
    }
    // check invalid chars
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (!UtilString.isChar(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Pad an int with zeros
   * 
   * @param l
   *          the number to be padded
   * @param size
   *          the targeted size
   * @return
   */
  public static String padNumber(final long l, final int size) {
    final StringBuilder sb = new StringBuilder(Long.toString(l));
    while (sb.length() < size) {
      sb.insert(0, '0');
    }
    return sb.toString();
  }

  /**
   * Parse a string to an object
   * 
   * @param sValue
   * @param cType
   * @return parsed item
   * @throws Exception
   */
  public static Object parse(final String sValue, final Class<?> cType) throws Exception {
    Object oDefaultValue = sValue; // String by default
    if (cType.equals(Boolean.class)) {
      // "y" and "n" is an old boolean
      // attribute notation prior to 1.0
      if (sValue.equals("y")) {
        oDefaultValue = true;
      } else if (sValue.equals("n")) {
        oDefaultValue = false;
      } else {
        oDefaultValue = Boolean.parseBoolean(sValue);
      }
    } else if (cType.equals(Date.class)) {
      oDefaultValue = getAdditionDateFormatter().parseObject(sValue);
    } else if (cType.equals(Long.class)) {
      oDefaultValue = Long.parseLong(sValue);
    } else if (cType.equals(Double.class)) {
      oDefaultValue = Double.parseDouble(sValue);
    } else if (cType.equals(Class.class)) {
      oDefaultValue = Class.forName(sValue);
    }
    return oDefaultValue;
  }

  /**
   * Rot13 encode/decode,
   * <p>
   * Thx
   * http://www.idevelopment.info/data/Programming/java/security/java_cryptography_extension/rot13.java
   * </p>
   * 
   * @param in
   *          text to encode / decode in rote 13
   * @return encoded /decoded text
   */
  public static String rot13(final String in) {
    if (UtilString.isVoid(in)) {
      return "";
    }
    int abyte = 0;
    final StringBuilder tempReturn = new StringBuilder();
    for (int i = 0; i < in.length(); i++) {
      abyte = in.charAt(i);
      int cap = abyte & 32;
      abyte &= ~cap;
      abyte = ((abyte >= 'A') && (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;
      tempReturn.append((char) abyte);
    }
    return tempReturn.toString();
  }

  /**
   * 
   * @param s
   *          string to be checked
   * @return whether provided string is a number or not
   */
  public static boolean isNumber(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if ((c < '0') || (c > '9')) {
        return false;
      }
    }
    return true;
  }

  /**
   * 
   * @param tested
   *          the string to be tested
   * @param key
   *          the search criteria, can be several words separated by a space
   * @return whether the given tested string matches the key
   */
  public static boolean matchesIgnoreCaseAndOrder(final String tested, final String key) {
    String testedLower = tested.toLowerCase();
    String keyLower = key.toLowerCase();
    StringTokenizer st = new StringTokenizer(testedLower, " ");
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (keyLower.indexOf(token) == -1) {
        return false;
      }
    }
    return true;
  }

}