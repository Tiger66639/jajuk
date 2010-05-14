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
 *  $Revision$
 */
package org.jajuk.services.players;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.jajuk.base.Album;
import org.jajuk.base.Device;
import org.jajuk.base.Directory;
import org.jajuk.base.File;
import org.jajuk.base.FileManager;
import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.core.SessionService;
import org.jajuk.services.webradio.WebRadio;
import org.jajuk.ui.helpers.JajukTimer;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.UtilString;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * Manages playing sequences
 * <p>
 * Avoid to synchronize these methods because they are called very often and AWT
 * dispatcher thread is frozen when JVM execute a static synchronized method,
 * even outside AWT dispatcher thread
 * </p>
 * 
 * General todo-items:
 * 
 * TODO: we catch exceptions a lot in various places here. Why? We should
 * probably rather avoid or handle them correctly
 * 
 * TODO: insert() and push() are quite similar, but implemented differently,
 * they should be combined
 * 
 * TOOD: the queue/planned handling is cumbersome and planned tracks are not
 * correctly handled sometimes, should be refactored into separate class
 */
public final class QueueModel {

  /** Currently played track index. */
  private static int index;

  /** Last played track. */
  private static StackItem itemLast;

  /** The Fifo itself, contains jajuk File objects. This also includes an optional bunch of planned tracks which are accessible with separate methods. */
  private static volatile QueueList alQueue = new QueueList();

  /** Stop flag*. */
  private static volatile boolean bStop = true;

  /** First played file flag. */
  private static boolean bFirstFile = true;

  /** Whether we are currently playing radio. */
  private static boolean playingRadio = false;

  /** Current played radio. */
  private static WebRadio currentRadio;

  /**
   * No constructor, this class is used statically only.
   */
  private QueueModel() {
  }

  /**
   * FIFO total re-initialization.
   */
  public static void reset() {
    clear();
    JajukTimer.getInstance().reset();
    itemLast = null;
  }

  /**
   * Remove all items from the given album just before and after the given
   * index, i.e. remove all tracks before and after the current one that have
   * the same album.
   * 
   * @param index
   *          The index from where to remove.
   * @param album
   *          The album to remove.
   */
  public static void resetAround(int index, Album album) {
    int begin = 0;
    int end = 0;
    for (int i = index; i >= 0; i--) {
      if (alQueue.get(i).getFile().getTrack().getAlbum().equals(album)) {
        begin = i;
      }
    }
    for (int i = index; i < alQueue.size(); i++) {
      if (alQueue.get(i).getFile().getTrack().getAlbum().equals(album)) {
        end = i;
      }
    }
    remove(begin, end);

  }

  /**
   * Set given repeat mode to all in FIFO.
   * 
   * @param bRepeat
   *          True, if repeat mode should be turned on, false otherwise.
   */
  public static void setRepeatModeToAll(boolean bRepeat) {
    for (StackItem item : alQueue) {
      item.setRepeat(bRepeat);
    }
  }

  /**
   * Asynchronous version of push (needed to perform long-task out of awt
   * dispatcher thread).
   * 
   * @param alItems
   *          The list of items to push.
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   */
  public static void push(final List<StackItem> alItems, final boolean bKeepPrevious) {
    push(alItems, bKeepPrevious, false);
  }

  /**
   * Asynchronous version of push (needed to perform long-task out of awt
   * dispatcher thread).
   * 
   * @param alItems
   *          The list of items to push.
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   * @param bPushNext
   *          whether the selection is added after playing track (mutual
   *          exclusive with simple push)
   */
  public static void push(final List<StackItem> alItems, final boolean bKeepPrevious,
      final boolean bPushNext) {
    Thread t = new Thread("Queue Push Thread") { // do it in a thread to make
      // UI more reactive
      @Override
      public void run() {
        try {
          UtilGUI.waiting();
          pushCommand(alItems, bKeepPrevious, bPushNext);
        } catch (Exception e) {
          Log.error(e);
        } finally {
          // refresh playlist editor
          ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
          UtilGUI.stopWaiting();
        }
      }
    };
    t.setPriority(Thread.MAX_PRIORITY);
    t.start();
  }

  /**
   * Asynchronous version of push (needed to perform long-task out of awt
   * dispatcher thread).
   * 
   * @param item
   *          The item to push.
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   */
  public static void push(final StackItem item, final boolean bKeepPrevious) {
    push(item, bKeepPrevious, false);
  }

