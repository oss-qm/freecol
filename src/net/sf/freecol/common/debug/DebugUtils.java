/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.common.debug;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.LostCityRumour.RumourType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Utilities for in game debug support.
 *
 * Several client GUI features have optional debug routines.  These
 * routines are typically quite special in that they need to intrusive
 * things with the server, and do not have much in common with the
 * client code while still sometimes needing user interaction.  They
 * also have considerable similarity amongst themselves.  So they have
 * been collected here.
 *
 * The client GUI features that are the source of these routines are:
 *   - The colony panel
 *   - The debug menu
 *   - The tile popup menu
 */
public class DebugUtils {

    private static final Logger logger = Logger.getLogger(DebugUtils.class.getName());


    /**
     * Debug action to add buildings to the user colonies.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param buildingTitle A label for the choice dialog.
     */
    public static void addBuildings(final FreeColClient freeColClient,
                                    String buildingTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();

        List<ChoiceItem<BuildingType>> buildings
            = new ArrayList<ChoiceItem<BuildingType>>();
        for (BuildingType b : game.getSpecification().getBuildingTypeList()) {
            String msg = Messages.message(b.toString() + ".name");
            buildings.add(new ChoiceItem<BuildingType>(msg, b));
        }
        BuildingType buildingType
            = gui.showChoiceDialog(null, buildingTitle, "Cancel", buildings);
        if (buildingType == null) return;

        final Game sGame = server.getGame();
        final BuildingType sBuildingType = server.getSpecification()
            .getBuildingType(buildingType.getId());
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        List<String> results = new ArrayList<String>();
        int fails = 0;
        for (Colony sColony : sPlayer.getColonies()) {
            Colony.NoBuildReason reason = sColony.getNoBuildReason(sBuildingType);
            results.add(sColony.getName() + ": " + reason.toString());
            if (reason == Colony.NoBuildReason.NONE) {
                Building sBuilding = new ServerBuilding(sGame, sColony,
                                                        sBuildingType);
                sColony.addBuilding(sBuilding);
            } else {
                fails++;
            }
        }
        gui.showInformationMessage(Utils.join(", ", results));
        if (fails < sPlayer.getNumberOfSettlements()) {
            // Brutally resynchronize
            freeColClient.getConnectController().reconnect();
        }
    }

    /**
     * Debug action to add a founding father.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param fatherTitle A label for the choice dialog.
     */
    public static void addFathers(final FreeColClient freeColClient,
                                  String fatherTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        List<ChoiceItem<FoundingFather>> fathers
            = new ArrayList<ChoiceItem<FoundingFather>>();
        for (FoundingFather father : sSpec.getFoundingFathers()) {
            if (!sPlayer.hasFather(father)) {
                String msg = Messages.message(father.getNameKey());
                fathers.add(new ChoiceItem<FoundingFather>(msg, father));
            }
        }

        FoundingFather father
            = gui.showChooseFoundingFatherDialog(fathers, fatherTitle);
        if (father != null) {
            server.getInGameController()
                .addFoundingFather((ServerPlayer)sPlayer, father);
        }
    }

