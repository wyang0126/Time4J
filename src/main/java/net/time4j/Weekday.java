/*
 * -----------------------------------------------------------------------
 * Copyright © 2012 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (Weekday.java) is part of project Time4J.
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

import net.time4j.base.GregorianDate;
import net.time4j.base.GregorianMath;
import net.time4j.engine.ChronoCondition;
import net.time4j.format.CalendarText;
import net.time4j.format.OutputContext;
import net.time4j.format.TextWidth;

import java.util.Locale;

import static net.time4j.format.CalendarText.ISO_CALENDAR_TYPE;


/**
 * <p>Wochentagsaufz&auml;hlung im ISO-8601-Format. </p>
 *
 * <p>Verschiedene Methoden mit einem {@code Weekmodel}-Argument
 * unterst&uuml;tzen zus&auml;tzlich andere Wochenmodelle. </p>
 *
 * @author  Meno Hochschild
 */
public enum Weekday
    implements ChronoCondition<GregorianDate> { // TODO: ChronoOperator

    //~ Statische Felder/Initialisierungen --------------------------------

    /** Montag mit dem numerischen ISO-Wert {@code 1}. */
    MONDAY,

    /** Dienstag mit dem numerischen ISO-Wert {@code 2}. */
    TUESDAY,

    /** Mittwoch mit dem numerischen ISO-Wert {@code 3}. */
    WEDNESDAY,

    /** Donnerstag mit dem numerischen ISO-Wert {@code 4}. */
    THURSDAY,

    /** Freitag mit dem numerischen ISO-Wert {@code 5}. */
    FRIDAY,

    /** Samstag mit dem numerischen ISO-Wert {@code 6}. */
    SATURDAY,

    /** Sonntag mit dem numerischen ISO-Wert {@code 7}. */
    SUNDAY;

    private static final Weekday[] ENUMS = Weekday.values(); // Cache

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Liefert den korrespondierenden kalendarischen Integer-Wert
     * entsprechend der ISO-8601-Norm. </p>
     *
     * @return  (monday=1, tuesday=2, wednesday=3, thursday=4, friday=5,
     *          saturday=6, sunday=7)
     * @see     #valueOf(int)
     * @see     Weekmodel#ISO
     */
    public int getValue() {

        return (this.ordinal() + 1);

    }

    /**
     * <p>Liefert eine Wochentagsnummer passend zur im Modell enthaltenen
     * Regel, mit welchem Tag eine Woche beginnt. </p>
     *
     * <p>Wird z.B. die in den USA &uuml;bliche Regel angewandt, da&szlig;
     * der erste Tag einer Woche der Sonntag sein soll, dann hat der Sonntag
     * die Nummer 1 (statt 7 nach ISO-8601). </p>
     *
     * @param   model       localized week model
     * @return  localized weekday number (1 - 7)
     * @see     Weekmodel#getFirstDayOfWeek()
     * @see     #values(Weekmodel)
     * @see     #valueOf(int, Weekmodel)
     */
    public int getValue(Weekmodel model) {

        int shift = model.getFirstDayOfWeek().ordinal();
        return ((7 + this.ordinal() - shift) % 7) + 1;

    }

    /**
     * <p>Liefert ein Array, das passend zur im Model enthaltenen Regel
     * sortiert ist, mit welchem Tag eine Woche beginnt. </p>
     *
     * <p>Die vom Java-Compiler generierte {@code values()}-Methode ohne
     * Argument richtet sich nach dem ISO-8601-Wochenmodell. Diese Methode
     * ist die &uuml;berladene Variante, in der die Sortierung angepasst
     * ist. </p>
     *
     * @param   model       localized week model
     * @return  new weekday array
     * @see     Weekmodel#getFirstDayOfWeek()
     * @see     #getValue(Weekmodel)
     * @see     #valueOf(int, Weekmodel)
     */
    public static Weekday[] values(Weekmodel model) {

        Weekday[] enums = new Weekday[7];
        Weekday wd = model.getFirstDayOfWeek();

        for (int i = 0; i < 7; i++) {
            enums[i] = wd;
            wd = wd.next();
        }

        return enums;

    }

    /**
     * <p>Liefert die zum kalendarischen Integer-Wert passende
     * Enum-Konstante entsprechend der ISO-8601-Norm. </p>
     *
     * @param   dayOfWeek       (monday=1, tuesday=2, wednesday=3, thursday=4,
     *                          friday=5, saturday=6, sunday=7)
     * @return  weekday as enum
     * @throws  IllegalArgumentException if the argument is out of range
     * @see     #getValue()
     * @see     Weekmodel#ISO
     */
    public static Weekday valueOf(int dayOfWeek) {

        if ((dayOfWeek < 1) || (dayOfWeek > 7)) {
            throw new IllegalArgumentException("Out of range: " + dayOfWeek);
        }

        return ENUMS[dayOfWeek - 1];

    }

    /**
     * <p>Liefert die zum kalendarischen Integer-Wert passende
     * Enum-Konstante passend zum angegebenen Wochenmodell. </p>
     *
     * @param   dayOfWeek   localized weekday number (1 - 7)
     * @param   model       localized week model
     * @return  weekday as enum
     * @throws  IllegalArgumentException if the int-argument is out of range
     * @see     Weekmodel#getFirstDayOfWeek()
     * @see     #values(Weekmodel)
     * @see     #getValue(Weekmodel)
     */
    public static Weekday valueOf(
        int dayOfWeek,
        Weekmodel model
    ) {

        if (
            (dayOfWeek < 1)
            || (dayOfWeek > 7)
        ) {
            throw new IllegalArgumentException(
                "Weekday out of range: " + dayOfWeek);
        }

        int shift = model.getFirstDayOfWeek().ordinal();
        return ENUMS[(dayOfWeek - 1 + shift) % 7];

    }

    /**
     * <p>Liefert den Wochentag zum angegebenen Datum. </p>
     *
     * <p>Grundlage ist der gregorianische Kalender proleptisch f&uuml;r
     * alle Zeiten ohne Kalenderwechsel angewandt. Es wird also so getan,
     * als ob der gregorianische Kalender schon vor dem 15. Oktober 1582
     * existiert h&auml;tte, so wie im ISO-8601-Format vorgesehen. </p>
     *
     * @param   year            proleptic iso year
     * @param   monthOfYear     gregorian month
     * @param   dayOfMonth      day of month (1 - 31)
     * @return  weekday
     * @throws  IllegalArgumentException if the day is out of range
     */
    public static Weekday valueOf(
        int year,
        Month monthOfYear,
        int dayOfMonth
    ) {

        return Weekday.valueOf(
            GregorianMath.getDayOfWeek(
                year,
                monthOfYear.getValue(),
                dayOfMonth
            )
        );

    }

    /**
     * <p>Ermittelt den n&auml;chsten Wochentag. </p>
     *
     * <p>Auf den Sonntag angewandt ist das Ergebnis der Montag. </p>
     *
     * @return  next weekday
     */
    public Weekday next() {

        return this.roll(1);

    }

    /**
     * <p>Ermittelt den vorherigen Wochentag. </p>
     *
     * <p>Auf den Montag angewandt ist das Ergebnis der Sonntag. </p>
     *
     * @return  previous weekday
     */
    public Weekday previous() {

        return this.roll(-1);

    }

    /**
     * <p>Rollt um die angegebene Anzahl von Tagen vor oder zur&uuml;ck. </p>
     *
     * @param   days    count of days (maybe negative)
     * @return  result of rolling operation
     */
    public Weekday roll(int days) {

        return Weekday.valueOf((this.ordinal() + (days % 7 + 7)) % 7 + 1);

    }

    /**
     * <p>Liefert eine Beschreibung in der angegebenen Sprache in Langform
     * und entspricht {@code getDisplayName(locale, true)}. </p>
     *
     * @param   locale      language setting
     * @return  descriptive text (long form, never {@code null})
     * @see     #getDisplayName(Locale, boolean)
     */
    public String getDisplayName(Locale locale) {

        return this.getDisplayName(locale, true);

    }

    /**
     * <p>Liefert den sprachabh&auml;ngigen Beschreibungstext. </p>
     *
     * <p>&Uuml;ber das zweite Argument kann gesteuert werden, ob eine kurze
     * oder eine lange Form des Beschreibungstexts ausgegeben werden soll. Das
     * ist besonders sinnvoll in Benutzeroberfl&auml;chen, wo zwischen der
     * Beschriftung und der detaillierten Erl&auml;uterung einer graphischen
     * Komponente unterschieden wird. </p>
     *
     * @param   locale      language setting
     * @param   longText    {@code true} if the long form is required else
     *                      {@code false} for the short form
     * @return  short or long descriptive text (never {@code null})
     */
    public String getDisplayName(
        Locale locale,
        boolean longText
    ) {

        CalendarText names =
            CalendarText.getInstance(ISO_CALENDAR_TYPE, locale);
        TextWidth tw = (longText ? TextWidth.WIDE : TextWidth.ABBREVIATED);
        return names.getWeekdays(tw, OutputContext.FORMAT).print(this);

    }

    @Override
    public boolean test(GregorianDate context) {

        return (GregorianMath.getDayOfWeek(context) == this.getValue());

    }

}