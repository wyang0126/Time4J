/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2014 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (IntervalFactory.java) is part of project Time4J.
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

package net.time4j.range;

import net.time4j.engine.ChronoEntity;
import net.time4j.engine.Temporal;


/**
 * <p>Creates finite temporal intervals. </p>
 *
 * <p>This interface realizes a generic way to create intervals. For the
 * four basic temporal types of Time4J ({@code PlainDate}, {@code PlainTime},
 * {@code PlainTimestamp} and {@code Moment}), there are specialized static
 * factory methods on the appropriate interval types. </p>
 *
 * @param   <T> temporal type of time points within a given interval
 * @author  Meno Hochschild
 * @since   1.3
 */
/*[deutsch]
 * <p>Erzeugt begrenzte Zeitintervalle. </p>
 *
 * <p>Dieses Interface realisiert einen generischen Weg der Intervallerzeugung.
 * Die Intervalltypen passend zu den vier Basiszeittypen von Time4J
 * ({@code PlainDate}, {@code PlainTime}, {@code PlainTimestamp} und
 * {@code Moment}) bieten spezialisierte statische Fabrikmethoden an. </p>
 *
 * @param   <T> temporal type of time points within a given interval
 * @author  Meno Hochschild
 * @since   1.3
 */
public interface IntervalFactory<T extends ChronoEntity<T> & Temporal<? super T>> {

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Creates a finite interval between given time points. </p>
     *
     * <p>If given time points are calendrical then this method will create
     * a closed interval else a half-open interval with an open upper
     * boundary. </p>
     *
     * @param   start   time point of lower boundary
     * @param   end     time point of upper boundary
     * @return  new interval
     * @throws  IllegalArgumentException if start is after end
     * @since   1.3
     * @see     net.time4j.engine.Calendrical
     */
    /*[deutsch]
     * <p>Erzeugt ein begrenztes Intervall zwischen den angegebenen
     * Zeitpunkten. </p>
     *
     * <p>Sind die angegebenen Zeitpunkte kalendarisch, dann wird ein
     * geschlossenes Intervall erzeugt, sonst ein rechts-offenes Intervall. </p>
     *
     * @param   start   time point of lower boundary
     * @param   end     time point of upper boundary
     * @return  new interval
     * @throws  IllegalArgumentException if start is after end
     * @since   1.3
     * @see     net.time4j.engine.Calendrical
     */
    ChronoInterval<T> between(
        T start,
        T end
    );

}