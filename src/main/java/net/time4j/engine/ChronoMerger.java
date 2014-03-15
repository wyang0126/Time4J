/*
 * -----------------------------------------------------------------------
 * Copyright © 2012 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (ChronoMerger.java) is part of project Time4J.
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

package net.time4j.engine;

import net.time4j.base.TimeSource;


/**
 * <p>Erzeugt aus chronologischen Informationen einen neuen Zeitpunkt. </p>
 *
 * <p>Dieses Interface abstrahiert das Wissen der jeweiligen Zeitwertklasse,
 * wie aus beliebigen chronologischen Informationen eine neue Zeitwertinstanz
 * zu konstruieren ist und wird zum Beispiel beim Parsen von textuellen
 * Darstellungen zu Zeitwertobjekten ben&ouml;tigt. Die konkreten Algorithmen
 * sind in den jeweiligen Subklassen von {@code ChronoEntity} dokumentiert. </p>
 *
 * <p>Die Benutzung dieses Low-Level-Interface bleibt in der Regel Time4J
 * vorbehalten und dient vorwiegend der internen Formatunterst&uuml;tzung. </p>
 *
 * <p>Implementierungshinweis: Alle Klassen dieses Typs m&uuml;ssen
 * <i>immutable</i>, also unver&auml;nderlich sein. </p>
 *
 * @param   <T> generic type of time context
 *          (compatible to {@link ChronoEntity})
 * @author  Meno Hochschild
 */
public interface ChronoMerger<T> {

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Konstruiert eine neue Entit&auml;t, die der aktuellen Zeit
     * entspricht. </p>
     *
     * <p>In einer rein datumsbezogenen kalendarischen Chronologie wird hier
     * das aktuelle Tagesdatum erzeugt, indem zus&auml;tzlich &uuml;ber die
     * Attribute die notwendige Zeitzone ermittelt wird. </p>
     *
     * @param   clock           source for current time
     * @param   attributes      configuration attributes which might contain
     *                          the time zone to translate current time to
     *                          local time
     * @return  new time context or {@code null} if given data are insufficient
     */
    T createFrom(
        TimeSource<?> clock,
        AttributeQuery attributes
    );

    /**
     * <p>Konstruiert eine neue Entit&auml;t basierend auf den angegebenen
     * chronologischen Daten. </p>
     *
     * <p>Typischerweise wird mit verschiedenen Priorit&auml;ten das Argument
     * {@code parsedValues} nach Elementen abgefragt, die gruppenweise einen
     * Zeitwert konstruieren. Zum Beispiel kann ein Datum entweder &uuml;ber
     * die Epochentage, die Gruppe Jahr-Monat-Tag oder die Gruppe Jahr und Tag
     * des Jahres konstruiert werden. </p>
     *
     * <p>Gew&ouml;hnlich ruft ein Textinterpretierer diese Methode auf,
     * nachdem ein Text elementweise in chronologische Werte aufgel&ouml;st
     * wurde. </p>
     *
     * @param   parsedValues    interpreted elements with their values
     * @param   attributes      configuration attributes given by parser
     * @return  new time context or {@code null} if given data are insufficient
     * @throws  IllegalArgumentException in any case of inconsistent data
     */
    T createFrom(
        ChronoEntity<?> parsedValues,
        AttributeQuery attributes
    );

    /**
     * <p>Transformiert den aktuellen Kontext unter Beachtung der Attribute
     * bei Bedarf in die tats&auml;chlich zu formatierende Entit&auml;t. </p>
     *
     * @param   attributes      controls attributes during formatting
     * @return  replacement entity which will finally be used for formatting
     */
    ChronoEntity<?> preformat(
        T context,
        AttributeQuery attributes
    );

}