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


import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Encapsulates data common to all instances of a particular kind of
 * {@link Building}, such as the number of workplaces, and the types
 * of goods it produces and consumes.
 */
public final class BuildingType extends BuildableType
    implements Comparable<BuildingType> {

    private int level = 1;
    private int workPlaces = 3;
    private int basicProduction = 3;
    private int minSkill = UNDEFINED;
    private int maxSkill = INFINITY;
    private int upkeep = 0;
    private int priority = Consumer.BUILDING_PRIORITY;

    private GoodsType consumes = null;
    private GoodsType produces = null;
    private Modifier productionModifier = null;
    private BuildingType upgradesFrom = null;
    private BuildingType upgradesTo = null;


    /**
     * Creates a new <code>BuildingType</code> instance.
     *
     * @param id a <code>String</code> value
     * @param specification a <code>Specification</code> value
     */
    public BuildingType(String id, Specification specification) {
        super(id, specification);

        setModifierIndex(Modifier.BUILDING_PRODUCTION_INDEX);
    }


    /**
     * Get the level of this BuildingType.
     *
     * @return The building level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the number of workplaces, that is the maximum number of
     * Units that can work in this BuildingType.
     *
     * @return The number of work places.
     */
    public int getWorkPlaces() {
        return workPlaces;
    }

    /**
     * Get the production of a single Unit in this BuildingType before
     * any modifiers are applied.
     *
     * @return The base production of this building type.
     */
    public int getBasicProduction() {
        return basicProduction;
    }

    /**
     * Gets the amount of gold necessary to maintain a Building of
     * this type for one turn.
     *
     * @return The per turn upkeep for this building type.
     */
    public int getUpkeep() {
        return upkeep;
    }

    /**
     * The consumption priority of a Building of this type. The higher
     * the priority, the earlier will the Consumer be allowed to
     * consume the goods it requires.
     *
     * @return The consumption priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Can a unit of a given type be added to a Building of this type?
     *
     * @param unitType The <code>UnitType</code> to check.
     * @return True if the unit type can be added.
     */
    public boolean canAdd(UnitType unitType) {
        return workPlaces > 0
            && unitType.hasSkill()
            && unitType.getSkill() >= minSkill
            && unitType.getSkill() <= maxSkill;
    }

    /**
     * Gets the type of the building type, which is trivially just this
     * object.
     *
     * @return This.
     */
    public FreeColGameObjectType getType() {
        return this;
    }

    /**
     * Gets the base production modifier for this building type.
     *
     * @return The base modifier if any.
     */
    public Modifier getProductionModifier() {
        return productionModifier;
    }

    /**
     * Gets the BuildingType this BuildingType upgrades from.
     *
     * @return The <code>BuildingType</code> that upgrades to this one.
     */
    public BuildingType getUpgradesFrom() {
        return upgradesFrom;
    }

    /**
     * Get the BuildingType this BuildingType upgrades to.
     *
     * @return The <code>BuildingType</code> to upgrade to from this one.
     */
    public BuildingType getUpgradesTo() {
        return upgradesTo;
    }

    /**
     * Gets the first level of this BuildingType.
     *
     * @return The base <code>BuildingType</code>.
     */
    public BuildingType getFirstLevel() {
        BuildingType buildingType = this;
        while (buildingType.getUpgradesFrom() != null) {
            buildingType = buildingType.getUpgradesFrom();
        }
        return buildingType;
    }

    /**
     * Is this building type automatically built in any colony?
     *
     * @return True if this building type is automatically built.
     */
    public boolean isAutomaticBuild() {
        return !needsGoodsToBuild() && getUpgradesFrom() == null;
    }

    /**
     * Get the type of goods consumed by this BuildingType.
     *
     * @return The consumed <code>GoodsType</code>.
     */
    public GoodsType getConsumedGoodsType() {
        return consumes;
    }

    /**
     * Gets the type of goods produced by this BuildingType.
     *
     * @return The produced <code>GoodsType</code>.
     */
    public GoodsType getProducedGoodsType() {
        return produces;
    }

    /**
     * Get the index for the given Modifier.
     *
     * @param modifier The <code>Modifier</code> to check.
     * @return A modifier index.
     */
    @Override
    public final int getModifierIndex(Modifier modifier) {
        if (produces != null && produces.getId().equals(modifier.getId())) {
            return Modifier.AUTO_PRODUCTION_INDEX;
        } else {
            return getModifierIndex();
        }
    }


    // Interface Comparable

    /**
     * Compares this BuildingType to another.  BuildingTypes are
     * simply sorted according to the order in which they are defined
     * in the specification.
     *
     * @param other The other <code>BuildingType</code> to compare with.
     * @return A comparison result.
     */
    public int compareTo(BuildingType other) {
        return getIndex() - other.getIndex();
    }


    // Serialization

    private static final String BASIC_PRODUCTION_TAG = "basicProduction";
    private static final String CONSUMES_TAG = "consumes";
    private static final String MAX_SKILL_TAG = "maxSkill";
    private static final String MIN_SKILL_TAG = "minSkill";
    private static final String PRIORITY_TAG = "priority";
    private static final String PRODUCES_TAG = "produces";
    private static final String UPGRADES_FROM_TAG = "upgradesFrom";
    private static final String UPKEEP_TAG = "upkeep";
    private static final String WORKPLACES_TAG = "workplaces";

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (upgradesFrom != null) {
            writeAttribute(out, UPGRADES_FROM_TAG, upgradesFrom);
        }

        writeAttribute(out, WORKPLACES_TAG, workPlaces);

        writeAttribute(out, BASIC_PRODUCTION_TAG, basicProduction);

        if (minSkill != UNDEFINED) {
            writeAttribute(out, MIN_SKILL_TAG, minSkill);
        }

        if (maxSkill < INFINITY) {
            writeAttribute(out, MAX_SKILL_TAG, maxSkill);
        }

        if (upkeep > 0) {
            writeAttribute(out, UPKEEP_TAG, upkeep);
        }

        if (priority != Consumer.BUILDING_PRIORITY) {
            writeAttribute(out, PRIORITY_TAG, priority);
        }

        if (consumes != null) {
            writeAttribute(out, CONSUMES_TAG, consumes);
        }

        if (produces != null) {
            writeAttribute(out, PRODUCES_TAG, produces);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        BuildingType parent = spec.getType(in, EXTENDS_TAG, 
                                           BuildingType.class, this);

        upgradesFrom = spec.getType(in, UPGRADES_FROM_TAG,
                                    BuildingType.class, (BuildingType)null);
        if (upgradesFrom == null) {
            level = 1;
        } else {
            upgradesFrom.upgradesTo = this;
            level = upgradesFrom.level + 1;
        }

        workPlaces = getAttribute(in, WORKPLACES_TAG, parent.workPlaces);

        basicProduction = getAttribute(in, BASIC_PRODUCTION_TAG,
                                       parent.basicProduction);

        minSkill = getAttribute(in, MIN_SKILL_TAG, parent.minSkill);

        maxSkill = getAttribute(in, MAX_SKILL_TAG, parent.maxSkill);

        upkeep = getAttribute(in, UPKEEP_TAG, parent.upkeep);

        priority = getAttribute(in, PRIORITY_TAG, parent.priority);

        consumes = spec.getType(in, CONSUMES_TAG, GoodsType.class,
                                parent.consumes);

        produces = spec.getType(in, PRODUCES_TAG, GoodsType.class,
                                parent.produces);
        if (produces != null && basicProduction > 0) {
            productionModifier = new Modifier(produces.getId(), this,
                                              basicProduction,
                                              Modifier.Type.ADDITIVE);
        }

        if (parent != this) { // Handle "extends" for super-type fields
            if (!hasAttribute(in, REQUIRED_POPULATION_TAG)) {
                setRequiredPopulation(parent.getRequiredPopulation());
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    // @compat 0.9.x
    /**
     * Compatibility hack, called from the specification reader when
     * it is finishing up.
     */
    public void fixup09x() {
        try {
            if (hasAbility(Ability.AUTO_PRODUCTION)) {
                if (!hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    // old-style auto-production
                    Ability ability = new Ability(Ability.AVOID_EXCESS_PRODUCTION);
                    addAbility(ability);
                    removeModifiers("model.goods.horses");
                    float value = ("model.building.country".equals(getId()))
                        ? 50 : 25;
                    Modifier modifier = new Modifier("model.modifier.breedingDivisor",
                                                     this, value,
                                                     Modifier.Type.ADDITIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                    modifier = new Modifier("model.modifier.breedingFactor",
                                            this, 2, Modifier.Type.ADDITIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                }
                if (getModifierSet("model.modifier.consumeOnlySurplusProduction").isEmpty()) {
                    Modifier modifier = new Modifier("model.modifier.consumeOnlySurplusProduction",
                                                     this, 0.5f, Modifier.Type.MULTIPLICATIVE);
                    addModifier(modifier);
                    getSpecification().addModifier(modifier);
                }
            }
        } catch (Exception e) {} // no such ability, we don't care
    }
    // @end compatibility code

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "building-type".
     */
    public static String getXMLElementTagName() {
        return "building-type";
    }
}
