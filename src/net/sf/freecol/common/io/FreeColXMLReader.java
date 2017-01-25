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

package net.sf.freecol.common.io;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.StreamReaderDelegate;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColSpecObjectType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIMain;


/**
 * A wrapper for {@code XMLStreamReader} and potentially an
 * underlying stream.  Adds on many useful utilities for reading
 * XML and FreeCol values.
 */
public class FreeColXMLReader extends StreamReaderDelegate
    implements Closeable {

    private static final Logger logger = Logger.getLogger(FreeColXMLReader.class.getName());

    public static enum ReadScope {
        SERVER,     // Loading the game in the server
        NORMAL,     // Normal interning read
        NOINTERN,   // Do not intern any object that are read
    }

    /** Trace all reads? */
    private boolean tracing = false;

    /** The stream to read from. */
    private InputStream inputStream = null;

    /** The read scope to apply. */
    private ReadScope readScope;

    /** A cache of uninterned objects. */
    private Map<String, FreeColObject> uninterned
        = new HashMap<String, FreeColObject>();


    /**
     * Creates a new {@code FreeColXMLReader}.
     *
     * @param bis The {@code BufferedInputStream} to create
     *     an {@code FreeColXMLReader} for.
     * @exception IOException if thrown while creating the
     *     {@code XMLStreamReader}.
     */
    public FreeColXMLReader(BufferedInputStream bis) throws IOException {
        super();

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            setParent(xif.createXMLStreamReader(bis, "UTF-8"));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.inputStream = bis;
        this.readScope = ReadScope.NORMAL;
        this.uninterned.clear();
    }

    /**
     * Creates a new {@code FreeColXMLReader}.
     *
     * @param inputStream The {@code InputStream} to create
     *     an {@code FreeColXMLReader} for.
     * @exception IOException if thrown while creating the
     *     {@code XMLStreamReader}.
     */
    public FreeColXMLReader(InputStream inputStream) throws IOException {
        this(new BufferedInputStream(inputStream));
    }

    /**
     * Creates a new {@code FreeColXMLReader}.
     *
     * @param file The {@code File} to create
     *     an {@code FreeColXMLReader} for.
     * @exception IOException if thrown while creating the
     *     {@code XMLStreamReader}.
     */
    public FreeColXMLReader(File file) throws IOException {
        this(new FileInputStream(file));
    }


    /**
     * Creates a new {@code FreeColXMLReader}.
     *
     * @param reader A {@code Reader} to create
     *     an {@code FreeColXMLReader} for.
     * @exception IOException if thrown while creating the
     *     {@code FreeColXMLReader}.
     */
    public FreeColXMLReader(Reader reader) throws IOException {
        super();

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            setParent(xif.createXMLStreamReader(reader));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        this.inputStream = null;
        this.readScope = ReadScope.NORMAL;
        this.uninterned.clear();
    }


    /**
     * Set the tracing state.
     *
     * @param tracing The new tracing state.
     * @return This reader.
     */
    public FreeColXMLReader setTracing(boolean tracing) {
        this.tracing = tracing;
        return this;
    }

    /**
     * Should reads from this stream intern their objects into the
     * enclosing game?
     *
     * @return True if this is an interning stream.
     */
    public boolean shouldIntern() {
        return this.readScope != ReadScope.NOINTERN;
    }

    /**
     * Get the read scope.
     *
     * @return The {@code ReadScope}.
     */
    public ReadScope getReadScope() {
        return this.readScope;
    }

    /**
     * Set the read scope.
     *
     * @param readScope The new {@code ReadScope}.
     * @return This reader.
     */
    public FreeColXMLReader setReadScope(ReadScope readScope) {
        this.readScope = readScope;
        return this;
    }

    /**
     * Look up an identifier in an enclosing game.  If not interning
     * prefer an non-interned result.
     *
     * @param game The {@code Game} to consult.
     * @param id The object identifier.
     * @return The {@code FreeColObject} found, or null if none.
     */
    private FreeColObject lookup(Game game, String id) {
        FreeColObject fco = (shouldIntern()) ? null : uninterned.get(id);
        return (fco != null) ? fco
            : (game == null) ? null
            : game.getFreeColGameObject(id);
    }

    /**
     * Closes both the {@code XMLStreamReader} and
     * the underlying stream if any.
     *
     * Implements interface Closeable.
     */
    @Override
    public void close() {
        try {
            super.close();
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Error closing stream.", xse);
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Error closing stream.", ioe);
            }
            inputStream = null;
        }
    }

    // @compat 0.10.x
    /**
     * Reads the identifier attribute.
     *
     * Normally a simple getAttribute() would be sufficient, but
     * while we are allowing both the obsolete ID_ATTRIBUTE and the correct
     * ID_ATTRIBUTE_TAG, this routine is useful.
     *
     * When 0.10.x is obsolete, remove this routine and replace its
     * uses with just getAttribute(in, ID_ATTRIBUTE_TAG, (String)null)
     * or equivalent.
     *
     * @return The identifier found, or null if none present.
     */
    public String readId() {
        String id = getAttribute(FreeColObject.ID_ATTRIBUTE_TAG, (String)null);
        if (id == null) {
            id = getAttribute(FreeColObject.ID_ATTRIBUTE, (String)null);
        }
        return id;
    }
    // end @compat 0.10.x

    /** Map for the XMLStreamConstants. */
    private static final Map<Integer, String> tagStrings
        = makeUnmodifiableMap(new Integer[] {
                XMLStreamConstants.ATTRIBUTE,
                XMLStreamConstants.CDATA,
                XMLStreamConstants.CHARACTERS,
                XMLStreamConstants.COMMENT,
                XMLStreamConstants.DTD,
                XMLStreamConstants.END_DOCUMENT,
                XMLStreamConstants.END_ELEMENT,
                XMLStreamConstants.ENTITY_DECLARATION,
                XMLStreamConstants.ENTITY_REFERENCE,
                XMLStreamConstants.NAMESPACE,
                XMLStreamConstants.NOTATION_DECLARATION,
                XMLStreamConstants.PROCESSING_INSTRUCTION,
                XMLStreamConstants.SPACE,
                XMLStreamConstants.START_DOCUMENT,
                XMLStreamConstants.START_ELEMENT },
            new String[] {
                "Attribute", "CData", "Characters", "Comment", "DTD",
                "EndDocument", "EndElement", "EntityDeclaration",
                "EntityReference", "Namespace", "NotationDeclaration",
                "ProcessingInstruction", "Space", "StartDocument",
                "StartElement" });
    /**
     * {@inheritDoc}
     */
    @Override
    public int nextTag() throws XMLStreamException {
        int tag = super.nextTag();
        if (tracing) {
            switch (tag) {
            case XMLStreamConstants.START_ELEMENT:
                System.err.println("[" + getLocalName());
                break;
            case XMLStreamConstants.END_ELEMENT:
                System.err.println(getLocalName() + "]");
                break;
            default:
                System.err.println((tagStrings.containsKey(tag))
                    ? tagStrings.get(tag)
                    : "Weird tag: " + tag);
                break;
            }
        }
        return tag;
    }

    /**
     * Is the stream at the given tag?
     *
     * @param tag The tag to test.
     * @return True if at the given tag.
     */
    public boolean atTag(String tag) {
        return getLocalName().equals(tag);
    }

    /**
     * Expect a particular tag.
     *
     * @param tag The expected tag name.
     * @exception XMLStreamException if the expected tag is not found.
     */
    public void expectTag(String tag) throws XMLStreamException {
        final String endTag = getLocalName();
        if (!endTag.equals(tag)) {
            throw new XMLStreamException("Parse error, " + tag
                + " expected, not: " + endTag);
        }
    }

    /**
     * Check if there are more tags in the current element.
     *
     * @return True if the stream has not reached the end of the
     *     current element.
     * @exception XMLStreamException if there is an error with the stream.
     */
    public boolean moreTags() throws XMLStreamException {
        return nextTag() != XMLStreamConstants.END_ELEMENT;
    }

    /**
     * Close the current tag, checking that it did indeed close correctly.
     *
     * @param tag The expected tag name.
     * @exception XMLStreamException if a closing tag is not found.
     */
    public void closeTag(String tag) throws XMLStreamException {
        if (moreTags()) {
            throw new XMLStreamException("Parse error, END_ELEMENT expected,"
                + " not: " + getLocalName());
        }
        expectTag(tag);
    }

    /**
     * Close the current tag, but accept some alternative elements first.
     *
     * @param tag The expected tag to close.
     * @param others Alternate elements to accept.
     * @exception XMLStreamException if a closing tag is not found.
     */
    public void closeTag(String tag, String... others) throws XMLStreamException {
        for (int next = nextTag(); next != XMLStreamConstants.END_ELEMENT;
             next = nextTag()) {
            String at = find(others, s -> atTag(s));
            if (at == null) {
                throw new XMLStreamException("Parse error, END_ELEMENT(" + tag
                    + " or alternatives) expected, not: " + getLocalName());
            }
            closeTag(at);
        }
        expectTag(tag);
    }

    /**
     * Extract the current tag and its attributes from an input stream.
     * Useful for error messages.
     *
     * @return A simple display of the stream state.
     */
    public String currentTag() {
        StringBuilder sb = new StringBuilder(getLocalName());
        sb.append(", attributes:");
        int n = getAttributeCount();
        for (int i = 0; i < n; i++) {
            sb.append(' ').append(getAttributeLocalName(i))
                .append("=\"").append(getAttributeValue(i)).append('"');
        }
        return sb.toString();
    }

    /**
     * Is there an attribute present in the stream?
     *
     * @param attributeName An attribute name
     * @return True if the attribute is present.
     */
    public boolean hasAttribute(String attributeName) {
        return getParent().getAttributeValue(null, attributeName) != null;
    }

    /**
     * Gets a boolean from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The boolean attribute value, or the default value if none found.
     */
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        return (attrib == null) ? defaultValue
            : Boolean.parseBoolean(attrib);
    }

    /**
     * Gets a float from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The float attribute value, or the default value if none found.
     */
    public float getAttribute(String attributeName, float defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        float result = defaultValue;
        if (attrib != null) {
            try {
                result = Float.parseFloat(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not a float: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets an int from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The int attribute value, or the default value if none found.
     */
    public int getAttribute(String attributeName, int defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        int result = defaultValue;
        if (attrib != null) {
            try {
                result = Integer.decode(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not an integer: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a long from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The long attribute value, or the default value if none found.
     */
    public long getAttribute(String attributeName, long defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        long result = defaultValue;
        if (attrib != null) {
            try {
                result = Long.decode(attrib);
            } catch (NumberFormatException e) {
                logger.warning(attributeName + " is not a long: " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a string from an attribute in a stream.
     *
     * @param attributeName The attribute name.
     * @param defaultValue The default value.
     * @return The string attribute value, or the default value if none found.
     */
    public String getAttribute(String attributeName, String defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        return (attrib == null) ? defaultValue
            : attrib;
    }

    /**
     * Gets an enum from an attribute in a stream.
     *
     * @param <T> The expected enum type.
     * @param attributeName The attribute name.
     * @param returnClass The class of the return value.
     * @param defaultValue The default value.
     * @return The enum attribute value, or the default value if none found.
     */
    public <T extends Enum<T>> T getAttribute(String attributeName,
                                              Class<T> returnClass,
                                              T defaultValue) {
        final String attrib = getParent().getAttributeValue(null,
                                                            attributeName);
        T result = defaultValue;
        if (attrib != null) {
            try {
                result = Enum.valueOf(returnClass,
                                      attrib.toUpperCase(Locale.US));
            } catch (Exception e) {
                logger.warning(attributeName + " is not a "
                    + defaultValue.getClass().getName() + ": " + attrib);
            }
        }
        return result;
    }

    /**
     * Gets a FreeCol object from an attribute in a stream.
     *
     * @param <T> The expected attribute type.
     * @param game The {@code Game} to look in.
     * @param attributeName The attribute name.
     * @param returnClass The {@code FreeColObject} type to expect.
     * @param defaultValue The default value.
     * @return The {@code FreeColObject} found, or the default
     *     value if not.
     * @exception XMLStreamException if the wrong class was passed.
     */
    public <T extends FreeColObject> T getAttribute(Game game,
        String attributeName, Class<T> returnClass,
        T defaultValue) throws XMLStreamException {

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        if (attrib == null) return defaultValue;
        FreeColObject fco = lookup(game, attrib);
        try {
            return returnClass.cast(fco);
        } catch (ClassCastException cce) {
            throw new XMLStreamException(cce);
        }
    }

    /**
     * Get a FreeCol AI object from an attribute in a stream.
     *
     * @param <T> The expected attribute type.
     * @param aiMain The {@code AIMain} that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The {@code AIObject} type to expect.
     * @param defaultValue The default value.
     * @return The {@code AIObject} found, or the default value if not.
     */
    public <T extends AIObject> T getAttribute(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue) {
        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        return (attrib == null) ? defaultValue
            : aiMain.getAIObject(attrib, returnClass);
    }

    /**
     * Find a new location from a stream attribute.  This is necessary
     * because {@code Location} is an interface.
     *
     * @param game The {@code Game} to look in.
     * @param attributeName The attribute to check.
     * @param make If true, try to make the location if it is not found.
     * @return The {@code Location} found.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public Location getLocationAttribute(Game game, String attributeName,
        boolean make) throws XMLStreamException {

        if (attributeName == null) return null;

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        if (attrib != null) {
            FreeColObject fco = lookup(game, attrib);
            if (fco == null && make) {
                Class<? extends FreeColGameObject> c
                    = game.getLocationClass(attrib);
                if (c != null) {
                    fco = makeFreeColObject(game, attributeName, c,
                        getReadScope() == ReadScope.SERVER);
                }
            }
            if (fco instanceof Location) return (Location)fco;
                logger.warning("Not a location: " + attrib);
        }
        return null;
    }

    /**
     * Reads an XML-representation of a list of some general type.
     *
     * @param <T> The list member type.
     * @param tag The tag for the list.
     * @param type The type of the items to be added.  This type
     *     needs to have a constructor accepting a single {@code String}.
     * @return The list.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public <T> List<T> readList(String tag, Class<T> type)
        throws XMLStreamException {

        expectTag(tag);

        final int length = getAttribute(FreeColObject.ARRAY_SIZE_TAG, -1);
        if (length < 0) return Collections.<T>emptyList();

        List<T> list = new ArrayList<>(length);
        for (int x = 0; x < length; x++) {
            try {
                final String value = getAttribute(FreeColObject.arrayKey(x),
                                                  (String)null);
                T object = null;
                if (value != null) {
                    Constructor<T> c = type.getConstructor(type);
                    object = c.newInstance(new Object[] {value});
                }
                list.add(object);
            } catch (IllegalAccessException|InstantiationException
                |InvocationTargetException|NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        closeTag(tag);
        return list;
    }

    /**
     * Reads an XML-representation of a list of
     * {@code FreeColSpecObjectType}s.
     *
     * @param <T> The list member type.
     * @param tag The tag for the list.
     * @param spec The {@code Specification} to find items in.
     * @param type The type of the items to be added.  The type must exist
     *     in the supplied specification.
     * @return The list.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public <T extends FreeColSpecObjectType> List<T> readList(Specification spec,
        String tag, Class<T> type) throws XMLStreamException {

        expectTag(tag);

        final int length = getAttribute(FreeColObject.ARRAY_SIZE_TAG, -1);
        if (length < 0) return Collections.<T>emptyList();

        List<T> list = new ArrayList<>(length);
        for (int x = 0; x < length; x++) {
            T value = getType(spec, FreeColObject.arrayKey(x), type, (T)null);
            if (value == null) logger.warning("Null list value(" + x + ")");
            list.add(value);
        }

        closeTag(tag);
        return list;
    }

    /**
     * Find a {@code FreeColGameObject} of a given class
     * from a stream attribute.
     *
     * Use this routine when the object is optionally already be
     * present in the game.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param attributeName The attribute name.
     * @param returnClass The class to expect.
     * @param defaultValue A default value to return if not found.
     * @param required If true a null result should throw an exception.
     * @return The {@code FreeColGameObject} found, or the default
     *     value if not found.
     * @exception XMLStreamException if the attribute is missing.
     */
    public <T extends FreeColGameObject> T findFreeColGameObject(Game game,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        T ret = getAttribute(game, attributeName, returnClass, (T)null);
        if (ret == (T)null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Either get an existing {@code FreeColObject} from a stream
     * attribute or create it if it does not exist.
     *
     * Use this routine when the object may not necessarily already be
     * present in the game, but is expected to be defined eventually.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param attributeName The required attribute name.
     * @param returnClass The class of object.
     * @param required If true a null result should throw an exception.
     * @return The {@code FreeColObject} found or made, or null
     *     if the attribute was not present.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public <T extends FreeColObject> T makeFreeColObject(Game game,
        String attributeName, Class<T> returnClass,
        boolean required) throws XMLStreamException {
        final String id =
            // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            // end @compat
            getAttribute(attributeName, (String)null);

        if (id == null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            }
        } else {
            FreeColObject fco = lookup(game, id);
            if (fco == null) {
                T ret = game.newInstance(returnClass,
                                         getReadScope() == ReadScope.SERVER);
                if (ret == null) {
                    String err = "Failed to create " + returnClass.getName()
                        + " with id: " + id;
                    if (required) {
                        throw new XMLStreamException(err);
                    } else {
                        logger.warning(err);
                    }
                } else {
                    if (shouldIntern() && ret instanceof FreeColGameObject) {
                        ((FreeColGameObject)ret).internId(id);
                    } else {
                        uninterned.put(id, ret);
                    }
                }
                return ret;
            } else {
                try {
                    return returnClass.cast(fco);
                } catch (ClassCastException cce) {
                    throw new XMLStreamException(cce);
                }
            }
        }
        return null;
    }

    /**
     * Do a normal interning read of a {@code FreeColObject}.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param returnClass The class to expect.
     * @return The {@code FreeColObject} found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    private <T extends FreeColObject> T internedRead(Game game,
        Class<T> returnClass) throws XMLStreamException {

        T ret = makeFreeColObject(game, FreeColObject.ID_ATTRIBUTE_TAG,
                                  returnClass, false);
        if (ret != null) ret.readFromXML(this);
        return ret;
    }

    /**
     * Do a special non-interning read of a {@code FreeColObject}.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param returnClass The class to expect.
     * @return The {@code FreeColObject} found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    private <T extends FreeColObject> T uninternedRead(Game game,
        Class<T> returnClass) throws XMLStreamException {

        T ret = game.newInstance(returnClass,
                                 getReadScope() == ReadScope.SERVER);
        if (ret == null) {
            throw new XMLStreamException("Could not create instance of "
                + returnClass.getName());
        }
        String id = readId();
        if (id == null) {
            throw new XMLStreamException("Object identifier not found.");
        }
        uninterned.put(id, ret);
        ret.readFromXML(this);
        return ret;
    }

    /**
     * Reads a {@code FreeColObject} from a stream.
     * Expects the object to be identified by the standard ID_ATTRIBUTE_TAG.
     *
     * Use this routine when the object may or may not have been
     * referenced and created-by-id in this game, but this is the
     * point where it is authoritatively defined.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param returnClass The class to expect.
     * @return The {@code FreeColObject} found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public <T extends FreeColObject> T readFreeColObject(Game game,
        Class<T> returnClass) throws XMLStreamException {
        return (shouldIntern())
            ? internedRead(game, returnClass)
            : uninternedRead(game, returnClass);
    }

    /**
     * Find a FreeCol AI object from an attribute in a stream.
     *
     * @param <T> The actual return type.
     * @param aiMain The {@code AIMain} that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The {@code AIObject} type to expect.
     * @param defaultValue The default value.
     * @param required If true a null result should throw an exception.
     * @exception XMLStreamException if there is problem reading the stream.
     * @return The {@code AIObject} found, or the default value if not.
     */
    public <T extends AIObject> T findAIObject(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        T ret = getAttribute(aiMain, attributeName, returnClass, (T)null);
        if (ret == (T)null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Either get an existing {@code AIObject} from a stream
     * attribute or create it if it does not exist.
     *
     * Use this routine when the object may not necessarily already be
     * present in the game, but is expected to be defined eventually.
     *
     * @param <T> The actual return type.
     * @param aiMain The {@code AIMain} that contains the object.
     * @param attributeName The attribute name.
     * @param returnClass The {@code AIObject} type to expect.
     * @param defaultValue The default value.
     * @param required If true, throw exceptions on missing data.
     * @exception XMLStreamException if there is problem reading the stream.
     * @return The {@code AIObject} found, or the default value if not.
     */
    public <T extends AIObject> T makeAIObject(AIMain aiMain,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {

        final String id =
            // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            // end @compat
            getAttribute(attributeName, (String)null);

        T ret = null;
        if (id == null) {
            if (required) {
                throw new XMLStreamException("Missing " + attributeName
                    + " for " + returnClass.getName() + ": " + currentTag());
            }
        } else {
            ret = aiMain.getAIObject(id, returnClass);
            if (ret == null) {
                try {
                    Constructor<T> c = returnClass.getConstructor(AIMain.class,
                                                                  String.class);
                    ret = returnClass.cast(c.newInstance(aiMain, id));
                    if (required && ret == null) {
                        throw new XMLStreamException("Constructed null "
                            + returnClass.getName() + " for " + id
                            + ": " + currentTag());
                    }
                } catch (NoSuchMethodException | SecurityException
                        | InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException
                        | XMLStreamException e) {
                    if (required) {
                        throw new XMLStreamException(e);
                    } else {
                        logger.log(Level.WARNING, "Failed to create AIObject: "
                                   + id, e);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Should the game object type being read clear its containers before
     * reading the child elements?
     *
     * Usually true, but not if the type is extending another one.
     *
     * @return True if the containers should be cleared.
     */
    public boolean shouldClearContainers() {
        return !hasAttribute(FreeColSpecObjectType.EXTENDS_TAG)
            && !hasAttribute(FreeColSpecObjectType.PRESERVE_TAG);
    }

    /**
     * Get a FreeColSpecObjectType by identifier from a stream from a
     * specification.
     *
     * @param <T> The actual return type.
     * @param spec The {@code Specification} to look in.
     * @param attributeName the name of the attribute identifying the
     *     {@code FreeColSpecObjectType}.
     * @param returnClass The expected class of the return value.
     * @param defaultValue A default value to return if the attributeName
     *     attribute is not present.
     * @return The {@code FreeColSpecObjectType} found, or the
     *     {@code defaultValue}.
     */
    public <T extends FreeColSpecObjectType> T getType(Specification spec,
        String attributeName, Class<T> returnClass, T defaultValue) {

        final String attrib =
        // @compat 0.10.7
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
        // end @compat
            getAttribute(attributeName, (String)null);

        return (attrib == null) ? defaultValue
            : spec.getType(attrib, returnClass);
    }

    // @compat 0.10.7
    public <T extends FreeColSpecObjectType> T getRole(Specification spec,
        String attributeName, Class<T> returnClass, T defaultValue) {

        String attrib =
            (FreeColObject.ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId() :
            getAttribute(attributeName, (String)null);

        if (attrib == null) {
            return defaultValue;
        }
        attrib = Role.fixRoleId(attrib);
        return spec.getType(attrib, returnClass);
    }
    // end @compat

    /**
     * Copy a FreeColObject by serializing it and reading back the result
     * with a non-interning stream.
     *
     * @param <T> The actual return type.
     * @param game The {@code Game} to look in.
     * @param returnClass The class to expect.
     * @return The copied {@code FreeColObject} found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public <T extends FreeColObject> T copy(Game game, Class<T> returnClass)
        throws XMLStreamException {

        setReadScope(ReadScope.NOINTERN);
        nextTag();
        return uninternedRead(game, returnClass);
    }

    /**
     * Seek to an identifier in this stream.
     *
     * @param id The identifier to find.
     * @return This {@code FreeColXMLReader} positioned such that the
     *     required identifier is current, or null on error or if not found.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public FreeColXMLReader seek(String id) throws XMLStreamException {
        nextTag();
        for (int type = getEventType(); type != XMLEvent.END_DOCUMENT;
             type = getEventType()) {
            if (type == XMLEvent.START_ELEMENT
                && id.equals(readId())) return this;
            nextTag();
        }
        return null;
    }
}
