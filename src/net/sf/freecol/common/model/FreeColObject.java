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

package net.sf.freecol.common.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColXMLWriter.WriteScope;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The FreeCol root class.  Maintains an identifier, and an optional link
 * to the specification this object uses.
 *
 * All FreeColObjects are trivially sortable on the basis of their
 * identifiers as a consequence of the Comparable implementation here.
 * Do not override this any further, use explicit comparators if more fully
 * featured sorting is required.
 */
public abstract class FreeColObject
    implements Comparable<FreeColObject>, ObjectWithId {

    protected static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    /** Comparator by FCO identifier. */
    public static final Comparator<? super FreeColObject> fcoComparator
        = new Comparator<FreeColObject>() {
                public int compare(FreeColObject fco1, FreeColObject fco2) {
                    return FreeColObject.compareIds(fco1, fco2);
                }
            };

    public static final int INFINITY = Integer.MAX_VALUE;
    public static final int UNDEFINED = Integer.MIN_VALUE;

    /**
     * Class object for use by {@link net.sf.freecol.client.ClientOptions}
     */
    protected int classindex = 1000;

    /**
     * Public assessor for {@code classindex} field
     *
     * @return The integer.
     */
    public int getClassIndex () {
        return this.classindex;
    }


    /** The identifier of an object. */
    private String id;

    /** An optional property change container, allocated on demand. */
    private PropertyChangeSupport pcs = null;


    // Note: no constructors here.  There are some nasty cases where
    // we can not easily determine the identifier at construction
    // time, so it is better to just let things call setId() when
    // ready.  However we do have this utility.

    /**
     * Instantiate a FreeColObject class with its trivial constructor.
     *
     * @param <T> The actual instance type.
     * @param returnClass The required FreeColObject class.
     * @return The new object or null on error.
     */
    public static <T extends FreeColObject> T newInstance(Class<T> returnClass) {
        try {
            return Introspector.instantiate(returnClass,
                new Class[] {}, new Object[] {});
        } catch (Introspector.IntrospectorException ex) {
            logger.log(Level.WARNING, "Unable to instantiate: "
                + returnClass.getName(), ex);
        }
        return null;
    }

    /**
     * Get the FreeColObject class corresponding to a class name.
     *
     * @param name The class name.
     * @return The class, or null if none found.
     */
    @SuppressWarnings("unchecked")
    public static <T extends FreeColObject> Class<T> getFreeColObjectClass(String name) {
        final String type = "net.sf.freecol.common.model."
            + capitalize(name);
        final Class<T> c = (Class<T>)Introspector.getClassByName(type);
        if (c != null) return c;
        logger.warning("getFreeColObjectClass could not find: " + type);
        return null;
    }

    // Identifier handling

    /**
     * Get the object unique identifier.
     *
     * @return The identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Set the object identifier.
     *
     * @param newId The new object identifier.
     */
    public void setId(final String newId) {
        this.id = newId;
    }

    /**
     * Gets the identifier of this object with the given prefix
     * removed if the id of the object starts with the prefix, and the
     * entire id otherwise.
     *
     * @param prefix The prefix to test.
     * @return An identifier.
     */
    public final String getSuffix(String prefix) {
        return (getId().startsWith(prefix))
            ? getId().substring(prefix.length())
            : getId();
    }

    /**
     * Gets the usual suffix of this object's identifier, that is
     * everything after the last '.'.
     *
     * @return The usual identifier suffix.
     */
    public final String getSuffix() {
        String id = getId();
        return (id == null) ? null : lastPart(id, ".");
    }

    /**
     * Get the type part of the identifier.
     *
     * @param id The identifier to examine.
     * @return The type part of the identifier, or null on error.
     */
    public static String getIdType(String id) {
        if (id != null) {
            int col = id.lastIndexOf(':');
            return (col >= 0) ? id.substring(0, col) : id;
        }
        return null;
    }

    /**
     * Get the type part of the identifier of this object.
     *
     * @return The type part of the identifier, or null on error.
     */
    public String getIdType() {
        return getIdType(getId());
    }

    /**
     * Gets the numeric part of the identifier.
     *
     * @return The numeric part of the identifier, or negative on error.
     */
    public int getIdNumber() {
        if (id != null) {
            int col = id.lastIndexOf(':');
            if (col >= 0) {
                String s = id.substring(col + 1);
                // @compat 0.11.6
                // AI used to generate ids with <thing>:am<number>
                // which changed to <thing>:am:<number>
                if (s.startsWith("am")) s = s.substring(2);
                // end @compat 0.11.6
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException nfe) {}
            }
        }
        return -1;
    }

    /**
     * Compare two FreeColObjects by their identifiers.
     *
     * @param fco1 The first {@code FreeColObject} to compare.
     * @param fco2 The second {@code FreeColObject} to compare.
     * @return The comparison result.
     */
    public static int compareIds(FreeColObject fco1, FreeColObject fco2) {
        if (fco1 == null) {
            return (fco2 == null) ? 0 : -1;
        } else if (fco2 == null) {
            return 1;
        }
        String id1 = fco1.getId();
        String id2 = fco2.getId();
        if (id1 == null) {
            return (id2 == null) ? 0 : -1;
        } else if (id2 == null) {
            return 1;
        }
        int cmp = fco1.getIdType().compareTo(fco2.getIdType());
        return (cmp > 0) ? 1 : (cmp < 0) ? -1
            : Utils.compareTo(fco1.getIdNumber(), fco2.getIdNumber());
    }


    // Specification handling.
    //
    // Base FreeColObjects do not contain a Specification, but
    // FreeColSpecObjects do, and FreeColGameObjects have access to
    // the Specification in the Game.  Noop implementations here, to
    // be overridden by subclasses.

    /**
     * Get the specification.
     *
     * @return The {@code Specification} used by this object.
     */
    public Specification getSpecification() {
        return null;
    }

    /**
     * Sets the specification for this object.
     *
     * @param specification The {@code Specification} to use.
     */
    protected void setSpecification(@SuppressWarnings("unused") Specification specification) {}


    // Game handling.
    // Base FreeColObjects do not contain a Game, but several subclasses
    // (like FreeColGameObject) do.

    /**
     * Gets the game this object belongs to.
     *
     * @return The {@code Game} this object belongs to.
     */
    public Game getGame() {
        return null;
    }

    /**
     * Sets the game object this object belongs to.
     *
     * @param game The {@code Game} to set.
     */
    public void setGame(@SuppressWarnings("unused") Game game) {}


    // Property change support

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (pcs == null) pcs = new PropertyChangeSupport(this);
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs == null) pcs = new PropertyChangeSupport(this);
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        if (pcs != null) {
            pcs.firePropertyChange(event);
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (pcs != null) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return (pcs == null) ? new PropertyChangeListener[0]
            : pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return (pcs == null) ? new PropertyChangeListener[0]
            : pcs.getPropertyChangeListeners(propertyName);
    }

    public boolean hasListeners(String propertyName) {
        return (pcs == null) ? false : pcs.hasListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(listener);
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(propertyName, listener);
        }
    }


    // Feature container support
    //
    // Base FreeColObjects do not directly implement a feature container,
    // but some subclasses provide them.  As long as getFeatureContainer()
    // works, these routines should too.

    /**
     * Gets the feature container for this object, if any.
     *
     * @return The {@code FeatureContainer} for this object.
     */
    public FeatureContainer getFeatureContainer() {
        return null;
    }

    /**
     * Is any of the given abilities present in this object?
     *
     * @param id The object identifier.
     * @return True if the ability is present.
     */
    public final boolean hasAnyAbility(String... abilities) {
        for (String a : abilities)
            if (hasAbility(id, null))
                return true;
        return false;
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id) {
        return hasAbility(id, null);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColSpecObjectType fcgot) {
        return hasAbility(id, fcgot, Turn.UNDEFINED);
    }

    /**
     * Is an ability present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return True if the ability is present.
     */
    public final boolean hasAbility(String id, FreeColSpecObjectType fcgot,
                                    int turn) {

        System.out.println("FreeColObject::hasAbility() id="+id+" fcgot="+fcgot);
        new Exception().printStackTrace();
        return FeatureContainer.hasAbility(getAbilities(id, fcgot, turn));
    }

    /**
     * Gets a sorted copy of the abilities of this object.
     *
     * @return A list of abilities.
     */
    public final List<Ability> getSortedAbilities() {
        return sort(getAbilities());
    }

    /**
     * Gets a copy of the abilities of this object.
     *
     * @return A set of abilities.
     */
    public final List<Ability> getAbilities() {
        return getAbilities(null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @return A list of abilities.
     */
    public final List<Ability> getAbilities(String id) {
        return getAbilities(id, null);
    }

    /**
     * Gets the set of abilities with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @return A list of abilities.
     */
    public final List<Ability> getAbilities(String id,
                                              FreeColSpecObjectType fcgot) {
        return getAbilities(id, fcgot, Turn.UNDEFINED);
    }

    /**
     * Gets the set of abilities with the given identifier from this
     * object.  Subclasses with complex ability handling should
     * override this as all prior routines are derived from it.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     ability applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A list of abilities.
     */
    public List<Ability> getAbilities(String id,
                                        FreeColSpecObjectType fcgot,
                                        int turn) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? Ability.EMPTY_LIST
            : fc.getAbilities(id, fcgot, turn);
    }

    /**
     * Add the given ability to this object.
     *
     * @param ability An {@code Ability} to add.
     * @return True if the ability was added.
     */
    public boolean addAbility(Ability ability) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? false : fc.addAbility(ability);
    }

    /**
     * Remove the given ability from this object.
     *
     * @param ability An {@code Ability} to remove.
     * @return The ability removed or null on failure.
     */
    public Ability removeAbility(Ability ability) {
        FeatureContainer fc = getFeatureContainer();
        return (fc == null) ? null : fc.removeAbility(ability);
    }

    /**
     * Remove all abilities with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeAbilities(String id) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeAbilities(id);
    }


    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @return True if the modifier is present.
     */
    public final boolean hasModifier(String id) {
        return hasModifier(id, null);
    }

    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return True if the modifier is present.
     */
    public final boolean hasModifier(String id, FreeColSpecObjectType fcgot) {
        return hasModifier(id, fcgot, Turn.UNDEFINED);
    }

    /**
     * Is an modifier present in this object?
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return True if the modifier is present.
     */
    public boolean hasModifier(String id, FreeColSpecObjectType fcgot,
                               int turn) {
        return getModifiers(id, fcgot, turn).size() != 0;
    }

    /**
     * Checks if this object contains a given modifier key.
     *
     * @param key The key to check.
     * @return True if the key is present.
     */
    public final boolean containsModifierKey(String key) {
        return getModifiers(key).size() != 0;
    }

    /**
     * Gets a sorted copy of the modifiers of this object.
     *
     * @return A list of modifiers.
     */
    public final List<Modifier> getSortedModifiers() {
        List<Modifier> result = getModifiers();
        Collections.sort(result, Modifier.ascendingModifierIndexComparator);
        return result;
    }

    /**
     * Fill in the modifiers of this object.
     *
     * @param result list to fill in the modifiers.
     */
    public final void fillModifiers(List<Modifier> result) {
        fillModifiers(result, null);
    }

    /**
     * Gets a copy of the modifiers of this object.
     *
     * @return A set of modifiers.
     */
    public final List<Modifier> getModifiers() {
        List<Modifier> result = new ArrayList<>();
        fillModifiers(result);
        return result;
    }

    /**
     * Fill in the set of modifiers with the given identifier from this object.
     *
     * @param list The list to fill into.
     * @param id The object identifier.
     */
    public final void fillModifiers(List<Modifier> result, String id) {
        fillModifiers(result, id, null);
    }

    /**
     * Get the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     *
     * @return A set of modifiers.
     */
    public final List<Modifier> getModifiers(String id) {
        List<Modifier> result = new ArrayList<>();
        fillModifiers(result, id);
        return result;
    }

    /**
     * Fill in the set of modifiers with the given identifier from this object.
     *
     * @param result The list to fill into.
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     */
    public final void fillModifiers(List<Modifier> result, String id,
                                               FreeColSpecObjectType fcgot) {
        fillModifiers(result, id, fcgot, Turn.UNDEFINED);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return A list of modifiers.
     */
    public final List<Modifier> getModifiers(String id,
                                               FreeColSpecObjectType fcgot) {
        List<Modifier> result = new ArrayList<>();
        fillModifiers(result, id, fcgot);
        return result;
    }

    /**
     * Fills in the set of modifiers with the given identifier from this object.
     *
     * Subclasses with complex modifier handling may override this
     * routine.
     *
     * @param result The list to fill into.
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     */
    public void fillModifiers(List<Modifier> result, String id,
                                         FreeColSpecObjectType fcgot,
                                         int turn) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null)
            fc.fillModifiers(result, id, fcgot, turn);
    }

    /**
     * Gets the set of modifiers with the given identifier from this object.
     *
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @param turn An optional applicable {@code Turn}.
     * @return A list of modifiers.
     */
    public List<Modifier> getModifiers(String id,
                                         FreeColSpecObjectType fcgot,
                                         int turn) {
        List<Modifier> result = new ArrayList<>();
        fillModifiers(result, id, fcgot, turn);
        return result;
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param id The object identifier.
     * @return The modified number.
     */
    public final float applyModifiers(float number, int turn, String id) {
        return applyModifiers(number, turn, id, null);
    }

    /**
     * Applies this objects modifiers with the given identifier to the
     * given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param id The object identifier.
     * @param fcgot An optional {@code FreeColSpecObjectType} the
     *     modifier applies to.
     * @return The modified number.
     */
    public final float applyModifiers(float number, int turn,
                                      String id, FreeColSpecObjectType fcgot) {
        return applyModifiers(number, turn, getModifiers(id, fcgot, turn));
    }

    /**
     * Applies a collection of modifiers to the given number.
     *
     * @param number The number to modify.
     * @param turn An optional applicable {@code Turn}.
     * @param mods The {@code Modifier}s to apply.
     * @return The modified number.
     */
    public static final float applyModifiers(float number, int turn,
                                             List<Modifier> mods) {
        return FeatureContainer.applyModifiers(number, turn, mods);
    }

    /**
     * Add the given modifier to this object.
     *
     * @param modifier An {@code Modifier} to add.
     * @return True if the modifier was added.
     */
    public boolean addModifier(Modifier modifier) {
        FeatureContainer fc = getFeatureContainer();
        if (fc == null) return false;
        return fc.addModifier(modifier);
    }

    /**
     * Remove the given modifier from this object.
     *
     * @param modifier An {@code Modifier} to remove.
     * @return The modifier removed.
     */
    public Modifier removeModifier(Modifier modifier) {
        FeatureContainer fc = getFeatureContainer();
        if (fc == null) return null;
        return fc.removeModifier(modifier);
    }

    /**
     * Remove all abilities with a given identifier.
     *
     * @param id The object identifier.
     */
    public void removeModifiers(String id) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeModifiers(id);
    }


    /**
     * Adds all the features in an object to this object.
     *
     * @param fco The {@code FreeColObject} to add features from.
     */
    public void addFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.addFeatures(fco);
    }

    /**
     * Removes all the features in an object from this object.
     *
     * @param fco The {@code FreeColObject} to find features to remove in.
     */
    public void removeFeatures(FreeColObject fco) {
        FeatureContainer fc = getFeatureContainer();
        if (fc != null) fc.removeFeatures(fco);
    }

    /**
     * Get the defence modifiers applicable to this object.
     *
     * @return A list of defence {@code Modifier}s.
     */
    public List<Modifier> getDefenceModifiers() {
        return getModifiers(Modifier.DEFENCE);
    }


    // Comparison support

    /**
     * Base for Comparable implementations.
     *
     * @param other The other {@code FreeColObject} subclass to compare.
     * @return The comparison result.
     */
    @Override
    public int compareTo(FreeColObject other) {
        return compareIds(this, other);
    }


    // Miscellaneous routines

    /**
     * Log a collection of {@code FreeColObject}s.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to log.
     * @param lb A {@code LogBuilder} to log to.
     */
    public static <T extends FreeColObject> void logFreeColObjects(Collection<T> c, LogBuilder lb) {
        lb.add("[");
        for (T t : c) lb.add(t.getSuffix(), " ");
        lb.shrink(" ");
        lb.add("]");
    }

    /**
     * Invoke a method for this object.
     *
     * @param <T> The actual return type.
     * @param methodName The name of the method.
     * @param returnClass The expected return class.
     * @param defaultValue The default value.
     * @return The result of invoking the method, or the default value
     *     on failure.
     */
    protected <T> T invokeMethod(String methodName, Class<T> returnClass,
                                 T defaultValue) {
        if (methodName != null && returnClass != null) {
            try {
                return Introspector.invokeMethod(this, methodName, returnClass);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Invoke failed: " + methodName, ex);
            }
        }
        return defaultValue;
    }


    // Serialization

    /** XML tag name for identifier attribute. */
    public static final String ID_ATTRIBUTE_TAG = "id";

    // @compat 0.10.x
    /** Obsolete identifier attribute. */
    public static final String ID_ATTRIBUTE = "ID";
    // end @compat

    /** XML tag name for array elements. */
    public static final String ARRAY_SIZE_TAG = "xLength";

    /** XML attribute tag to denote partial updates. */
    private static final String PARTIAL_ATTRIBUTE_TAG = "partial";
    // @compat 0.10.x
    private static final String OLD_PARTIAL_ATTRIBUTE_TAG = "PARTIAL";
    // end @compat

    /** XML tag name for value attributes, used in many places. */
    protected static final String VALUE_TAG = "value";


    /**
     * Debugging tool, dump object XML to System.err.
     */
    public void dumpObject() {
        save(System.err, WriteScope.toSave(), false);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file) {
        return save(file, WriteScope.toSave());
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @param scope The {@code WriteScope} to use.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file, WriteScope scope) {
        return save(file, scope, false);
    }

    /**
     * Writes the object to the given file.
     *
     * @param file The {@code File} to write to.
     * @param scope The {@code WriteScope} to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     */
    public boolean save(File file, WriteScope scope, boolean pretty) {
        try (
            FileOutputStream fos = new FileOutputStream(file);
        ) {
            return save(fos, scope, pretty);
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.WARNING, "No file: " + file.getPath(), fnfe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FileOutputStream", ioe);
        }
        return false;
    }

    /**
     * Writes the object to the given output stream
     *
     * @param out The {@code OutputStream} to write to.
     * @param scope The {@code WriteScope} to use.
     * @param pretty Attempt to indent the output nicely.
     * @return True if the save proceeded without error.
     */
    public boolean save(OutputStream out, WriteScope scope, boolean pretty) {
        if (scope == null) scope = FreeColXMLWriter.WriteScope.toSave();
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(out, scope, pretty);
        ) {
            xw.writeStartDocument("UTF-8", "1.0");

            this.toXML(xw);

            xw.writeEndDocument();

            xw.flush();

            return true;
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Exception writing object.", xse);

        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter.", ioe);
        }
        return false;
    }

    /**
     * Serialize this FreeColObject to a string.
     *
     * @param scope The write scope to use.
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(WriteScope scope) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        try (
            FreeColXMLWriter xw = new FreeColXMLWriter(sw, scope);
        ) {
            this.toXML(xw);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter,", ioe);
            return null;
        }

        return sw.toString();
    }

    /**
     * Serialize this FreeColObject to a string.
     *
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize() throws XMLStreamException {
        return serialize(WriteScope.toServer());
    }

    /**
     * Serialize this FreeColObject to a string for a target player.
     *
     * @param player The {@code Player} to serialize the object to.
     * @return The serialized object, or null if the stream could not be
     *     created.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public String serialize(Player player) throws XMLStreamException {
        return serialize(WriteScope.toClient(player));
    }

    /**
     * Copy a FreeColObject.
     *
     * The copied object and its internal descendents will be
     * identical to the original objects, but not present in the game.
     * Newly created objects will prefer to refer to other newly
     * created objects.  Thus if you copy a tile, an internal colony
     * on the tile will also be copied, and the copied tile will refer
     * to the copied colony and the copied colony refer to the copied
     * tile, but both will refer to the original uncopied owning player.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @param returnClass The expected return class.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass) {
        T ret = null;
        try (
            FreeColXMLReader xr = new FreeColXMLReader(new StringReader(this.serialize()));
        ) {
            ret = xr.copy(game, returnClass);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to copy: " + getId(), e);
        }
        return ret;
    }

    /**
     * Copy a FreeColObject for a target player.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to add the object to.
     * @param returnClass The expected return class.
     * @param player The {@code Player} that will see the result.
     * @return The copied object, or null on error.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass,
                                            Player player) {
        T ret = null;
        try (
            FreeColXMLReader xr = new FreeColXMLReader(new StringReader(this.serialize(player)));
        ) {
            ret = xr.copy(game, returnClass);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to copy: " + getId(), e);
        }
        return ret;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * All attributes will be made visible.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        toXML(xw, getXMLTagName());
    }

    /**
     * This method writes an XML-representation of this object with
     * a specified tag to the given stream.
     *
     * Almost all FreeColObjects end up calling these, and implementing
     * their own write{Attributes,Children} methods which begin by
     * calling their superclass.  This allows a clean nesting of the
     * serialization routines throughout the class hierarchy.
     *
     * All attributes will be made visible.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @param tag The tag to use.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        writeAttributes(xw);

        writeChildren(xw);

        xw.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * To be overridden if required by any object that has attributes
     * and uses the toXML(FreeColXMLWriter, String) call.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        if (getId() == null) {
            logger.warning("FreeColObject with null identifier: " + this);
        } else {
            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        }
    }

    /**
     * Write the children of this object to a stream.
     *
     * To be overridden if required by any object that has children
     * and uses the toXML(FreeColXMLWriter, String) call.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        // do nothing
    }

    /**
     * This method writes a partial XML-representation of this object to
     * the given stream using only the mandatory and specified fields.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality, but it depends ultimately
     * on the presence of a getFieldName() method that returns a type
     * compatible with String.valueOf.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public final void toXMLPartial(FreeColXMLWriter xw,
                                   String[] fields) throws XMLStreamException {
        final Class theClass = getClass();

        try {
            xw.writeStartElement(getXMLTagName());

            xw.writeAttribute(ID_ATTRIBUTE_TAG, getId());

            xw.writeAttribute(PARTIAL_ATTRIBUTE_TAG, true);

            for (String field : fields) {
                Introspector intro = new Introspector(theClass, field);
                xw.writeAttribute(field, intro.getter(this));
            }

            xw.writeEndElement();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Partial write failed for "
                       + theClass.getName(), e);
        }
    }


    /**
     * Initializes this object from an XML-representation of this object,
     * unless the PARTIAL_ATTRIBUTE tag is present which indicates
     * a partial update of an existing object.
     *
     * @param xr The input stream with the XML.
     * @exception XMLStreamException if there are any problems reading
     *     the stream.
     */
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        if (xr.hasAttribute(PARTIAL_ATTRIBUTE_TAG)
            // @compat 0.10.x
            || xr.hasAttribute(OLD_PARTIAL_ATTRIBUTE_TAG)
            // end @compat
            ) {
            readFromXMLPartial(xr);
        } else {
            readAttributes(xr);

            readChildren(xr);
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        String newId = xr.readId();
        if (newId != null) setId(newId);
    }

    /**
     * Reads the children of this object from an XML stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        String tag = xr.getLocalName();
        if (tag == null) {
            throw new XMLStreamException("Parse error, null opening tag.");
        }
        try {
            while (xr.moreTags()) {
                readChild(xr);
            }
        } catch (XMLStreamException xse) {
            logger.log(Level.SEVERE, "nextTag failed at " + tag, xse);
        }
        xr.expectTag(tag);
    }

    /**
     * Reads a single child object.  Subclasses must override to read
     * their enclosed elements.  This particular instance of the
     * routine always throws XMLStreamException because we should
     * never arrive here.  However it is very useful to always call
     * super.readChild() when an unexpected tag is encountered, as the
     * exception thrown here provides some useful debugging context.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        throw new XMLStreamException("In " + getXMLTagName()
            + ", unexpected tag " + xr.getLocalName()
            + ", at: " + xr.currentTag());
    }

    /**
     * Updates this object from an XML-representation of this object.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality.  It depends ultimately on
     * the presence of a setFieldName() method that takes a parameter
     * type T where T.valueOf(String) exists.
     *
     * @param xr The input stream with the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXMLPartial(FreeColXMLReader xr) throws XMLStreamException {
        final Class theClass = getClass();
        final String tag = xr.getLocalName();
        int n = xr.getAttributeCount();

        setId(xr.readId());

        for (int i = 0; i < n; i++) {
            String name = xr.getAttributeLocalName(i);

            if (ID_ATTRIBUTE_TAG.equals(name)
                // @compat 0.10.x
                || ID_ATTRIBUTE.equals(name)
                // end @compat
                || PARTIAL_ATTRIBUTE_TAG.equals(name)) continue;

            try {
                Introspector intro = new Introspector(theClass, name);
                intro.setter(this, xr.getAttributeValue(i));

            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not set field " + name, e);
            }
        }

        xr.closeTag(tag);
    }

    /**
     * Make the standard array key.
     *
     * @param i The array index.
     * @return The array key.
     */
    public static String arrayKey(int i) {
        return "x" + String.valueOf(i);
    }

    /**
     * Get the serialization tag for this object.
     *
     * @return The tag.
     */
    public abstract String getXMLTagName();


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof FreeColObject) {
            FreeColObject fco = (FreeColObject)o;
            return Utils.equals(this.id, fco.id);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Utils.hashCode(this.id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + ":" + getId();
    }
}