  /**
   * Asynchronous version of push (needed to perform long-task out of awt
   * dispatcher thread).
   * 
   * @param item
   *          The item to push.
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   * @param bPushNext
   *          whether the selection is added after playing track (mutual
   *          exclusive with simple push)
   */
  public static void push(final StackItem item, final boolean bKeepPrevious, final boolean bPushNext) {
    Thread t = new Thread("Queue Push Thread") {
      // do it in a thread to make UI more reactive
      @Override
      public void run() {
        try {
          UtilGUI.waiting();
          pushCommand(item, bKeepPrevious, bPushNext);
        } catch (Exception e) {
          Log.error(e);
        } finally {
          // refresh queue
          ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
          UtilGUI.stopWaiting();
        }
      }
    };
    t.setPriority(Thread.MAX_PRIORITY);
    t.start();
  }

  /**
   * Launch a web radio.
   * 
   * @param radio
   *          webradio to launch
   */
  public static void launchRadio(WebRadio radio) {
    try {
      UtilGUI.waiting();
      /**
       * Force buttons to opening mode by default, then if they start correctly,
       * a PLAYER_PLAY event will be notified to update to final state. We
       * notify synchronously to make sure the order between these two events
       * will be correct*
       */
      ObservationManager.notifySync(new JajukEvent(JajukEvents.PLAY_OPENING));
      currentRadio = radio;
      // Play the stream
      boolean bPlayOK = Player.play(radio);
      if (bPlayOK) { // refresh covers if play is started
        Log.debug("Now playing :" + radio.toString());
        playingRadio = true;
        // Store current radio for next startup
        Conf.setProperty(Const.CONF_DEFAULT_WEB_RADIO, radio.getName());
        // Send an event that a track has been launched
        Properties pDetails = new Properties();
        pDetails.put(Const.DETAIL_CONTENT, radio);
        ObservationManager.notify(new JajukEvent(JajukEvents.WEBRADIO_LAUNCHED, pDetails));
        bStop = false;
      }
    } catch (Throwable t) {// catch even Errors (OutOfMemory for example)
      Log.error(122, t);
      playingRadio = false;
    } finally {
      UtilGUI.stopWaiting(); // stop the waiting cursor
    }
  }

  /**
   * Push some files in the fifo.
   * 
   * @param item
   *          , item to be played
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   * @param bPushNext
   *          whether the selection is added after playing track (mutual
   *          exclusive with simple push)
   */
  private static void pushCommand(StackItem item, boolean bKeepPrevious, final boolean bPushNext) {
    List<StackItem> alFiles = new ArrayList<StackItem>(1);
    alFiles.add(item);
    pushCommand(alFiles, bKeepPrevious, bPushNext);
  }