    /**
     * Debug action to add gold.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void addGold(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        String response = gui.showInputDialog(null,
            StringTemplate.key("menuBar.debug.addGold"),
            Integer.toString(1000), "ok", "cancel", true);
        int gold;
        try {
            gold = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        player.modifyGold(gold);
        sPlayer.modifyGold(gold);
    }

    /**
     * Debug action to add immigration.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void addImmigration(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        String response = gui.showInputDialog(null,
            StringTemplate.key("menuBar.debug.addImmigration"),
            Integer.toString(100), "ok", "cancel", true);
        int crosses;
        try {
            crosses = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        player.incrementImmigration(crosses);
        sPlayer.incrementImmigration(crosses);
    }

    /**
     * Debug action to add liberty to the player colonies.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void addLiberty(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        String response = gui.showInputDialog(null,
            StringTemplate.key("menuBar.debug.addLiberty"),
            Integer.toString(100), "ok", "cancel", true);
        int liberty;
        try {
            liberty = Integer.parseInt(response);
        } catch (NumberFormatException x) {
            return;
        }
        for (Colony c : player.getColonies()) {
            c.addLiberty(liberty);
            sGame.getFreeColGameObject(c.getId(), Colony.class)
                .addLiberty(liberty);
        }
    }

    /**
     * Adds a change listener to a menu (the debug menu in fact),
     * that changes the label on a menu item when the skip status changes.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param menu The menu to add the change listener to.
     * @param item The menu item whose label should change.
     */
    public static void addSkipChangeListener(final FreeColClient freeColClient,
                                             JMenu menu, final JMenuItem item) {
        final FreeColServer server = freeColClient.getFreeColServer();

        menu.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    boolean skipping = server.getInGameController()
                        .getSkippedTurns() > 0;
                    item.setText(Messages.message((skipping)
                            ? "menuBar.debug.stopSkippingTurns"
                            : "menuBar.debug.skipTurns"));
                }
            });
    }

    /**
     * Debug action to add a new unit to a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param tile The <code>Tile</code> to add to.
     */
    public static void addNewUnitToTile(final FreeColClient freeColClient,
                                        final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Game game = freeColClient.getGame();
        final Specification sSpec = sGame.getSpecification();
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        final Tile sTile = sGame.getFreeColGameObject(tile.getId(), Tile.class);
        final GUI gui = freeColClient.getGUI();

        List<ChoiceItem<UnitType>> uts = new ArrayList<ChoiceItem<UnitType>>();
        for (UnitType t : sSpec.getUnitTypeList()) {
            String msg = Messages.message(t.toString() + ".name");
            uts.add(new ChoiceItem<UnitType>(msg, t));
        }
        UnitType unitChoice = gui.showChoiceDialog(null, "Select Unit Type",
                                                   "Cancel", uts);
        if (unitChoice == null) return;
        
        Unit carrier = null, sCarrier = null;
        if (!sTile.isLand() && !unitChoice.isNaval()) {
            for (Unit u : sTile.getUnitList()) {
                if (u.isNaval()
                    && u.getSpaceLeft() >= unitChoice.getSpaceTaken()) {
                    sCarrier = u;
                    carrier = game.getFreeColGameObject(sCarrier.getId(),
                                                        Unit.class);
                    break;
                }
            }
        }
        Location loc = (sCarrier != null) ? sCarrier : sTile;
        ServerUnit sUnit = new ServerUnit(sGame, loc, sPlayer, unitChoice);
        sUnit.setMovesLeft(sUnit.getInitialMovesLeft());

        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter xsw = XMLOutputFactory.newInstance()
                .createXMLStreamWriter(sw);
            ((FreeColGameObject)loc).toXML(xsw, sPlayer, true, true);
            xsw.close();

            XMLStreamReader xsr = XMLInputFactory.newInstance()
                .createXMLStreamReader(new StringReader(sw.toString()));
            xsr.nextTag();
            if (carrier == null) {
                tile.updateFreeColGameObject(xsr, Tile.class);
            } else {
                carrier.updateFreeColGameObject(xsr, Unit.class);
            }
            xsr.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Add unit fail: " + sw.toString(), e);
        }

        Unit unit = game.getFreeColGameObject(sUnit.getId(), Unit.class);
        if (unit != null) {
            gui.setActiveUnit(unit);
            player.invalidateCanSeeTiles();
            gui.refresh();
        }
    }

    /**
     * Debug action to add goods to a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param unit The <code>Unit</code> to add to.
     */
    public static void addUnitGoods(final FreeColClient freeColClient,
                                    final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = sGame.getSpecification();
        final GUI gui = freeColClient.getGUI();

        List<ChoiceItem<GoodsType>> gtl
            = new ArrayList<ChoiceItem<GoodsType>>();
        for (GoodsType t : sSpec.getGoodsTypeList()) {
            if (t.isFoodType() && t != sSpec.getPrimaryFoodType()) continue;
            String msg = Messages.message(t.toString() + ".name");
            gtl.add(new ChoiceItem<GoodsType>(msg, t));
        }
        GoodsType goodsType = gui.showChoiceDialog(null, "Select Goods Type",
                                                   "Cancel", gtl);
        if (goodsType == null) return;

        String amount = gui.showInputDialog(null, 
            StringTemplate.name("Select Goods Amount"),
            "20", "ok", "cancel", true);
        if (amount == null) return;

        int a;
        try {
            a = Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {
            return;
        }
        GoodsType sGoodsType = sSpec.getGoodsType(goodsType.getId());
        GoodsContainer ugc = unit.getGoodsContainer();
        GoodsContainer sgc = sGame.getFreeColGameObject(ugc.getId(),
                                                        GoodsContainer.class);
        ugc.setAmount(goodsType, a);
        sgc.setAmount(sGoodsType, a);
    }

    /**
     * Debug action to check for client-server desynchronization.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void checkDesyncAction(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();
        final Map map = game.getMap();
        final Player player = freeColClient.getMyPlayer();
        final Game sGame = server.getGame();
        final Map sMap = sGame.getMap();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        final GUI gui = freeColClient.getGUI();

        boolean problemDetected = false;
        for (Tile t : sMap.getAllTiles()) {
            if (sPlayer.canSee(t)) {
                for (Unit u : t.getUnitList()) {
                    if (!u.isVisibleTo(sPlayer)) continue;
                    if (game.getFreeColGameObject(u.getId(), Unit.class) == null) {
                        System.out.println("Desynchronization detected: Unit missing on client-side");
                        System.out.println(Messages.message(Messages.getLabel(u))
                            + "(" + u.getId() + "). Position: "
                            + u.getTile().getPosition());
                        try {
                            System.out.println("Possible unit on client-side: "
                                + map.getTile(u.getTile().getPosition())
                                .getFirstUnit().getId());
                        } catch (NullPointerException npe) {}
                        System.out.println();
                        problemDetected = true;
                    } else {
                        Unit cUnit = game.getFreeColGameObject(u.getId(), Unit.class);
                        if (cUnit.getTile() != null
                            && !cUnit.getTile().getId().equals(u.getTile().getId())) {
                            System.out.println("Deynchronization detected: Unit located on different tiles");
                            System.out.println("Server: " + Messages.message(Messages.getLabel(u))
                                + "(" + u.getId() + "). Position: "
                                + u.getTile().getPosition());
                            System.out.println("Client: "
                                + Messages.message(Messages.getLabel(cUnit))
                                + "(" + cUnit.getId() + "). Position: "
                                + cUnit.getTile().getPosition());
                            System.out.println();
                            problemDetected = true;
                        }
                    }
                }
            }
        }

        gui.showInformationMessage((problemDetected)
            ? "menuBar.debug.compareMaps.problem"
            : "menuBar.debug.compareMaps.checkComplete");
    }

    /**
     * Debug action to display an AI colony plan.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param colony The <code>Colony</code> to summarize.
     */
    public static void displayColonyPlan(final FreeColClient freeColClient,
                                         final Colony colony) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final AIMain aiMain = server.getAIMain();
        final AIColony aiColony = aiMain.getAIColony(colony);

        StringBuilder sb = new StringBuilder();
        if (aiColony == null) {
            sb.append(colony.getName() + "is not an AI colony.");
        } else {
            sb.append(aiColony.getColonyPlan().toString());
            sb.append("\n\nTILE IMPROVEMENTS:\n");
            for (TileImprovementPlan tip : aiColony.getTileImprovementPlans()) {
                sb.append(tip.toString());
                sb.append("\n");
            }
            sb.append("\n\nWISHES:\n");
            for (Wish w : aiColony.getWishes()) {
                sb.append(w.toString());
                sb.append("\n");
            }
            sb.append("\n\nEXPORT GOODS:\n");
            for (AIGoods aig : aiColony.getAIGoods()) {
                sb.append(aig.toString());
                sb.append("\n");
            }
        }

        freeColClient.getGUI().showInformationMessage(sb.toString());
    }

    /**
     * Debug action to display Europe.
     *
     * Called from the debug popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void displayEurope(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final AIMain aiMain = server.getAIMain();

        StringBuilder sb = new StringBuilder();
        for (Player tp : sGame.getPlayers()) {
            Player p = sGame.getFreeColGameObject(tp.getId(), Player.class);
            if (p.getEurope() == null) continue;
            List<Unit> inEurope = new ArrayList<Unit>();
            List<Unit> toEurope = new ArrayList<Unit>();
            List<Unit> toAmerica = new ArrayList<Unit>();
            HashMap<String,List<Unit>> units
                = new HashMap<String, List<Unit>>();
            units.put("To Europe", toEurope);
            units.put("In Europe", inEurope);
            units.put("To America", toAmerica);

            sb.append("\n==");
            sb.append(Messages.message(p.getNationName()));
            sb.append("==\n");

            for (Unit u : p.getEurope().getUnitList()) {
                if (u.getDestination() instanceof Map) {
                    toAmerica.add(u);
                    continue;
                }
                if (u.getDestination() instanceof Europe) {
                    toEurope.add(u);
                    continue;
                }
                inEurope.add(u);
            }

            for (String label : units.keySet()) {
                List<Unit> list = units.get(label);
                if (list.size() > 0){
                    sb.append("\n->" + label + "\n");
                    for (Unit u : list) {
                        sb.append('\n');
                        sb.append(Messages.message(Messages.getLabel(u)));
                        if (u.isUnderRepair()) {
                            sb.append(" (Repairing)");
                        } else {
                            sb.append("    ");
                            AIUnit aiu = aiMain.getAIUnit(u);
                            if (aiu.getMission() == null) {
                                sb.append(" (None)");
                            } else {
                                sb.append(aiu.getMission().toString()
                                    .replaceAll("\n", "    \n"));
                            }
                        }
                    }
                    sb.append('\n');
                }
            }
        }
        freeColClient.getGUI().showInformationMessage(sb.toString());
    }

    /**
     * Debug action to display a mission.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param unit The <code>Unit</code> to display.
     */
    public static void displayMission(final FreeColClient freeColClient,
                                      final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final AIMain aiMain = server.getAIMain();
        final AIUnit aiUnit = aiMain.getAIUnit(unit);

        String msg = (aiUnit.getMission() instanceof TransportMission)
            ? ((TransportMission)aiUnit.getMission()).toFullString()
            : "Unit has no transport mission.";
        freeColClient.getGUI().showInformationMessage(msg);
    }

    /**
     * Debug action to dump a players units/iterators to stderr.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void displayUnits(final FreeColClient freeColClient) {
        final Player player = freeColClient.getMyPlayer();
        List<Unit> all = player.getUnits();
        StringBuilder sb = new StringBuilder("\nActive units:\n");

        Unit u, first = player.getNextActiveUnit();
        if (first != null) {
            sb.append(first.toString() + "\nat "
                + ((FreeColGameObject)first.getLocation()) + "\n");
            all.remove(first);
            while (player.hasNextActiveUnit()
                && (u = player.getNextActiveUnit()) != first) {
                sb.append(u.toString() + "\nat "
                    + ((FreeColGameObject)u.getLocation()) + "\n");
                all.remove(u);
            }
        }
        sb.append("Going-to units:\n");
        first = player.getNextGoingToUnit();
        if (first != null) {
            all.remove(first);
            sb.append(first.toString() + "\nat "
                + ((FreeColGameObject)first.getLocation()) + "\n");
            while (player.hasNextGoingToUnit()
                && (u = player.getNextGoingToUnit()) != first) {
                sb.append(u.toString() + "\nat "
                    + ((FreeColGameObject)u.getLocation()) + "\n");
                all.remove(u);
            }
        }
        sb.append("Remaining units:\n");
        while (!all.isEmpty()) {
            u = all.remove(0);
            sb.append(u.toString() + "\nat "
                + ((FreeColGameObject)u.getLocation()) + "\n");
        }

        freeColClient.getGUI().showInformationMessage(sb.toString());
    }

    /**
     * Debug action to dump a tile to stderr.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param tile The <code>Tile</code> to dump.
     */
    public static void dumpTile(final FreeColClient freeColClient,
                                final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();

        System.err.println("\nClient side:");
        tile.dumpObject();
        System.err.println("\n\nServer side:");
        sGame.getFreeColGameObject(tile.getId()).dumpObject();
        System.err.println("\n");
    }

    /**
     * Debug action to reset the moves left of the units on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param units The <code>Unit</code>s to reactivate.
     */
    public static void resetMoves(final FreeColClient freeColClient,
                                  List<Unit> units) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final GUI gui = freeColClient.getGUI();

        boolean first = true;
        for (Unit u : units) {
            Unit su = sGame.getFreeColGameObject(u.getId(), Unit.class);
            u.setMovesLeft(u.getInitialMovesLeft());
            su.setMovesLeft(su.getInitialMovesLeft());
            if (first) {
                gui.setActiveUnit(u);
                first = false;
            }
        }
        gui.refresh();
    }

    /**
     * Debug action to reveal or hide the map.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param reveal If true, reveal the map, else hide the map.
     */
    public static void revealMap(final FreeColClient freeColClient,
                                 boolean reveal) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game game = freeColClient.getGame();

        server.exploreMapForAllPlayers(reveal);
        game.getSpecification().getBooleanOption(GameOptions.FOG_OF_WAR)
            .setValue(reveal);
    }

    /**
     * Debug action to set the amount of goods in a colony.
     *
     * Called from the colony panel.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param colony The <code>Colony</code> to set goods amounts in.
     */
    public static void setColonyGoods(final FreeColClient freeColClient,
                                      final Colony colony) {
        final Specification spec = colony.getSpecification();
        List<ChoiceItem<GoodsType>> gtl
            = new ArrayList<ChoiceItem<GoodsType>>();
        for (GoodsType t : spec.getGoodsTypeList()) {
            if (t.isFoodType() && t != spec.getPrimaryFoodType()) continue;
            String msg = Messages.message(t.toString() + ".name");
            gtl.add(new ChoiceItem<GoodsType>(msg, t));
        }
        GoodsType goodsType = freeColClient.getGUI().showChoiceDialog(null,
            "Select Goods Type", "Cancel", gtl);
        if (goodsType == null) return;

        String amount = freeColClient.getGUI().showInputDialog(null,
                StringTemplate.name("Select Goods Amount"),
                Integer.toString(colony.getGoodsCount(goodsType)),
                "ok", "cancel", true);
        if (amount == null) return;

        int a;
        try {
            a = Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {
            return;
        }

        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Specification sSpec = server.getSpecification();
        final GoodsType sGoodsType = sSpec.getGoodsType(goodsType.getId());
        GoodsContainer cgc = colony.getGoodsContainer();
        GoodsContainer sgc = sGame.getFreeColGameObject(cgc.getId(),
                                                        GoodsContainer.class);
        cgc.setAmount(goodsType, a);
        sgc.setAmount(sGoodsType, a);
    }

    /**
     * Debug action to set the next monarch action.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param monarchTitle A label for the choice dialog.
     */
    public static void setMonarchAction(final FreeColClient freeColClient,
                                        String monarchTitle) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        final GUI gui = freeColClient.getGUI();

        List<ChoiceItem<MonarchAction>> actions
            = new ArrayList<ChoiceItem<MonarchAction>>();
        for (MonarchAction action : MonarchAction.values()) {
            actions.add(new ChoiceItem<MonarchAction>(action));
        }
        MonarchAction action
            = gui.showChoiceMonarchActionDialog(monarchTitle, actions);
        server.getInGameController().setMonarchAction(sPlayer, action);
    }

    /**
     * Debug action to set the lost city rumour type on a tile.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param tile The <code>Tile</code> to operate on.
     */
    public static void setRumourType(final FreeColClient freeColClient,
                                     final Tile tile) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Tile sTile = sGame.getFreeColGameObject(tile.getId(),
                                                      Tile.class);

        List<ChoiceItem<RumourType>> rumours
            = new ArrayList<ChoiceItem<RumourType>>();
        for (RumourType rumour : RumourType.values()) {
            if (rumour == RumourType.NO_SUCH_RUMOUR) continue;
            rumours.add(new ChoiceItem<RumourType>(rumour.toString(), rumour));
        }
        RumourType rumourChoice = freeColClient.getGUI()
            .showChoiceDialog(null, "Select Lost City Rumour", "Cancel",
                              rumours);
        tile.getTileItemContainer().getLostCityRumour().setType(rumourChoice);
        sTile.getTileItemContainer().getLostCityRumour()
            .setType(rumourChoice);
    }

    /**
     * Debug action to skip turns.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void skipTurns(FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();

        if (server.getInGameController().getSkippedTurns() > 0) {
            server.getInGameController().setSkippedTurns(0);
            return;
        }

        String response = freeColClient.getGUI().showInputDialog(null,
            StringTemplate.key("menuBar.debug.skipTurns"),
            Integer.toString(10), "ok", "cancel", true);
        if (response == null) return;

        int skip;
        try {
            skip = Integer.parseInt(response);
        } catch (NumberFormatException nfe) {
            skip = -1;
        }
        if (skip > 0) freeColClient.skipTurns(skip);
    }

    /**
     * Debug action to step the random number generator.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void stepRNG(FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final GUI gui = freeColClient.getGUI();

        boolean more = true;
        while (more) {
            int val = server.getInGameController().stepRandom();
            more = gui.showConfirmDialog(null,
                StringTemplate.template("menuBar.debug.randomValue")
                .addAmount("%value%", val), "more", "ok");
        }
    }

    /**
     * Debug action to summarize information about a native settlement
     * that is normally hidden.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param is The <code>IndianSettlement</code> to summarize.
     */
    public static void summarizeSettlement(final FreeColClient freeColClient,
                                           final IndianSettlement is) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final AIMain aiMain = server.getAIMain();
        final Specification sSpec = sGame.getSpecification();
        final IndianSettlement sis = sGame.getFreeColGameObject(is.getId(),
            IndianSettlement.class);

        StringBuilder sb = new StringBuilder(sis.getName());
        sb.append("\n\nAlarm\n");
        Player mostHated = sis.getMostHated();
        for (Player p : sGame.getLiveEuropeanPlayers()) {
            Tension tension = sis.getAlarm(p);
            sb.append(Messages.message(p.getNationName())
                + " " + ((tension == null) ? "(none)"
                    : Integer.toString(tension.getValue()))
                + ((mostHated == p) ? " (most hated)" : "")
                + " " + Messages.message(sis.getShortAlarmLevelMessageId(p))
                + " " + sis.getContactLevel(p)
                + "\n");
        }

        sb.append("\nGoods Present\n");
        for (Goods goods : sis.getCompactGoods()) {
            sb.append(Messages.message(goods.getLabel(true)) + "\n");
        }

        sb.append("\nGoods Production\n");
        for (GoodsType type : sSpec.getGoodsTypeList()) {
            int prod = sis.getTotalProductionOf(type);
            if (prod > 0) {
                sb.append(Messages.message(type.getNameKey())
                          + " " + prod + "\n");
            }
        }

        sb.append("\nPrices (buy 1/100 / sell 1/100)\n");
        GoodsType[] wanted = sis.getWantedGoods();
        for (GoodsType type : sSpec.getGoodsTypeList()) {
            if (!type.isStorable()) continue;
            int i;
            for (i = wanted.length - 1; i >= 0; i--) {
                if (type == wanted[i]) break;
            }
            sb.append(Messages.message(type.getNameKey())
                + ": " + sis.getPriceToBuy(type, 1)
                + "/" + sis.getPriceToBuy(type, 100)
                + " / " + sis.getPriceToSell(type, 1)
                + "/" + sis.getPriceToSell(type, 100)
                + ((i < 0) ? "" : " wanted[" + Integer.toString(i) + "]")
                + "\n");
        }

        sb.append("\nUnits present\n");
        for (Unit u : sis.getUnitList()) {
            Mission m = aiMain.getAIUnit(u).getMission();
            sb.append(u + " at " + ((FreeColGameObject)u.getLocation()));
            if (m != null) {
                sb.append(" " + Utils.lastPart(m.getClass().getName(), "."));
            }
            sb.append("\n");
        }            
        sb.append("\nUnits owned\n");
        for (Unit u : sis.getOwnedUnits()) {
            Mission m = aiMain.getAIUnit(u).getMission();
            sb.append(u + " at " + ((FreeColGameObject)u.getLocation()));
            if (m != null) {
                sb.append(" " + Utils.lastPart(m.getClass().getName(), "."));
            }
            sb.append("\n");
        }

        sb.append("\nTiles\n");
        for (Tile t : sis.getOwnedTiles()) {
            sb.append(t + "\n");
        }

        sb.append("\nConvert Progress = " + sis.getConvertProgress());
        sb.append("\nLast Tribute = " + sis.getLastTribute());

        freeColClient.getGUI().showInformationMessage(sb.toString());
    }

    /**
     * Debug action to take ownership of a settlement.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param settlement The <code>Settlement</code> to take ownership of.
     */
    public static void takeOwnership(final FreeColClient freeColClient,
                                     final Settlement settlement) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Settlement sSettlement
            = (Settlement) sGame.getFreeColGameObject(settlement.getId());
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
            
        sSettlement.changeOwner(sPlayer);
        freeColClient.getConnectController().reconnect();
    }

    /**
     * Debug action to take ownership of a unit.
     *
     * Called from tile popup menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     * @param unit The <code>Unit</code> to take ownership of.
     */
    public static void takeOwnership(final FreeColClient freeColClient,
                                     final Unit unit) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Unit sUnit = sGame.getFreeColGameObject(unit.getId(), Unit.class);
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);

        sUnit.setOwner(sPlayer);
        for (Unit u : sUnit.getUnitList()) {
            u.setOwner(sPlayer);
        }
        freeColClient.getConnectController().reconnect();
    }

    /**
     * Debug action to run the AI.
     *
     * Called from the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> in effect.
     */
    public static void useAI(final FreeColClient freeColClient) {
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game sGame = server.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Player sPlayer = sGame.getFreeColGameObject(player.getId(),
                                                          Player.class);
        final AIMain aiMain = server.getAIMain();
        final AIPlayer ap = aiMain.getAIPlayer(player);

        ap.setDebuggingConnection(freeColClient.getClient().getConnection());
        ap.startWorking();
        freeColClient.getConnectController().reconnect();
    }
}
