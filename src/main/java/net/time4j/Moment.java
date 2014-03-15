/*
 * -----------------------------------------------------------------------
 * Copyright © 2013 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (Moment.java) is part of project Time4J.
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
import net.time4j.format.Leniency;
import net.time4j.scale.LeapSeconds;
import net.time4j.scale.TimeScale;
import net.time4j.scale.UniversalTime;
import net.time4j.tz.TZID;
import net.time4j.tz.TimeZone;
import net.time4j.tz.TransitionStrategy;
import net.time4j.tz.ZonalOffset;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.time4j.PlainDate.CALENDAR_DATE;
import static net.time4j.PlainTime.*;
import static net.time4j.SI.NANOSECONDS;
import static net.time4j.SI.SECONDS;
import static net.time4j.scale.TimeScale.GPS;
import static net.time4j.scale.TimeScale.POSIX;
import static net.time4j.scale.TimeScale.TAI;
import static net.time4j.scale.TimeScale.UTC;


/**
 * <p>Repr&auml;sentiert einen Zeitpunkt auf der Weltzeitlinie mit Bezug
 * auf die UTC-Zeitzone (UTC+00:00 / Greenwich-Meridian). </p>
 *
 * <p>Im JDK hei&szlig;t das &Auml;quivalent {@code java.util.Date}. Diese
 * Klasse speichert im Gegensatz zum JDK die Epochenzeit nicht in Milli-,
 * sondern in Nanosekunden auf 96-Bit-Basis. </p>
 *
 * <p>Registriert sind alle Elemente, die in den lokalen Klassen
 * {@code PlainDate} und {@code PlainTime} registriert sind (mit Ausnahme
 * der Dezimalelemente), au&szlig;erdem werden alle Elemente von
 * {@link Weekmodel} mit unterst&uuml;tzt. Die Elemente erlauben den
 * Zugriff bezogen auf die Zeitzone UTC. Falls &uuml;ber sie zonale
 * Operatoren zur Verf&uuml;gung stehen, sind auch Manipulationen
 * in beliebigen Zeitzonen m&ouml;glich. </p>
 *
 * @author      Meno Hochschild
 * @concurrency <immutable>
 */
