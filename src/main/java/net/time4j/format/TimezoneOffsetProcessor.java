/*
 * -----------------------------------------------------------------------
 * Copyright © 2013 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (TimezoneOffsetProcessor.java) is part of project Time4J.
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

import net.time4j.base.UnixTime;
import net.time4j.engine.AttributeQuery;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.tz.TZID;
import net.time4j.tz.TimeZone;
import net.time4j.tz.ZonalOffset;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.time4j.tz.ZonalOffset.Sign.AHEAD_OF_UTC;
import static net.time4j.tz.ZonalOffset.Sign.BEHIND_UTC;


/**
 * <p>Verarbeitet einen festen Zeitzonen-Offset. </p>
 *
 * @author  Meno Hochschild
 */
final class TimezoneOffsetProcessor
    implements FormatProcessor<TZID> {

    //~ Statische Felder/Initialisierungen --------------------------------

    /**
     * <p>Spezial-Instanz nur zum Parsen im LONG-extended-Format ohne
     * Ersatztext (Formatieren nicht m&ouml;glich). </p>
     */
    static final TimezoneOffsetProcessor EXTENDED_LONG_PARSER =
        new TimezoneOffsetProcessor();

    //~ Instanzvariablen --------------------------------------------------

    private final DisplayMode precision;
    private final boolean extended;
    private final List<String> zeroOffsets;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Erzeugt eine neue Instanz. </p>
     *
     * @param   precision       display mode of offset format
     * @param   extended        extended or basic ISO-8601-mode
     * @param   zeroOffsets     list of replacement texts if offset is zero
     * @throws  IllegalArgumentException if replacement text is white-space-only
     */
    TimezoneOffsetProcessor(
        DisplayMode precision,
        boolean extended,
        List<String> zeroOffsets
    ) {
        super();

        if (precision == null) {
            throw new NullPointerException("Missing display mode.");
        } else if (zeroOffsets.isEmpty()) {
            throw new IllegalArgumentException("Missing zero offsets.");
        } else {
            for (String zo : zeroOffsets) {
                if (zo.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        "Zero Offset must not be white-space-only.");
                }
            }
        }

        this.precision = precision;
        this.extended = extended;
        this.zeroOffsets = zeroOffsets;

    }

    private TimezoneOffsetProcessor() {
        super();

        this.precision = DisplayMode.LONG;
        this.extended = true;
        this.zeroOffsets = Collections.emptyList();

    }

    //~ Methoden ----------------------------------------------------------

    @Override
    public void print(
        ChronoEntity<?> formattable,
        Appendable buffer,
        Attributes attributes,
        Set<ElementPosition> positions,
        FormatStep step
    ) throws IOException {

        int start = -1;
        int printed = 0;

        if (buffer instanceof CharSequence) {
            start = ((CharSequence) buffer).length();
        }

        TZID tzid = formattable.get(TimeZone.identifier());
        ZonalOffset offset;

        if (tzid instanceof ZonalOffset) {
            offset = (ZonalOffset) tzid;
        } else if (formattable instanceof UnixTime) {
            offset = TimeZone.of(tzid).getOffset((UnixTime) formattable);
        } else if (tzid == null) {
            offset = getOffset(formattable, step, attributes);
        } else {
            throw new IllegalArgumentException(
                "Cannot extract time zone offset from: " + formattable);
        }

        int total = offset.getIntegralAmount();
        int fraction = offset.getFractionalAmount();

        if ((total | fraction) == 0) {
            String zeroOffset = this.zeroOffsets.get(0);
            buffer.append(zeroOffset);
            printed = zeroOffset.length();
        } else {
            boolean negative = ((total < 0) || (fraction < 0));
            buffer.append(negative ? '-' : '+');
            printed++;

            int absValue = Math.abs(total);
            int h = absValue / 3600;
            int m = (absValue / 60) % 60;
            int s = absValue % 60;

            if (h < 10) {
                buffer.append('0');
                printed++;
            }

            String hours = String.valueOf(h);
            buffer.append(hours);
            printed += hours.length();

            if (
                (this.precision != DisplayMode.SHORT)
                || (m != 0)
            ) {
                if (this.extended) {
                    buffer.append(':');
                    printed++;
                }

                if (m < 10) {
                    buffer.append('0');
                    printed++;
                }

                String minutes = String.valueOf(m);
                buffer.append(minutes);
                printed += minutes.length();

                if (this.precision != DisplayMode.MEDIUM) {
                    if (
                        (this.precision == DisplayMode.FULL)
                        || ((s | fraction) != 0)
                    ) {
                        if (this.extended) {
                            buffer.append(':');
                            printed++;
                        }

                        if (s < 10) {
                            buffer.append('0');
                            printed++;
                        }

                        String seconds = String.valueOf(s);
                        buffer.append(seconds);
                        printed += seconds.length();

                        if (fraction != 0) {
                            buffer.append('.');
                            printed++;
                            String f = String.valueOf(Math.abs(fraction));
                            for (int i = 0, n = 9 - f.length(); i < n; i++) {
                                buffer.append('0');
                                printed++;
                            }
                            buffer.append(f);
                            printed += f.length();
                        }
                    }
                }
            }
        }

        if (
            (start != -1)
            && (printed > 0)
            && (positions != null)
        ) {
            positions.add(
                new ElementPosition(
                    ZonalElement.TIMEZONE_ID,
                    start,
                    start + printed));
        }

    }

    @Override
    public void parse(
        CharSequence text,
        ParseLog status,
        Attributes attributes,
        Map<ChronoElement<?>, Object> parsedResult,
        FormatStep step
    ) {

        int len = text.length();
        int start = status.getPosition();
        int pos = start;

        if (pos >= len) {
            status.setError(start, "Missing time zone offset.");
            return;
        }

        for (String zeroOffset : this.zeroOffsets) {
            int zl = zeroOffset.length();

            if (len - pos >= zl) {
                String compare = text.subSequence(pos, pos + zl).toString();

                boolean caseInsensitive =
                    step.getAttribute(
                        Attributes.PARSE_CASE_INSENSITIVE,
                        attributes,
                        Boolean.TRUE
                    ).booleanValue();

                if (
                    (caseInsensitive && compare.equalsIgnoreCase(zeroOffset))
                    || (!caseInsensitive && compare.equals(zeroOffset))
                ) {
                    parsedResult.put(ZonalElement.TIMEZONE_ID, ZonalOffset.UTC);
                    status.setPosition(pos + zl);
                    return;
                }
            }
        }

        char c = text.charAt(pos);
        pos++;
        ZonalOffset.Sign sign;

        if (c == '+') {
            sign = AHEAD_OF_UTC;
        } else if (c == '-') {
            sign = BEHIND_UTC;
        } else {
            status.setError(start, "Missing sign of time zone offset.");
            return;
        }

        Leniency leniency =
            step.getAttribute(
                Attributes.LENIENCY,
                attributes,
                Leniency.SMART
            );

        int hours = parseNum(text, pos, leniency);

        if (hours == -1000) {
            status.setError(
                pos,
                "Hour part in time zone offset "
                + "does not match expected pattern HH.");
            return;
        }

        if (hours < 0) {
            hours = ~hours;
            pos++;
        } else {
            pos += 2;
        }

        if (pos >= len) {
            if (this.precision == DisplayMode.SHORT) {
                parsedResult.put(
                    ZonalElement.TIMEZONE_ID,
                    ZonalOffset.of(sign, hours));
                status.setPosition(pos);
            } else {
                status.setError(
                    pos,
                    "Missing minute part in time zone offset.");
            }
            return;
        }

        int minutes = 0;
        int seconds = 0;
        int fraction = 0;

        if (this.extended) {
            if (text.charAt(pos) == ':') {
                pos++;
            } else if (this.precision == DisplayMode.SHORT) {
                parsedResult.put(
                    ZonalElement.TIMEZONE_ID,
                    ZonalOffset.of(sign, hours));
                status.setPosition(pos);
                return;
            } else if (leniency.isStrict()) {
                status.setError(pos, "Colon expected in time zone offset.");
                return;
            }
        }

        minutes = parseNum(text, pos, Leniency.STRICT);

        if (minutes == -1000) {
            if (
                (this.precision == DisplayMode.SHORT)
                && !this.extended
            ) {
                parsedResult.put(
                    ZonalElement.TIMEZONE_ID,
                    ZonalOffset.of(sign, hours));
                status.setPosition(pos);
            } else {
                status.setError(
                    pos,
                    "Minute part in time zone offset "
                    + "does not match expected pattern mm.");
            }
            return;
        }

        pos += 2;

        if (
            (this.precision == DisplayMode.LONG)
            || (this.precision == DisplayMode.FULL)
        ) {
            if (this.extended) {
                if (text.charAt(pos) == ':') {
                    pos++;
                } else if (leniency.isStrict()) {
                    status.setError(pos, "Colon expected in time zone offset.");
                    return;
                }
            }

            seconds = parseNum(text, pos, Leniency.STRICT);

            if (seconds == -1000) {
                if (this.precision == DisplayMode.FULL) {
                    status.setError(
                        pos,
                        "Second part in time zone offset "
                        + "does not match expected pattern ss.");
                    return;
                } else {
                    seconds = 0;
                }
            } else {
                pos += 2;

                if (pos + 10 <= len) {
                    char dot = text.charAt(pos);

                    if (dot == '.') {
                        pos++;

                        for (int i = pos, n = pos + 9; i < n; i++) {
                            char digit = text.charAt(i);

                            if ((digit >= '0') && (digit <= '9')) {
                                fraction = fraction * 10 + (digit - '0');
                                pos++;
                            } else {
                                status.setError(
                                    pos,
                                    "9 digits in fractional part of "
                                    + "time zone offset expected.");
                                return;
                            }
                        }
                    }
                }
            }

        }

        ZonalOffset offset;

        if (fraction == 0) {
            offset = ZonalOffset.of(sign, hours, minutes, seconds);
        } else {
            int total = hours * 3600 + minutes * 60 + seconds;

            if (sign == BEHIND_UTC) {
                total = -total;
                fraction = -fraction;
            }

            offset = new ZonalOffset(total, fraction);
        }

        parsedResult.put(ZonalElement.TIMEZONE_ID, offset);
        status.setPosition(pos);

    }

    @Override
    public ChronoElement<TZID> getElement() {

        return ZonalElement.TIMEZONE_ID;

    }

    @Override
    public FormatProcessor<TZID> withElement(ChronoElement<TZID> element) {

        return this;

    }

    @Override
    public boolean isNumerical() {

        return false;

    }

    private static ZonalOffset getOffset(
        ChronoEntity<?> formattable,
        FormatStep step,
        Attributes attributes
    ) {

        AttributeQuery aq = step.getQuery(attributes);

        if (aq.contains(Attributes.TIMEZONE_ID)) {
            TZID tzid = aq.get(Attributes.TIMEZONE_ID);

            if (tzid instanceof ZonalOffset) {
                return (ZonalOffset) tzid;
            }
        }

        throw new IllegalArgumentException(
            "Cannot extract time zone offset from format attributes for: "
            + formattable);

    }

    private static int parseNum(
        CharSequence text,
        int pos,
        Leniency leniency
    ) {

        int total = 0;

        for (int i = 0; i < 2; i++) {
            char c;

            if (pos + i >= text.length()) {
                c = '\u0000';
            } else {
                c = text.charAt(pos + i);
            }

            if ((c >= '0') && (c <= '9')) {
                total = total * 10 + (c - '0');
            } else if ((i == 0) || leniency.isStrict()) {
                return -1000;
            } else {
                return ~total;
            }
        }

        return total;

    }

}