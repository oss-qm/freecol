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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FreeColSpecObject;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A group of units with a common origin and purpose.
 */
public class Force extends FreeColSpecObject {

    public static final String TAG = "force";

    /** The number of land units in the REF. */
    private final List<AbstractUnit> landUnits = new ArrayList<>();

    /** The number of naval units in the REF. */
    private final List<AbstractUnit> navalUnits = new ArrayList<>();

    // Internal variables that do not need serialization.
    /** The space required to transport all land units. */
    private int spaceRequired;

    /** The current naval transport capacity. */
    private int capacity;


    /**
     * Basic constructor.
     *
     * @param specification The {@code Specification} for this object.
     */
    public Force(Specification specification) {
        super(specification);
    }

    /**
     * Create a new Force.
     *
     * @param spec The {@code Specification} for this object.
     * @param units A list of {@code AbstractUnit}s defining the force.
     * @param ability An optional ability name required of the units
     *     in the force.
     */
    public Force(Specification spec, List<AbstractUnit> units, String ability) {
        this(spec);

        this.capacity = this.spaceRequired = 0;
        for (AbstractUnit au : units) {
            if (ability == null || au.getType(spec).hasAbility(ability)) {
                add(au);
            } else {
                logger.warning("Found unit lacking required ability \""
                    + ability + "\": " + au);
            }
        }
    }


    /**
     * Get the cargo space required for the land units of this force.
     *
     * @return The required cargo space.
     */
    public final int getSpaceRequired() {
        return this.spaceRequired;
    }

    /**
     * Get the cargo space provided by the naval units of this force.
     *
     * @return The provided cargo space.
     */
    public final int getCapacity() {
        return this.capacity;
    }

    /**
     * Update the space and capacity variables.
     */
    public final void updateSpaceAndCapacity() {
        final Specification spec = getSpecification();
        this.capacity = sum(this.navalUnits,
                            nu -> nu.getType(spec).canCarryUnits(),
                            nu -> nu.getType(spec).getSpace() * nu.getNumber());
        this.spaceRequired = sum(this.landUnits,
                                 lu -> lu.getType(spec).getSpaceTaken() * lu.getNumber());
    }

    /**
     * Gets all units.
     *
     * @return A copy of the list of all units.
     */
    public final List<AbstractUnit> getUnitList() {
        List<AbstractUnit> result = new ArrayList<>();
        result.addAll(this.landUnits);
        result.addAll(this.navalUnits);
        return result;
    }

    /**
     * Clear the land unit list.
     */
    public final void clearLandUnits() {
        this.landUnits.clear();
        this.spaceRequired = 0;
    }

    /**
     * Clear the naval unit list.
     */
    public final void clearNavalUnits() {
        this.navalUnits.clear();
        this.capacity = 0;
    }

    /**
     * Gets the land units.
     *
     * @return A list of the  land units.
     */
    public final List<AbstractUnit> getLandUnitsList() {
        return AbstractUnit.deepCopy(this.landUnits);
    }

    /**
     * Gets the naval units.
     *
     * @return A copy of the list of the naval units.
     */
    public final List<AbstractUnit> getNavalUnitsList() {
        return AbstractUnit.deepCopy(this.navalUnits);
    }

    /**
     * Is this Force empty?
     *
     * @return True if there are no land or naval units.
     */
    public final boolean isEmpty() {
        return this.landUnits.isEmpty() && this.navalUnits.isEmpty();
    }

    /**
     * Add abstract units to this Force.
     *
     * @param au The addition to this {@code Force}.
     */
    public void add(AbstractUnit au) {
        final Specification spec = getSpecification();
        final UnitType unitType = au.getType(spec);
        final int n = au.getNumber();
        final Predicate<AbstractUnit> matchPred = a ->
            spec.getUnitType(a.getId()) == unitType
                && a.getRoleId().equals(au.getRoleId());

        if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
            AbstractUnit refUnit = find(this.navalUnits, matchPred);
            if (refUnit != null) {
                refUnit.setNumber(refUnit.getNumber() + n);
                if (unitType.canCarryUnits()) {
                    this.capacity += unitType.getSpace() * n;
                }
            } else {
                this.navalUnits.add(au);
            }
        } else {
            AbstractUnit refUnit = find(this.landUnits, matchPred);
            if (refUnit != null) {
                refUnit.setNumber(refUnit.getNumber() + n);
                this.spaceRequired += unitType.getSpaceTaken() * n;
            } else {
                this.landUnits.add(au);
            }
        }
    }

    /**
     * Calculate the approximate offence power of this force.
     *
     * @param naval If true, consider only naval units, otherwise
     *     consider the land units.
     * @return The approximate offence power.
     */
    public double calculateStrength(boolean naval) {
        return AbstractUnit.calculateStrength(getSpecification(),
            (naval) ? this.navalUnits : this.landUnits);
    }

    // @compat 0.10.x
    public void fixOldREFRoles() {
        Iterator<AbstractUnit> aui = landUnits.iterator();
        List<AbstractUnit> todo = new ArrayList<>();
        while (aui.hasNext()) {
            AbstractUnit au = aui.next();
            if ("SOLDIER".equals(au.getRoleId())
                || "model.role.soldier".equals(au.getRoleId())) {
                au.setRoleId("model.role.infantry");
                aui.remove();
                todo.add(au);
            } else if ("DRAGOON".equals(au.getRoleId())
                || "model.role.dragoon".equals(au.getRoleId())) {
                au.setRoleId("model.role.cavalry");
                aui.remove();
                todo.add(au);
            }
        }
        while (!todo.isEmpty()) add(todo.remove(0));
        updateSpaceAndCapacity();
    }
    // end @compat 0.10.x


    // Serialization

    // @compat 0.10.5
    // do not delete! just revert to private, public for now because of
    // back compatibility code in Monarch
    public static final String LAND_UNITS_TAG = "landUnits";
    public static final String NAVAL_UNITS_TAG = "navalUnits";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        xw.writeStartElement(NAVAL_UNITS_TAG);

        for (AbstractUnit unit : this.navalUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeStartElement(LAND_UNITS_TAG);

        for (AbstractUnit unit : this.landUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearLandUnits();
        clearNavalUnits();

        while (xr.moreTags()) {
            final String tag = xr.getLocalName();

            if (LAND_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    AbstractUnit au = new AbstractUnit(xr);
                    if (au != null) add(au);
                }
            } else if (NAVAL_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    AbstractUnit au = new AbstractUnit(xr);
                    if (au != null) add(au);
                }
            } else {
                logger.warning("Bogus Force tag: " + tag);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
