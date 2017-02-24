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

package net.sf.freecol.client.gui.panel.report;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.label.GoodsLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays the Production Report.
 */
public final class ReportProductionPanel extends ReportPanel {

    /** The number of selection boxes. */
    private static final int NUMBER_OF_GOODS = 4;

    /** The goods types available for selection. */
    private final List<GoodsType> goodsTypes;

    /** The boxes with which to select goods types for display. */
    private final List<JComboBox<String>> boxes = new ArrayList<>();


    /**
     * The constructor that will add the items to this panel.
     *
     * FIXME: can we extend this to cover farmed goods?
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportProductionPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportProductionAction");

        this.goodsTypes = new ArrayList<>();
        List<String> goodsNames = new ArrayList<>();
        for (GoodsType gt : getSpecification().getGoodsTypeList()) {
            if (!gt.isFarmed()) {
                this.goodsTypes.add(gt);
                goodsNames.add(Messages.getName(gt));
            }
        }

        goodsNames.add(0, Messages.message("nothing"));
        String[] model = goodsNames.toArray(new String[0]);
        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            JComboBox<String> newBox = new JComboBox<>(model);
            newBox.setSelectedIndex(0);
            this.boxes.add(newBox);
        }

        reportPanel.setLayout(new MigLayout("gap 0 0", "[fill]", "[fill]"));
        update();
    }


    private void update() {
        reportPanel.removeAll();
        JLabel selectLabel
            = Utility.localizedLabel("report.production.selectGoods");
        reportPanel.add(selectLabel);

        JButton selectButton
            = Utility.localizedButton("report.production.update");
        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                update();
            }});
        reportPanel.add(selectButton, "wrap");

        List<GoodsType> selectedTypes = new ArrayList<>();
        for (int index = 0; index < NUMBER_OF_GOODS; index++) {
            JComboBox<String> box = this.boxes.get(index);
            reportPanel.add(box);
            int selectedIndex = box.getSelectedIndex();
            if (selectedIndex > 0) {
                selectedTypes.add(this.goodsTypes.get(selectedIndex - 1));
            }
        }

        if (!selectedTypes.isEmpty()) {
            final Specification spec = getSpecification();
            final FreeColClient fcc = this.freeColClient;
            List<Set<BuildingType>> basicBuildingTypes = new ArrayList<>();
            for (GoodsType gt : selectedTypes) {
                Set<BuildingType> bts = new HashSet<BuildingType>();
                for (BuildingType bt : spec.getBuildingTypeList()) {
                    if (gt == bt.getProducedGoodsType() || bt.hasModifier(gt.getId()))
                        bts.add(bt.getFirstLevel());
                }
            }

            // labels
            JLabel newLabel;
            newLabel = Utility.localizedLabel("Colony");
            newLabel.setBorder(Utility.TOPLEFTCELLBORDER);
            reportPanel.add(newLabel, "newline 20");

            for (int index = 0; index < selectedTypes.size(); index++) {
                newLabel = Utility.localizedLabel(selectedTypes.get(index));
                newLabel.setBorder(Utility.TOPCELLBORDER);
                reportPanel.add(newLabel);

                for (BuildingType bt : basicBuildingTypes.get(index)) {
                    newLabel = Utility.localizedLabel(bt);
                    newLabel.setBorder(Utility.TOPCELLBORDER);
                    reportPanel.add(newLabel);
                }
            }

            for (Colony colony : this.freeColClient.getMySortedColonies()) {
                // colonyButton
                JButton colonyButton = Utility.getLinkButton(colony.getName(),
                    null, colony.getId());
                colonyButton.setBorder(Utility.LEFTCELLBORDER);
                colonyButton.addActionListener(this);
                reportPanel.add(colonyButton, "newline");

                // production
                for (int index = 0; index < selectedTypes.size(); index++) {
                    GoodsType goodsType = selectedTypes.get(index);
                    int newValue = colony.getNetProductionOf(goodsType);
                    Goods goods = new Goods(colony.getGame(), colony,
                                            goodsType, newValue);
                    GoodsLabel goodsLabel = new GoodsLabel(getGUI(), goods);
                    goodsLabel.setHorizontalAlignment(JLabel.LEADING);
                    goodsLabel.setBorder(Utility.CELLBORDER);
                    reportPanel.add(goodsLabel);

                    for (BuildingType bt : basicBuildingTypes.get(index)) {
                        Building building = colony.getBuilding(bt);
                        if (building == null) {
                            newLabel = new JLabel();
                            newLabel.setBorder(Utility.CELLBORDER);
                            reportPanel.add(newLabel);
                        } else {
                            BuildingPanel buildingPanel
                                = new BuildingPanel(fcc, building);
                            buildingPanel.setBorder(Utility.CELLBORDER);
                            buildingPanel.initialize();
                            reportPanel.add(buildingPanel);
                        }
                    }
                }
            }
        }
        revalidate();
        repaint();
    }
}
