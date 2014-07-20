/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2014 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (Moment.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j;

import net.time4j.base.GregorianMath;
import net.time4j.base.MathUtils;
import net.time4j.base.TimeSource;
import net.time4j.base.UnixTime;
import net.time4j.engine.AttributeQuery;
import net.time4j.engine.ChronoElement;
import net.time4j.engine.ChronoEntity;
import net.time4j.engine.ChronoException;
import net.time4j.engine.ChronoMerger;
import net.time4j.engine.ChronoOperator;
import net.time4j.engine.Chronology;
import net.time4j.engine.ElementRule;
import net.time4j.engine.EpochDays;
import net.time4j.engine.Temporal;
import net.time4j.engine.TimeAxis;
import net.time4j.engine.TimePoint;
import net.time4j.engine.UnitRule;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarType;
import net.time4j.format.ChronoFormatter;
import net.time4j.format.ChronoPattern;
import net.time4j.format.DisplayMode;
import net.time4j.format.Leniency;
import net.time4j.format.TextWidth;
import net.time4j.scale.LeapSeconds;
import net.time4j.scale.TimeScale;
import net.time4j.scale.UniversalTime;
import net.time4j.tz.TZID;
import net.time4j.tz.Timezone;
import net.time4j.tz.TransitionStrategy;
import net.time4j.tz.ZonalOffset;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.time4j.PlainTime.*;
import static net.time4j.SI.NANOSECONDS;
import static net.time4j.SI.SECONDS;
import static net.time4j.scale.TimeScale.GPS;
import static net.time4j.scale.TimeScale.POSIX;
import static net.time4j.scale.TimeScale.TAI;
import static net.time4j.scale.TimeScale.UTC;


/**
 * <p>Represents an instant/moment on the universal timeline with reference
 * to the timezone UTC (UTC+00:00 / Greenwich-meridian). </p>
 *
 * <p>The JDK-equivalent is traditionally the class {@code java.util.Date}.
 * In contrast to that old class this class stores the elapsed time not just
 * in millisecond but in nanosecond precision on 96-bit base. </p>
 *
 * <p>Following elements which are declared as constants are registered by
 * this class with access in UTC timezone (always without base unit): </p>
 *
 * <ul>
 *  <li>{@link PlainDate#YEAR}</li>
 *  <li>{@link PlainDate#YEAR_OF_WEEKDATE}</li>
 *  <li>{@link PlainDate#QUARTER_OF_YEAR}</li>
 *  <li>{@link PlainDate#MONTH_OF_YEAR}</li>
 *  <li>{@link PlainDate#MONTH_AS_NUMBER}</li>
 *  <li>{@link PlainDate#DAY_OF_MONTH}</li>
 *  <li>{@link PlainDate#DAY_OF_QUARTER}</li>
 *  <li>{@link PlainDate#DAY_OF_WEEK}</li>
 *  <li>{@link PlainDate#DAY_OF_YEAR}</li>
 *  <li>{@link PlainDate#WEEKDAY_IN_MONTH}</li>
 *  <li>{@link PlainTime#AM_PM_OF_DAY}</li>
 *  <li>{@link PlainTime#CLOCK_HOUR_OF_AMPM}</li>
 *  <li>{@link PlainTime#CLOCK_HOUR_OF_DAY}</li>
 *  <li>{@link PlainTime#DIGITAL_HOUR_OF_AMPM}</li>
 *  <li>{@link PlainTime#DIGITAL_HOUR_OF_DAY}</li>
 *  <li>{@link PlainTime#ISO_HOUR}</li>
 *  <li>{@link PlainTime#MINUTE_OF_HOUR}</li>
 *  <li>{@link PlainTime#MINUTE_OF_DAY}</li>
 *  <li>{@link PlainTime#SECOND_OF_MINUTE}</li>
 *  <li>{@link PlainTime#SECOND_OF_DAY}</li>
 *  <li>{@link PlainTime#MILLI_OF_SECOND}</li>
 *  <li>{@link PlainTime#MICRO_OF_SECOND}</li>
 *  <li>{@link PlainTime#NANO_OF_SECOND}</li>
 *  <li>{@link PlainTime#MILLI_OF_DAY}</li>
 *  <li>{@link PlainTime#MICRO_OF_DAY}</li>
 *  <li>{@link PlainTime#NANO_OF_DAY}</li>
 * </ul>
 *
 * <p>Furthermore, all elements of class {@link Weekmodel} are supported.
 * The elements allow the access to values relative to the timezone UTC.
 * If a {@code Moment} is queried for its hour then it will yield the hour
 * in the timezone UTC. This is different from the JDK-classes
 * {@code java.util.Date} (accessing the default timezone) and
 * {@code java.time.Instant} (no element access possible). A {@code Moment}
 * offers a dual view both on a machine counter as well as on a tuple of
 * date- and time-values, always in the timezone UTC. </p>
 *
 * <p>A {@code Moment} is also capable of delivering the date- and time-values
 * in a different timezone if the method {@link #inZonalView(TZID)} is called.
 * If zonal operators are defined by any elements then manipulations of related
 * data are possible in any timezone. </p>
 *
 * <h3>Time arithmetic</h3>
 *
 * <p>The main time units are defined by {@link SI} (counting possible
 * UTC-leapseconds) and {@link TimeUnit}. Latter unit type can be used
 * if a better interoperability is needed for external APIs which ignore
 * leapseconds. Both kinds of time units can be used in the methods
 * {@code plus(long, unit)}, {@code minus(long, unit)} and
 * {@code until(Moment, unit)}. </p>
 *
 * <p>Furthermore, there is the option to use local time units like
 * {@code ClockUnit} which count the virtual ticks on an analogue clock
 * and are ignorant of daylight-saving switches. As entry point users can
 * use the {@code Duration}-methods {@link Duration#earlier(Timezone)} and
 * {@link Duration#later(Timezone)}. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
/*[deutsch]
 * <p>Repr&auml;sentiert einen Zeitpunkt auf der Weltzeitlinie mit Bezug
 * auf die UTC-Zeitzone (UTC+00:00 / Greenwich-Meridian). </p>
 *
 * <p>Im JDK hei&szlig;t das &Auml;quivalent {@code java.util.Date}. Diese
 * Klasse speichert im Gegensatz zum JDK die Epochenzeit nicht in Milli-,
 * sondern in Nanosekunden auf 96-Bit-Basis. </p>
 *
 * <p>Registriert sind folgende als Konstanten deklarierte Elemente mit
 * Zugriff in der UTC-Zeitzone (immer ohne Basiseinheit): </p>
 *
 * <ul>
 *  <li>{@link PlainDate#YEAR}</li>
 *  <li>{@link PlainDate#YEAR_OF_WEEKDATE}</li>
 *  <li>{@link PlainDate#QUARTER_OF_YEAR}</li>
 *  <li>{@link PlainDate#MONTH_OF_YEAR}</li>
 *  <li>{@link PlainDate#MONTH_AS_NUMBER}</li>
 *  <li>{@link PlainDate#DAY_OF_MONTH}</li>
 *  <li>{@link PlainDate#DAY_OF_QUARTER}</li>
 *  <li>{@link PlainDate#DAY_OF_WEEK}</li>
 *  <li>{@link PlainDate#DAY_OF_YEAR}</li>
 *  <li>{@link PlainDate#WEEKDAY_IN_MONTH}</li>
 *  <li>{@link PlainTime#AM_PM_OF_DAY}</li>
 *  <li>{@link PlainTime#CLOCK_HOUR_OF_AMPM}</li>
 *  <li>{@link PlainTime#CLOCK_HOUR_OF_DAY}</li>
 *  <li>{@link PlainTime#DIGITAL_HOUR_OF_AMPM}</li>
 *  <li>{@link PlainTime#DIGITAL_HOUR_OF_DAY}</li>
 *  <li>{@link PlainTime#ISO_HOUR}</li>
 *  <li>{@link PlainTime#MINUTE_OF_HOUR}</li>
 *  <li>{@link PlainTime#MINUTE_OF_DAY}</li>
 *  <li>{@link PlainTime#SECOND_OF_MINUTE}</li>
 *  <li>{@link PlainTime#SECOND_OF_DAY}</li>
 *  <li>{@link PlainTime#MILLI_OF_SECOND}</li>
 *  <li>{@link PlainTime#MICRO_OF_SECOND}</li>
 *  <li>{@link PlainTime#NANO_OF_SECOND}</li>
 *  <li>{@link PlainTime#MILLI_OF_DAY}</li>
 *  <li>{@link PlainTime#MICRO_OF_DAY}</li>
 *  <li>{@link PlainTime#NANO_OF_DAY}</li>
 * </ul>
 *
 * <p>Dar&uuml;berhinaus sind alle Elemente der Klasse {@link Weekmodel}
 * nutzbar. Die Elemente erlauben den Zugriff bezogen auf die Zeitzone UTC.
 * Wird zum Beispiel ein {@code Moment} nach seiner aktuellen Stunde gefragt,
 * dann wird die Stunde in der UTC-Zeitzone zur&uuml;ckgegeben. Das steht im
 * Kontrast zu den JDK-Klassen {@code java.util.Date} (Zugriff auf die lokale
 * Zeitzone) und {@code java.time.Instant} (kein Element- bzw. Feldzugriff
 * m&ouml;glich). Ein {@code Moment} bietet also eine duale Sicht sowohl auf
 * einen maschinellen Z&auml;hler als auch auf ein Tupel von Datums- und
 * Zeitwerten, immer in der Zeitzone UTC. </p>
 *
 * <p>Ein {@code Moment} kann die Datums- und Zeitwerte auch in einer anderen
 * Zeitzone liefern, wenn die Methode {@link #inZonalView(TZID)} aufgerufen
 * wird. Falls &uuml;ber die Elemente zonale Operatoren zur Verf&uuml;gung
 * stehen, sind auch Manipulationen in beliebigen Zeitzonen m&ouml;glich. </p>
 *
 * <h3>Zeitarithmetik</h3>
 *
 * <p>Als Zeiteinheiten kommen vor allem {@link SI} (mit Z&auml;hlung
 * von Schaltsekunden) und {@link TimeUnit} in Betracht. Letztere Einheit
 * kann verwendet werden, wenn eine bessere Interoperabilit&auml;t mit
 * externen APIs notwendig ist, die UTC-Schaltsekunden ignorieren. Beide
 * Arten von Zeiteinheiten werden in den Methoden {@code plus(long, unit)},
 * {@code minus(long, unit)} und {@code until(Moment, unit)} verwendet. </p>
 *
 * <p>Au&szlig;erdem gibt es die M&ouml;glichkeit, lokale Zeiteinheiten wie
 * in {@code ClockUnit} definiert bezogen auf eine Zeitzone anzuwenden. Ein
 * Einstiegspunkt daf&uuml;r ist mit den {@code Duration}-Methoden
 * {@link Duration#earlier(Timezone)} und {@link Duration#later(Timezone)}
 * vorhanden. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
@CalendarType("iso8601")
public final class Moment
    extends TimePoint<TimeUnit, Moment>
    implements UniversalTime, Temporal<UniversalTime> {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final long UTC_GPS_DELTA =
        ((1980 - 1972) * 365 + 2 + 5) * 86400 + 9;
    private static final long POSIX_UTC_DELTA =
        2 * 365 * 86400;
    private static final long POSIX_GPS_DELTA =
        POSIX_UTC_DELTA + UTC_GPS_DELTA - 9; // -9 => without leap seconds

    private static final int MIO = 1000000;
    private static final int MRD = 1000000000;
    private static final int POSITIVE_LEAP_MASK = 0x40000000;

    private static final long MIN_LIMIT;
    private static final long MAX_LIMIT;

    static {
        long mjdMin = GregorianMath.toMJD(GregorianMath.MIN_YEAR, 1, 1);
        long mjdMax = GregorianMath.toMJD(GregorianMath.MAX_YEAR, 12, 31);

        MIN_LIMIT =
            EpochDays.UNIX.transform(
                mjdMin,
                EpochDays.MODIFIED_JULIAN_DATE)
            * 86400;
        MAX_LIMIT =
            EpochDays.UNIX.transform(
                mjdMax,
                EpochDays.MODIFIED_JULIAN_DATE)
            * 86400 + 86399;
    }

    private static final Moment MIN =
        new Moment(MIN_LIMIT, 0, TimeScale.POSIX);
    private static final Moment MAX =
        new Moment(MAX_LIMIT, MRD - 1, TimeScale.POSIX);

    /**
     * <p>Start of UNIX-era = [1970-01-01T00:00:00,000000000Z]. </p>
     */
    /*[deutsch]
     * <p>Start der UNIX-&Auml;ra = [1970-01-01T00:00:00,000000000Z]. </p>
     */
    public static final Moment UNIX_EPOCH = new Moment(0, 0, TimeScale.POSIX);

    private static final ChronoFormatter<Moment> FORMATTER_RFC_1123;
    private static final Moment START_LS_CHECK =
        new Moment(86400, 0, TimeScale.UTC);
    private static final Set<ChronoElement<?>> HIGH_TIME_ELEMENTS;
    private static final Map<ChronoElement<?>, Integer> LOW_TIME_ELEMENTS;
    private static final Map<TimeUnit, Double> UNIT_LENGTHS;

    static {
        Set<ChronoElement<?>> high = new HashSet<ChronoElement<?>>();
        high.add(ISO_HOUR);
        high.add(DIGITAL_HOUR_OF_DAY);
        high.add(DIGITAL_HOUR_OF_AMPM);
        high.add(CLOCK_HOUR_OF_DAY);
        high.add(CLOCK_HOUR_OF_AMPM);
        high.add(AM_PM_OF_DAY);
        high.add(MINUTE_OF_HOUR);
        high.add(MINUTE_OF_DAY);
        HIGH_TIME_ELEMENTS = Collections.unmodifiableSet(high);

        Map<ChronoElement<?>, Integer> low =
            new HashMap<ChronoElement<?>, Integer>();
        low.put(SECOND_OF_MINUTE, Integer.valueOf(1));
        low.put(SECOND_OF_DAY, Integer.valueOf(1));
        low.put(MILLI_OF_SECOND, Integer.valueOf(1000));
        low.put(MILLI_OF_DAY, Integer.valueOf(1000));
        low.put(MICRO_OF_SECOND, Integer.valueOf(MIO));
        low.put(MICRO_OF_DAY, Integer.valueOf(MIO));
        low.put(NANO_OF_SECOND, Integer.valueOf(MRD));
        low.put(NANO_OF_DAY, Integer.valueOf(MRD));
        LOW_TIME_ELEMENTS = Collections.unmodifiableMap(low);

        Map<TimeUnit, Double> unitLengths =
            new EnumMap<TimeUnit, Double>(TimeUnit.class);
        unitLengths.put(TimeUnit.DAYS, 86400.0);
        unitLengths.put(TimeUnit.HOURS, 3600.0);
        unitLengths.put(TimeUnit.MINUTES, 60.0);
        unitLengths.put(TimeUnit.SECONDS, 1.0);
        unitLengths.put(TimeUnit.MILLISECONDS, 0.001);
        unitLengths.put(TimeUnit.MICROSECONDS, 0.000001);
        unitLengths.put(TimeUnit.NANOSECONDS, 0.000000001);
        UNIT_LENGTHS = Collections.unmodifiableMap(unitLengths);
    }

    private static final TimeAxis<TimeUnit, Moment> ENGINE;

    static {
        TimeAxis.Builder<TimeUnit, Moment> builder =
            TimeAxis.Builder.setUp(
                TimeUnit.class, Moment.class, new Merger(), MIN, MAX);

        for (TimeUnit unit : TimeUnit.values()) {
            builder.appendUnit(
                unit,
                new TimeUnitRule(unit),
                UNIT_LENGTHS.get(unit),
                UNIT_LENGTHS.keySet());
        }

        Set<ChronoElement<?>> dateElements =
            Chronology.lookup(PlainDate.class).getRegisteredElements();

        for (ChronoElement<?> element : dateElements) {
            if (!element.name().equals("CALENDAR_DATE")) {
                doAppend(builder, element);
            }
        }

        Set<ChronoElement<?>> timeElements =
            Chronology.lookup(PlainTime.class).getRegisteredElements();

        for (ChronoElement<?> element : timeElements) {
            if (
                !element.name().startsWith("DECIMAL")
                && !element.name().equals("PRECISION")
                && !element.name().equals("WALL_TIME")
            ) {
                doAppend(builder, element);
            }
        }

        ENGINE = builder.build();

        FORMATTER_RFC_1123 =
            ChronoFormatter.setUp(Moment.class, Locale.ENGLISH)
            .startSection(Attributes.PARSE_CASE_INSENSITIVE, Boolean.TRUE)
            .startOptionalSection()
            .startSection(Attributes.TEXT_WIDTH, TextWidth.ABBREVIATED)
            .addText(PlainDate.DAY_OF_WEEK)
            .endSection()
            .addLiteral(", ")
            .endSection()
            .addInteger(PlainDate.DAY_OF_MONTH, 1, 2)
            .addLiteral(' ')
            .startSection(Attributes.TEXT_WIDTH, TextWidth.ABBREVIATED)
            .addText(PlainDate.MONTH_OF_YEAR)
            .endSection()
            .addLiteral(' ')
            .addFixedInteger(PlainDate.YEAR, 4)
            .addLiteral(' ')
            .addFixedInteger(PlainTime.DIGITAL_HOUR_OF_DAY, 2)
            .addLiteral(':')
            .addFixedInteger(PlainTime.MINUTE_OF_HOUR, 2)
            .startOptionalSection()
            .addLiteral(':')
            .addFixedInteger(PlainTime.SECOND_OF_MINUTE, 2)
            .endSection()
            .addLiteral(' ')
            .addTimezoneOffset(
                DisplayMode.MEDIUM,
                false,
                Arrays.asList("GMT", "UT", "Z"))
            .endSection()
            .build();
    }

    private static final long serialVersionUID = -3192884724477742274L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final long posixTime;
    private transient final int fraction;

    //~ Konstruktoren -----------------------------------------------------

    private Moment(
        long elapsedTime,
        int nanosecond,
        TimeScale scale
    ) {
        super();

        if (scale == POSIX) {
            this.posixTime = elapsedTime;
            this.fraction = nanosecond;
        } else {
            LeapSeconds ls = LeapSeconds.getInstance();

            if (ls.isEnabled()) {
                long utcTime;

                if (scale == UTC) {
                    utcTime = elapsedTime;
                } else if (scale == TAI) {
                    utcTime = MathUtils.safeSubtract(elapsedTime, 10);

                    if (utcTime < 0) {
                        throw new IllegalArgumentException(
                            "TAI not supported before 1972-01-01: " + elapsedTime);
                    }
                } else if (scale == GPS) {
                    utcTime = MathUtils.safeAdd(elapsedTime, UTC_GPS_DELTA);

                    if (utcTime < UTC_GPS_DELTA) {
                        throw new IllegalArgumentException(
                            "GPS not supported before 1980-01-06: " + elapsedTime);
                    }
                } else {
                    throw new UnsupportedOperationException(
                        "Not yet implemented: " + scale.name());
                }

                long unix = ls.strip(utcTime);
                long diff = (utcTime - ls.enhance(unix));
                this.posixTime = unix;

                if (
                    (diff == 0)
                    || (unix == MAX_LIMIT)
                ) {
                    this.fraction = nanosecond;
                } else if (diff == 1) { // positive Schaltsekunde
                    this.fraction = (nanosecond | POSITIVE_LEAP_MASK);
                } else {
                    throw new IllegalStateException(
                        "Cannot handle leap shift of " + elapsedTime + ".");
                }
            } else {
                throw new IllegalStateException(
                    "Leap seconds are not supported by configuration.");
            }
        }

        checkUnixTime(this.posixTime);
        checkFraction(nanosecond);

    }

    // Deserialisierung
    private Moment(
        int nano,
        long unixTime
    ) {
        super();

        // keine Prüfung des Nano-Anteils und Schaltsekunden-Bits
        checkUnixTime(unixTime);

        this.posixTime = unixTime;
        this.fraction = nano;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Equivalent to {@code Moment.of(elapsedTime, 0, scale)}. </p>
     *
     * @param   elapsedTime     elapsed seconds on given time scale
     * @param   scale           time scale reference
     * @return  new moment instance
     * @throws  IllegalArgumentException if elapsed time is out of range limits
     *          beyond year +/-999,999,999 or out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    /*[deutsch]
     * <p>Entspricht {@code Moment.of(elapsedTime, 0, scale)}. </p>
     *
     * @param   elapsedTime     elapsed seconds on given time scale
     * @param   scale           time scale reference
     * @return  new moment instance
     * @throws  IllegalArgumentException if elapsed time is out of range limits
     *          beyond year +/-999,999,999 or out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    public static Moment of(
        long elapsedTime,
        TimeScale scale
    ) {

        return Moment.of(elapsedTime, 0, scale);

    }

    /**
     * <p>Creates a new UTC-timestamp by given time coordinates on given
     * time scale. </p>
     *
     * <p>The given elapsed time {@code elapsedTime} will be internally
     * transformed into the UTC-epochtime, should another time scale than UTC
     * be given. The time scale TAI will only be supported earliest on UTC
     * start 1972-01-01, the time scale GPS earliest on 1980-01-06. </p>
     *
     * @param   elapsedTime     elapsed seconds on given time scale
     * @param   nanosecond      nanosecond fraction of last second
     * @param   scale           time scale reference
     * @return  new moment instance
     * @throws  IllegalArgumentException if the nanosecond is not in the range
     *          {@code 0 <= nanosecond <= 999,999,999} or if elapsed time is
     *          out of supported range limits beyond year +/-999,999,999 or
     *          out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    /*[deutsch]
     * <p>Konstruiert einen neuen UTC-Zeitstempel mit Hilfe von
     * Zeitkoordinaten auf der angegebenen Zeitskala. </p>
     *
     * <p>Die angegebene verstrichene Zeit {@code elapsedTime} wird intern
     * in die UTC-Epochenzeit umgerechnet, sollte eine andere Zeitskala als
     * UTC angegeben sein. Die Zeitskala TAI wird erst ab der UTC-Epoche
     * 1972-01-01 unterst&uuml;tzt, die Zeitskala GPS erst ab 1980-01-06. </p>
     *
     * @param   elapsedTime     elapsed seconds on given time scale
     * @param   nanosecond      nanosecond fraction of last second
     * @param   scale           time scale reference
     * @return  new moment instance
     * @throws  IllegalArgumentException if the nanosecond is not in the range
     *          {@code 0 <= nanosecond <= 999,999,999} or if elapsed time is
     *          out of supported range limits beyond year +/-999,999,999 or
     *          out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    public static Moment of(
        long elapsedTime,
        int nanosecond,
        TimeScale scale
    ) {

        if (
            (elapsedTime == 0)
            && (nanosecond == 0)
            && (scale == TimeScale.POSIX)
        ) {
            return Moment.UNIX_EPOCH;
        }

        return new Moment(elapsedTime, nanosecond, scale);

    }

    /**
     * <p>Common conversion method. </p>
     *
     * @param   ut      UNIX-timestamp
     * @return  corresponding {@code Moment}
     */
    /*[deutsch]
     * <p>Allgemeine Konversionsmethode. </p>
     *
     * @param   ut      UNIX-timestamp
     * @return  corresponding {@code Moment}
     */
    public static Moment from(UnixTime ut) {

        if (ut instanceof Moment) {
            return Moment.class.cast(ut);
        } else if (ut instanceof UniversalTime) {
            UniversalTime utc = UniversalTime.class.cast(ut);
            return Moment.of(
                utc.getElapsedTime(UTC),
                utc.getNanosecond(UTC),
                UTC);
        } else {
            return Moment.of(
                ut.getPosixTime(),
                ut.getNanosecond(),
                TimeScale.POSIX);
        }

    }

    @Override
    public long getPosixTime() {

        return this.posixTime;

    }

    @Override
    public long getElapsedTime(TimeScale scale) {

        if (scale == POSIX) {
            return this.posixTime;
        }

        long utc = this.getEpochTime();

        switch (scale) {
            case UTC:
                return utc;
            case TAI:
                if (utc < 0) {
                    throw new IllegalArgumentException(
                        "TAI not supported before 1972-01-01: " + this);
                } else {
                    return utc + 10;
                }
            case GPS:
                if (LeapSeconds.getInstance().strip(utc) < POSIX_GPS_DELTA) {
                    throw new IllegalArgumentException(
                        "GPS not supported before 1980-01-06: " + this);
                } else {
                    long gps =
                        LeapSeconds.getInstance().isEnabled()
                        ? utc
                        : (utc + 9);
                    return gps - UTC_GPS_DELTA;
                }
            default:
                throw new UnsupportedOperationException(
                    "Not yet implemented: " + scale);
        }

    }

    @Override
    public int getNanosecond() {

        return (this.fraction & (~POSITIVE_LEAP_MASK));

    }

    @Override
    public int getNanosecond(TimeScale scale) {

        switch (scale) {
            case POSIX:
            case UTC:
                return this.getNanosecond();
            case TAI:
                if (this.posixTime < POSIX_UTC_DELTA) {
                    throw new IllegalArgumentException(
                        "TAI not supported before 1972-01-01: " + this);
                } else {
                    return this.getNanosecond();
                }
            case GPS:
                long utc = this.getEpochTime();
                if (LeapSeconds.getInstance().strip(utc) < POSIX_GPS_DELTA) {
                    throw new IllegalArgumentException(
                        "GPS not supported before 1980-01-06: " + this);
                } else {
                    return this.getNanosecond();
                }
            default:
                throw new UnsupportedOperationException(
                    "Not yet implemented: " + scale);
        }

    }

    @Override
    public boolean isLeapSecond() {

        return (this.isPositiveLS() && LeapSeconds.getInstance().isEnabled());

    }

    /**
     * <p>Represents this timestamp as decimal value in given time scale. </p>
     *
     * @param   scale       time scale reference
     * @return  decimal value in given time scale as seconds inclusive fraction
     * @throws  IllegalArgumentException if this instance is out of range
     *          for given time scale
     */
    /*[deutsch]
     * <p>Stellt diese Zeit als Dezimalwert in der angegebenen Zeitskala
     * dar. </p>
     *
     * @param   scale       time scale reference
     * @return  decimal value in given time scale as seconds inclusive fraction
     * @throws  IllegalArgumentException if this instance is out of range
     *          for given time scale
     */
    public BigDecimal transform(TimeScale scale) {

        BigDecimal elapsedTime =
            new BigDecimal(this.getElapsedTime(scale)).setScale(9);
        BigDecimal nanosecond = new BigDecimal(this.getNanosecond(scale));
        return elapsedTime.add(nanosecond.movePointLeft(9));

    }

    @Override
    public boolean isAfter(UniversalTime temporal) {

        Moment other = Moment.from(temporal);
        return (this.compareTo(other) > 0);

    }

    @Override
    public boolean isBefore(UniversalTime temporal) {

        Moment other = Moment.from(temporal);
        return (this.compareTo(other) < 0);

    }

    @Override
    public boolean isSimultaneous(UniversalTime temporal) {

        Moment other = Moment.from(temporal);
        return (this.compareTo(other) == 0);

    }

    /**
     * <p>Converts this instance to a local timestamp in the system
     * timezone. </p>
     *
     * @return  local timestamp in system timezone (leap seconds will
     *          always be lost)
     * @see     Timezone#ofSystem()
     * @see     #inZonalView(TZID)
     * @see     #inZonalView(String)
     */
    /*[deutsch]
     * <p>Wandelt diese Instanz in einen lokalen Zeitstempel um. </p>
     *
     * @return  local timestamp in system timezone (leap seconds will
     *          always be lost)
     * @see     Timezone#ofSystem()
     * @see     #inZonalView(TZID)
     * @see     #inZonalView(String)
     */
    public PlainTimestamp inLocalView() {

        return this.in(Timezone.ofSystem());

    }

    /**
     * <p>Converts this instance to a local timestamp in given timezone. </p>
     *
     * @param   tzid    timezone id
     * @return  local timestamp in given timezone (leap seconds will
     *          always be lost)
     * @see     #inLocalView()
     */
    /*[deutsch]
     * <p>Wandelt diese Instanz in einen lokalen Zeitstempel um. </p>
     *
     * @param   tzid    timezone id
     * @return  local timestamp in given timezone (leap seconds will
     *          always be lost)
     * @see     #inLocalView()
     */
    public PlainTimestamp inZonalView(TZID tzid) {

        return this.in(Timezone.of(tzid));

    }

    /**
     * <p>Converts this instance to a local timestamp in given timezone. </p>
     *
     * @param   tzid    timezone id
     * @return  local timestamp in given timezone (leap seconds will
     *          always be lost)
     * @see     #inZonalView(TZID)
     * @see     #inLocalView()
     */
    /*[deutsch]
     * <p>Wandelt diese Instanz in einen lokalen Zeitstempel um. </p>
     *
     * @param   tzid    timezone id
     * @return  local timestamp in given timezone (leap seconds will
     *          always be lost)
     * @see     #inZonalView(TZID)
     * @see     #inLocalView()
     */
    public PlainTimestamp inZonalView(String tzid) {

        return this.in(Timezone.of(tzid));

    }

    /**
     * <p>Adds an amount of given SI-unit to this timestamp
     * on the UTC time scale. </p>
     *
     * @param   amount  amount in units to be added
     * @param   unit    time unit defined in UTC time space
     * @return  changed copy of this instance
     * @throws  UnsupportedOperationException if this moment is before 1972
     */
    /*[deutsch]
     * <p>Addiert einen Betrag in der angegegebenen SI-Zeiteinheit auf die
     * UTC-Zeit dieses Zeitstempels. </p>
     *
     * @param   amount  amount in units to be added
     * @param   unit    time unit defined in UTC time space
     * @return  changed copy of this instance
     * @throws  UnsupportedOperationException wenn der Zeitpunkt vor 1972 ist
     */
    public Moment plus(
        long amount,
        SI unit
    ) {

        Moment.check1972(this);

        switch (unit) {
            case SECONDS:
                if (LeapSeconds.getInstance().isEnabled()) {
                    return new Moment(
                        MathUtils.safeAdd(this.getEpochTime(), amount),
                        this.getNanosecond(),
                        TimeScale.UTC);
                } else {
                    return Moment.of(
                        MathUtils.safeAdd(this.posixTime, amount),
                        this.getNanosecond(),
                        TimeScale.POSIX
                    );
                }
            case NANOSECONDS:
                long sum =
                    MathUtils.safeAdd(this.getNanosecond(), amount);
                int nano = MathUtils.floorModulo(sum, MRD);
                long second = MathUtils.floorDivide(sum, MRD);

                if (LeapSeconds.getInstance().isEnabled()) {
                    return new Moment(
                        MathUtils.safeAdd(this.getEpochTime(), second),
                        nano,
                        TimeScale.UTC
                    );
                } else {
                    return Moment.of(
                        MathUtils.safeAdd(this.posixTime, second),
                        nano,
                        TimeScale.POSIX
                    );
                }
            default:
                throw new UnsupportedOperationException();
        }

    }

    /**
     * <p>Subtracts an amount of given SI-unit from this timestamp
     * on the UTC time scale. </p>
     *
     * @param   amount  amount in SI-units to be subtracted
     * @param   unit    time unit defined in UTC time space
     * @return  changed copy of this instance
     * @throws  UnsupportedOperationException if this moment is before 1972
     */
    /*[deutsch]
     * <p>Subtrahiert einen Betrag in der angegegebenen Zeiteinheit von der
     * UTC-Zeit dieses Zeitstempels. </p>
     *
     * @param   amount  amount in SI-units to be subtracted
     * @param   unit    time unit defined in UTC time space
     * @return  changed copy of this instance
     * @throws  UnsupportedOperationException wenn der Zeitpunkt vor 1972 ist
     */
    public Moment minus(
        long amount,
        SI unit
    ) {

        return this.plus(MathUtils.safeNegate(amount), unit);

    }

    /**
     * <p>Calculates the time distance between this timestamp and given
     * end timestamp in given SI-unit on the UTC time scale. </p>
     *
     * @param   end     end time point
     * @param   unit    time unit defined in UTC time space
     * @return  count of SI-units between this instance and end time point
     * @throws  UnsupportedOperationException if any moment is before 1972
     */
    /*[deutsch]
     * <p>Bestimmt den zeitlichen Abstand zu einem Endzeitpunkt in der
     * angegebenen Zeiteinheit auf der UTC-Zeitskala. </p>
     *
     * @param   end     end time point
     * @param   unit    time unit defined in UTC time space
     * @return  count of SI-units between this instance and end time point
     * @throws  UnsupportedOperationException wenn ein Zeitpunkt vor 1972 ist
     */
    public long until(
        Moment end,
        SI unit
    ) {

        return unit.between(this, end);

    }

    /**
     * <p>Creates a new formatter which uses the given pattern in the
     * default locale for formatting and parsing UTC-timestamps. </p>
     *
     * <p>Note: The formatter can be adjusted to other locales and timezones
     * however. </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @return  format object for formatting {@code Moment}-objects
     *          using system locale and system timezone
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     ChronoFormatter#with(Locale)
     * @see     ChronoFormatter#withTimezone(net.time4j.tz.TZID)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Musters
     * in der Standard-Sprach- und L&auml;ndereinstellung und in der
     * System-Zeitzone. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen oder Zeitzonen
     * angepasst werden. </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @return  format object for formatting {@code Moment}-objects
     *          using system locale and system timezone
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     ChronoFormatter#with(Locale)
     * @see     ChronoFormatter#withTimezone(net.time4j.tz.TZID)
     */
    public static ChronoFormatter<Moment> localFormatter(
        String formatPattern,
        ChronoPattern patternType
    ) {

        return ChronoFormatter
            .setUp(Moment.class, Locale.getDefault())
            .addPattern(formatPattern, patternType)
            .build()
            .withStdTimezone();

    }

    /**
     * <p>Creates a new formatter which uses the given display mode in the
     * default locale for formatting and parsing UTC-timestamps. </p>
     *
     * <p>Note: The formatter can be adjusted to other locales and timezones
     * however. </p>
     *
     * @param   mode        formatting style
     * @return  format object for formatting {@code Moment}-objects
     *          using system locale and system timezone
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     ChronoFormatter#with(Locale)
     * @see     ChronoFormatter#withTimezone(net.time4j.tz.TZID)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Stils
     * in der Standard-Sprach- und L&auml;ndereinstellung und in der
     * System-Zeitzone. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen oder Zeitzonen
     * angepasst werden. </p>
     *
     * @param   mode        formatting style
     * @return  format object for formatting {@code Moment}-objects
     *          using system locale and system timezone
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     ChronoFormatter#with(Locale)
     * @see     ChronoFormatter#withTimezone(net.time4j.tz.TZID)
     */
    public static ChronoFormatter<Moment> localFormatter(DisplayMode mode) {

        int style = PatternType.getFormatStyle(mode);
        DateFormat df = DateFormat.getDateTimeInstance(style, style);
        String pattern = PatternType.getFormatPattern(df);

        return ChronoFormatter
            .setUp(Moment.class, Locale.getDefault())
            .addPattern(pattern, PatternType.SIMPLE_DATE_FORMAT)
            .build()
            .withStdTimezone();

    }

    /**
     * <p>Creates a new formatter which uses the given pattern and locale
     * for formatting and parsing UTC-timestamps. </p>
     *
     * <p>Note: The formatter can be adjusted to other locales and timezones
     * however. Without any explicit timezone the timezone UTC will be used
     * in printing and parsing requires a timezone information in the text
     * to be parsed. However, if a timezone is explicitly used, this zone
     * will be used in printing while it serves as replacement value during
     * parsing (if the text is missing a timezone information). </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @param   locale          locale setting
     * @return  format object for formatting {@code Moment}-objects
     *          using given locale
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     #localFormatter(String,ChronoPattern)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Musters
     * in der angegebenen Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. Eine
     * Zeitzone ist nicht angegeben, was bedeutet, da&szlig; beim Formatieren
     * die UTC-Zeitzone verwendet wird und beim Parsen eine Zeitzoneninformation
     * im zu interpretierenden Text vorhanden sein mu&szlig;. Wird eine
     * Zeitzone angegeben, dann wird diese beim Formatieren benutzt und
     * dient beim Parsen als Ersatzwert. </p>
     *
     * @param   formatPattern   format definition as pattern
     * @param   patternType     pattern dialect
     * @param   locale          locale setting
     * @return  format object for formatting {@code Moment}-objects
     *          using given locale
     * @throws  IllegalArgumentException if resolving of pattern fails
     * @see     PatternType
     * @see     #localFormatter(String,ChronoPattern)
     */
    public static ChronoFormatter<Moment> formatter(
        String formatPattern,
        ChronoPattern patternType,
        Locale locale
    ) {

        return ChronoFormatter
            .setUp(Moment.class, locale)
            .addPattern(formatPattern, patternType)
            .build();

    }

    /**
     * <p>Creates a new formatter which uses the given display mode and locale
     * for formatting and parsing UTC-timestamps. </p>
     *
     * <p>Note: The formatter can be adjusted to other locales and timezones
     * however. Without any explicit timezone the timezone UTC will be used
     * in printing and parsing requires a timezone information in the text
     * to be parsed. However, if a timezone is explicitly used, this zone
     * will be used in printing while it serves as replacement value during
     * parsing (if the text is missing a timezone information). </p>
     *
     * @param   mode        formatting style
     * @param   locale      locale setting
     * @return  format object for formatting {@code Moment}-objects
     *          using given locale
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     #localFormatter(DisplayMode)
     */
    /*[deutsch]
     * <p>Erzeugt ein neues Format-Objekt mit Hilfe des angegebenen Stils
     * und in der angegebenen Sprach- und L&auml;ndereinstellung. </p>
     *
     * <p>Das Format-Objekt kann an andere Sprachen angepasst werden. Eine
     * Zeitzone ist nicht angegeben, was bedeutet, da&szlig; beim Formatieren
     * die UTC-Zeitzone verwendet wird und beim Parsen eine Zeitzoneninformation
     * im zu interpretierenden Text vorhanden sein mu&szlig;. </p>
     *
     * @param   mode        formatting style
     * @param   locale      locale setting
     * @return  format object for formatting {@code Moment}-objects
     *          using given locale
     * @throws  IllegalStateException if format pattern cannot be retrieved
     * @see     #localFormatter(DisplayMode)
     */
    public static ChronoFormatter<Moment> formatter(
        DisplayMode mode,
        Locale locale
    ) {

        int style = PatternType.getFormatStyle(mode);
        DateFormat df = DateFormat.getDateTimeInstance(style, style, locale);
        String pattern = PatternType.getFormatPattern(df);

        return ChronoFormatter
            .setUp(Moment.class, locale)
            .addPattern(pattern, PatternType.SIMPLE_DATE_FORMAT)
            .build();

    }

    /**
     * <p>Defines the RFC-1123-format which is for example used in mail
     * headers. </p>
     *
     * <p>Equivalent to the pattern &quot;[EEE, ]d MMM yyyy HH:mm[:ss] XX&quot;
     * where the timezone offset XX is modified such that in case of zero
     * offset the expression &quot;GMT&quot; is preferred. As zero offset
     * will also be accepted &quot;UT&quot; or &quot;Z&quot;. The text elements
     * will always be interpreted in English and are case-insensitive. </p>
     *
     * <p>Note: In contrast to the RFC-1123-standard this method does not
     * support military timezone abbreviations (A-Y) or north-american
     * timezone names (EST, EDT, CST, CDT, MST, MDT, PST, PDT). </p>
     *
     * @return  formatter object for RFC-1123 (technical internet-timestamp)
     */
    /*[deutsch]
     * <p>Definiert das RFC-1123-Format, das zum Beispiel in Mail-Headers
     * verwendet wird. </p>
     *
     * <p>Entspricht &quot;[EEE, ]d MMM yyyy HH:mm[:ss] XX&quot;, wobei
     * der Zeitzonen-Offset XX so modifiziert ist, da&szlig; im Fall eines
     * Null-Offsets bevorzugt der Ausdruck &quot;GMT&quot; benutzt wird. Als
     * Null-Offset werden auch &quot;UT&quot; oder &quot;Z&quot; akzeptiert.
     * Die Textelemente werden ohne Beachtung der Gro&szlig;- oder
     * Kleinschreibung in Englisch interpretiert. </p>
     *
     * <p>Zu beachten: Im Gegensatz zum RFC-1123-Standard unterst&uuml;tzt die
     * Methode keine milit&auml;rischen Zeitzonen (A-Y) oder nordamerikanischen
     * Zeitzonennamen (EST, EDT, CST, CDT, MST, MDT, PST, PDT). </p>
     *
     * @return  formatter object for RFC-1123 (technical internet-timestamp)
     */
    public static ChronoFormatter<Moment> formatterRFC1123() {

        return FORMATTER_RFC_1123;

    }

    @Override
    public int compareTo(Moment moment) {

        long u1 = this.getEpochTime();
        long u2 = moment.getEpochTime();

        if (u1 < u2) {
            return -1;
        } else if (u1 > u2) {
            return 1;
        } else {
            int result = this.getNanosecond() - moment.getNanosecond();
            return ((result > 0) ? 1 : ((result < 0) ? -1 : 0));
        }

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof Moment) {
            Moment that = (Moment) obj;

            if (this.posixTime != that.posixTime) {
                return false;
            }

            if (LeapSeconds.getInstance().isEnabled()) {
                return (this.fraction == that.fraction);
            } else {
                return (this.getNanosecond() == that.getNanosecond());
            }
        } else {
            return false;
        }

    }

    @Override
    public int hashCode() {

        long value = (this.posixTime ^ (this.posixTime >>> 32));
        return (19 * ((int) value) + 37 * this.getNanosecond());

    }

    /**
     * <p>Provides a canonical representation in the ISO-format
     * [yyyy-MM-ddTHH:mm:ss,fffffffffZ]. </p>
     *
     * <p>Example:
     * The expression {@code Moment.of(1341100824, 210, TimeScale.UTC)}
     * has the representation &quot;2012-06-30T23:59:60,000000210Z&quot;. </p>
     *
     * @return  ISO-8601-formatted string
     */
    /*[deutsch]
     * <p>Erzeugt eine kanonische Darstellung im ISO-Format
     * [yyyy-MM-ddTHH:mm:ss,fffffffffZ]. </p>
     *
     * <p>Beispiel:
     * Der Ausdruck {@code Moment.of(1341100824, 210, TimeScale.UTC)}
     * hat die Darstellung &quot;2012-06-30T23:59:60,000000210Z&quot;. </p>
     *
     * @return  ISO-8601-formatted string
     */
    @Override
    public String toString() {

        // Datum berechnen
        PlainDate date = this.getDateUTC();

        // Uhrzeit berechnen
        int timeOfDay = getTimeOfDay(this);
        int minutes = timeOfDay / 60;
        int hour = minutes / 60;
        int minute = minutes % 60;
        int second = timeOfDay % 60;

        // LS-Korrektur (negative LS => 59!!!, positive LS => 60)
        second += LeapSeconds.getInstance().getShift(this.getEpochTime());

        StringBuilder sb = new StringBuilder(50);

        // Datum formatieren
        sb.append(date);

        // Separator
        sb.append('T');

        // Uhrzeit formatieren
        format(hour, 2, sb);
        sb.append(':');
        format(minute, 2, sb);
        sb.append(':');
        format(second, 2, sb);
        sb.append(',');
        format(this.getNanosecond(), 9, sb);

        // UTC-Symbol anhängen
        sb.append('Z');

        return sb.toString();

    }

    /**
     * <p>Creates a formatted view of this instance taking in account
     * given time scale. </p>
     *
     * <pre>
     *  Moment moment =
     *      PlainDate.of(2012, Month.JUNE, 30)
     *      .at(PlainTime.of(23, 59, 59, 999999999))
     *      .atUTC()
     *      .plus(1, SI.SECONDS); // move to leap second
     *
     *  System.out.println(moment.toString(TimeScale.POSIX));
     *  // Output: POSIX-2012-06-30T23:59:59,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.UTC));
     *  // Output: UTC-2012-06-30T23:59:60,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.TAI));
     *  // Output: TAI-2012-07-01T00:00:34,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.GPS));
     *  // Output: GPS-2012-07-01T00:00:15,999999999Z
     * </pre>
     *
     * @param   scale   time scale to be used for formatting
     * @return  formatted string with date-time fields in timezone UTC
     * @throws  IllegalArgumentException if this instance is out of range
     *          for given time scale
     * @see     #getElapsedTime(TimeScale)
     */
    /*[deutsch]
     * <p>Erzeugt eine formatierte Sicht dieser Instanz unter
     * Ber&uuml;cksichtigung der angegebenen Zeitskala. </p>
     *
     * <pre>
     *  Moment moment =
     *      PlainDate.of(2012, Month.JUNE, 30)
     *      .at(PlainTime.of(23, 59, 59, 999999999))
     *      .atUTC()
     *      .plus(1, SI.SECONDS); // move to leap second
     *
     *  System.out.println(moment.toString(TimeScale.POSIX));
     *  // Ausgabe: POSIX-2012-06-30T23:59:59,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.UTC));
     *  // Ausgabe: UTC-2012-06-30T23:59:60,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.TAI));
     *  // Ausgabe: TAI-2012-07-01T00:00:34,999999999Z
     *
     *  System.out.println(moment.toString(TimeScale.GPS));
     *  // Ausgabe: GPS-2012-07-01T00:00:15,999999999Z
     * </pre>
     *
     * @param   scale   time scale to be used for formatting
     * @return  formatted string with date-time fields in timezone UTC
     * @throws  IllegalArgumentException if this instance is out of range
     *          for given time scale
     * @see     #getElapsedTime(TimeScale)
     */
    public String toString(TimeScale scale) {

        StringBuilder sb = new StringBuilder(50);
        sb.append(scale.name());
        sb.append('-');

        switch (scale) {
            case POSIX:
                sb.append(PlainTimestamp.from(this, ZonalOffset.UTC));
                sb.append('Z');
                break;
            case UTC:
                sb.append(this.toString());
                break;
            case TAI:
                Moment tai =
                    new Moment(
                        this.getNanosecond(),
                        MathUtils.safeAdd(
                            this.getElapsedTime(TimeScale.TAI),
                            POSIX_UTC_DELTA)
                        );
                sb.append(PlainTimestamp.from(tai, ZonalOffset.UTC));
                sb.append('Z');
                break;
            case GPS:
                Moment gps =
                    new Moment(
                        this.getNanosecond(),
                        MathUtils.safeAdd(
                            this.getElapsedTime(TimeScale.GPS),
                            POSIX_GPS_DELTA)
                        );
                sb.append(PlainTimestamp.from(gps, ZonalOffset.UTC));
                sb.append('Z');
                break;
            default:
                throw new UnsupportedOperationException(scale.name());
        }

        return sb.toString();

    }

    /**
     * <p>Provides a static access to the associated time axis respective
     * chronology which contains the chronological rules. </p>
     *
     * @return  chronological system as time axis (never {@code null})
     */
    /*[deutsch]
     * <p>Liefert die zugeh&ouml;rige Zeitachse, die alle notwendigen
     * chronologischen Regeln enth&auml;lt. </p>
     *
     * @return  chronological system as time axis (never {@code null})
     */
    public static TimeAxis<TimeUnit, Moment> axis() {

        return ENGINE;

    }

    /**
     * <p>A {@code Moment} ist always in timezone UTC. </p>
     *
     * @return  {@code true}
     */
    /*[deutsch]
     * <p>Ein {@code Moment} ist fest an die Zeitzone UTC gekoppelt. </p>
     *
     * @return  {@code true}
     */
    @Override
    public boolean hasTimezone() {

        return true;

    }

    /**
     * <p>A {@code Moment} ist always in timezone UTC. </p>
     *
     * @return  {@link ZonalOffset#UTC}
     */
    /*[deutsch]
     * <p>Ein {@code Moment} ist fest an die Zeitzone UTC gekoppelt. </p>
     *
     * @return  {@link ZonalOffset#UTC}
     */
    @Override
    public TZID getTimezone() {

        return ZonalOffset.UTC;

    }

    @Override
    protected TimeAxis<TimeUnit, Moment> getChronology() {

        return ENGINE;

    }

    @Override
    protected Moment getContext() {

        return this;

    }

    /**
     * <p>Pr&uuml;ft, ob eine negative Schaltsekunde vorliegt. </p>
     *
     * @param   posixTime   UNIX-time in seconds
     * @param   ts          local timestamp
     * @throws  ChronoException if a negative leap second is touched
     */
    static void checkNegativeLS(
        long posixTime,
        PlainTimestamp ts
    ) {

        LeapSeconds ls = LeapSeconds.getInstance();

        if (
            ls.supportsNegativeLS()
            && (ls.strip(ls.enhance(posixTime)) > posixTime)
        ) {
            throw new ChronoException(
                "Illegal local timestamp due to "
                + "negative leap second: " + ts);
        }

    }

    /**
     * <p>Pr&uuml;ft, ob der Zeitpunkt vor 1972 liegt. </p>
     *
     * @param   context     Pr&uuml;fzeitpunkt
     * @throws  UnsupportedOperationException wenn der Zeitpunkt vor 1972 ist
     */
    static void check1972(Moment context) {

        if (context.posixTime < POSIX_UTC_DELTA) {
            throw new UnsupportedOperationException(
                "Cannot calculate SI-duration before 1972-01-01.");
        }

    }

    private long getEpochTime() {

        if (LeapSeconds.getInstance().isEnabled()) {
            long time = LeapSeconds.getInstance().enhance(this.posixTime);
            return (this.isPositiveLS() ? time + 1 : time);
        } else {
            return this.posixTime - POSIX_UTC_DELTA;
        }

    }

    // Datum in der UTC-Zeitzone
    private PlainDate getDateUTC() {

        return PlainDate.of(
            MathUtils.floorDivide(this.posixTime, 86400),
            EpochDays.UNIX);

    }

    // Uhrzeit in der UTC-Zeitzone (ohne Schaltsekunde)
    private PlainTime getTimeUTC() {

        int timeOfDay = getTimeOfDay(this);
        int minutes = timeOfDay / 60;
        int hour = minutes / 60;
        int minute = minutes % 60;
        int second = timeOfDay % 60;
        int nano = this.getNanosecond();

        return PlainTime.of(hour, minute, second, nano);

    }

    private boolean isPositiveLS() {

        return ((this.fraction >>> 30) != 0);

    }

    private boolean isNegativeLS() {

        LeapSeconds ls = LeapSeconds.getInstance();

        if (ls.supportsNegativeLS()) {
            long ut = this.posixTime;
            return (ls.strip(ls.enhance(ut)) > ut);
        } else {
            return false;
        }

    }

    private static void checkUnixTime(long unixTime) {

        if (
            (unixTime > MAX_LIMIT)
            || (unixTime < MIN_LIMIT)
        ) {
            throw new IllegalArgumentException(
                "UNIX time (UT1) out of supported range: " + unixTime);
        }

    }

    private static void checkFraction(int nanoFraction) {

        if ((nanoFraction >= MRD) || (nanoFraction < 0)) {
            throw new IllegalArgumentException(
                "Nanosecond out of range: " + nanoFraction);
        }

    }

    private static void format(
        int value,
        int max,
        StringBuilder sb
    )  {

        int n = 1;

        for (int i = 0; i < max - 1; i++) {
            n *= 10;
        }

        while ((value < n) && (n >= 10)) {
            sb.append('0');
            n = n / 10;
        }

        sb.append(String.valueOf(value));

    }

    // Anzahl der POSIX-Sekunden des Tages
    private static int getTimeOfDay(Moment context) {

        return MathUtils.floorModulo(context.posixTime, 86400);

    }

    // Schaltsekundenkorrektur
    private static Moment moveEventuallyToLS(Moment adjusted) {

        PlainDate date = adjusted.getDateUTC();
        PlainTime time = adjusted.getTimeUTC();

        if (
            (LeapSeconds.getInstance().getShift(date) == 1)
            && (time.getHour() == 23)
            && (time.getMinute() == 59)
            && (time.getSecond() == 59)
        ) {
            return adjusted.plus(1, SI.SECONDS);
        } else {
            return adjusted;
        }

    }

    private PlainTimestamp in(Timezone tz) {

        return PlainTimestamp.from(this, tz.getOffset(this));

    }

    // wildcard capture
    private static <V> void doAppend(
        TimeAxis.Builder<TimeUnit, Moment> builder,
        ChronoElement<V> element
    ) {

        builder.appendElement(element, FieldRule.of(element));

    }

    /**
     * @serialData  Uses <a href="../../serialized-form.html#net.time4j.SPX">
     *              a dedicated serialization form</a> as proxy. The format
     *              is bit-compressed. Overall until 13 data bytes are used.
     *              The first byte contains in the four most significant bits
     *              the type-ID {@code 4}. The lowest bit is {@code 1} if this
     *              instance is a positive leap second. The bit (2) will be
     *              set if there is a non-zero nanosecond part. After this
     *              header byte eight bytes follow containing the unix time
     *              (as long) and optional four bytes with the fraction part.
     *
     * Schematic algorithm:
     *
     * <pre>
     *  int header = 4;
     *  header <<= 4;
     *
     *  if (isLeapSecond()) {
     *      header |= 1;
     *  }
     *
     *  int fraction = getNanosecond();
     *
     *  if (fraction > 0) {
     *      header |= 2;
     *  }
     *
     *  out.writeByte(header);
     *  out.writeLong(getPosixTime());
     *
     *  if (fraction > 0) {
     *      out.writeInt(fraction);
     *  }
     * </pre>
     */
    private Object writeReplace() throws ObjectStreamException {

        return new SPX(this, SPX.MOMENT_TYPE);

    }

    /**
     * @serialData  Blocks because a serialization proxy is required.
     * @throws      InvalidObjectException (always)
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        throw new InvalidObjectException("Serialization proxy required.");

    }

    /**
     * Serialisierungsmethode.
     *
     * @param   out         output stream
     * @throws  IOException
     */
    void writeTimestamp(ObjectOutput out)
        throws IOException {

        int header = SPX.MOMENT_TYPE;
        header <<= 4;

        if (this.isPositiveLS()) {
            header |= 1;
        }

        int fp = this.getNanosecond();

        if (fp > 0) {
            header |= 2;
        }

        out.writeByte(header);
        out.writeLong(this.posixTime);

        if (fp > 0) {
            out.writeInt(fp);
        }

    }

    /**
     * Deserialisierungsmethode.
     *
     * @param   in          input stream
     * @param   positiveLS  positive leap second indicated?
     * @return  deserialized instance
     * @throws  IOException
     */
    static Moment readTimestamp(
        ObjectInput in,
        boolean positiveLS,
        boolean hasNanos
    ) throws IOException {

        long unixTime = in.readLong();
        int nano = (hasNanos ? in.readInt() : 0);

        if (unixTime == 0) {
            if (positiveLS) {
                throw new InvalidObjectException(
                    "UTC epoch is no leap second.");
            } else if (nano == 0) {
                return UNIX_EPOCH;
            }
        }

        if (
            (unixTime == MIN_LIMIT)
            && (nano == 0)
        ) {
            if (positiveLS) {
                throw new InvalidObjectException("Minimum is no leap second.");
            }
            return MIN;
        } else if (
            (unixTime == MAX_LIMIT)
            && (nano == MRD - 1)
        ) {
            if (positiveLS) {
                throw new InvalidObjectException("Maximum is no leap second.");
            }
            return MAX;
        } else {
            checkFraction(nano);
        }

        if (positiveLS) {
            LeapSeconds ls = LeapSeconds.getInstance();
            if (
                !ls.isEnabled() // keep LS-state when propagating to next vm
                || ls.isPositiveLS(ls.enhance(unixTime) + 1)
            ) {
                nano |= POSITIVE_LEAP_MASK;
            } else {
                long packed = GregorianMath.toPackedDate(unixTime);
                int month = GregorianMath.readMonth(packed);
                int day = GregorianMath.readDayOfMonth(packed);
                throw new InvalidObjectException(
                    "Not registered as leap second event: "
                    + GregorianMath.readYear(packed)
                    + "-"
                    + ((month < 10) ? "0" : "")
                    + month
                    + ((day < 10) ? "0" : "")
                    + day
                    + " [Please check leap second configurations "
                    + "either of emitter vm or this target vm]"
                );
            }
        }

        return new Moment(nano, unixTime);

    }

    //~ Innere Klassen ----------------------------------------------------

    /**
     * <p>Delegiert Anpassungen von {@code Moment}-Instanzen an einen
     * {@code ChronoOperator<PlainTimestamp>} mit Hilfe einer Zeitzone. </p>
     *
     * @concurrency <immutable>
     */
    static final class Operator
        implements ChronoOperator<Moment> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoOperator<PlainTimestamp> delegate;
        private final ChronoElement<?> element;
        private final int type;
        private final Timezone tz;

        //~ Konstruktoren -------------------------------------------------

        /**
         * <p>Erzeugt einen Operator, der einen {@link Moment} mit
         * Hilfe der Systemzeitzone anpassen kann. </p>
         *
         * @param   delegate    delegating operator
         * @param   element     element reference
         * @param   type        operator type
         */
        Operator(
            ChronoOperator<PlainTimestamp> delegate,
            ChronoElement<?> element,
            int type
        ) {
            super();

            this.delegate = delegate;
            this.element = element;
            this.type = type;
            this.tz = null;

        }

        /**
         * <p>Erzeugt einen Operator, der einen {@link Moment} mit
         * Hilfe einer Zeitzonenreferenz anpassen kann. </p>
         *
         * @param   delegate    delegating operator
         * @param   element     element reference
         * @param   type        operator type
         * @param   tz          timezone
         */
        Operator(
            ChronoOperator<PlainTimestamp> delegate,
            ChronoElement<?> element,
            int type,
            Timezone tz
        ) {
            super();

            this.delegate = delegate;
            this.element = element;
            this.type = type;
            this.tz = tz;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Moment apply(Moment moment) {

            // Spezialfall feingranulare Zeitarithmetik in der UTC-Ära
            if (
                LOW_TIME_ELEMENTS.containsKey(this.element)
                && moment.isAfter(START_LS_CHECK)
                && ((this.type == ElementOperator.OP_DECREMENT)
                    || (this.type == ElementOperator.OP_INCREMENT)
                    || (this.type == ElementOperator.OP_LENIENT))
            ) {
                int step = LOW_TIME_ELEMENTS.get(this.element).intValue();
                long amount = 1;

                if (this.type == ElementOperator.OP_DECREMENT) {
                    amount = -1;
                } else if (this.type == ElementOperator.OP_LENIENT) {
                    long oldValue =
                        Number.class.cast(moment.get(this.element)).longValue();
                    long newValue =
                        LenientOperator.class.cast(this.delegate).getValue();
                    amount = MathUtils.safeSubtract(newValue, oldValue);
                }

                switch (step) {
                    case 1:
                        return moment.plus(amount, SECONDS);
                    case 1000:
                        return moment.plus(
                            MathUtils.safeMultiply(MIO, amount), NANOSECONDS);
                    case MIO:
                        return moment.plus(
                            MathUtils.safeMultiply(1000, amount), NANOSECONDS);
                    case MRD:
                        return moment.plus(amount, NANOSECONDS);
                    default:
                        throw new AssertionError();
                }
            }

            // lokale Transformation
            Timezone timezone = (
                (this.tz == null)
                ? Timezone.ofSystem()
                : this.tz);

            PlainTimestamp ts = moment.in(timezone).with(this.delegate);
            Moment result = ts.at(timezone);

            // hier kann niemals die Schaltsekunde erreicht werden
            if (this.type == ElementOperator.OP_FLOOR) {
                return result;
            }

            // Schaltsekundenprüfung, weil lokale Transformation keine LS kennt
            if (result.isNegativeLS()) {
                if (this.tz.getStrategy() == Timezone.STRICT_MODE) {
                    throw new ChronoException(
                        "Illegal local timestamp due to "
                        + "negative leap second: " + ts);
                } else {
                    return result;
                }
            }

            if (
                this.element.isDateElement()
                || HIGH_TIME_ELEMENTS.contains(this.element)
            ) {
                if (
                    moment.isLeapSecond()
                    || (this.type == ElementOperator.OP_CEILING)
                ) {
                    return moveEventuallyToLS(result);
                }
            } else if (this.element == SECOND_OF_MINUTE) {
                if (
                    (this.type == ElementOperator.OP_MAXIMIZE)
                    || (this.type == ElementOperator.OP_CEILING)
                ) {
                    return moveEventuallyToLS(result);
                }
            } else if (
                (this.element == MILLI_OF_SECOND)
                || (this.element == MICRO_OF_SECOND)
                || (this.element == NANO_OF_SECOND)
            ) {
                switch (this.type) {
                    case ElementOperator.OP_MINIMIZE:
                    case ElementOperator.OP_MAXIMIZE:
                    case ElementOperator.OP_CEILING:
                        if (moment.isLeapSecond()) {
                            result = result.plus(1, SI.SECONDS);
                        }
                        break;
                    default:
                        // no-op
                }
            }

            return result;

        }

    }

    private static class TimeUnitRule
        implements UnitRule<Moment> {

        //~ Instanzvariablen ----------------------------------------------

        private final TimeUnit unit;

        //~ Konstruktoren -------------------------------------------------

        TimeUnitRule(TimeUnit unit) {
            super();

            this.unit = unit;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Moment addTo(
            Moment context,
            long amount
        ) {

            if (this.unit.compareTo(TimeUnit.SECONDS) >= 0) {
                long secs =
                    MathUtils.safeMultiply(amount, this.unit.toSeconds(1));
                return Moment.of(
                    MathUtils.safeAdd(context.getPosixTime(), secs),
                    context.getNanosecond(),
                    TimeScale.POSIX
                );
            } else { // MILLIS, MICROS, NANOS
                long nanos =
                    MathUtils.safeMultiply(amount, this.unit.toNanos(1));
                long sum = MathUtils.safeAdd(context.getNanosecond(), nanos);
                int nano = MathUtils.floorModulo(sum, MRD);
                long second = MathUtils.floorDivide(sum, MRD);

                return Moment.of(
                    MathUtils.safeAdd(context.getPosixTime(), second),
                    nano,
                    TimeScale.POSIX
                );
            }

        }

        @Override
        public long between(
            Moment start,
            Moment end
        ) {

            long delta = 0;

            if (this.unit.compareTo(TimeUnit.SECONDS) >= 0) {
                delta = (end.getPosixTime() - start.getPosixTime());
                if (delta < 0) {
                    if (end.getNanosecond() > start.getNanosecond()) {
                        delta++;
                    }
                } else if (delta > 0) {
                    if (end.getNanosecond() < start.getNanosecond()) {
                        delta--;
                    }
                }
            } else { // MILLIS, MICROS, NANOS
                delta =
                    MathUtils.safeAdd(
                        MathUtils.safeMultiply(
                            MathUtils.safeSubtract(
                                end.getPosixTime(),
                                start.getPosixTime()
                            ),
                            MRD
                        ),
                        end.getNanosecond() - start.getNanosecond()
                     );
            }

            switch (this.unit) {
                case DAYS:
                    delta = delta / 86400;
                    break;
                case HOURS:
                    delta = delta / 3600;
                    break;
                case MINUTES:
                    delta = delta / 60;
                    break;
                case SECONDS:
                    break;
                case MILLISECONDS:
                    delta = delta / MIO;
                    break;
                case MICROSECONDS:
                    delta = delta / 1000;
                    break;
                case NANOSECONDS:
                    break;
                default:
                    throw new UnsupportedOperationException(this.unit.name());
            }

            return delta;

        }

    }

    private static class FieldRule<V>
        implements ElementRule<Moment, V> {

        //~ Instanzvariablen ----------------------------------------------

        private final ChronoElement<V> element;

        //~ Konstruktoren -------------------------------------------------

        private FieldRule(ChronoElement<V> element) {
            super();

            this.element = element;

        }

        //~ Methoden ------------------------------------------------------

        static <V> FieldRule<V> of(ChronoElement<V> element) {

            return new FieldRule<V>(element);

        }

        @Override
        public V getValue(Moment context) {

            Object ret = null;

            if (this.element.isDateElement()) {
                return context.getDateUTC().get(this.element);
            } else if (this.element == SECOND_OF_MINUTE) {
                ret =
                    Integer.valueOf(
                        context.isLeapSecond()
                        ? 60
                        : (getTimeOfDay(context) % 60));
            } else if (this.element.isTimeElement()) {
                return context.getTimeUTC().get(this.element);
            }

            assert (ret != null);
            return this.element.getType().cast(ret);

        }

        @Override
        public V getMinimum(Moment context) {

            if (this.element.isDateElement()) {
                return context.getDateUTC().getMinimum(this.element);
            } else if (this.element.isTimeElement()) {
                return this.element.getDefaultMinimum();
            }

            throw new ChronoException(
                "Missing rule for: " + this.element.name());

        }

        @Override
        public V getMaximum(Moment context) {

            if (this.element.isDateElement()) {
                return context.getDateUTC().getMaximum(this.element);
            }

            Object ret = null;

            if (this.element == SECOND_OF_MINUTE) {
                ret = Integer.valueOf(getMaxSecondOfMinute(context));
            } else if (this.element.isTimeElement()) {
                return this.element.getDefaultMaximum();
            }

            assert (ret != null);
            return this.element.getType().cast(ret);

        }

        @Override
        public boolean isValid(
            Moment context,
            V value
        ) {

            if (this.element.isDateElement()) {
                return context.getDateUTC().isValid(this.element, value);
            } else if (this.element.isTimeElement()) {
                if (Number.class.isAssignableFrom(this.element.getType())) {
                    if (value == null) {
                        return false;
                    }
                    long min =
                        Number.class.cast(this.getMinimum(context)).longValue();
                    long max =
                        Number.class.cast(this.getMaximum(context)).longValue();
                    long val = Number.class.cast(value).longValue();
                    return ((min <= val) && (max >= val));
                } else {
                    return context.getTimeUTC().isValid(this.element, value);
                }
            }

            throw new ChronoException(
                "Missing rule for: " + this.element.name());

        }

        @Override
        public Moment withValue(
            Moment context,
            V value,
            boolean lenient
        ) {

            assert (lenient == false);

            if (value == null) {
                throw new NullPointerException("Missing value.");
            } else if (!this.isValid(context, value)) {
                throw new IllegalArgumentException(
                    value + " invalid on [" + this.element.name()
                    + "] in context: " + context);
            }

            if (
                LOW_TIME_ELEMENTS.containsKey(this.element)
                && context.isAfter(START_LS_CHECK)
            ) {
                long delta =
                    MathUtils.safeSubtract(
                        Number.class.cast(value).longValue(),
                        Number.class.cast(this.getValue(context)).longValue());
                int step = LOW_TIME_ELEMENTS.get(this.element).intValue();

                switch (step) {
                    case 1:
                        return context.plus(delta, SECONDS);
                    case 1000:
                        return context.plus(delta * MIO, NANOSECONDS);
                    case MIO:
                        return context.plus(delta * 1000, NANOSECONDS);
                    case MRD:
                        return context.plus(delta, NANOSECONDS);
                    default:
                        throw new AssertionError();
                }
            }

            PlainTimestamp ts = context.inZonalView(ZonalOffset.UTC);
            ts = ts.with(this.element, value);
            Moment result = ts.atTimezone(ZonalOffset.UTC);

            if (
                this.element.isDateElement()
                || HIGH_TIME_ELEMENTS.contains(this.element)
            ) {
                if (context.isLeapSecond()) {
                    return moveEventuallyToLS(result);
                }
            }

            return result;

        }

        @Override
        public ChronoElement<?> getChildAtFloor(Moment context) {

            // Operatoren nur für PlainXYZ definiert!
            throw new AssertionError("Should never be called.");

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(Moment context) {

            // Operatoren nur für PlainXYZ definiert!
            throw new AssertionError("Should never be called.");

        }

        private static int getMaxSecondOfMinute(Moment context) {

            int minutes = getTimeOfDay(context) / 60;
            int second = 59;

            if (((minutes / 60) == 23) && ((minutes % 60) == 59)) {
                PlainDate date = context.getDateUTC();
                second += LeapSeconds.getInstance().getShift(date);
            }

            return second;

        }

    }

    private static class Merger
        implements ChronoMerger<Moment> {

        //~ Methoden ------------------------------------------------------

        @Override
        public Moment createFrom(
            TimeSource<?> clock,
            AttributeQuery attributes
        ) {

            return Moment.from(clock.currentTime());

        }

        @Override
        public Moment createFrom(
            ChronoEntity<?> entity,
            AttributeQuery attrs,
            boolean pp
        ) {

            if (entity instanceof UnixTime) {
                return Moment.from(UnixTime.class.cast(entity));
            }

            Moment result = null;
            PlainTimestamp ts = null;
            boolean leapsecond = false;

            if (entity.contains(LeapsecondElement.INSTANCE)) {
                leapsecond = true;
                entity.with(SECOND_OF_MINUTE, Integer.valueOf(60));
            }

            ChronoElement<PlainTimestamp> self =
                PlainTimestamp.axis().element();

            if (entity.contains(self)) {
                ts = entity.get(self);
            } else {
                ts = PlainTimestamp.axis().createFrom(entity, attrs, pp);
            }

            if (ts == null) {
                return null;
            }

            TZID tzid = null;

            if (entity.hasTimezone()) {
                tzid = entity.getTimezone();
            }

            if (
                (tzid == null)
                && attrs.contains(Attributes.TIMEZONE_ID)
            ) {
                tzid = attrs.get(Attributes.TIMEZONE_ID); // Ersatzwert
            }

            if (tzid != null) {
                if (attrs.contains(Attributes.TRANSITION_STRATEGY)) {
                    TransitionStrategy strategy =
                        attrs.get(Attributes.TRANSITION_STRATEGY);
                    result = ts.at(Timezone.of(tzid).with(strategy));
                } else {
                    result = ts.atTimezone(tzid);
                }
            } else {
                Leniency leniency =
                    attrs.get(Attributes.LENIENCY, Leniency.SMART);
                if (leniency.isLax()) {
                    result = ts.atStdTimezone();
                    tzid = Timezone.ofSystem().getID();
                }
            }

            if (leapsecond && (result != null)) {
                ZonalOffset offset;

                if (tzid instanceof ZonalOffset) {
                    offset = (ZonalOffset) tzid;
                } else {
                    offset = Timezone.of(tzid).getOffset(result);
                }

                if (
                    (offset.getFractionalAmount() != 0)
                    || ((offset.getAbsoluteSeconds() % 60) != 0)
                ) {
                    throw new IllegalArgumentException(
                        "Leap second is only allowed "
                        + " with timezone-offset in full minutes: "
                        + offset);
                }

                Moment test;

                if (result.getDateUTC().getYear() >= 1972) {
                    test = result.plus(1, SECONDS);
                } else {
                    test =
                        new Moment(
                            result.getNanosecond(),
                            result.getPosixTime() + 1);
                }

                Leniency leniency =
                    attrs.get(Attributes.LENIENCY, Leniency.SMART);

                if (leniency.isLax()) {
                    result = test;
                } else if (LeapSeconds.getInstance().isEnabled()) {
                    if (test.isPositiveLS()) {
                        result = test;
                    } else {
                        throw new IllegalArgumentException(
                            "SECOND_OF_MINUTE parsed as invalid leapsecond: "
                            + test);
                    }
                }
            }

            return result;

        }

        @Override
        public ChronoEntity<?> preformat(
            Moment context,
            AttributeQuery attributes
        ) {

            if (attributes.contains(Attributes.TIMEZONE_ID)) {
                TZID tzid = attributes.get(Attributes.TIMEZONE_ID);

                if (tzid != ZonalOffset.UTC) {
                    return new ZonalTimestamp(context, tzid);
                }
            }

            return context;

        }

        @Override
        public Chronology<?> preparser() {

            return PlainTimestamp.axis();

        }

    }

}