@CalendarType("iso8601")
public final class Moment
    extends TimePoint<SI, Moment>
    implements UniversalTime, Temporal<UniversalTime> {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final long UTC_GPS_DELTA =
        ((1980 - 1972) * 365 + 2 + 5) * 86400 + 9;
    private static final long POSIX_UTC_DELTA =
        2 * 365 * 86400;
    private static final long POSIX_GPS_DELTA =
        POSIX_UTC_DELTA + UTC_GPS_DELTA;

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
     * <p>Start der UNIX-&Auml;ra = [1970-01-01T00:00:00,000000000Z]. </p>
     */
    public static final Moment UNIX_EPOCH = new Moment(0, 0, TimeScale.POSIX);

    private static final Moment START_LS_CHECK =
        new Moment(86400, TimeScale.UTC);
    private static final Set<ChronoElement<?>> HIGH_TIME_ELEMENTS;
    private static final Map<ChronoElement<?>, Integer> LOW_TIME_ELEMENTS;

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
    }

    private static final TimeAxis<SI, Moment> ENGINE;

    static {
        TimeAxis.Builder<SI, Moment> builder =
            TimeAxis.Builder.setUp(
                SI.class, Moment.class, new Merger(), MIN, MAX)
            .appendUnit(
                SI.SECONDS,
                new SIRule(SI.SECONDS),
                SI.SECONDS.getLength())
            .appendUnit(
                SI.NANOSECONDS,
                new SIRule(SI.NANOSECONDS),
                SI.NANOSECONDS.getLength());

        Set<ChronoElement<?>> dateElements =
            Chronology.lookup(PlainDate.class).getRegisteredElements();

        for (ChronoElement<?> element : dateElements) {
            doAppend(builder, element);
        }

        Set<ChronoElement<?>> timeElements =
            Chronology.lookup(PlainTime.class).getRegisteredElements();

        for (ChronoElement<?> element : timeElements) {
            if (!element.name().startsWith("DECIMAL")) {
                doAppend(builder, element);
            }
        }

        ENGINE = builder.build();
    }

    private static final long serialVersionUID = 1L;

    //~ Instanzvariablen --------------------------------------------------

    private transient final long posixTime;
    private transient final int fraction;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Entspricht {@code new Moment(elapsedTime, 0, scale)}. </p>
     *
     * @param   elapsedTime     elapsed seconds on given time scale
     * @param   scale           time scale reference
     * @throws  IllegalArgumentException if elapsed time is out of range limits
     *          beyond year +/-999,999,999 or out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    public Moment(
        long elapsedTime,
        TimeScale scale
    ) {
        this(elapsedTime, 0, scale);

    }

    /**
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
     * @throws  IllegalArgumentException if the nanosecond is not in the range
     *          {@code 0 <= nanosecond <= 999,999,999} or if elapsed time is
     *          out of supported range limits beyond year +/-999,999,999 or
     *          out of time scale range
     * @throws  IllegalStateException if time scale is not POSIX but
     *          leap second support is switched off by configuration
     * @see     LeapSeconds#isEnabled()
     */
    public Moment(
        long elapsedTime,
        int nanosecond,
        TimeScale scale
    ) {
        super();

        LeapSeconds ls = LeapSeconds.getInstance();

        if (scale == POSIX) {
            this.posixTime = elapsedTime;
            this.fraction = nanosecond;
        } else if (ls.isEnabled()) {
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
     * <p>Wandelt diese Instanz in einen lokalen Zeitstempel um. </p>
     *
     * @return  local timestamp in system time zone (leap seconds will
     *          always be lost)
     */
    public PlainTimestamp inStdTimezone() {

        return this.inTimezone(TimeZone.ofSystem());

    }

    /**
     * <p>Wandelt diese Instanz in einen lokalen Zeitstempel um. </p>
     *
     * @param   tzid    time zone id
     * @return  local timestamp in given time zone (leap seconds will
     *          always be lost)
     */
    public PlainTimestamp inTimezone(TZID tzid) {

        return this.inTimezone(TimeZone.of(tzid));

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
     * <p>Erzeugt eine kanonische Darstellung im ISO-Format
     * [yyyy-MM-ddTHH:mm:ss,fffffffffZ]. </p>
     *
     * <p>Beispiel:
     * Der Ausdruck {@code new Moment(1341100824, 210, TimeScale.UTC)}
     * hat die Darstellung [2012-06-30T23:59:60,000000210Z]. </p>
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
     * <p>Erzeugt eine formatierte Sicht dieser Instanz unter
     * Ber&uuml;cksichtigung der angegebenen Zeitskala. </p>
     *
     * <pre>
     *  Moment moment =
     *      PlainDate.of(2012, Month.JUNE, 30)
     *      .atTime(PlainTime.of(23).with(PlainTime.ISO_HOUR.atCeiling()))
     *      .inTimezone(ZonalOffset.UTC, TransitionStrategy.STRICT)
     *      .plus(1, SI.SECONDS);
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
     * @return  formatted string with date-time fields in time zone UTC
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
                            POSIX_GPS_DELTA - 9)
                        );
                sb.append(PlainTimestamp.from(gps, ZonalOffset.UTC));
                sb.append('Z');
                break;
            default:
                throw new UnsupportedOperationException(scale.name());
        }

        return sb.toString();

    }

    @Override
    public TimeAxis<SI, Moment> getChronology() {

        return ENGINE;

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

    private PlainTimestamp inTimezone(TimeZone tz) {

        ZonalOffset offset = tz.getOffset(this);
        return PlainTimestamp.from(this, offset);

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
        return new PlainTime(hour, minute, second, nano);

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

    private static Moment from(UniversalTime ut) {

        if (ut instanceof Moment) {
            return Moment.class.cast(ut);
        }

        return new Moment(ut.getElapsedTime(UTC), ut.getNanosecond(UTC), UTC);

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

    // wildcard capture
    private static <V> void doAppend(
        TimeAxis.Builder<SI, Moment> builder,
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
     *  int fraction = getFractionalPart();
     *
     *  if (fraction > 0) {
     *      header |= 2;
     *  }
     *
     *  out.writeByte(header);
     *  out.writeLong(get(IsoElement.UNIX_TIME));
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
        private final TZID tzid;
        private final TransitionStrategy strategy;
        private final ChronoElement<?> element;
        private final OperatorType type;

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
            OperatorType type
        ) {
            super();

            this.delegate = delegate;
            this.tzid = null;
            this.strategy = TransitionStrategy.PUSH_FORWARD;
            this.element = element;
            this.type = type;

        }

        /**
         * <p>Erzeugt einen Operator, der einen {@link Moment} mit
         * Hilfe einer Zeitzonenreferenz anpassen kann. </p>
         *
         * @param   tzid        time zone id
         * @param   strategy    conflict resolving strategy
         * @param   element     element reference
         * @param   type        operator type
         */
        Operator(
            ChronoOperator<PlainTimestamp> delegate,
            TZID tzid,
            TransitionStrategy strategy,
            ChronoElement<?> element,
            OperatorType type
        ) {
            super();

            if (tzid == null) {
                throw new NullPointerException("Missing time zone id.");
            } else if (strategy == null) {
                throw new NullPointerException("Missing transition strategy.");
            }

            this.delegate = delegate;
            this.tzid = tzid;
            this.strategy = strategy;
            this.element = element;
            this.type = type;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Moment apply(Moment moment) {

            // Spezialfall feingranulare Zeitarithmetik in der UTC-Ära
            if (
                LOW_TIME_ELEMENTS.containsKey(this.element)
                && moment.isAfter(START_LS_CHECK)
                && ((this.type == OperatorType.DECREMENT)
                    || (this.type == OperatorType.INCREMENT))
            ) {
                int step = LOW_TIME_ELEMENTS.get(this.element).intValue();
                int sign = 1;

                if (this.type == OperatorType.DECREMENT) {
                    sign = -1;
                }

                switch (step) {
                    case 1:
                        return moment.plus(sign, SECONDS);
                    case 1000:
                        return moment.plus(MIO * sign, NANOSECONDS);
                    case MIO:
                        return moment.plus(1000 * sign, NANOSECONDS);
                    case MRD:
                        return moment.plus(sign, NANOSECONDS);
                    default:
                        throw new AssertionError();
                }
            }

            // lokale Transformation
            TimeZone tz = (
                (this.tzid == null)
                ? TimeZone.ofSystem()
                : TimeZone.of(this.tzid));

            PlainTimestamp ts = moment.inTimezone(tz).with(this.delegate);
            Moment result = ts.inTimezone(tz, this.strategy);

            // hier kann niemals die Schaltsekunde erreicht werden
            if (this.type == OperatorType.FLOOR) {
                return result;
            }

            // Schaltsekundenprüfung, weil lokale Transformation keine LS kennt
            if (result.isNegativeLS()) {
                if (this.strategy == TransitionStrategy.STRICT) {
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
                    || (this.type == OperatorType.CEILING)
                ) {
                    return moveEventuallyToLS(result);
                }
            } else if (this.element == SECOND_OF_MINUTE) {
                if (
                    (this.type == OperatorType.MAXIMIZE)
                    || (this.type == OperatorType.CEILING)
                ) {
                    return moveEventuallyToLS(result);
                }
            } else if (
                (this.element == MILLI_OF_SECOND)
                || (this.element == MICRO_OF_SECOND)
                || (this.element == NANO_OF_SECOND)
            ) {
                switch (this.type) {
                    case MINIMIZE:
                    case MAXIMIZE:
                    case CEILING:
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

    private static class SIRule
        implements UnitRule<Moment> {

        //~ Instanzvariablen ----------------------------------------------

        private final SI unit;

        //~ Konstruktoren -------------------------------------------------

        SIRule(SI unit) {
            super();

            this.unit = unit;

        }

        //~ Methoden ------------------------------------------------------

        @Override
        public Moment addTo(
            Moment context,
            long amount
        ) {

            check(context);

            switch (this.unit) {
                case SECONDS:
                    if (LeapSeconds.getInstance().isEnabled()) {
                        return new Moment(
                            MathUtils.safeAdd(context.getEpochTime(), amount),
                            context.getNanosecond(),
                            TimeScale.UTC);
                    } else {
                        return new Moment(
                            MathUtils.safeAdd(context.posixTime, amount),
                            context.getNanosecond(),
                            TimeScale.POSIX
                        );
                    }
                case NANOSECONDS:
                    long sum =
                        MathUtils.safeAdd(context.getNanosecond(), amount);
                    int nano = MathUtils.floorModulo(sum, MRD);
                    long second = MathUtils.floorDivide(sum, MRD);

                    if (LeapSeconds.getInstance().isEnabled()) {
                        return new Moment(
                            MathUtils.safeAdd(context.getEpochTime(), second),
                            nano,
                            TimeScale.UTC
                        );
                    } else {
                        return new Moment(
                            MathUtils.safeAdd(context.posixTime, second),
                            nano,
                            TimeScale.POSIX
                        );
                    }
                default:
                    throw new UnsupportedOperationException();
            }

        }

        @Override
        public long between(
            Moment start,
            Moment end
        ) {

            check(start);
            check(end);

            switch (this.unit) {
                case SECONDS:
                    long delta = (end.getEpochTime() - start.getEpochTime());
                    if (delta < 0) {
                        if (end.getNanosecond() > start.getNanosecond()) {
                            delta++;
                        }
                    } else if (delta > 0) {
                        if (end.getNanosecond() < start.getNanosecond()) {
                            delta--;
                        }
                    }
                    return delta;
                case NANOSECONDS:
                    return MathUtils.safeAdd(
                        MathUtils.safeMultiply(
                            MathUtils.safeSubtract(
                                end.getEpochTime(),
                                start.getEpochTime()
                            ),
                            MRD
                        ),
                        end.getNanosecond() - start.getNanosecond()
                     );
                default:
                    throw new UnsupportedOperationException();
            }

        }

        private static void check(Moment context) {

            if (context.posixTime < POSIX_UTC_DELTA) {
                throw new UnsupportedOperationException(
                    "Cannot calculate SI-duration before 1972-01-01.");
            }

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

            if (!this.isValid(context, value)) {
                throw new IllegalArgumentException("Out of range: " + value);
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

            PlainTimestamp ts = context.inTimezone(ZonalOffset.UTC);

            if (this.element.isDateElement()) {
                ts =
                    ts.with(
                        CALENDAR_DATE,
                        ts.getCalendarDate().with(this.element, value));
            } else {
                ts =
                    ts.with(
                        WALL_TIME,
                        ts.getWallTime().with(this.element, value));
            }

            Moment result =
                ts.inTimezone(ZonalOffset.UTC, TransitionStrategy.PUSH_FORWARD);

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

            // Operatoren nur für PlainDate oder PlainTime definiert!
            throw new AssertionError("Should never be called.");

        }

        @Override
        public ChronoElement<?> getChildAtCeiling(Moment context) {

            // Operatoren nur für PlainDate oder PlainTime definiert!
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

            final UnixTime ut = clock.currentTime();

            if (ut instanceof UniversalTime) {
                return Moment.from(UniversalTime.class.cast(ut));
            } else{
                return new Moment(
                    ut.getPosixTime(),
                    ut.getNanosecond(),
                    TimeScale.POSIX);
            }

        }

        @Override
        public Moment createFrom(
            ChronoEntity<?> parsedValues,
            AttributeQuery attributes
        ) {

            Moment result = null;

            boolean leapsecond =
                parsedValues.contains(SECOND_OF_MINUTE)
                && (parsedValues.get(SECOND_OF_MINUTE).intValue() == 60);

            if (leapsecond) { // temporär, wird später kompensiert
                parsedValues.with(SECOND_OF_MINUTE, Integer.valueOf(59));
            }

            PlainTimestamp ts = null;

            try {
                ts =
                    PlainTimestamp.ENGINE.createFrom(
                        parsedValues,
                        attributes
                    );
            } finally {
                if (leapsecond) { // Restauration
                    parsedValues.with(SECOND_OF_MINUTE, Integer.valueOf(60));
                }
            }

            if (ts == null) {
                return null;
            }

            TZID tzid = parsedValues.get(TimeZone.identifier());

            if (
                (tzid == null)
                && attributes.contains(Attributes.TIMEZONE_ID)
            ) {
                tzid = attributes.get(Attributes.TIMEZONE_ID); // Ersatzwert
            }

            if (tzid != null) {
                result =
                    ts.inTimezone(
                        tzid,
                        attributes.get(
                            Attributes.TRANSITION_STRATEGY,
                            TransitionStrategy.PUSH_FORWARD
                        )
                    );
            } else {
                Leniency leniency =
                    attributes.get(Attributes.LENIENCY, Leniency.SMART);

                if (leniency.isLax()) {
                    result = ts.inStdTimezone();
                }
            }

            if (leapsecond && (result != null)) {
                ZonalOffset offset;

                if (tzid instanceof ZonalOffset) {
                    offset = (ZonalOffset) tzid;
                } else {
                    offset = TimeZone.of(tzid).getOffset(result);
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
                    attributes.get(Attributes.LENIENCY, Leniency.SMART);

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

            TZID tzid = ZonalOffset.UTC;

            if (attributes.contains(Attributes.TIMEZONE_ID)) {
                tzid = attributes.get(Attributes.TIMEZONE_ID);

                if (tzid != ZonalOffset.UTC) {
                    return new ZonalTimestamp(context, tzid);
                }
            }

            return context;

        }

    }

}