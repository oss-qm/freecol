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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.sf.freecol.common.util.CachingFunction;


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
     * Do all members of an array match a predicate?
     *
     * @param <T> The array member type.
     * @param array The array to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if all members pass the predicate test.
     */
    public static <T> boolean all(T[] array, Predicate<? super T> predicate) {
        return (array == null) ? true
            : all_internal(Arrays.stream(array), predicate);
    }

    /**
     * Do all members of an collection match a predicate?
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if all members pass the predicate test.
     */
    public static <T> boolean all(Collection<T> c,
                                  Predicate<? super T> predicate) {
        return (c == null) ? true : all_internal(c.stream(), predicate);
    }

    /**
     * Do all members of an stream match a predicate?
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if all members pass the predicate test.
     */
    public static <T> boolean all(Stream<T> stream,
                                  Predicate<? super T> predicate) {
        return (stream == null) ? true : all_internal(stream, predicate);
    }

    /**
     * Implementation of all().
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if all members pass the predicate test.
     */
    private static <T> boolean all_internal(Stream<T> stream,
                                            Predicate<? super T> predicate) {
        return stream.allMatch(predicate);
    }

    public static final Predicate alwaysTruePred
        = new Predicate() {
            public boolean test(Object o) {
                return true;
            }};

    /**
     * Helper to create a predicate which is always true.
     *
     * @param <T> The stream member type.
     * @return The always valid predicate for the stream type.
     */
    public static <T> Predicate<T> alwaysTrue() {
        return alwaysTruePred;
    }

    /**
     * Is an array non-empty?
     *
     * @param <T> The array member type.
     * @param array The array to test.
     * @return True if the array is non-empty.
     */
    public static <T> boolean any(T[] array) {
        return array != null && array.length > 0;
    }

    /**
     * Does any member of an array match a predicate?
     *
     * @param <T> The array member type.
     * @param array The array to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if any member passes the predicate test.
     */
    public static <T> boolean any(T[] array, Predicate<? super T> predicate) {
        return any_internal(Arrays.stream(array), predicate);
    }

    /**
     * Is a collection non-empty?
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to test.
     * @return True if the collection is non-empty.
     */
    public static <T> boolean any(Collection<T> c) {
        return c != null && !c.isEmpty();
    }

    /**
     * Does any member of a collection match a predicate?
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if any member passes the predicate test.
     */
    public static <T> boolean any(Collection<T> c,
                                  Predicate<? super T> predicate) {
        return (c == null) ? false : any_internal(c.stream(), predicate);
    }

    /**
     * Is a stream non-empty?
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @return True if the stream is non-empty.
     */
    public static <T> boolean any(Stream<T> stream) {
        return stream != null && stream.findFirst().isPresent();
    }

    /**
     * Does any member of a stream match a predicate?
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if any member passes the predicate test.
     */
    public static <T> boolean any(Stream<T> stream,
                                  Predicate<? super T> predicate) {
        return (stream == null) ? false : any_internal(stream, predicate);
    }

    /**
     * Implementation of any().
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if any member passes the predicate test.
     */
    private static <T> boolean any_internal(Stream<T> stream,
                                            Predicate<? super T> predicate) {
        return stream.anyMatch(predicate);
    }

    /**
     * Helper to create a caching ToIntFunction.
     *
     * @param <T> The argument type to be converted to int.
     * @param f The integer valued function to cache.
     * @return A caching {@code ToIntFunction}.
     */
    public static <T> ToIntFunction<T> cacheInt(Function<T, Integer> f) {
        return t -> new CachingFunction<T, Integer>(f).apply(t);
    }

    /**
     * Helper to create a caching comparator.
     *
     * @param <T> The argument type to be converted to int.
     * @param f The integer valued function to use in comparison.
     * @return A caching {@code Comparator}.
     */
    public static <T> Comparator<T> cachingIntComparator(Function<T, Integer> f) {
        return Comparator.comparingInt(cacheInt(f));
    }

    /**
     * Helper to create a caching ToDoubleFunction.
     *
     * @param <T> The argument type to be converted to double.
     * @param f The double valued function to cache.
     * @return A caching {@code ToDoubleFunction}.
     */
    public static <T> ToDoubleFunction<T> cacheDouble(Function<T, Double> f) {
        return t -> new CachingFunction<T, Double>(f).apply(t);
    }

    /**
     * Helper to create a caching comparator.
     *
     * @param <T> The argument type to be converted to double.
     * @param f The double valued function to use in comparison.
     * @return A caching {@code Comparator}.
     */
    public static <T> Comparator<T> cachingDoubleComparator(Function<T, Double> f) {
        return Comparator.comparingDouble(cacheDouble(f));
    }

    /**
     * Count the number of members of an array.
     *
     * @param <T> The array member type.
     * @param array The array to check.
     * @return The number of items that matched.
     */
    public static <T> int count(T[] array) {
        return (array == null) ? 0 : array.length;
    }

    /**
     * Count the number of members of an array that match a predicate.
     *
     * @param <T> The array member type.
     * @param array The array to check.
     * @param predicate A {@code Predicate} to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(T[] array,
                                Predicate<? super T> predicate) {
        return (array == null ? 0 : (int)(Arrays.<T>stream(array).filter(predicate).count()));
    }

    /**
     * Count the number of members of a collection that match a predicate.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to check.
     * @return The number of items that matched.
     */
    public static <T> int count(Collection<T> c) {
        return (c == null) ? 0 : c.size();
    }

    /**
     * Count the number of members of a collection that match a predicate.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to check.
     * @param predicate A {@code Predicate} to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(Collection<T> c,
                                Predicate<? super T> predicate) {
        return (c == null ? 0 : (int)(c.stream().filter(predicate).count()));
    }

    /**
     * Count the number of members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @return The number of items that matched.
     */
    public static <T> int count(Stream<T> stream) {
        return (stream == null) ? 0 : (int)stream.count();
    }

    /**
     * Count the number of members of a stream that match a predicate.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @param predicate A {@code Predicate} to test with.
     * @return The number of items that matched.
     */
    public static <T> int count(Stream<T> stream,
                                Predicate<? super T> predicate) {
        return (stream == null) ? 0 : (int)stream.filter(predicate).count();
    }

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
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The array member type.
     * @param array The array to search.
     * @param predicate A {@code Predicate} to match with.
     * @return The item found, or null if not found.
     */
    public static <T> T find(T[] array, Predicate<? super T> predicate) {
        return find_internal(Arrays.stream(array), predicate, null);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The array member type.
     * @param array The array to search.
     * @param predicate A {@code Predicate} to match with.
     * @param fail The result to return on failure.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(T[] array, Predicate<? super T> predicate,
                             T fail) {
        return find_internal(Arrays.stream(array), predicate, fail);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to search.
     * @param predicate A {@code Predicate} to match with.
     * @return The item found, or null if not found.
     */
    public static <T> T find(Collection<T> c, Predicate<? super T> predicate) {
        return find_internal(c.stream(), predicate, (T)null);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to search.
     * @param predicate A {@code Predicate} to match with.
     * @param fail The value to return if nothing is found.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(Collection<T> c, Predicate<? super T> predicate,
                             T fail) {
        return find_internal(c.stream(), predicate, fail);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The stream member type.
     * @param stream A {@code Stream} to search.
     * @param predicate A {@code Predicate} to match with.
     * @return The item found, or null if not found.
     */
    public static <T> T find(Stream<T> stream,
                             Predicate<? super T> predicate) {
        return (stream == null) ? null : find_internal(stream, predicate, null);
    }

    /**
     * Simple stream search for the first item that matches a predicate.
     *
     * @param <T> The stream member type.
     * @param stream A {@code Stream} to search.
     * @param predicate A {@code Predicate} to match with.
     * @param fail The value to return if nothing is found.
     * @return The item found, or fail if not found.
     */
    public static <T> T find(Stream<T> stream, Predicate<? super T> predicate,
                             T fail) {
        return (stream == null) ? fail : find_internal(stream, predicate, fail);
    }

    /**
     * Implement find().
     *
     * @param <T> The stream member type.
     * @param stream A {@code Stream} to search.
     * @param predicate A {@code Predicate} to match with.
     * @param fail The value to return if nothing is found.
     * @return The item found, or fail if not found.
     */
    private static <T> T find_internal(Stream<T> stream,
                                       Predicate<? super T> predicate,
                                       T fail) {
        return first_internal(stream.filter(predicate), fail);
    }

    /**
     * Get the first item of an array.
     *
     * @param <T> The array member type.
     * @param array The {@code Collection} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(T[] array) {
        return (array == null || array.length == 0) ? null
            : first_internal(Arrays.stream(array), null);
    }

    /**
     * Get the first item of a collection.
     *
     * @param <T> Generic type T
     * @param c The {@code Collection} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(Collection<T> c) {
        return (c == null || c.isEmpty()) ? null
            : first_internal(c.stream(), null);
    }

    /**
     * Get the first item of a stream.
     *
     * @param <T> Generic type T
     * @param stream The {@code Stream} to search.
     * @return The first item, or null on failure.
     */
    public static <T> T first(Stream<T> stream) {
        return (stream == null) ? null : first_internal(stream, null);
    }

    /**
     * Implement first().
     *
     * @param <T> Generic type T
     * @param stream The {@code Stream} to search.
     * @param fail The value to return on failure.
     * @return The first item, or fail on failure.
     */
    private static <T> T first_internal(Stream<T> stream, T fail) {
        return stream.findFirst().orElse(fail);
    }

    /**
     * Flatten an array into a stream derived from its component streams.
     *
     * @param <T> The array member type.
     * @param <R> The resulting stream member type.
     * @param array The array to flatten.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(T[] array,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return flatten_internal(Arrays.stream(array), mapper);
    }

    /**
     * Flatten an array into a stream derived from its component streams.
     *
     * @param <T> The array member type.
     * @param <R> The resulting stream member type.
     * @param array The array to flatten.
     * @param predicate A {@code Predicate} to filter the collection with.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(T[] array,
        Predicate<? super T> predicate,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return flatten_internal(Arrays.stream(array), predicate, mapper);
    }

    /**
     * Flatten a collection into a stream derived from its component streams.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting stream member type.
     * @param c The {@code Collection} to flatten.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(Collection<T> c,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return flatten_internal(c.stream(), mapper);
    }

    /**
     * Flatten a collection into a stream derived from its component streams.
     *
     * @param <T> The collection member type.
     * @param <R> The resulting stream member type.
     * @param c The {@code Collection} to flatten.
     * @param predicate A {@code Predicate} to filter the collection with.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped collection.
     */
    public static <T, R> Stream<R> flatten(Collection<T> c,
        Predicate<? super T> predicate,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return flatten_internal(c.stream(), predicate, mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The {@code Stream} to flatten.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped stream.
     */
    public static <T, R> Stream<R> flatten(Stream<T> stream,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return (stream == null) ? Stream.<R>empty()
            : flatten_internal(stream, mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The {@code Stream} to flatten.
     * @param predicate A {@code Predicate} to filter the collection with.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped stream.
     */
    public static <T, R> Stream<R> flatten(Stream<T> stream,
        Predicate<? super T> predicate,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return (stream == null) ? Stream.<R>empty()
            : flatten_internal(stream, predicate, mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The {@code Stream} to flatten.
     * @param predicate A {@code Predicate} to filter the collection with.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped stream.
     */
    private static <T, R> Stream<R> flatten_internal(Stream<T> stream,
        Predicate<? super T> predicate,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return stream.filter(predicate).flatMap(mapper);
    }

    /**
     * Flatten the members of a stream.
     *
     * @param <T> The stream member type.
     * @param <R> The resulting stream member type.
     * @param stream The {@code Stream} to flatten.
     * @param mapper A mapping {@code Function} to apply.
     * @return A stream of the mapped stream.
     */
    private static <T, R> Stream<R> flatten_internal(Stream<T> stream,
        Function<? super T, ? extends Stream<? extends R>> mapper) {
        return stream.filter(predicate).flatMap(mapper);
    }

    /**
     * Create a predicate for a type that returns true if the argument is
     * not null.
     *
     * @param <T> The input type.
     * @return A suitable {@code Predicate}.
     */
    public static <T> Predicate<T> isNotNull() {
        return (T t) -> t != null;
    }

    /**
     * Create a predicate for a type that returns true if the argument is
     * not null.
     *
     * @param <T> The input type.
     * @param <V> A type to transform to.
     * @param mapper A function to transform the input type.
     * @return A suitable {@code Predicate}.
     */
    public static <T,V> Predicate<T> isNotNull(Function<? super T,V> mapper) {
        return (T t) -> mapper.apply(t) != null;
    }

    /**
     * Create a predicate for a type that returns true if the argument is
     * null.
     *
     * @param <T> The input type.
     * @return A suitable {@code Predicate}.
     */
    public static <T> Predicate<T> isNull() {
        return (T t) -> t == null;
    }

    /**
     * Create a predicate for a type that returns true if the argument is
     * not null.
     *
     * @param <T> The input type.
     * @param <V> A type to transform to.
     * @param mapper A function to transform the input type.
     * @return A suitable {@code Predicate}.
     */
    public static <T,V> Predicate<T> isNull(Function<? super T,V> mapper) {
        return (T t) -> mapper.apply(t) == null;
    }

    /**
     * Convenience function to convert a stream to an iterable.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to convert.
     * @return The suitable {@code Iterable}.
     */
    public static <T> Iterable<T> iterable(final Stream<T> stream) {
        return new Iterable<T>() {
            public Iterator<T> iterator() { return stream.iterator(); }
        };
    }

    /**
     * Create a predicate for a type that returns true if it equals a key.
     *
     * @param <T> The input type.
     * @param key The key to match.
     * @return A suitable {@code Predicate}.
     */
    public static <T> Predicate<T> matchKey(final T key) {
        return t -> t == key;
    }

    /**
     * Create a predicate for a type that returns true if it equals a key
     * in the sense of "equals".
     *
     * @param <T> The input type.
     * @param key The key to match.
     * @return A suitable {@code Predicate}.
     */
    public static <T> Predicate<T> matchKeyEquals(final T key) {
        return t -> Utils.equals(t, key);
    }

    /**
     * Create a predicate for a type that returns true if a mapper applied
     * to it causes it to equal a key.
     *
     * @param <T> The input type.
     * @param <K> The key type.
     * @param key The key to match.
     * @param mapper The mapper {@code Function} to apply.
     * @return A suitable {@code Predicate}.
     */
    public static <T, K> Predicate<T> matchKey(final K key,
                                               Function<T, K> mapper) {
        return t -> mapper.apply(t) == key;
    }

    /**
     * Create a predicate for a type that returns true if a mapper applied
     * to it causes it to equal a key.
     *
     * @param <T> The input type.
     * @param <K> The key type.
     * @param key The key to match.
     * @param mapper The mapper {@code Function} to apply.
     * @return A suitable {@code Predicate}.
     */
    public static <T, K> Predicate<T> matchKeyEquals(final K key,
                                                     Function<T, K> mapper) {
        return t -> Utils.equals(mapper.apply(t), key);
    }

    /**
     * Find the maximum int value in an array.
     *
     * @param <T> The collection member type.
     * @param array The array to check.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(T[] array, ToIntFunction<T> tif) {
        return max_internal(Arrays.stream(array), tif);
    }

    /**
     * Find the maximum int value in an array.
     *
     * @param <T> The collection member type.
     * @param array The array to check.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(T[] array, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return max_internal(Arrays.stream(array), predicate, tif);
    }

    /**
     * Find the maximum int value in a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to check.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Collection<T> c, ToIntFunction<T> tif) {
        return max_internal(c.stream(), tif);
    }

    /**
     * Find the maximum int value in a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to check.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Collection<T> c, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return max(c.stream(), predicate, tif);
    }

    /**
     * Find the maximum int value in a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Stream<T> stream, ToIntFunction<T> tif) {
        return (stream == null) ? MAX_DEFAULT
            : max_internal(stream, tif);
    }

    /**
     * Find the maximum int value in a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    public static <T> int max(Stream<T> stream, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return (stream == null) ? MAX_DEFAULT
            : max_internal(stream, predicate, tif);
    }

    /**
     * Implement max.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    private static <T> int max_internal(Stream<T> stream,
                                        Predicate<? super T> predicate,
                                        ToIntFunction<T> tif) {
        return stream.filter(predicate).mapToInt(tif).max()
            .orElse(MAX_DEFAULT);
    }

    /**
     * Implement max.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to check.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The maximum value found, or zero if the input is empty.
     */
    private static <T> int max_internal(Stream<T> stream,
                                        ToIntFunction<T> tif) {
        return stream.mapToInt(tif).max()
            .orElse(MAX_DEFAULT);
    }

    /**
     * Is an array null or empty?
     *
     * @param <T> The array member type.
     * @param array The array to test.
     * @return True if an array is null or empty.
     */
    public static <T> boolean none(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Do none of the members of an array match a predicate?
     *
     * @param <T> The array member type.
     * @param array The array to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if no member passes the predicate test.
     */
    public static <T> boolean none(T[] array, Predicate<? super T> predicate) {
        return none_internal(Arrays.stream(array), predicate);
    }

    /**
     * Is a collection empty?
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to test.
     * @return True if the collection is null or empty.
     */
    public static <T> boolean none(Collection<T> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Do none of the members of a collection match a predicate?
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if no member passes the predicate test.
     */
    public static <T> boolean none(Collection<T> c,
                                   Predicate<? super T> predicate) {
        return none_internal(c.stream(), predicate);
    }

    /**
     * Is a stream null or empty?
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @return True if the stream is null or empty.
     */
    public static <T> boolean none(Stream<T> stream) {
        return stream == null || !stream.findFirst().isPresent();
    }

    /**
     * Do none of the members of a stream match a predicate?
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if no member passes the predicate test.
     */
    public static <T> boolean none(Stream<T> stream,
                                   Predicate<? super T> predicate) {
        return (stream == null) ? true : none_internal(stream, predicate);
    }

    /**
     * Implementation of none().
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to test.
     * @param predicate The {@code Predicate} to test with.
     * @return True if no member passes the predicate test.
     */
    private static <T> boolean none_internal(Stream<T> stream,
                                             Predicate<? super T> predicate) {
        return stream.noneMatch(predicate);
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

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to collect.
     * @return A list of the stream contents.
     */
    public static <T extends Comparable<? super T>> List<T> sort(Stream<T> stream) {
        final Comparator<T> comparator = Comparator.naturalOrder();
        return (stream == null) ? Collections.<T>emptyList()
            : sort_internal(stream, comparator);
    }

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to collect.
     * @param comparator A {@code Comparator} to sort with.
     * @return A list of the stream contents.
     */
    public static <T> List<T> sort(Stream<T> stream,
                                   Comparator<? super T> comparator) {
        return (stream == null) ? Collections.<T>emptyList()
            : sort_internal(stream, comparator);
    }

    /**
     * Implement sort.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to collect.
     * @param comparator A {@code Comparator} to sort with.
     * @return A list of the stream contents.
     */
    private static <T> List<T> sort_internal(Stream<T> stream,
                                             Comparator<? super T> comparator) {
        return stream.sorted(comparator).collect(Collectors.<T>toList());
    }

    /**
     * Take the sum of the members of an array.
     *
     * @param <T> The collection member type.
     * @param array The array to sum.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(T[] array, ToIntFunction<T> tif) {
        return sum_internal(Arrays.stream(array), tif);
    }

    /**
     * Take the sum of the members of an array.
     *
     * @param <T> The collection member type.
     * @param array The array to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(T[] array, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return sum_internal(Arrays.stream(array), predicate, tif);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to sum.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Collection<T> c, ToIntFunction<T> tif) {
        return sum_internal(c.stream(), tif);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to map the stream to int with.
     * @return The sum of the values found.
     */
    public static <T> int sum(Collection<T> c, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return sum_internal(c.stream(), predicate, tif);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Stream<T> stream, ToIntFunction<T> tif) {
        return (stream == null) ? SUM_DEFAULT
            : sum_internal(stream, tif);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    public static <T> int sum(Stream<T> stream, Predicate<? super T> predicate,
                              ToIntFunction<T> tif) {
        return (stream == null) ? SUM_DEFAULT
            : sum_internal(stream, predicate, tif);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    private static <T> int sum_internal(Stream<T> stream,
                                        ToIntFunction<T> tif) {
        return stream.mapToInt(tif).sum();
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tif A {@code ToIntFunction} to convert members to an int.
     * @return The sum of the values found.
     */
    private static <T> int sum_internal(Stream<T> stream,
                                        Predicate<? super T> predicate,
                                        ToIntFunction<T> tif) {
        return stream.filter(predicate).mapToInt(tif).sum();
    }

    /**
     * Take the sum of the members of an array.
     *
     * @param <T> The collection member type.
     * @param array The array to sum.
     * @param tdf A {@code ToDoubleFunction} to convert members
     *     to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(T[] array, ToDoubleFunction<T> tdf) {
        return sumDouble_internal(Arrays.stream(array), tdf);
    }

    /**
     * Take the sum of the members of an array.
     *
     * @param <T> The collection member type.
     * @param array The array to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tdf A {@code ToDoubleFunction} to map the stream to
     *     double with.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(T[] array,
                                       Predicate<? super T> predicate,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble_internal(Arrays.stream(array), predicate, tdf);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to sum.
     * @param tdf A {@code ToDoubleFunction} to convert members
     *     to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Collection<T> c,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble_internal(c.stream(), tdf);
    }

    /**
     * Take the sum of the members of a collection.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to sum.
     * @param predicate A {@code Predicate} to match with.
     * @param tdf A {@code ToDoubleFunction} to map the stream to
     *     double with.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Collection<T> c,
                                       Predicate<? super T> predicate,
                                       ToDoubleFunction<T> tdf) {
        return sumDouble_internal(c.stream(), predicate, tdf);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param tdf A {@code ToDoubleFunction} to convert members
     *     to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Stream<T> stream,
                                       ToDoubleFunction<T> tdf) {
        return (stream == null) ? SUM_DOUBLE_DEFAULT
            : sumDouble_internal(stream, tdf);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param predicate A {@code Predicate} to select members.
     * @param tdf A {@code ToIntFunction} to convert members to a double.
     * @return The sum of the values found.
     */
    public static <T> double sumDouble(Stream<T> stream,
                                       Predicate<? super T> predicate,
                                       ToDoubleFunction<T> tdf) {
        return (stream == null) ? SUM_DOUBLE_DEFAULT
            : sumDouble_internal(stream, predicate, tdf);
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param predicate A {@code Predicate} to select members.
     * @param tdf A {@code ToIntFunction} to convert members to a double.
     * @return The sum of the values found.
     */
    private static <T> double sumDouble_internal(Stream<T> stream,
        Predicate<? super T> predicate,
        ToDoubleFunction<T> tdf) {
        return stream.filter(predicate).mapToDouble(tdf).sum();
    }

    /**
     * Take the sum of the members of a stream.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to sum.
     * @param tdf A {@code ToIntFunction} to convert members to a double.
     * @return The sum of the values found.
     */
    private static <T> double sumDouble_internal(Stream<T> stream,
        ToDoubleFunction<T> tdf) {
        return stream.mapToDouble(tdf).sum();
    }

    /**
     * Make a collector that takes lists and appends them.
     *
     * @param <T> The list member type.
     * @return A list appending collector.
     */
    public static <T> Collector<List<T>,?,List<T>> toAppendedList() {
        final BinaryOperator<List<T>> squash = (l1, l2) ->
            (l1.isEmpty()) ? l2 : (l1.addAll(l2)) ? l1 : l1;
        return Collectors.reducing(Collections.<T>emptyList(), squash);
    }

    /**
     * Convenience function to convert an array to a list.
     *
     * @param <T> The array member type.
     * @param array The array to convert.
     * @return A map of the stream contents.
     */
    public static <T> List<T> toList(T[] array) {
        return toList_internal(Arrays.stream(array));
    }

    /**
     * Convenience function to convert a collection to a list.
     *
     * @param <T> The collection member type.
     * @param c The {@code Collection} to convert.
     * @return A map of the stream contents.
     */
    public static <T> List<T> toList(Collection<T> c) {
        return toList_internal(c.stream());
    }

    /**
     * Convenience function to collect a stream to a list.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to collect.
     * @return A list of the stream contents.
     */
    public static <T> List<T> toList(Stream<T> stream) {
        return (stream == null) ? Collections.<T>emptyList()
            : toList_internal(stream);
    }

    /**
     * Implement toList.
     *
     * @param <T> The stream member type.
     * @param stream The {@code Stream} to collect.
     * @return A list of the stream contents.
     */
    private static <T> List<T> toList_internal(Stream<T> stream) {
        return stream.collect(Collectors.<T>toList());
    }

    /**
     * Create a new collector that accumulates to a list but excludes
     * null members.
     *
     * @param <T> The stream member type.
     * @return A list collectors.
     */
    public static <T> Collector<T,?,List<T>> toListNoNulls() {
        return Collector.<T,List<T>>of((Supplier<List<T>>)ArrayList::new,
            (left, right) -> { if (right != null) left.add(right); },
            (left, right) -> { left.addAll(right); return left; },
            Collector.Characteristics.IDENTITY_FINISH);
    }

    /**
     * Convert an iterator to a stream.
     *
     * @param <T> A {@link Stream}
     * @param iterator The {@code Iterator} to convert.
     * @return The resulting {@code Stream}.
     */
    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        return toStream(() -> iterator);
    }

    /**
     * Convert an iterable to a stream.
     *
     * @param <T> A {@link Stream}
     * @param iterable The {@code Iterable} to convert.
     * @return The resulting {@code Stream}.
     */
    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
