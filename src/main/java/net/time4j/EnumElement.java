/*
 * -----------------------------------------------------------------------
 * Copyright © 2013 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (EnumElement.java) is part of project Time4J.
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

package net.time4j;

import net.time4j.engine.AttributeQuery;
import net.time4j.engine.ChronoEntity;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarText;
import net.time4j.format.NumericalElement;
import net.time4j.format.OutputContext;
import net.time4j.format.ParseLog;
import net.time4j.format.TextElement;
import net.time4j.format.TextWidth;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.Locale;

import static net.time4j.format.CalendarText.ISO_CALENDAR_TYPE;


/**
 * <p>Allgemeines verstellbares chronologisches Element auf enum-Basis. </p>
 *
 * @param       <V> generic enum type of element values
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
final class EnumElement<V extends Enum<V>>
    extends AbstractValueElement<V, PlainDate>
    implements NavigableElement<V, PlainDate>,
               NumericalElement<V>,
               TextElement<V> {

    //~ Statische Felder/Initialisierungen --------------------------------

    /** Element-Index. */
    static final int MONTH = 101;
    /** Element-Index. */
    static final int DAY_OF_WEEK = 102;
    /** Element-Index. */
    static final int QUARTER_OF_YEAR = 103;

    private static final long serialVersionUID = 2055272540517425102L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final Class<V> type;
    private transient final V dmin;
    private transient final V dmax;
    private transient final int index;
    private transient final char symbol;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Konstruiert ein neues Element mit den angegebenen Details. </p>
     *
     * @param   name        name of element
     * @param   type        reified type of element values
     * @param   defaultMin  default minimum
     * @param   defaultMax  default maximum
     * @param   index       element index
     * @param   symbol      CLDR-symbol used in format patterns
     */
    EnumElement(
        String name,
        Class<V> type,
        V defaultMin,
        V defaultMax,
        int index,
        char symbol
    ) {
        super(name);

        this.type = type;
        this.dmin = defaultMin;
        this.dmax = defaultMax;
        this.index = index;
        this.symbol = symbol;

    }

    //~ Methoden ----------------------------------------------------------

    @Override
    public Class<V> getType() {

        return this.type;

    }

    @Override
    public char getSymbol() {

        return this.symbol;

    }

    @Override
    public V getDefaultMinimum() {

        return this.dmin;

    }

    @Override
    public V getDefaultMaximum() {

        return this.dmax;

    }

    @Override
    public boolean isDateElement() {

        return true;

    }

    @Override
    public boolean isTimeElement() {

        return false;

    }

    @Override
    public ZonalOperator<PlainDate> setToNext(V value) {

        return new NavigationOperator<V, PlainDate>(
            this, OperatorType.NAV_NEXT, value);

    }

    @Override
    public ZonalOperator<PlainDate> setToPrevious(V value) {

        return new NavigationOperator<V, PlainDate>(
            this, OperatorType.NAV_PREVIOUS, value);

    }

    @Override
    public ZonalOperator<PlainDate> setToNextOrSame(V value) {

        return new NavigationOperator<V, PlainDate>(
            this, OperatorType.NAV_NEXT_OR_SAME, value);

    }

    @Override
    public ZonalOperator<PlainDate> setToPreviousOrSame(V value) {

        return new NavigationOperator<V, PlainDate>(
            this, OperatorType.NAV_PREVIOUS_OR_SAME, value);

    }

    @Override
    public int numerical(V value) {

        return (value.ordinal() + 1);

    }

    @Override
    public void print(
        ChronoEntity<?> context,
        Appendable buffer,
        AttributeQuery attributes
    ) throws IOException {

        buffer.append(this.accessor(attributes).print(context.get(this)));

    }

    @Override
    public V parse(
        CharSequence text,
        ParseLog status,
        AttributeQuery attributes
    ) {

        boolean caseInsensitive =
            attributes
                .get(Attributes.PARSE_CASE_INSENSITIVE, Boolean.TRUE)
                .booleanValue();
        boolean partialCompare =
            attributes
                .get(Attributes.PARSE_PARTIAL_COMPARE, Boolean.FALSE)
                .booleanValue();

        return this.accessor(attributes).parse(
            text,
            status,
            this.getType(),
            caseInsensitive,
            partialCompare
        );

    }

    /**
     * <p>Liefert einen Zugriffsindex zur Optimierung der Elementsuche. </p>
     *
     * @return  int
     */
    int getIndex() {

        return this.index;

    }

    private CalendarText.Accessor accessor(AttributeQuery attributes) {

        CalendarText cnames =
            CalendarText.getInstance(
                attributes.get(Attributes.CALENDAR_TYPE, ISO_CALENDAR_TYPE),
                attributes.get(Attributes.LANGUAGE, Locale.ROOT));

        TextWidth textWidth =
            attributes.get(Attributes.TEXT_WIDTH, TextWidth.WIDE);

        switch (this.index) {
            case MONTH:
                return cnames.getMonths(
                    textWidth,
                    attributes.get(
                        Attributes.OUTPUT_CONTEXT,
                        OutputContext.FORMAT),
                    false);
            case DAY_OF_WEEK:
                return cnames.getWeekdays(
                    textWidth,
                    attributes.get(
                        Attributes.OUTPUT_CONTEXT,
                        OutputContext.FORMAT));
            case QUARTER_OF_YEAR:
                return cnames.getQuarters(
                    textWidth,
                    attributes.get(
                        Attributes.OUTPUT_CONTEXT,
                        OutputContext.FORMAT));
            default:
                throw new UnsupportedOperationException(this.name());
        }

    }

    private Object readResolve() throws ObjectStreamException {

        Object element = PlainDate.lookupElement(this.name());

        if (element == null) {
            throw new InvalidObjectException(this.name());
        } else {
            return element;
        }

    }

}