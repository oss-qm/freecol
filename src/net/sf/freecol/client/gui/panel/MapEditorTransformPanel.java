/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.control.MapEditorController.IMapTransform;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.model.ServerIndianSettlement;


/**
 * A panel for choosing the current {@code MapTransform}.
 *
 * <br><br>
 *
 * This panel is only used when running in
 * {@link net.sf.freecol.client.FreeColClient#isMapEditor() map editor mode}.
 *
 * @see MapEditorController#getMapTransform()
 * @see MapTransform
 */
public final class MapEditorTransformPanel extends FreeColPanel {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorTransformPanel.class.getName());

    private final JPanel listPanel;
    private JToggleButton settlementButton;
    private final ButtonGroup group;

    /**
     * A native nation to use for native settlement type and skill.
     */
    private static Nation nativeNation = null;


    /**
     * Creates a panel to choose a map transform.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MapEditorTransformPanel(FreeColClient freeColClient) {
        super(freeColClient, new BorderLayout());

        nativeNation = first(getSpecification().getIndianNations());

        listPanel = new JPanel(new GridLayout(2, 0));

        group = new ButtonGroup();
        //Add an invisible, move button to de-select all others
        group.add(new JToggleButton());
        buildList();

        JScrollPane sl = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sl.getViewport().setOpaque(false);
        add(sl);
    }


    /**
     * Builds the buttons for all the terrains.
     */
    private void buildList() {
        final Specification spec = getSpecification();
        List<TileType> tileList = spec.getTileTypeList();
        Dimension terrainSize = ImageLibrary.scaleDimension(ImageLibrary.TILE_OVERLAY_SIZE, 0.5f);
        for (TileType type : tileList) {
            listPanel.add(buildButton(SwingGUI.createTileImageWithOverlayAndForest(type, terrainSize),
                                      Messages.getName(type),
                                      new TileTypeTransform(type)));
        }
        Dimension riverSize = ImageLibrary.scaleDimension(ImageLibrary.TILE_SIZE, 0.5f);
        listPanel.add(buildButton(ImageLibrary.getRiverImage("0101", riverSize),
                                  Messages.message("mapEditorTransformPanel.minorRiver"),
                                  new RiverTransform(TileImprovement.SMALL_RIVER)));
        listPanel.add(buildButton(ImageLibrary.getRiverImage("0202", riverSize),
                                  Messages.message("mapEditorTransformPanel.majorRiver"),
                                  new RiverTransform(TileImprovement.LARGE_RIVER)));
        listPanel.add(buildButton(ImageLibrary.getMiscImage("image.tileitem."
                    + first(getSpecification().getResourceTypeList()).getId(), 0.75f),
            Messages.message("mapEditorTransformPanel.resource"),
            new ResourceTransform()));
        listPanel.add(buildButton(ImageLibrary.getMiscImage(ImageLibrary.LOST_CITY_RUMOUR, 0.5f),
                                  Messages.getName(ModelMessage.MessageType.LOST_CITY_RUMOUR),
                                  new LostCityRumourTransform()));
        SettlementType settlementType = nativeNation.getType().getCapitalType();
        settlementButton = buildButton(ImageLibrary.getSettlementImage(settlementType, 0.5f),
                                       Messages.message("settlement"),
                                       new SettlementTransform());
        listPanel.add(settlementButton);
    }

    /**
     * Builds the button for the given terrain.
     *
     * @param image an {@code Image} value
     * @param text a {@code String} value
     * @param mt a {@code MapTransform} value
     * @return A suitable button.
     */
    private JToggleButton buildButton(Image image, String text,
                                      final MapTransform mt) {
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.add(new JLabel(new ImageIcon(image)),
                             BorderLayout.CENTER);
        descriptionPanel.add(new JLabel(text, JLabel.CENTER),
                BorderLayout.PAGE_END);
        descriptionPanel.setBackground(Color.RED);
        mt.setDescriptionPanel(descriptionPanel);

        ImageIcon icon = new ImageIcon(image);
        final JToggleButton button = new JToggleButton(icon);
        button.setToolTipText(text);
        button.setOpaque(false);
        group.add(button);
        button.addActionListener((ActionEvent ae) -> {
                MapEditorController ctlr
                    = getFreeColClient().getMapEditorController();
                MapTransform newMapTransform = null;
                if (ctlr.getMapTransform() != mt) {
                    newMapTransform = mt;
                }
                ctlr.setMapTransform(newMapTransform);
                if (newMapTransform == null && mt != null) {
                    //select the invisible button, de-selecting all others
                    group.setSelected(group.getElements().nextElement()
                        .getModel(), true);
                }
            });
        button.setBorder(null);
        return button;
    }

    /**
     * Ask the user for a resource choice from a list.
     *
     * Ripped out of ResourceTransform to dodge the name clash with
     * CollectionUtils.transform which we want to use here.
     *
     * @param resources A list of {@code ResourceType}s to choose from.
     * @return The chosen {@code ResourceType}.
     */
    private ResourceType getResourceChoice(List<ResourceType> resources) {
        final Function<ResourceType, ChoiceItem<ResourceType>> mapper
            = rt -> new ChoiceItem<ResourceType>(Messages.getName(rt), rt);
        return getGUI().getChoice(null,
            Messages.message("mapEditorTransformPanel.chooseResource"),
            "cancel",
            transform(resources, alwaysTrue(), mapper));
    }

    /**
     * Set the native nation.
     *
     * @param newNativeNation The new native {@code Nation}.
     */
    public static void setNativeNation(Nation newNativeNation) {
        nativeNation = newNativeNation;
    }

    /**
     * Represents a transformation that can be applied to
     * a {@code Tile}.
     *
     * @see #transform(Tile)
     */
    public abstract static class MapTransform implements IMapTransform {

        /**
         * A panel with information about this transformation.
         */
        private JPanel descriptionPanel = null;

        /**
         * Applies this transformation to the given tile.
         * @param t The {@code Tile} to be transformed,
         */
        @Override
        public abstract void transform(Tile t);

        /**
         * A panel with information about this transformation.
         * This panel is currently displayed on the
         * {@link InfoPanel} when selected, but might be
         * used elsewhere as well.
         *
         * @return The panel or {@code null} if no panel
         *      has been set.
         */
        public JPanel getDescriptionPanel() {
            return descriptionPanel;
        }

        /**
         * Sets a panel that can be used for describing this
         * transformation to the user.
         *
         * @param descriptionPanel The panel.
         */
        public void setDescriptionPanel(JPanel descriptionPanel) {
            this.descriptionPanel = descriptionPanel;
        }
    }

    public final class TileTypeTransform extends MapTransform {
        private final TileType tileType;

        private TileTypeTransform(TileType tileType) {
            this.tileType = tileType;
        }

        public TileType getTileType() {
            return tileType;
        }

        @Override
        public void transform(Tile t) {
            t.changeType(tileType);
            t.removeLostCityRumour();
        }
    }

    private final class RiverTransform extends MapTransform {
        private final int magnitude;

        private RiverTransform(int magnitude) {
            this.magnitude = magnitude;
        }

        @Override
        public void transform(Tile tile) {
            TileImprovementType riverType =
                tile.getSpecification().getTileImprovementType("model.improvement.river");

            if (riverType.isTileTypeAllowed(tile.getType())
                && !tile.hasRiver()) {
                StringBuilder sb = new StringBuilder(64);
                for (Direction direction : Direction.longSides) {
                    Tile t = tile.getNeighbourOrNull(direction);
                    TileImprovement otherRiver = (t == null) ? null
                        : t.getRiver();
                    if (t == null || (t.isLand() && otherRiver == null)) {
                        sb.append('0');
                    } else {
                        sb.append(magnitude);
                    }
                }
                tile.addRiver(magnitude, sb.toString());
            }
        }
    }

    /**
     * Adds, removes or cycles through the available resources for
     * this Tile.  Cycles through the ResourceTypeList and picks the
     * next valid, or removes if end of list.
     */
    private class ResourceTransform extends MapTransform {

        @Override
        public void transform(Tile t) {
            // Check if there is a resource already
            Resource resource = null;
            if (t.getTileItemContainer() != null) {
                resource = t.getTileItemContainer().getResource();
            }
            if (resource != null) {
                t.getTileItemContainer().removeTileItem(resource);
            } else {
                List<ResourceType> resList = t.getType().getResourceTypes();
                switch (resList.size()) {
                case 0:
                    return;
                case 1:
                    ResourceType resourceType = first(resList);
                    // FIXME: create GUI for setting the quantity
                    t.addResource(new Resource(t.getGame(), t, resourceType,
                                  resourceType.getMaxValue()));
                    return;
                default:
                    ResourceType choice = getResourceChoice(resList);
                    if (choice != null) {
                        t.addResource(new Resource(t.getGame(), t, choice,
                                      choice.getMaxValue()));
                    }
                }
            }
        }
    }

    private class LostCityRumourTransform extends MapTransform {
        @Override
        public void transform(Tile t) {
            if (t.isLand()) {
                LostCityRumour rumour = t.getLostCityRumour();
                if (rumour == null) {
                    t.addLostCityRumour(new LostCityRumour(t.getGame(), t));
                } else {
                    t.removeLostCityRumour();
                }
            }
        }
    }

    private class SettlementTransform extends MapTransform {
        @Override
        public void transform(Tile t) {
            if (!t.isLand()
                || t.hasSettlement()
                || nativeNation == null) return;
            UnitType skill = first(((IndianNationType)nativeNation.getType())
                .getSkills()).getObject();
            Player nativePlayer = getGame().getPlayerByNation(nativeNation);
            if (nativePlayer == null) return;
            String name = nativePlayer.getSettlementName(null);
            ServerIndianSettlement sis
                = new ServerIndianSettlement(t.getGame(),
                    nativePlayer, name, t, false, skill, null);
            nativePlayer.addSettlement(sis);
            sis.placeSettlement(true);
            sis.addUnits(null);
            logger.info("Add settlement " + sis.getName() + " to tile " + t);
        }
    }
}