  /**
   * Push some stack items in the fifo.
   * 
   * @param alItems
   *          , list of items to be played
   * @param bKeepPrevious
   *          keep previous files or stop them to start a new one ?
   * @param bPushNext
   *          whether the selection is added in first in queue
   */
  private static void pushCommand(List<StackItem> alItems, boolean bKeepPrevious,
      final boolean bPushNext) {
    try {
      // wake up FIFO if stopped
      bStop = false;
      // first try to mount needed devices
      Iterator<StackItem> it = alItems.iterator();
      boolean bNoMount = false;
      while (it.hasNext()) {
        StackItem item = it.next();
        if (item == null) {
          it.remove();
          break;
        }
        // Do not synchronize this as we will wait for user response
        if (!item.getFile().getDirectory().getDevice().isMounted()) {
          if (!bNoMount) {
            // not mounted, ok let them a chance to mount it:
            final String sMessage = Messages.getString("Error.025") + " ("
                + item.getFile().getDirectory().getDevice().getName()
                + Messages.getString("FIFO.4");
            int i = Messages.getChoice(sMessage, JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
            if (i == JOptionPane.YES_OPTION) {
              try {
                item.getFile().getDirectory().getDevice().mount(true);
              } catch (Exception e) {
                it.remove();
                Log.error(e);
                Messages.showErrorMessage(11, item.getFile().getDirectory().getDevice().getName());
                return;
              }
            } else if (i == JOptionPane.NO_OPTION) {
              bNoMount = true; // do not ask again
              it.remove();
            } else if (i == JOptionPane.CANCEL_OPTION) {
              return;
            }
          } else {
            it.remove();
          }
        }
      }
      synchronized (QueueModel.class) {
        // test if we have yet some files to consider
        if (alItems.size() == 0) {
          Messages.showWarningMessage(Messages.getString("Warning.6"));
          return;
        }

        // Position of insert into the queue
        int pos = (alQueue.size() == 0) ? 0 : alQueue.size();

        // OK, stop current track if no append
        if (!bKeepPrevious && !bPushNext) {
          index = pos;
          Player.stop(false);
        }
        // if push to front, set pos to first item
        else if (bPushNext) {
          pos = index + 1;
        }
        // If push, not play, add items at the end
        else if (bKeepPrevious && alQueue.size() > 0) {
          pos = alQueue.size();
        }

        // add required tracks in the FIFO
        for (StackItem item : alItems) {
          if (pos >= alQueue.size()) {
            alQueue.add(item);
          } else {
            alQueue.add(pos, item);
          }
          pos++;
          JajukTimer.getInstance().addTrackTime(item.getFile());
        }
        // Apply repeat mode if required.
        if (Conf.getBoolean(Const.CONF_STATE_REPEAT_ALL)) {
          setRepeatModeToAll(true);
        }
        // launch track if required
        if (!Player.isPlaying()) {
          launch();
        }
        // computes planned tracks
        computesPlanned(true);
      }
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Contains repeated item.
   * 
   * @param items
   *          The items to check for repeat.
   * 
   * @return whether a stack item list contains a least one repeated item
   */
  private static boolean containsRepeatedItem(List<StackItem> items) {
    for (StackItem item : items) {
      if (item.isRepeat()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Contains repeat.
   * 
   * @return whether FIFO contains a least one repeated item
   */
  public static boolean containsRepeat() {
    return alQueue.containsRepeat();
  }

  /**
   * Finished method, called by the PlayerImpl when the track is finished or
   * should be finished (in case of intro mode or crass fade).
   */
  public static void finished() {
    finished(false);
  }

  /**
   * Finished method, called by the PlayerImpl when the track is finished or
   * should be finished (in case of intro mode or crass fade). IF foreceNext,
   * the next track is played, even in single repeat.
   * 
   * @param forceNext
   *          DOCUMENT_ME
   */
  public static void finished(boolean forceNext) {
    try {
      // If no playing item, just leave
      StackItem current = getCurrentItem();
      if (current == null) {
        return;
      }
      if (getPlayingFile() != null) {
        Properties details = new Properties();
        details.put(Const.DETAIL_CURRENT_FILE, getPlayingFile());
        ObservationManager.notify(new JajukEvent(JajukEvents.FILE_FINISHED, details));
      }

      if (current.isRepeat()) {
        // if the track was in repeat mode, don't remove it from the
        // fifo but inc index
        // find the next item is in repeat mode if any
        if (index < alQueue.size() - 1) {
          StackItem itemNext = alQueue.get(index + 1);
          // if next track is repeat, inc index
          if (itemNext.isRepeat() || forceNext) {
            index++;
            // no next track in repeat mode, come back to first
            // element in fifo
          }
        } else {
          if (Conf.getBoolean(Const.CONF_STATE_SHUFFLE) && alQueue.containsOnlyRepeat()) {
            UtilFeatures.forcedShuffle(alQueue);
          }
          StackItem itemNext = alQueue.get(0);
          // if next track is repeat, inc index
          if (itemNext.isRepeat() || forceNext) {
            index = 0;
          }
        }
      } else if (index < alQueue.size()) {
        StackItem item = alQueue.get(index);
        JajukTimer.getInstance().removeTrackTime(item.getFile());
        if (Conf.getBoolean(Const.CONF_DROP_PLAYED_TRACKS_FROM_QUEUE)) {
          alQueue.remove(index); // remove this file from fifo
        } else {
          index++;
        }
      }
      if (alQueue.size() == 0 || index >= alQueue.size()) { // nothing more to
        // play
        // check if we in continue mode
        if (Conf.getBoolean(Const.CONF_STATE_CONTINUE) && itemLast != null) {
          final StackItem item = alQueue.popNextPlanned();
          final File file;
          // if some tracks are planned (can be 0 if planned size=0)
          if (item != null) {
            file = item.getFile();
          } else {
            // otherwise, take next track from file manager
            file = FileManager.getInstance().getNextFile(itemLast.getFile());
          }

          if (file != null) {
            // push it, it will be played
            pushCommand(new StackItem(file), false, false);
          } else {
            // probably end of collection option "restart" off
            setEndOfQueue();
          }
        } else {
          setEndOfQueue();
          return;
        }
      } else {
        // something more in FIFO
        launch();
      }
      // computes planned tracks
      computesPlanned(false);
    } catch (Exception e) {
      Log.error(e);
    } finally {
      // refresh playlist editor
      ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
    }
  }

  /**
   * Sets the end of queue.
   */
  private static void setEndOfQueue() {
    // no ? just reset UI and leave
    JajukTimer.getInstance().reset();
    bStop = true;
    index = 0;
    if (alQueue.size() > 0) {
      ObservationManager.notify(new JajukEvent(JajukEvents.PLAYER_STOP));
    } else {
      ObservationManager.notify(new JajukEvent(JajukEvents.ZERO));
    }
  }

  /**
   * Launch track at given index in the fifo.
   */
  private static void launch() {
    try {
      UtilGUI.waiting();
      File fCurrent = getPlayingFile();
      /**
       * Force buttons to opening mode by default, then if they start correctly,
       * a PLAYER_PLAY event will be notified to update to final state. We
       * notify synchronously to make sure the order between these two events
       * will be correct*
       */
      ObservationManager.notifySync(new JajukEvent(JajukEvents.PLAY_OPENING));

      // check if we are in single repeat mode, transfer it to new launched
      // track
      if (Conf.getBoolean(Const.CONF_STATE_REPEAT)) {
        setRepeatModeToAll(false);
        getCurrentItem().setRepeat(true);
        ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
      }

      boolean bPlayOK = false;
      if (bFirstFile && !Conf.getBoolean(Const.CONF_STATE_INTRO)
          && Conf.getString(Const.CONF_STARTUP_MODE).equals(Const.STARTUP_MODE_LAST_KEEP_POS)) {
        // if it is the first played file of the session and we are in
        // startup mode keep position
        float fPos = Conf.getFloat(Const.CONF_STARTUP_LAST_POSITION);
        // play it
        bPlayOK = Player.play(fCurrent, fPos, Const.TO_THE_END);
      } else {
        if (Conf.getBoolean(Const.CONF_STATE_INTRO)) {
          // intro mode enabled
          bPlayOK = Player.play(fCurrent, Float.parseFloat(Conf
              .getString(Const.CONF_OPTIONS_INTRO_BEGIN)) / 100, 1000 * Integer.parseInt(Conf
              .getString(Const.CONF_OPTIONS_INTRO_LENGTH)));
        } else {
          // normal mode
          bPlayOK = Player.play(fCurrent, 0.0f, Const.TO_THE_END);
        }
      }

      if (bPlayOK) {
        // notify to devices like commandJPanel to update UI when the play
        // button has been pressed
        ObservationManager.notify(new JajukEvent(JajukEvents.PLAYER_PLAY));
        Log.debug("Now playing :" + fCurrent);
        // Send an event that a track has been launched
        Properties pDetails = new Properties();
        if (itemLast != null) {
          pDetails.put(Const.DETAIL_OLD, itemLast);
        }
        pDetails.put(Const.DETAIL_CURRENT_FILE_ID, fCurrent.getID());
        pDetails.put(Const.DETAIL_CURRENT_DATE, Long.valueOf(System.currentTimeMillis()));
        ObservationManager.notify(new JajukEvent(JajukEvents.FILE_LAUNCHED, pDetails));
        // save the last played track (even files in error are stored here as
        // we need this for computes next track to launch after an error)
        // We have to set this line here as we make directory change analyze
        // before for cover change
        itemLast = (StackItem) getCurrentItem().clone();
        playingRadio = false;
        bFirstFile = false;
        // add hits number
        fCurrent.getTrack().incHits(); // inc hits number

        // recalculate the total time left
        JajukTimer.getInstance().reset();
        for (int i = index; i < alQueue.size(); i++) {
          JajukTimer.getInstance().addTrackTime(alQueue.get(i).getFile());
        }

      } else {
        // Problem launching the track, try next one
        UtilGUI.stopWaiting();
        ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
        try {
          Thread.sleep(Const.WAIT_AFTER_ERROR);
        } catch (InterruptedException e) {
          Log.error(e);
        }
        // save the last played track (even files in error are stored here as
        // we need this for computes next track to launch after an error)
        if (getCurrentItem() != null) {
          itemLast = (StackItem) getCurrentItem().clone();
        } else {
          itemLast = null;
        }

        // We test if user required stop. Must be done here to make a chance to
        // stop before starting a new track
        if (!bStop) {
          finished();
        }
      }
    } catch (Throwable t) {// catch even Errors (OutOfMemory for example)
      Log.error(122, t);
    } finally {
      UtilGUI.stopWaiting(); // stop the waiting cursor
    }
  }

  /**
   * Set current index.
   * 
   * @param index
   *          The index to set.
   */
  public static void setIndex(int index) {
    QueueModel.index = index;
  }

  /**
   * Computes planned tracks.
   * 
   * @param bClear
   *          : clear planned tracks stack
   */
  public static void computesPlanned(boolean bClear) {
    // Check if we are in continue mode and we have some tracks in FIFO, if
    // not : no planned tracks
    if (!Conf.getBoolean(Const.CONF_STATE_CONTINUE) || containsRepeat() || alQueue.size() == 0) {
      alQueue.clearPlanned();
      return;
    }
    if (bClear) {
      alQueue.clearPlanned();
    }

    int missingPlannedSize = Conf.getInt(Const.CONF_OPTIONS_VISIBLE_PLANNED)
        - alQueue.sizePlanned();

    /*
     * To compute missing planned tracks in shuffle state, we get a global shuffle list and we sub
     * list it. This avoid calling a getShuffle() on file manager file by file because it is very
     * costly
     */
    if (Conf.getBoolean(Const.CONF_STATE_SHUFFLE)) {
      // first get a list of "candidates"
      List<File> alFiles = FileManager.getInstance().getGlobalShufflePlaylist();

      // then remove already planned tracks from the list
      alQueue.removePlannedFromList(alFiles);

      // cut down list to the number of files that are missing
      if (alFiles.size() >= missingPlannedSize) {
        alFiles = alFiles.subList(0, missingPlannedSize - 1);
      }

      // wrap the Files in StackItems and add them as planned items.
      List<StackItem> missingPlanned = UtilFeatures.createStackItems(alFiles, Conf
          .getBoolean(Const.CONF_STATE_REPEAT_ALL), false);
      alQueue.addPlanned(missingPlanned);
    } else {
      for (int i = 0; i < missingPlannedSize; i++) {
        StackItem item = null;
        StackItem siLast = null; // last item in fifo or planned
        // if planned stack contains yet some tracks
        if (alQueue.sizePlanned() > 0) {
          siLast = alQueue.getPlanned(alQueue.sizePlanned() - 1); // last one
        } else if (alQueue.size() > 0) { // if fifo contains yet some
          // tracks to play
          siLast = alQueue.get(alQueue.size() - 1); // last one
        }
        try {
          // if fifo contains yet some tracks to play
          if (siLast != null) {
            item = new StackItem(FileManager.getInstance().getNextFile(siLast.getFile()), false);
          } else { // nothing in fifo, take first files in
            // collection
            List<File> files = FileManager.getInstance().getFiles();
            item = new StackItem(files.get(0), false);
          }
          // Tell it is a planned item
          item.setPlanned(true);
          // add the new item
          alQueue.addPlanned(item);
        } catch (JajukException je) {
          // can be thrown if FileManager return a null file (like when
          // reaching end of collection)
          break;
        }
      }
    }
  }

  /**
   * Clears the fifo, for example when we want to add a group of files stopping
   * previous plays.
   */
  public static void clear() {
    alQueue.clear();
    index = 0;
    alQueue.clearPlanned();
  }

  /**
   * Contains only repeat.
   * 
   * @return whether the FIFO contains only repeated files
   */
  public static boolean containsOnlyRepeat() {
    return alQueue.containsOnlyRepeat();
  }

  /**
   * Get previous track, can add item in first index of FIFO.
   * 
   * @return new index of current file
   * 
   * @throws JajukException
   *           the jajuk exception
   */
  private static int addPrevious() throws JajukException {
    StackItem itemFirst = getItem(0);
    if (itemFirst != null) {
      if (index > 0) { // if we have some repeat files
        index--;
      } else { // we are at the first position
        if (itemFirst.isRepeat()) {
          // restart last repeated item in the loop
          index = getLastRepeatedItem();
        } else {
          // first is not repeated, just insert previous
          // file from collection
          StackItem item = new StackItem(FileManager.getInstance().getPreviousFile(
              alQueue.get(0).getFile()), Conf.getBoolean(Const.CONF_STATE_REPEAT_ALL), true);
          alQueue.add(0, item);
          index = 0;
        }
      }
    }
    return index;
  }

  /**
   * Play previous track.
   */
  public static void playPrevious() {
    try {
      bStop = false;
      // if playing, stop all playing players
      if (Player.isPlaying()) {
        Player.stop(true);
      }
      JajukTimer.getInstance().reset();
      JajukTimer.getInstance().addTrackTime(alQueue);
      addPrevious();
      launch();
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Play previous album.
   */
  public static void playPreviousAlbum() {
    try {
      bStop = false;
      // if playing, stop all playing players
      if (Player.isPlaying()) {
        Player.stop(true);
      }
      // we don't support album navigation inside repeated tracks
      if (getQueueSize() > 0 && getItem(0).isRepeat()) {
        playPrevious();
        return;
      }
      boolean bOK = false;
      Directory dir = null;
      if (getPlayingFile() != null) {
        dir = getPlayingFile().getDirectory();
      } else {// nothing in FIFO? just leave
        return;
      }
      while (!bOK) {
        int localindex = addPrevious();
        Directory dirTested = null;
        if (alQueue.get(localindex) == null) {
          return;
        } else {
          File file = alQueue.get(localindex).getFile();
          dirTested = file.getDirectory();
          if (dir.equals(dirTested)) { // yet in the same album
            continue;
          } else {
            // OK, previous is not in the same directory
            // than current track, now check if it is the
            // FIRST track from this new directory
            if (FileManager.getInstance().isVeryfirstFile(file) ||
            // this was the very first file from collection
                (FileManager.getInstance().getPreviousFile(file) != null && FileManager
                    .getInstance().getPreviousFile(file).getDirectory() != file.getDirectory())) {
              // if true, it was the first track from the dir
              bOK = true;
            }
          }
        }
      }
      launch();
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Play next track in selection.
   */
  public static void playNext() {
    try {
      bStop = false;
      // if playing, stop current
      if (Player.isPlaying()) {
        Player.stop(true);
      }
      // force a finish to current track if any
      if (getPlayingFile() != null) { // if stopped, nothing to stop
        finished(true); // stop current track
      } else if (itemLast != null) { // try to launch any previous
        // file
        pushCommand(itemLast, false, false);
      } else { // really nothing? play a shuffle track from collection
        File file = FileManager.getInstance().getShuffleFile();
        if (file != null) {
          pushCommand(new StackItem(file, Conf.getBoolean(Const.CONF_STATE_REPEAT_ALL), false),
              false, false);
        }
      }
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Play next track in selection.
   */
  public static void playNextAlbum() {
    try {
      bStop = false;

      // if playing, stop all playing players
      if (Player.isPlaying()) {
        Player.stop(true);
      }

      // we don't support album navigation inside repeated tracks
      if (getQueueSize() > 0 && getItem(0).isRepeat()) {
        playNext();
        return;
      }

      int indexFirstItem = -1;
      if (getPlayingFile() != null) {
        // ref directory
        Directory dir = getPlayingFile().getDirectory();
        // scan current fifo and try to launch the first track not from
        // this album
        for (int i = getIndex(); i < alQueue.size(); i++) {
          File file = getItem(i).getFile();
          if (!file.getDirectory().equals(dir)) {
            indexFirstItem = i;
            break;
          }
        }
      }
      if (indexFirstItem > 0) {
        // some tracks of other album were already in
        // fifo
        // add a fake album at the top the fifo because the
        // finish will drop first element and we won't
        // drop first track of the next album
        goTo(indexFirstItem);
      } else if (itemLast != null) {// void fifo, add next album
        File fileNext = itemLast.getFile();
        fileNext = FileManager.getInstance().getNextAlbumFile(fileNext);
        // Now add the associated album to the
        Album album = fileNext.getTrack().getAlbum();
        List<File> files = UtilFeatures.getPlayableFiles(album);
        List<StackItem> stack = UtilFeatures.createStackItems(UtilFeatures.applyPlayOption(files),
            Conf.getBoolean(Const.CONF_STATE_REPEAT_ALL), true);
        // Find index to go to (first index with a file whose dir is different
        // from current one)
        int index = getIndex();
        Directory currentDir = null;
        if (index < alQueue.size()) {
          currentDir = alQueue.get(index).getFile().getDirectory();
        }
        while (index < alQueue.size()
            && alQueue.get(index).getFile().getDirectory().equals(currentDir)) {
          index++;
        }
        alQueue.addAll(index, stack);
        goTo(index);
      }
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Get the currently played file or null if no playing file.
   * 
   * @return File
   */
  public static File getPlayingFile() {
    if (isStopped()) {
      return null;
    }
    StackItem item = getCurrentItem();
    return (item == null) ? null : item.getFile();
  }

  /**
   * Get the currently played file or null if no playing file.
   * 
   * @return File
   */
  public static String getPlayingFileTitle() {
    File file = getPlayingFile();
    if (file != null) {
      String pattern = Conf.getString(Const.CONF_PATTERN_FRAME_TITLE);
      String title = null;
      try {
        title = UtilString.applyPattern(file, pattern, false, false);
      } catch (JajukException e) {
        Log.error(e);
      }
      return title;
    }

    return null;
  }

  /**
   * Get the currently played stack item or null if no playing item.
   * 
   * @return stack item
   */
  public static StackItem getCurrentItem() {
    if (index < alQueue.size()) {
      return alQueue.get(index);
    } else {
      return null;
    }
  }

  /**
   * Get an item at given index in FIFO.
   * 
   * @param lIndex
   *          : index
   * 
   * @return stack item
   */
  public static StackItem getItem(int lIndex) {
    return alQueue.get(lIndex);
  }

  /**
   * Get index of the last repeated item, -1 if none repeated.
   * 
   * @return index
   */
  private static int getLastRepeatedItem() {
    int i = -1;
    Iterator<StackItem> iterator = alQueue.iterator();
    while (iterator.hasNext()) {
      StackItem item = iterator.next();
      if (item.isRepeat()) {
        i++;
      } else {
        break;
      }
    }
    return i;
  }

  /**
   * Return true if none file is playing or planned to play for the given
   * device.
   * 
   * @param device
   *          device to unmount
   * 
   * @return true, if can unmount
   */
  public static boolean canUnmount(Device device) {
    if (isStopped() || isPlayingRadio()) {
      return true;
    }
    if (getPlayingFile() != null && getPlayingFile().getDirectory().getDevice().equals(device)) {
      // is current track on this device?
      return false;
    }
    Iterator<StackItem> it = alQueue.iterator();
    // are next tracks in fifo on this device?
    while (it.hasNext()) {
      StackItem item = it.next();
      File file = item.getFile();
      if (file.getDirectory().getDevice().equals(device)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Stop request.
   */
  public static void stopRequest() {
    // fifo is over ( stop request ) , reinit labels in information panel
    // before exiting
    bStop = true;
    Player.stop(true); // stop player
    // No more playing webradio
    playingRadio = false;
    // notify views like commandJPanel to update ui
    ObservationManager.notify(new JajukEvent(JajukEvents.PLAYER_STOP));
  }

  /**
   * Returns whether FIFO is stopped or not <br>
   * Caution ! the FIFO may be stopped but current track is not void and a web
   * radio can be playing.
   * 
   * @return Returns whether FIFO is stopped or not
   */
  public static boolean isStopped() {
    return bStop;
  }

  /**
   * Gets the queue.
   * 
   * @return Returns a shallow copy of the fifo
   */
  public static List<StackItem> getQueue() {
    return alQueue.getQueue();
  }

  /**
   * Not for performance reasons.
   * 
   * @return FIFO size (do not use getFIFO().size() for performance reasons)
   */
  public static int getQueueSize() {
    return alQueue.size();
  }

  /**
   * Shuffle the FIFO, used when user select the Random mode.
   */
  public static void shuffle() {
    if (alQueue.size() > 1) {
      if (isStopped()) {
        UtilFeatures.forcedShuffle(alQueue);
      } else {
        // Make sure current track is kept to its position
        // so remove it and add it again after shuffling
        alQueue.remove(index);
        UtilFeatures.forcedShuffle(alQueue);
        alQueue.add(0, itemLast);
        index = 0;
        // Refresh Queue View
        ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
      }
    }
    alQueue.clearPlanned(); // force recomputes planned tracks
  }

  /**
   * Insert a file to play in FIFO at specified position.
   * 
   * @param iPos
   *          The position where the item is inserted.
   * @param item
   *          the item to insert.
   */
  public static void insert(StackItem item, int iPos) {
    List<StackItem> alStack = new ArrayList<StackItem>(1);
    alStack.add(item);
    insert(alStack, iPos);
    // refresh queue
    ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));

  }

  /**
   * Insert a file at specified position, any existing item at this position is
   * shifted on the right.
   * 
   * @param iPos
   *          The position where the items are inserted.
   * @param alFiles
   *          The list of items to insert.
   */
  public static void insert(List<StackItem> alFiles, int iPos) {
    if (iPos <= alQueue.size()) {
      // add in the FIFO, accept a file at
      // size() position to allow increasing
      // FIFO at the end
      alQueue.addAll(iPos, alFiles);
      JajukTimer.getInstance().addTrackTime(alFiles);
    }
    computesPlanned(false);
    // refresh queue
    ObservationManager.notify(new JajukEvent(JajukEvents.QUEUE_NEED_REFRESH));
  }

  /**
   * Put up an item from given index to index-1.
   * 
   * @param lIndex
   *          The index to move up in the queue.
   */
  public static void up(int lIndex) {
    if (lIndex == 0 || lIndex == alQueue.size()) {
      // Can't put up first track in queue or
      // first planned track.
      // This should be already made by ui behavior
      return;
    }
    if (lIndex < alQueue.size()) {
      StackItem item = alQueue.get(lIndex);
      alQueue.remove(lIndex); // remove the item
      alQueue.add(lIndex - 1, item); // add it again above
    } else { // planned track
      StackItem item = alQueue.getPlanned(lIndex - alQueue.size());
      alQueue.remove(lIndex - alQueue.size()); // remove the item
      // add it again above
      alQueue.add(lIndex - alQueue.size() - 1, item);
    }
  }

  /**
   * Put down an item from given index to index+1.
   * 
   * @param lIndex
   *          The index to move down in the queue.
   */
  public static void down(int lIndex) {
    if (/* lIndex == 0 || */lIndex == alQueue.size() - 1
        || lIndex == alQueue.size() + alQueue.sizePlanned() - 1) {
      // Can't put down current track, nor last track in FIFO, nor last
      // planned track. This should be already made by ui behavior
      return;
    }
    if (lIndex < alQueue.size()) {
      StackItem item = alQueue.get(lIndex);
      alQueue.remove(lIndex); // remove the item
      alQueue.add(lIndex + 1, item); // add it again above
    }
    // TODO: this seems to not take the planned queue into account, but up()
    // does!
  }

  /**
   * Go to given index and launch it.
   * 
   * @param pIndex
   *          The index to go to in the queue.
   */
  public static void goTo(final int pIndex) {
    bStop = false;
    try {
      if (containsRepeatedItem(alQueue)) {
        // if there are some tracks in repeat, mode
        if (getItem(pIndex).isRepeat()) {
          // the selected line is in repeat mode, ok,
          // keep repeat mode and just change index
          index = pIndex;
        } else {
          // the selected line was not a repeated item,
          // take it as a wish to reset repeat mode
          setRepeatModeToAll(false);
          Properties properties = new Properties();
          properties.put(Const.DETAIL_SELECTION, Const.FALSE);
          ObservationManager.notify(new JajukEvent(JajukEvents.REPEAT_MODE_STATUS_CHANGED,
              properties));
          if (Conf.getBoolean(Const.CONF_DROP_PLAYED_TRACKS_FROM_QUEUE)) {
            remove(0, pIndex - 1);
          } else {
            index = pIndex;
          }
        }
      } else {
        if (Conf.getBoolean(Const.CONF_DROP_PLAYED_TRACKS_FROM_QUEUE)) {
          remove(0, pIndex - 1);
        } else {
          index = pIndex;
        }
      }
      // need to stop before launching! this fix a
      // wrong EOM event in BasicPlayer
      Player.stop(false);
      launch();
    } catch (Exception e) {
      Log.error(e);
    }
  }

  /**
   * Remove files at specified positions.
   * 
   * @param iStart
   *          Position from where to start removing.
   * @param iStop
   *          Position from up to where items are removed.
   */
  public static void remove(int iStart, int iStop) {
    if (iStart <= iStop && iStart >= 0 && iStop < alQueue.size() + alQueue.sizePlanned()) {
      // check size drop items from the end to the beginning
      for (int i = iStop; i >= iStart; i--) {
        // FIFO items
        if (i >= alQueue.size()) {
          // remove this file from plan
          alQueue.removePlanned(i - alQueue.size());
          // complete missing planned tracks
          computesPlanned(false);

        } else { // planned items
          StackItem item = alQueue.get(i);
          JajukTimer.getInstance().removeTrackTime(item.getFile());
          // remove this file from fifo
          alQueue.remove(i);
          // Recomputes all planned tracks from last file in fifo
          computesPlanned(true);
        }
      }

    }
  }

  /**
   * Gets the last.
   * 
   * @return Last Stack item in FIFO
   */
  public static StackItem getLast() {
    if (alQueue.size() == 0) {
      return null;
    }
    return alQueue.get(alQueue.size() - 1);
  }

  /**
   * Gets the last played.
   * 
   * @return Last played item
   */
  public static StackItem getLastPlayed() {
    return itemLast;
  }

  /**
   * Gets the index.
   * 
   * @return Returns the index.
   */
  public static int getIndex() {
    return index;
  }

  /**
   * Gets the count tracks left.
   * 
   * @return the count tracks left
   */
  public static int getCountTracksLeft() {
    return alQueue.size() - index;
  }

  /**
   * Gets the planned.
   * 
   * @return Returns a shallow copy of planned files
   */
  public static List<StackItem> getPlanned() {
    return alQueue.getPlanned();
  }

  /**
   * Set the first file flag. This is used mainly to reposition into the file at
   * the position where playing was stopped before.
   * 
   * @param bFirstFile
   *          If this is the first file after startup.
   */
  public static void setFirstFile(boolean bFirstFile) {
    QueueModel.bFirstFile = bFirstFile;
  }

  /**
   * Store current FIFO as a list.
   * 
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public static void commit() throws IOException {
    java.io.File file = SessionService.getConfFileByPath(Const.FILE_FIFO);
    PrintWriter writer = new PrintWriter(
        new BufferedOutputStream(new FileOutputStream(file, false)));
    for (StackItem st : alQueue) {
      writer.println(st.getFile().getID());
    }
    writer.flush();
    writer.close();
  }

  /**
   * Return whether a web radio is being played.
   * 
   * @return whether a web radio is being played
   */
  public static boolean isPlayingRadio() {
    return playingRadio;
  }

  /**
   * Return current web radio if any or null otherwise.
   * 
   * @return current web radio if any or null otherwise
   */
  public static WebRadio getCurrentRadio() {
    return currentRadio;
  }

  /**
   * Return whether a track is being played.
   * 
   * @return whether a track is being played
   */
  public static boolean isPlayingTrack() {
    return !bStop && !isPlayingRadio();
  }

  /**
   * Gets the current file title.
   * 
   * @return a string representation for current played item or stop state
   */
  public static String getCurrentFileTitle() {
    String title = null;
    File file = getPlayingFile();
    if (isPlayingRadio()) {
      title = getCurrentRadio().getName();
    } else if (file != null && !isStopped()) {
      title = file.getHTMLFormatText();
    } else {
      title = Messages.getString("JajukWindow.18");
    }
    return title;
  }

  /**
   * Force FIFO cleanup, for example after files deletion
   */
  public static synchronized void clean() {
    Iterator<StackItem> it = alQueue.iterator();
    while (it.hasNext()) {
      StackItem si = it.next();
      if (FileManager.getInstance().getFileByID(si.getFile().getID()) == null) {
        it.remove();
      }
    }
    computesPlanned(true);
  }
}
