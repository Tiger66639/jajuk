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

package org.jajuk.ui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.jajuk.Main;
import org.jajuk.ui.InformationJPanel;
import org.jajuk.ui.ObservationManager;
import org.jajuk.ui.Observer;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.log.Log;

import com.jgoodies.animation.Animation;
import com.jgoodies.animation.Animations;
import com.jgoodies.animation.Animator;
import com.jgoodies.animation.animations.BasicTextAnimation;
import com.jgoodies.animation.components.BasicTextLabel;

/**
 *  Animation-based view 
 *
 * @author     bflorat
 * @created    13 août 2004
 */
public class AnimationView extends ViewAdapter implements ITechnicalStrings,Observer,ComponentListener {
	
	private static final int DEFAULT_FRAME_RATE = 15;
	private static final int DEFAULT_DURATION = 5000;
	private static final int DEFAULT_PAUSE = 500;
	/**Current panel width**/
	private int iSize;
	private BasicTextLabel btl1;
	private JLabel jl1;
	private Animator animator;
	
	public AnimationView(){
	}
	
	/* (non-Javadoc)
	 * @see org.jajuk.ui.views.IView#getID()
	 */
	public String getID() {
		return getClass().getName(); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.jajuk.ui.views.IView#getDesc()
	 */
	public String getDesc() {
		return "AnimationView.0"; //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.jajuk.ui.views.IView#populate()
	 */
	public void populate() {
		setLayout(new BorderLayout());
		addComponentListener(this);
		btl1 = new BasicTextLabel(" "); //$NON-NLS-1$
		btl1.setOpaque(false);
		setBackground(Color.WHITE);
		setForeground(Color.BLACK);
		add(btl1);
		
		ObservationManager.register(EVENT_INFORMATION_DISPLAY,this);
		//check if a track has already been lauched
		update(EVENT_INFORMATION_DISPLAY);
	}
	
	/**Set the text to be displayed**/
	public void setText(final String sText){
		SwingUtilities.invokeLater(new Runnable() { //this is mandatory to get actual getWitdth
			public void run() {
				iSize = AnimationView.this.getWidth(); //current width. Must be called inside an invoke and wait, otherwise, returns zero
				Font font = null;
				boolean bOk = false;
				int i = 40;
				while (!bOk){
					font = new Font("dialog", Font.BOLD, i); //$NON-NLS-1$
					FontMetrics fontMetrics = Main.getWindow().getFontMetrics(font);
					int iFontSize = SwingUtilities.computeStringWidth(fontMetrics,sText);
					if (iFontSize<=iSize-150){
						bOk = true;
					}
					else{
						i --;
					}
				}
				btl1.setFont(font);
				if ( animator!=null ){
					animator.stop();
				}
				Animation animPause = Animations.pause(DEFAULT_PAUSE);
				Animation anim = null;
				//select a random animation
				int iShuffle = (int)(Math.random() * 3);
				switch(iShuffle){
				case 0:
					anim = BasicTextAnimation.defaultScale(btl1,DEFAULT_DURATION,sText,Color.darkGray);
					break;
				case 1:
					anim = BasicTextAnimation.defaultSpace(btl1,DEFAULT_DURATION,sText,Color.darkGray);
					break;
				case 2:
					anim = BasicTextAnimation.defaultFade(btl1,DEFAULT_DURATION,sText,Color.darkGray);
					break;
				}
				Animation animAll = Animations.sequential(anim,animPause);
				anim = Animations.repeat(Float.POSITIVE_INFINITY,animAll);
				animator = new Animator(anim,DEFAULT_FRAME_RATE);
				animator.start();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see org.jajuk.ui.Observer#update(java.lang.String)
	 */
	public void update(String subject) {
		if (subject.equals(EVENT_INFORMATION_DISPLAY)){
			String s = InformationJPanel.getInstance().getMessage();
			if (s != null  && !s.trim().equals("")){ //$NON-NLS-1$
				setText(s);   
			}
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
	 */
	public void componentResized(ComponentEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				iSize = SwingUtilities.getRootPane(AnimationView.this).getWidth(); //current  width
				update(EVENT_INFORMATION_DISPLAY);//force redisplay
			}
		});
		Log.debug("View resized, new width="+iSize);
	}
}
