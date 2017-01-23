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
import java.io.File;
import java.util.logging.Logger;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The panel where you choose your nation and color and connected players are
 * shown.
 */
public final class StartGamePanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(StartGamePanel.class.getName());

    private boolean singlePlayerGame;

    private JCheckBox readyBox;

    private JTextField chat;

    private JTextArea chatArea;

    private JButton start, cancel, gameOptions, mapGeneratorOptions;

    private PlayersTable table;

    private final ActionListener startCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row > -1 && col > -1){
            table.getCellEditor(row, col).stopCellEditing();
        }
        if (!checkVictoryConditions()) return;
        // The ready flag was set to false for single player
        // mode in order to allow the player to change
        // whatever he wants.
        if (singlePlayerGame) getMyPlayer().setReady(true);
        StartGamePanel.this.freeColClient.getPreGameController().requestLaunch();
    }};

    private final ActionListener cancelCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        final SwingGUI gui = getGUI();
        StartGamePanel.this.freeColClient.getConnectController().newGame();
        gui.removeFromCanvas(StartGamePanel.this);
        gui.showNewPanel();
    }};

    private final ActionListener readyBoxCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        StartGamePanel.this.freeColClient.getPreGameController().setReady(readyBox.isSelected());
        refreshPlayersTable();
    }};

    private final ActionListener chatCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        if (!chat.getText().isEmpty()) {
            StartGamePanel.this.freeColClient.getPreGameController().chat(chat.getText());
            displayChat(getMyPlayer().getName(), chat.getText(), false);
            chat.setText("");
        }
    }};

    private final ActionListener gameOptionsCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        final FreeColClient fcc = StartGamePanel.this.freeColClient;
        OptionGroup go = getGUI().showGameOptionsDialog(fcc.isAdmin(), true);
        if (go != null) {
            fcc.getGame().setGameOptions(go);
            fcc.getPreGameController().updateGameOptions();
        }
    }};

    private final ActionListener mapGeneratorOptionsCmd
    = new ActionListener() { public void actionPerformed(ActionEvent ae) {
        final FreeColClient fcc = StartGamePanel.this.freeColClient;
        OptionGroup mgo = getGUI().showMapGeneratorOptionsDialog(fcc.isAdmin());
        if (mgo != null) {
            fcc.getGame().setMapGeneratorOptions(mgo);
            fcc.getPreGameController().updateMapGeneratorOptions();
        }
    }};

    /**
     * Create the panel from which to start a game.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public StartGamePanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("fill, wrap 2"));
    }


    public void initialize(boolean singlePlayer) {
        removeAll();
        this.singlePlayerGame = singlePlayer;

        if (singlePlayer || getMyPlayer().isAdmin()) {
            Specification spec = getSpecification();

            String gtag = GameOptions.TAG;
            File gof = FreeColDirectories
                .getOptionsFile(FreeColDirectories.GAME_OPTIONS_FILE_NAME);
            OptionGroup gog = (gof.exists()) ? OptionGroup.load(gof, gtag, spec)
                : null;
            gog = (gog != null) ? spec.mergeGroup(gog)
                : spec.getOptionGroup(gtag);
            gog.save(gof, null, true);

            String mtag = MapGeneratorOptions.TAG;
            File mof = FreeColDirectories
                .getOptionsFile(FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME);
            OptionGroup mog = (mof.exists()) ? OptionGroup.load(mof, mtag, spec)
                : null;
            mog = (mog != null) ? spec.mergeGroup(mog)
                : spec.getOptionGroup(mtag);
            mog.save(mof, null, true);
        }

        NationOptions nationOptions = getGame().getNationOptions();

        cancel = Utility.localizedButton("cancel");
        setCancelComponent(cancel);

        JScrollPane chatScroll = null, tableScroll;

        table = new PlayersTable(this.freeColClient, nationOptions,
                                 getMyPlayer());

        start = Utility.localizedButton("startGame");

        gameOptions = Utility.localizedButton(Messages
            .nameKey(GameOptions.TAG));

        mapGeneratorOptions = Utility.localizedButton(Messages
            .nameKey(MapGeneratorOptions.TAG));

        readyBox = new JCheckBox(Messages.message("startGamePanel.iAmReady"));

        if (singlePlayerGame) {
            // If we set the ready flag to false then the player will
            // be able to change the settings as he likes.
            getMyPlayer().setReady(false);
            // Pretend as if the player is ready.
            readyBox.setSelected(true);
        } else {
            readyBox.setSelected(getMyPlayer().isReady());
            chat = new JTextField();
            chatArea = new JTextArea();
            chatScroll = new JScrollPane(chatArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        refreshPlayersTable();
        tableScroll = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.getViewport().setOpaque(false);

        add(tableScroll, "width 600:, grow");
        if (!singlePlayerGame) {
            add(chatScroll, "width 250:, grow");
        }
        add(mapGeneratorOptions, "newline, split 2, growx, top, sg");
        add(gameOptions, "growx, top, sg");
        if (!singlePlayerGame) {
            add(chat, "grow, top");
        }
        add(readyBox, "newline");
        add(start, "newline, span, split 2, tag ok");
        add(cancel, "tag cancel");

        if (!singlePlayerGame) {
            chat.addActionListener(chatCmd);
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            chatArea.setText("");
        }

        start.addActionListener(startCmd);
        cancel.addActionListener(cancelCmd);
        readyBox.addActionListener(readyBoxCmd);
        gameOptions.addActionListener(gameOptionsCmd);
        mapGeneratorOptions.addActionListener(mapGeneratorOptionsCmd);

        setEnabled(true);
    }

    /**
     * Check that the user has not specified degenerate victory conditions
     * that are automatically true.
     *
     * @return True if the victory conditions are sensible.
     */
    private boolean checkVictoryConditions() {
        Specification spec = getSpecification();

        if (!(singlePlayerGame
            && spec.getBoolean(GameOptions.VICTORY_DEFEAT_EUROPEANS)
            && !spec.getBoolean(GameOptions.VICTORY_DEFEAT_REF)))
            return true;

        for (Map.Entry<Nation,NationState> e :
                getGame().getNationOptions().getNations().entrySet()) {
            Nation nation = e.getKey();
            if (nation.getType().isEuropean() && !nation.isUnknownEnemy()
                    && e.getValue() != NationState.NOT_AVAILABLE)
                return true;
        }

        getGUI().showInformationMessage("info.noEuropeans");
        return false;
    }

    /**
     * Displays a chat message to the user.
     *
     * @param senderName The name of the player who sent the chat
     *     message to the server.
     * @param message The chat message.
     * @param privateChat 'true' if the message is a private one, 'false'
     *     otherwise.
     */
    public void displayChat(String senderName, String message,
                            boolean privateChat) {
        if (privateChat) {
            chatArea.append(senderName + " (" + Messages.message("private")
                + "): " + message + '\n');
        } else {
            chatArea.append(senderName + ": " + message + '\n');
        }
    }

    /**
     * Refreshes the table that displays the players and the choices that
     * they've made.
     */
    public void refreshPlayersTable() {
        if (table != null) table.update();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        // Do not propagate to superclass.  This panel is reused so
        // avoid the destructive cleanups in FreeColPanel.removeNotify.

        start.removeActionListener(startCmd);
        cancel.removeActionListener(cancelCmd);
        readyBox.removeActionListener(readyBoxCmd);
        gameOptions.removeActionListener(gameOptionsCmd);
        mapGeneratorOptions.removeActionListener(mapGeneratorOptionsCmd);
        if (chat != null) chat.removeActionListener(chatCmd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        start.requestFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        Component[] components = getComponents();
        for (Component component : components) {
            component.setEnabled(enabled);
        }

        if (singlePlayerGame && enabled) {
            readyBox.setEnabled(false);
        }

        if (enabled) {
            start.setEnabled(this.freeColClient.isAdmin());
        }

        gameOptions.setEnabled(enabled);
    }
}
