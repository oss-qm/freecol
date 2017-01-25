/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;


/**
 * Superclass for all popup panels in FreeCol.
 */
public abstract class FreeColPopup extends JDialog implements ActionListener {

    private static final Logger logger = Logger.getLogger(FreeColPopup.class.getName());

    // Create some constants that the panels can use to for various states/actions.
    protected static final String CANCEL = "CANCEL";
    protected static final String OK = "OK";
    protected static final String HELP = "HELP";

    // The margin to use.
    protected static final int MARGIN = 3;

    public final FreeColClient freeColClient;

    protected boolean editable = true;

    protected JButton okButton = Utility.localizedButton("ok");

    /**
     * Constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    protected FreeColPopup(FreeColClient freeColClient, JFrame frame) {
        super(frame);

        System.out.println("FreeColPopup(): constructor");

        this.freeColClient = freeColClient;

        okButton.setActionCommand(OK);
        okButton.addActionListener(this);
        setCancelComponent(okButton);

        /* --- */

//        this.options = options;
//        int paneType = JOptionPane.QUESTION_MESSAGE;
//        switch (type) {
//        case PLAIN:    paneType = JOptionPane.PLAIN_MESSAGE; break;
//        case QUESTION: paneType = JOptionPane.QUESTION_MESSAGE; break;
//        }

//        int def = selectDefault(options);
//        ChoiceItem<T> ci = (def >= 0) ? options.get(def) : null;
//        if (obj instanceof StringTemplate) {
//            obj = Utility.localizedTextArea((StringTemplate)obj);
//        } else if(obj instanceof String) {
//            obj = Utility.getDefaultTextArea((String)obj);
//        }
//        this.pane = new JOptionPane(obj, paneType, JOptionPane.DEFAULT_OPTION,
//                                    icon, selectOptions(), ci);
//        this.pane.setBorder(Utility.DIALOG_BORDER);
//        this.pane.setOpaque(false);
//        this.pane.setName("FreeColPopup");
//        this.pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
//        this.pane.addPropertyChangeListener(this);
//        this.pane.setSize(this.pane.getPreferredSize());
//        setComponentOrientation(this.pane.getComponentOrientation());
//        Container contentPane = getContentPane();
//        contentPane.add(this.pane);
//        setSize(getPreferredSize());
//        setResizable(false);
        setResizable(true);
//        setUndecorated(true);
        setUndecorated(false);
        setModal(false);

        setTitle("Please Wait...");
//        setSubcomponentsNotOpaque(this.pane);
        try { // Layout failures might not get logged.
            pack();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Packing failure", e);
        }
        setLocationRelativeTo(frame);

        WindowAdapter adapter = new WindowAdapter() {
            private boolean gotFocus = false;

            @Override
            public void windowClosing(WindowEvent we) {
                System.out.println("windowClosing");
//                    if (!FreeColPopup.this.responded()) {
//                        FreeColPopup.this.setValue(null);
//                    }
            }

            @Override
            public void windowGainedFocus(WindowEvent we) {
                System.out.println("windowGainedFocus");
                if (!gotFocus) { // Once window gets focus, initialize.
//                        FreeColPopup.this.pane.selectInitialValue();
                    gotFocus = true;
                }
            }
        };
        addWindowListener(adapter);
        addWindowFocusListener(adapter);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent ce) {
                System.out.println("componentShown()");
                // Reset value to ensure closing works properly.
//                   FreeColPopup.this.pane
//                       .setValue(JOptionPane.UNINITIALIZED_VALUE);
            }
        });
        addMouseListener(new MouseAdapter() {
            private Point loc;

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("mousePressed()");
                loc = SwingUtilities
                    .convertPoint((Component)e.getSource(),
                        e.getX(), e.getY(), null);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("mouseReleased()");
                if (loc == null) return;
                Point now = SwingUtilities
                    .convertPoint((Component)e.getSource(),
                        e.getX(), e.getY(), null);
                int dx = now.x - loc.x;
                int dy = now.y - loc.y;
                Point p = FreeColPopup.this.getLocation();
                FreeColPopup.this.setLocation(p.x + dx, p.y + dy);
                loc = null;
            }
        });

        System.out.println("FreeColPopup(): constructor done");
    }

    /**
     * Constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    protected FreeColPopup(FreeColClient freeColClient, JFrame frame, MigLayout layout) {
        this(freeColClient, frame);
        // FIXME: what to do w/ the layout ?
    }

    /**
     * Is this panel editable?
     *
     * @return True if the panel is editable.
     */
    protected boolean isEditable() {
        return editable;
    }

    /**
     * Get the game.
     *
     * @return The current {@code Game}.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Get the GUI.
     *
     * @return The current {@code GUI}.
     */
    protected SwingGUI getGUI() {
        return (SwingGUI)freeColClient.getGUI();
    }

    /**
     * Get the image library.
     *
     * @return The {@code ImageLibrary}.
     */
    protected ImageLibrary getImageLibrary() {
        return getGUI().getImageLibrary();
    }

    /**
     * Get the game specification.
     *
     * @return The {@code Specification}.
     */
    protected Specification getSpecification() {
        return freeColClient.getGame().getSpecification();
    }

    /**
     * Get the player.
     *
     * @return The client {@code Player}.
     */
    protected Player getMyPlayer() {
        return freeColClient.getMyPlayer();
    }

    /**
     * Get the client options.
     *
     * @return The {@code ClientOptions}.
     */
    protected ClientOptions getClientOptions() {
        return (freeColClient == null) ? null
            : freeColClient.getClientOptions();
    }

    /**
     * Get the client controller.
     *
     * @return The client {@code InGameController}.
     */
    public InGameController igc() {
        return freeColClient.getInGameController();
    }

    /**
     * Create a button for a colony.
     *
     * @param colony The {@code Colony} to create a button for.
     * @return The new button.
     */
    public JButton createColonyButton(Colony colony) {
        JButton button = Utility.getLinkButton(colony.getName(), null,
                                               colony.getId());
        button.addActionListener(this);
        return button;
    }

    /**
     * Make the given button the CANCEL button.
     *
     * @param cancelButton an {@code AbstractButton} value
     */
    public final void setCancelComponent(AbstractButton cancelButton) {
        if (cancelButton == null) throw new NullPointerException();

// FIXME: how ot do that w/ on a JDialog ?
//        InputMap inputMap
//            = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true),
//                     "release");
//
//        Action cancelAction = cancelButton.getAction();
//        getActionMap().put("release", cancelAction);
    }

    /**
     * Add a routine to be called when this panel closes.
     * Triggered by Canvas.notifyClose.
     *
     * @param runnable Some code to run on close.
     */
    public void addClosingCallback(final Runnable runnable) {
        addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if ("closing".equals(e.getPropertyName())) {
                        runnable.run();
                        FreeColPopup.this.removePropertyChangeListener(this);
                    }
                }
            });
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        if (OK.equals(command)) {
            getGUI().removeFromCanvas(this);
        } else {
            logger.warning("Bad event: " + command);
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        // removeNotify gets called when a JPanel has no parent any
        // more, that is the best opportunity available for JPanels
        // to be given a chance to remove leak generating references.

        if (okButton == null) return; // Been here before

        // We need to make sure the layout is cleared because some
        // versions of MigLayout are leaky.
        setLayout(null);

        okButton.removeActionListener(this);
        okButton = null;

        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        // The OK button requests focus if it exists.
        if (okButton != null) okButton.requestFocus();
    }
}
