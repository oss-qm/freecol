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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Unit.Role;


/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    /**
     * The role of this AbstractUnit.
     */
    private Role role = Role.DEFAULT;

    /**
     * The number of units.
     */
    private int number = 1;


    /**
     * Empty constructor.
     */
    public AbstractUnit() {}

    /**
     * Create a new AbstractUnit.
     *
     * @param id The unit id.
     * @param someRole The unit <code>Role</code>.
     * @param someNumber The number of units.
     */
    public AbstractUnit(String id, Role someRole, int someNumber) {
        setId(id);
        this.role = someRole;
        this.number = someNumber;
    }

    /**
     * Create a new AbstractUnit.
     *
     * @param unitType The type of unit to create.
     * @param someRole The unit <code>Role</code>.
     * @param someNumber The number of units.
     */
    public AbstractUnit(UnitType unitType, Role someRole, int someNumber) {
        this(unitType.getId(), someRole, someNumber);
    }

    /**
     * Creates a new <code>AbstractUnit</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnit(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }


    public AbstractUnit clone() {
        return new AbstractUnit(getId(), getRole(), getNumber());
    }


    /**
     * Get the <code>UnitType</code> value.
     *
     * @param specification A <code>Specification</code> to search in.
     * @return The unit type.
     */
    public final UnitType getUnitType(Specification specification) {
        return specification.getUnitType(getId());
    }

    /**
     * Get the <code>Role</code> value.
     *
     * @return The unit role.
     */
    public final Role getRole() {
        return role;
    }

    /**
     * Set the <code>Role</code> value.
     *
     * @param newRole The new <code>Role</code> value.
     */
    public final void setRole(final Role newRole) {
        this.role = newRole;
    }

    /**
     * Get the number of units.
     *
     * @return The number of units.
     */
    public final int getNumber() {
        return number;
    }

    /**
     * Set the number of units.
     *
     * @param newNumber The new number of units.
     */
    public final void setNumber(final int newNumber) {
        this.number = newNumber;
    }

    /**
     * Gets a description of this abstract unit.
     *
     * @param spec A <code>Specification<code> to query.
     * @return A label.
     */
    public StringTemplate getLabel(Specification spec) {
        return StringTemplate.template("abstractUnit")
            .addAmount("%number%", getNumber())
            .add("%unit%", getUnitType(spec).getNameKey());
    }

    /**
     * Gets the equipment necessary to create a Unit with the same
     * type and role as this AbstractUnit.
     *
     * @param spec A <code>Specification<code> to query.
     * @return An array of equipment types.
     */
    public EquipmentType[] getEquipment(Specification spec) {
        List<EquipmentType> equipment = new ArrayList<EquipmentType>();
        switch (role) {
        case PIONEER:
            EquipmentType tools = spec.getEquipmentType("model.equipment.tools");
            for (int count = 0; count < tools.getMaximumCount(); count++) {
                equipment.add(tools);
            }
            break;
        case MISSIONARY:
            equipment.add(spec.getEquipmentType("model.equipment.missionary"));
            break;
        case SOLDIER:
            equipment.add(spec.getEquipmentType("model.equipment.muskets"));
            break;
        case SCOUT:
            equipment.add(spec.getEquipmentType("model.equipment.horses"));
            break;
        case DRAGOON:
            equipment.add(spec.getEquipmentType("model.equipment.muskets"));
            equipment.add(spec.getEquipmentType("model.equipment.horses"));
            break;
        case DEFAULT:
        default:
            break;
        }
        return equipment.toArray(new EquipmentType[equipment.size()]);
    }


    // Serialization

    private static final String ROLE_TAG = "role";
    private static final String NUMBER_TAG = "number";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, ROLE_TAG, role);

        writeAttribute(out, NUMBER_TAG, number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        role = getAttribute(in, ROLE_TAG, Role.class, Role.DEFAULT);

        number = getAttribute(in, NUMBER_TAG, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(number) + " " + getId()
            + " (" + role.toString() + ")";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "abstractUnit".
     */
    public static String getXMLElementTagName() {
        return "abstractUnit";
    }
}
