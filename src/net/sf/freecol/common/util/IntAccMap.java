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

import java.util.HashMap;
import java.util.Map;

public class IntAccMap<T> extends HashMap<T,Integer>
{
    public int inc(T key) {
        return addInt(key, 1);
    }

    public int addInt(T key, int value) {
        if (!containsKey(key)) {
            put(key, value);
            return value;
        }

        Integer oldval = get(key);
        if (oldval == null) {
            put(key, value);
            return value;
        }

        int newval = oldval + value;
        put(key, newval);
        return newval;
    }

    public int mulInt(T key, int value) {
        if (!containsKey(key)) {
            put(key, value);
            return value;
        }

        Integer oldval = get(key);
        if (oldval == null) {
            put(key, value);
            return value;
        }

        int newval = oldval * value;
        put(key, newval);
        return newval;
    }

    public int getInt(T key) {
        if (!containsKey(key))
            return 0;

        Integer val = get(key);
        if (val == null)
            return 0;

        return val;
    }
}
