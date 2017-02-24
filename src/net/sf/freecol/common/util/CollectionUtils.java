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

package net.sf.freecol.common.util;

import java.lang.Iterable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Collection of small static helper methods using Collections.
 */
public class CollectionUtils {

    private static final int MAX_DEFAULT = Integer.MIN_VALUE;
    private static final int MIN_DEFAULT = Integer.MAX_VALUE;
    private static final int SUM_DEFAULT = 0;
    private static final double SUM_DOUBLE_DEFAULT = 0.0;
    private static final double PRODUCT_DEFAULT = 1.0;

    /** Useful comparators for mapEntriesBy* */
    public static final Comparator<Integer> ascendingIntegerComparator
        = new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return a - b;
            }};

    public static final Comparator<Integer> descendingIntegerComparator
        = new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                return b - a;
            }};

    public static final Comparator<Double> ascendingDoubleComparator
        = new Comparator<Double>() {
            public int compare(Double a, Double b) {
                return Double.compare(a, b);
            }};

    public static final Comparator<Double> descendingDoubleComparator
        = new Comparator<Double>() {
            public int compare(Double a, Double b) {
                return Double.compare(b, a);
            }};

    public static final Comparator<List<?>> ascendingListLengthComparator
        = new Comparator<List<?>>() {
            public int compare(List<?> a, List<?> b) {
                return a.size() - b.size();
            }};

    public static final Comparator<List<?>> descendingListLengthComparator
        = new Comparator<List<?>>() {
            public int compare(List<?> a, List<?> b) {
                return b.size() - a.size();
            }};

    /**
     * Make an unmodifiable set with specified members.
     *
     * @param <T> The type of the set members.
     * @param members The set members.
     * @return An unmodifiable set containing the members.
     */
    @SafeVarargs
    public static <T> Set<T> makeUnmodifiableSet(T... members) {
        Set<T> tmp = new HashSet<>();
        for (T t : members) tmp.add(t);
        return Collections.<T>unmodifiableSet(tmp);
    }

    /**
     * Make an unmodifiable list with specified members.
     *
     * @param <T> The type of the list members.
     * @param members The list members.
     * @return An unmodifiable list containing the members.
     */
    @SafeVarargs
    public static <T> List<T> makeUnmodifiableList(T... members) {
        List<T> tmp = new ArrayList<>();
        for (T t : members) tmp.add(t);
        return Collections.<T>unmodifiableList(tmp);
    }

    /**
     * Make an unmodifiable map with member pairs specified in two arrays.
     *
     * The array lengths *must* match.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     * @param keys The array of keys.
     * @param values The array of values.
     * @return An unmodifiable map containing the specified members.
     */
    public static <K,V> Map<K,V> makeUnmodifiableMap(K[] keys, V[] values) {
        if (keys.length != values.length) {
            throw new RuntimeException("Length mismatch");
        }
        Map<K,V> tmp = new HashMap<K,V>();
        for (int i = 0; i < keys.length; i++) {
            tmp.put(keys[i], values[i]);
        }
        return Collections.<K,V>unmodifiableMap(tmp);
    }

    /**
     * Appends a value to a list member of a map with a given key.
     *
     * @param <T> The map value collection member type.
     * @param <K> The map key type.
     * @param map The {@code Map} to add to.
     * @param key The key with which to look up the list in the map.
     * @param value The value to append.
     */
    public static <T,K> void appendToMapList(Map<K, List<T>> map,
                                             K key, T value) {
        List<T> l = map.get(key);
        if (l == null) {
            l = new ArrayList<>();
            l.add(value);
            map.put(key, l);
        } else if (!l.contains(value)) {
            l.add(value);
        }
    }

    /**
     * Increment the count in an integer valued map for a given key.
     *
     * @param <K> The map key type.
     * @param map The map to increment within.
     * @param key The key to increment the value for.
     * @return The new count associated with the key.
     */
    public static <K> int incrementMapCount(Map<K, Integer> map, K key) {
        int count = map.containsKey(key) ? map.get(key) : 0;
        map.put(key, count+1);
        return count+1;
    }

    /**
     * Given a list, return an iterable that yields all permutations
     * of the original list.
     *
     * Obviously combinatorial explosion will occur, so use with
     * caution only on lists that are known to be short.
     *
     * @param <T> The list member type.
     * @param l The original list.
     * @return A iterable yielding all the permutations of the original list.
     */
    public static <T> Iterable<List<T>> getPermutations(final List<T> l) {
        if (l == null) return null;
        return new Iterable<List<T>>() {
            @Override
            public Iterator<List<T>> iterator() {
                return new Iterator<List<T>>() {
                    private final List<T> original = new ArrayList<>(l);
                    private final int n = l.size();
                    private final int np = factorial(n);
                    private int index = 0;

                    private int factorial(int n) {
                        int total = n;
                        while (--n > 1) total *= n;
                        return total;
                    }

                    @Override
                    public boolean hasNext() {
                        return index < np;
                    }

                    // FIXME: see if we can do it with one array:-)
                    @Override
                    public List<T> next() {
                        List<T> pick = new ArrayList<>(original);
                        List<T> result = new ArrayList<>();
                        int current = index++;
                        int divisor = np;
                        for (int i = n; i > 0; i--) {
                            divisor /= i;
                            int j = current / divisor;
                            result.add(pick.remove(j));
                            current -= j * divisor;
                        }
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new RuntimeException("remove() not implemented");
                    }
                };
            }
        };
    }

    /**
     * Are all members of a collection the same (in the sense of ==).
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to examine.
     * @return True if all members are the same.
     */
    public static <T> boolean allSame(final Collection<T> c) {
        T datum = null;
        boolean first = true;
        for (T t : c) {
            if (first) datum = t; else if (t != datum) return false;
            first = false;
        }
        return true;
    }

    /**
     * Rotate a list by N places.
     *
     * @param <T> The list member type.
     * @param list The {@code List} to rotate.
     * @param n The number of places to rotate by (positive or negative).
     */
    public static <T> void rotate(final List<T> list, int n) {
        final int len = list.size();
        if (len <= 0 || n == 0) return;
        n %= len;
        if (n > 0) {
            for (; n > 0; n--) {
                T t = list.remove(0);
                list.add(t);
            }
        } else {
            for (; n < 0; n++) {
                T t = list.remove(n-1);
                list.add(0, t);
            }
        }
    }

    /**
     * Reverse a list.
     *
     * @param <T> The list member type.
     * @param list The {@code List} to reverse.
     */
    public static <T> void reverse(final List<T> list) {
        final int len = list.size();
        if (len <= 0) return;
        for (int i = 0, j = len-1; i < j; i++, j--) {
            T t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
    }

    /**
     * Check if two lists contents are equal but also checks for null.
     *
     * @param <T> The list member type.
     * @param one First list to compare
     * @param two Second list to compare
     * @return True if the list contents are all either both null or
     *     equal in the sense of their equals() method.
     */
    public static <T> boolean listEquals(List<T> one, List<T> two) {
        if (one == null) return two == null;
        if (two == null) return false;

        Iterator<T> oneI = one.iterator();
        Iterator<T> twoI = two.iterator();
        for (;;) {
            if (oneI.hasNext()) {
                if (twoI.hasNext()) {
                    if (!Utils.equals(oneI.next(), twoI.next())) break;
                } else {
                    break;
                }
            } else {
                return !twoI.hasNext();
            }
        }
        return false;
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The {@code Map} to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K extends Comparable<? super K>,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map) {
        return sort(map.entrySet(), new Comparator<Entry<K, V>>() {
            public int compare(Entry<K, V> a, Entry<K, V> b) {
                return a.getKey().compareTo(b.getKey());
            }});
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The {@code Map} to extract entries from.
     * @param comparator A {@code Comparator} for the values.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByKey(Map<K, V> map, final Comparator<K> comparator) {
        return sort(map.entrySet(),
            new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> a, Entry<K,V> b) {
                    return comparator.compare(a.getKey(), b.getKey());
                }});
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The {@code Map} to extract entries from.
     * @return A list of entries from the map sorted by key.
     */
    public static <K,V extends Comparable<? super V>> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map) {
        return sort(map.entrySet(), new Comparator<Entry<K, V>>() {
            public int compare(Entry<K, V> a, Entry<K, V> b) {
                return a.getValue().compareTo(b.getValue());
            }});
    }

    /**
     * Get the entries in a map in a sorted order.
     *
     * @param <K> The map key type.
     * @param <V> The map value type.
     * @param map The {@code Map} to extract entries from.
     * @param comparator A {@code Comparator} for the values.
     * @return A list of entries from the map sorted by value.
     */
    public static <K,V> List<Entry<K,V>>
        mapEntriesByValue(Map<K, V> map, final Comparator<V> comparator) {
        return sort(map.entrySet(),
            new Comparator<Entry<K,V>>() {
                public int compare(Entry<K,V> a, Entry<K,V> b) {
                    return comparator.compare(a.getValue(), b.getValue());
                }});
    }

    // Stream-based routines from here on

    /**
     * Implement dump().
     *
     * @param header Optional informational string to print first.
     * @param stream The {@code Stream} to print.
     */
    private static <T> void dump(String header, Iterable<T> it) {
        if (header != null) System.err.print(header);
        System.err.print("[ ");
        for (T v : it) {
                System.err.print(v);
                System.err.print(' ');
        }
        System.err.println(']');
    }

    /**
     * Dump a map to {@code System.err}.
     *
     * @param header Optional informational string to print first.
     * @param map The {@code Map} to print.
     */
    public static void dump(String header, Map<?,?> map) {
        if (header != null) System.err.print(header);
        System.err.print("[ ");
        for (Entry<?,?> e : map.entrySet()) {
                System.err.print(e.getKey());
                System.err.print(',');
                System.err.print(e.getValue());
                System.err.print(' ');
        }
        System.err.println(']');
    }

    /**
     * Get the first item of an array.
     *
     * @param <T> The array member type.
     * @param array The {@code Collection} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(T[] array) {
        return (array == null || array.length == 0) ? null : array[0];
    }

    /**
     * Get the first item of a list.
     *
     * @param <T> Generic type T
     * @param c The {@code Collection} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(List<T> l) {
        return (l == null || l.isEmpty()) ? null : l.get(0);
    }

    /**
     * Get the first item of a collection.
     *
     * @param <T> Generic type T
     * @param c The {@code Collection} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(Collection<T> c) {
        if (c == null || c.isEmpty()) return null;
        Iterator<T> it = c.iterator();
        return it.hasNext() ? it.next() : null;
    }

    /**
     * Convenience function to convert an array to a sorted list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T> sort(T[] array) {
        List<T> result = Arrays.<T>asList(array);
        Collections.sort(result);
        return result;
    }

    /**
     * Convenience function to convert an array to a sorted list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @param comparator A {@code Comparator} to sort with.
     * @return A list of the stream contents.
     */
    public static <T> List<T> sort(T[] array, Comparator<? super T> comparator) {
        List<T> result = Arrays.<T>asList(array);
        Collections.sort(result, comparator);
        return result;
    }

    /**
     * Convenience function to convert a collection to a sorted list.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to convert.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T> sort(Collection<T> c) {
        ArrayList<T> result = new ArrayList<T>(c);
        Collections.sort(result);
        return result;
    }

    /**
     * Convenience function to convert a collection to a map.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to convert.
     * @param comparator A {@code Comparator} to sort with.
     * @return A map of the stream contents.
     */
    public static <T> List<T> sort(Collection<T> c,
                                   Comparator<? super T> comparator) {
        List<T> result = new ArrayList<T>(c);
        Collections.sort(result, comparator);
        return result;
    }
}
