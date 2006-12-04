package ext;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jajuk.ui.JajukButton;
import org.jajuk.util.ITechnicalStrings;
import org.jajuk.util.Util;

/**
 * Copyright santhosh kumar
 * 
 * @author santhosh kumar - santhosh@in.fiorano.com Drop down button
 */
public abstract class DropDownButton extends JajukButton implements ChangeListener,
		PopupMenuListener, ActionListener, PropertyChangeListener,
		ITechnicalStrings {
	
	private final JButton arrowButton = new JajukButton(Util
			.getIcon(ICON_DROP_DOWN));

	private boolean popupVisible = false;

	public DropDownButton(ImageIcon icon) {
		super(icon);
		getModel().addChangeListener(this);
		arrowButton.getModel().addChangeListener(this);
		arrowButton.addActionListener(this);
		arrowButton.setBorder(null);
		arrowButton.setOpaque(false);
		arrowButton.setMargin(new Insets(1, 0, 1, 0));
		addPropertyChangeListener("enabled", this); // NOI18N //$NON-NLS-1$
	}

	/*------------------------------[ PropertyChangeListener ]---------------------------------------------------*/

	public void propertyChange(PropertyChangeEvent evt) {
		arrowButton.setEnabled(isEnabled());
	}

	/*------------------------------[ ChangeListener ]---------------------------------------------------*/

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == getModel()) {
			if (popupVisible && !getModel().isRollover()) {
				getModel().setRollover(true);
				return;
			}
			arrowButton.getModel().setRollover(
					getModel().isRollover());
			arrowButton.setSelected(getModel().isArmed()
					&& getModel().isPressed());
		} else {
			if (popupVisible && !arrowButton.getModel().isSelected()) {
				arrowButton.getModel().setSelected(true);
				return;
			}
			getModel().setRollover(
					arrowButton.getModel().isRollover());
		}
	}

	/*------------------------------[ ActionListener ]---------------------------------------------------*/

	public void actionPerformed(ActionEvent ae) {
		JPopupMenu popup = getPopupMenu();
		popup.addPopupMenuListener(this);
		popup.show(this, 0, getHeight());
	}

	/*------------------------------[ PopupMenuListener ]---------------------------------------------------*/

	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		popupVisible = true;
		getModel().setRollover(true);
		arrowButton.getModel().setSelected(true);
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		popupVisible = false;

		getModel().setRollover(false);
		arrowButton.getModel().setSelected(false);
		((JPopupMenu) e.getSource()).removePopupMenuListener(this);
		// act as good programmer :)
	}

	public void popupMenuCanceled(PopupMenuEvent e) {
		popupVisible = false;
	}

	/*------------------------------[ Other Methods ]---------------------------------------------------*/

	protected abstract JPopupMenu getPopupMenu();

	public JButton addToToolBar(JToolBar toolbar) {
		JToolBar tempBar = new JToolBar();
		tempBar.setBorder(null);
		tempBar.setRollover(true);
		tempBar.setOpaque(false);
		tempBar.setAlignmentX(0.5f);
		tempBar.setRollover(true);
		tempBar.setFloatable(false);
		tempBar.add(this);
		tempBar.add(arrowButton);
		toolbar.add(tempBar);
		return this;
	}
}