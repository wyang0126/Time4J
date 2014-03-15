/*
 * -----------------------------------------------------------------------
 * Copyright © 2012 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (ParsedValues.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.format;

import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoException;
import net.time4j.engine.ChronoFunction;
import net.time4j.engine.Chronology;
import net.time4j.tz.TimeZone;

import java.util.Iterator;
import java.util.Map;


/**
 * <p>Definiert eine aktualisierbare Wertquelle mit chronologischen Elementen,
 * denen beliebige Werte ohne weitere Validierung zugeordnet sind. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <mutable>
 */
class ParsedValues
    extends ChronoEntity<ParsedValues>
    implements Iterable<ChronoElement<?>> {

    //~ Instanzvariablen --------------------------------------------------

    private final Map<ChronoElement<?>, Object> map;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Standard-Konstruktor. </p>
     *
     * @param   map     zu umh&uuml;llende Map
     */
    ParsedValues(Map<ChronoElement<?>, Object> map) {
        super();

        if (map == null) {
            throw new NullPointerException("Missing element-value-map.");
        }

        map.remove(null);
        this.map = map;

    }

    //~ Methoden ----------------------------------------------------------

    @Override
    public boolean contains(ChronoElement<?> element) {

        return this.map.containsKey(element);

    }

    @Override
    public <V> V get(ChronoElement<V> element) {

        V value = element.getType().cast(this.map.get(element));

        if (value == null) {
            throw new ChronoException("No value found for: " + element.name());
        } else {
            return value;
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R get(ChronoFunction<? super ParsedValues, R> function) {

        if (function == TimeZone.identifier()) {
            return (R) this.map.get(ZonalElement.TIMEZONE_ID);
        }

        return super.get(function);

    }

    @Override
    public <V> boolean isValid(
        ChronoElement<V> element,
        V value // optional
    ) {

        if (element == null) {
            throw new NullPointerException("Missing chronological element.");
        }

        return true;

    }

    @Override
    public <V> ParsedValues with(
        ChronoElement<V> element,
        V value // optional
    ) {

        if (element == null) {
            throw new NullPointerException("Missing chronological element.");
        } else if (value == null) {
            this.map.remove(element);
        } else {
            this.map.put(element, value);
        }

        return this;

    }

    @Override
    public <V> V getMinimum(ChronoElement<V> element) {

        return element.getDefaultMinimum();

    }

    @Override
    public <V> V getMaximum(ChronoElement<V> element) {

        return element.getDefaultMaximum();

    }

    @Override
    public Chronology<ParsedValues> getChronology() {

        throw new UnsupportedOperationException(
            "Parsed values do not have any chronology.");

    }

    /**
     * <p>Vergleichsmethode. </p>
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof ParsedValues) {
            ParsedValues that = (ParsedValues) obj;
            return this.map.equals(that.map);
        } else {
            return false;
        }

    }

    /**
     * <p>Berechnet den Hash-Code. </p>
     */
    @Override
    public int hashCode() {

        return this.map.hashCode();

    }

    /**
     * <p>Gibt den internen Zustand in String-Form aus. </p>
     */
    @Override
    public String toString() {

        boolean first = true;
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');

        for (ChronoElement<?> key : this.map.keySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            
            sb.append(key.name());
            sb.append('=');
            sb.append(this.map.get(key));
        }

        sb.append('}');
        return sb.toString();

    }

    /**
     * <p>Liefert alle enthaltenen Elemente. </p>
     */
    @Override
    public Iterator<ChronoElement<?>> iterator() {

        return this.map.keySet().iterator();

    }

}