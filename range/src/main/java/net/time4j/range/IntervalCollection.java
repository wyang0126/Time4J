/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2014 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (IntervalCollection.java) is part of project Time4J.
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

import net.time4j.Moment;
import net.time4j.PlainDate;
import net.time4j.PlainTime;
import net.time4j.PlainTimestamp;
import net.time4j.engine.Temporal;
import net.time4j.engine.TimeLine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * <p>Represents a sorted list of intervals. </p>
 *
 * <p>Any instance can first be achieved by calling one of the static
 * {@code onXYZAxis()}-methods and then be filled with any count of
 * typed intervals via {@code plus(...)}-methods. </p>
 *
 * @param   <T> generic type characterizing the associated time axis
 * @author  Meno Hochschild
 * @since   2.0
 * @concurrency <immutable>
 * @see     DateInterval#comparator()
 * @see     ClockInterval#comparator()
 * @see     TimestampInterval#comparator()
 * @see     MomentInterval#comparator()
 */
/*[deutsch]
 * <p>Repr&auml;sentiert eine sortierte Liste von Intervallen. </p>
 *
 * <p>Zuerst kann eine Instanz mit Hilfe von statischen Fabrikmethoden
 * wie {@code onXYZAxis()} erhalten und dann mit einer beliebigen Zahl
 * von typisierten Intervallen gef&uuml;llt werden - via {@code plus(...)}
 * -Methoden. </p>
 *
 * @param   <T> generic type characterizing the associated time axis
 * @author  Meno Hochschild
 * @since   2.0
 * @concurrency <immutable>
 * @see     DateInterval#comparator()
 * @see     ClockInterval#comparator()
 * @see     TimestampInterval#comparator()
 * @see     MomentInterval#comparator()
 */
public abstract class IntervalCollection<T extends Temporal<? super T>>
    implements Serializable {

    //~ Instanzvariablen --------------------------------------------------

    private transient final List<ChronoInterval<T>> intervals;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>For subclasses only. </p>
     */
    IntervalCollection() {
        super();

        this.intervals = Collections.emptyList();

    }

    /**
     * <p>For subclasses only. </p>
     *
     * @param   intervals   sorted list of finite intervals
     */
    IntervalCollection(List<ChronoInterval<T>> intervals) {
        super();

        this.intervals = Collections.unmodifiableList(intervals);

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Yields an empty instance on the date axis. </p>
     *
     * @return  empty {@code IntervalCollection} for date intervals
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine leere Instanz auf der Datumsachse. </p>
     *
     * @return  empty {@code IntervalCollection} for date intervals
     * @since   2.0
     */
    public static IntervalCollection<PlainDate> onDateAxis() {

        return DateWindows.EMPTY;

    }

    /**
     * <p>Yields an empty instance on the walltime axis. </p>
     *
     * @return  empty {@code IntervalCollection} for clock intervals
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine leere Instanz auf der Uhrzeitachse. </p>
     *
     * @return  empty {@code IntervalCollection} for clock intervals
     * @since   2.0
     */
    public static IntervalCollection<PlainTime> onClockAxis() {

        return ClockWindows.EMPTY;

    }

    /**
     * <p>Yields an empty instance on the timestamp axis. </p>
     *
     * @return  empty {@code IntervalCollection} for timestamp intervals
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine leere Instanz auf der Zeitstempelachse. </p>
     *
     * @return  empty {@code IntervalCollection} for timestamp intervals
     * @since   2.0
     */
    public static IntervalCollection<PlainTimestamp> onTimestampAxis() {

        return TimestampWindows.EMPTY;

    }

    /**
     * <p>Yields an empty instance on the UTC-axis. </p>
     *
     * @return  empty {@code IntervalCollection} for moment intervals
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine leere Instanz auf der UTC-Achse. </p>
     *
     * @return  empty {@code IntervalCollection} for moment intervals
     * @since   2.0
     */
    public static IntervalCollection<Moment> onMomentAxis() {

        return MomentWindows.EMPTY;

    }

    /**
     * <p>Returns all appended intervals. </p>
     *
     * @return  unmodifiable sorted list of intervals
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert alle hinzugef&uuml;gten Intervalle. </p>
     *
     * @return  unmodifiable sorted list of intervals
     * @since   2.0
     */
    public List<ChronoInterval<T>> getIntervals() {

        return this.intervals;

    }

    /**
     * <p>Gives an answer if this instance contains no intervals. </p>
     *
     * @return  {@code true} if there are no intervals else {@code false}
     * @since   2.0
     */
    /*[deutsch]
     * <p>Gibt eine Antwort ob diese Instanz keine Intervalle enth&auml;lt. </p>
     *
     * @return  {@code true} if there are no intervals else {@code false}
     * @since   2.0
     */
    public boolean isEmpty() {

        return this.intervals.isEmpty();

    }

    /**
     * <p>Returns the overall minimum of this interval collection. </p>
     *
     * <p>The minimum is always inclusive. </p>
     *
     * @return  lower limit of this instance or {@code null} if infinite
     * @throws  NoSuchElementException if there are no intervals
     * @since   2.0
     * @see     #isEmpty()
     */
    /*[deutsch]
     * <p>Liefert das totale Minimum dieser Intervall-Menge. </p>
     *
     * <p>Das Minimum ist immer inklusive. </p>
     *
     * @return  lower limit of this instance or {@code null} if infinite
     * @throws  NoSuchElementException if there are no intervals
     * @since   2.0
     * @see     #isEmpty()
     */
    public T getMinimum() {

        if (this.isEmpty()) {
            throw new NoSuchElementException(
                "Empty time windows have no minimum.");
        }

        return this.intervals.get(0).getStart().getTemporal();

    }

    /**
     * <p>Returns the overall maximum of this interval collection. </p>
     *
     * <p>The maximum is always inclusive. </p>
     *
     * @return  upper limit of this instance or {@code null} if infinite
     * @throws  NoSuchElementException if there are no intervals
     * @since   2.0
     * @see     #isEmpty()
     */
    /*[deutsch]
     * <p>Liefert das totale Maximum dieser Intervall-Menge. </p>
     *
     * <p>Das Maximum ist immer inklusive. </p>
     *
     * @return  upper limit of this instance or {@code null} if infinite
     * @throws  NoSuchElementException if there are no intervals
     * @since   2.0
     * @see     #isEmpty()
     */
    public T getMaximum() {

        if (this.isEmpty()) {
            throw new NoSuchElementException(
                "Empty time windows have no maximum.");
        }

        int n = this.intervals.size();
        Boundary<T> upper = this.intervals.get(n - 1).getEnd();
        T max = upper.getTemporal();

        if (upper.isInfinite()) {
            return null;
        }

        if (this.isCalendrical()) {
            if (upper.isOpen()) {
                max = this.getTimeLine().stepBackwards(max);
            }

            for (int i = n - 2; i >= 0; i--) {
                Boundary<T> test = this.intervals.get(i).getEnd();
                T candidate = test.getTemporal();

                if (test.isInfinite()) {
                    return null;
                } else if (test.isOpen()) {
                    candidate = this.getTimeLine().stepBackwards(candidate);
                }

                if (candidate.isAfter(max)) {
                    max = candidate;
                }
            }
        } else {
            T last = null;

            if (upper.isClosed()) {
                T next = this.getTimeLine().stepForward(max);
                if (next == null) {
                    last = max;
                } else {
                    max = next;
                }
            }

            for (int i = n - 2; i >= 0; i--) {
                Boundary<T> test = this.intervals.get(i).getEnd();
                T candidate = test.getTemporal();

                if (test.isInfinite()) {
                    return null;
                } else if (last != null) {
                    continue;
                } else if (test.isClosed()) {
                    T next = this.getTimeLine().stepForward(candidate);
                    if (next == null) {
                        last = candidate;
                        continue;
                    } else {
                        candidate = next;
                    }
                }

                if (candidate.isAfter(max)) {
                    max = candidate;
                }
            }

            if (last != null) {
                max = last;
            } else {
                max = this.getTimeLine().stepBackwards(max);
            }
        }

        return max;

    }

    /**
     * <p>Adds the given interval to this interval collection. </p>
     *
     * @param   interval    the new interval to be added
     * @return  new IntervalCollection-instance containing a sum of
     *          the own intervals and the given one while this instance
     *          remains unaffected
     * @since   2.0
     */
    /*[deutsch]
     * <p>F&uuml;gt das angegebene Intervall hinzu. </p>
     *
     * @param   interval    the new interval to be added
     * @return  new IntervalCollection-instance containing a sum of
     *          the own intervals and the given one while this instance
     *          remains unaffected
     * @since   2.0
     */
    public IntervalCollection<T> plus(ChronoInterval<T> interval) {

        if (interval == null) {
            throw new NullPointerException("Missing interval.");
        }

        List<ChronoInterval<T>> windows =
            new ArrayList<ChronoInterval<T>>(this.intervals);
        windows.add(interval);
        Collections.sort(windows, this.getComparator());
        return this.create(windows);

    }

    /**
     * <p>Adds the given intervals to this interval collection. </p>
     *
     * @param   intervals       the new intervals to be added
     * @return  new IntervalCollection-instance containing a sum of
     *          the own intervals and the given one while this instance
     *          remains unaffected
     * @since   2.0
     */
    /*[deutsch]
     * <p>F&uuml;gt die angegebenen Intervalle hinzu. </p>
     *
     * @param   intervals       the new intervals to be added
     * @return  new IntervalCollection-instance containing a sum of
     *          the own intervals and the given one while this instance
     *          remains unaffected
     * @since   2.0
     */
    public IntervalCollection<T> plus(
        List<? extends ChronoInterval<T>> intervals) {

        if (intervals.isEmpty()) {
            return this;
        }

        List<ChronoInterval<T>> windows =
            new ArrayList<ChronoInterval<T>>(this.intervals);
        windows.addAll(intervals);
        Collections.sort(windows, this.getComparator());
        return this.create(windows);

    }

    /**
     * <p>Determines a filtered version of this interval collection within
     * given range. </p>
     *
     * @param   timeWindow  time window filter
     * @return  new interval collection containing only timepoints within
     *          given range
     * @throws  IllegalArgumentException if lower is after upper
     * @since   2.1
     */
    /*[deutsch]
     * <p>Bestimmt eine gefilterte Version dieser Intervallmenge
     * innerhalb der angegebenen Grenzen. </p>
     *
     * @param   timeWindow  time window filter
     * @return  new interval collection containing all timepoints within
     *          given range which do not belong to this instance
     * @throws  IllegalArgumentException if lower is after upper
     * @since   2.1
     */
    public IntervalCollection<T> withTimeWindow(ChronoInterval<T> timeWindow) {

        Boundary<T> lower = timeWindow.getStart();
        Boundary<T> upper = timeWindow.getEnd();

        if (
            this.isEmpty()
            || (lower.isInfinite() && upper.isInfinite())
        ) {
            return this;
        }

        List<ChronoInterval<T>> parts = new ArrayList<ChronoInterval<T>>();

        for (ChronoInterval<T> interval : this.intervals) {
            if (
                interval.isFinite()
                && timeWindow.contains(interval.getStart().getTemporal())
                && timeWindow.contains(interval.getEnd().getTemporal())
            ) {
                parts.add(interval);
                continue;
            }

            List<ChronoInterval<T>> pair = new ArrayList<ChronoInterval<T>>(2);
            pair.add(timeWindow);
            pair.add(interval);
            Collections.sort(pair, this.getComparator());
            IntervalCollection<T> is = this.create(pair).withIntersection();

            if (!is.isEmpty()) {
                parts.add(is.getIntervals().get(0));
            }
        }

        return this.create(parts);

    }

    /**
     * <p>Determines the complement of this interval collection within
     * given range. </p>
     *
     * @param   timeWindow  time window filter
     * @return  new interval collection containing all timepoints within
     *          given range which do not belong to this instance
     * @throws  IllegalArgumentException if lower is after upper
     * @since   2.1
     */
    /*[deutsch]
     * <p>Bestimmt die Komplement&auml;rmenge zu dieser Intervallmenge
     * innerhalb der angegebenen Grenzen. </p>
     *
     * @param   timeWindow  time window filter
     * @return  new interval collection containing all timepoints within
     *          given range which do not belong to this instance
     * @throws  IllegalArgumentException if lower is after upper
     * @since   2.1
     */
    public IntervalCollection<T> withComplement(ChronoInterval<T> timeWindow) {

        IntervalCollection<T> coll = this.withTimeWindow(timeWindow);

        if (coll.isEmpty()) {
            return this.create(Collections.singletonList(timeWindow));
        }

        Boundary<T> lower = timeWindow.getStart();
        Boundary<T> upper = timeWindow.getEnd();
        List<ChronoInterval<T>> gaps = new ArrayList<ChronoInterval<T>>();

        // left edge
        T min = coll.getMinimum();

        if (min != null) {
            if (lower.isInfinite()) {
                this.addLeft(gaps, min);
            } else {
                T s = lower.getTemporal();
                if (lower.isOpen()) {
                    s = this.getTimeLine().stepBackwards(s);
                    if (s == null) {
                        this.addLeft(gaps, min);
                    } else {
                        this.addLeft(gaps, s, min);
                    }
                } else {
                    this.addLeft(gaps, s, min);
                }
            }
        }

        // inner gaps
        gaps.addAll(coll.withGaps().getIntervals());

        // right edge
        T max = coll.getMaximum();

        if (max != null) {
            T s = this.getTimeLine().stepForward(max);
            if (s != null) {
                Boundary<T> bs = Boundary.ofClosed(s);
                Boundary<T> be;
                if (upper.isInfinite()) {
                    be = upper;
                    gaps.add(this.newInterval(bs, be));
                } else if (this.isCalendrical()) {
                    if (upper.isClosed()) {
                        be = upper;
                    } else {
                        T e = upper.getTemporal();
                        e = this.getTimeLine().stepBackwards(e);
                        be = Boundary.ofClosed(e);
                    }
                    if (!s.isAfter(be.getTemporal())) {
                        gaps.add(this.newInterval(bs, be));
                    }
                } else {
                    if (upper.isOpen()) {
                        be = upper;
                    } else {
                        T e = upper.getTemporal();
                        e = this.getTimeLine().stepForward(e);
                        if (e == null) {
                            be = Boundary.infiniteFuture();
                        } else {
                            be = Boundary.ofOpen(e);
                        }
                    }
                    if (s.isBefore(be.getTemporal())) {
                        gaps.add(this.newInterval(bs, be));
                    }
                }
            }
        }

        return this.create(gaps);

    }

    /**
     * <p>Searches for all gaps with time points which are not covered by any
     * interval of this instance. </p>
     *
     * @return  new interval collection containing the inner gaps between
     *          the own intervals while this instance remains unaffected
     * @since   2.0
     */
    /*[deutsch]
     * <p>Sucht die L&uuml;cken mit allen Zeitpunkten, die nicht zu irgendeinem
     * Intervall dieser Instanz geh&ouml;ren. </p>
     *
     * @return  new interval collection containing the inner gaps between
     *          the own intervals while this instance remains unaffected
     * @since   2.0
     */
    public IntervalCollection<T> withGaps() {

        int len = this.intervals.size();

        if (len == 0) {
            return this;
        } else if (len == 1) {
            List<ChronoInterval<T>> zero = Collections.emptyList();
            return this.create(zero);
        }

        List<ChronoInterval<T>> gaps = new ArrayList<ChronoInterval<T>>();
        T previous = null;

        for (int i = 0, n = len - 1; i < n; i++) {
            ChronoInterval<T> current = this.intervals.get(i);

            if (current.getEnd().isInfinite()) {
                break;
            }

            T gapStart = current.getEnd().getTemporal();

            if (current.getEnd().isClosed()) {
                gapStart = this.getTimeLine().stepForward(gapStart);
                if (gapStart == null) {
                    break;
                }
            }

            if (
                (previous == null)
                || gapStart.isAfter(previous)
            ) {
                previous = gapStart;
            } else {
                gapStart = previous;
            }

            T gapEnd = this.intervals.get(i + 1).getStart().getTemporal();

            if (
                (gapEnd == null)
                || !gapEnd.isAfter(gapStart)
            ) {
                continue;
            }

            IntervalEdge edge = IntervalEdge.OPEN;

            if (this.isCalendrical()) {
                edge = IntervalEdge.CLOSED;
                gapEnd = this.getTimeLine().stepBackwards(gapEnd);
                if (gapEnd == null) {
                    continue;
                }
            }

            Boundary<T> s = Boundary.ofClosed(gapStart);
            Boundary<T> e = Boundary.of(edge, gapEnd);
            gaps.add(this.newInterval(s, e));
        }

        return this.create(gaps);

    }

    /**
     * <p>Combines all intervals to disjunct blocks which never overlap. </p>
     *
     * @return  new interval collection containing disjunct blocks
     *          while this instance remains unaffected
     * @since   2.0
     */
    /*[deutsch]
     * <p>Kombiniert alle Intervalle zu disjunkten Bl&ouml;cken, die sich
     * nicht &uuml;berlappen. </p>
     *
     * @return  new interval collection containing disjunct blocks
     *          while this instance remains unaffected
     * @since   2.0
     */
    public IntervalCollection<T> withBlocks() {

        if (this.intervals.size() < 2) {
            return this;
        }

        Boundary<T> s;
        Boundary<T> e;

        boolean calendrical = this.isCalendrical();
        IntervalEdge edge = (
            calendrical
            ? IntervalEdge.CLOSED
            : IntervalEdge.OPEN);

        List<ChronoInterval<T>> gaps = this.withGaps().intervals;
        List<ChronoInterval<T>> blocks = new ArrayList<ChronoInterval<T>>();
        T start = this.getMinimum();

        for (int i = 0, n = gaps.size(); i < n; i++) {
            T end = gaps.get(i).getStart().getTemporal();

            if (calendrical) {
                end = this.getTimeLine().stepBackwards(end);
            }

            s = this.createStartBoundary(start);
            e = Boundary.of(edge, end);
            blocks.add(this.newInterval(s, e));

            Boundary<T> b = gaps.get(i).getEnd();
            start = b.getTemporal();

            if (b.isClosed()) {
                start = this.getTimeLine().stepForward(start);
            }
        }

        T max = this.getMaximum();
        s = this.createStartBoundary(start);

        if ((max != null) && !calendrical) {
            max = this.getTimeLine().stepForward(max);
        }

        if (max == null) {
            e = Boundary.infiniteFuture();
        } else {
            e = Boundary.of(edge, max);
        }

        blocks.add(this.newInterval(s, e));
        return this.create(blocks);

    }

    /**
     * <p>Determines the intersection of all contained intervals. </p>
     *
     * <p>Note: This instance remains unaffected as specified for immutable
     * classes. </p>
     *
     * @return  new interval collection containing the intersection interval,
     *          maybe empty (if there is no intersection)
     * @since   2.0
     */
    /*[deutsch]
     * <p>Ermittelt die Schnittmenge aller enthaltenen Intervalle. </p>
     *
     * <p>Hinweis: Diese Instanz bleibt unver&auml;ndert, weil die Klasse
     * <i>immutable</i> (unver&auml;nderlich) ist. </p>
     *
     * @return  new interval collection containing the intersection interval,
     *          maybe empty (if there is no intersection)
     * @since   2.0
     */
    public IntervalCollection<T> withIntersection() {

        int len = this.intervals.size();

        if (len < 2) {
            return this;
        }

        T latestStart = this.intervals.get(len - 1).getStart().getTemporal();
        T earliestEnd = null;

        for (int i = 0; i < len; i++) {
            Boundary<T> b = this.intervals.get(i).getEnd();
            T candidate = b.getTemporal();

            if (b.isInfinite()) {
                continue;
            } else if (this.isCalendrical()) {
                if (b.isOpen()) {
                    candidate = this.getTimeLine().stepBackwards(candidate);
                }
            } else if (b.isClosed()) {
                candidate = this.getTimeLine().stepForward(candidate);
                if (candidate == null) {
                    continue;
                }
            }

            if (
                (earliestEnd == null)
                || candidate.isBefore(earliestEnd)
            ) {
                earliestEnd = candidate;
            }
        }

        if (earliestEnd == null) {
            Boundary<T> s = this.createStartBoundary(latestStart);
            Boundary<T> e = Boundary.infiniteFuture();
            ChronoInterval<T> interval = this.newInterval(s, e);
            return this.create(Collections.singletonList(interval));
        } else if (this.isCalendrical()) {
            if (!earliestEnd.isBefore(latestStart)) {
                Boundary<T> s = this.createStartBoundary(latestStart);
                Boundary<T> e = Boundary.ofClosed(earliestEnd);
                ChronoInterval<T> interval = this.newInterval(s, e);
                return this.create(Collections.singletonList(interval));
            }
        } else if (earliestEnd.isAfter(latestStart)) {
            Boundary<T> s = this.createStartBoundary(latestStart);
            Boundary<T> e = Boundary.ofOpen(earliestEnd);
            ChronoInterval<T> interval = this.newInterval(s, e);
            return this.create(Collections.singletonList(interval));
        }

        List<ChronoInterval<T>> zero = Collections.emptyList();
        return this.create(zero);

    }

    /**
     * <p>Equivalent to {@code plus(other.getIntervals())}. </p>
     *
     * @param   other       another interval collection whose intervals
     *                      are to be added to this instance
     * @return  new merged interval collection
     * @since   2.0
     */
    /*[deutsch]
     * <p>&Auml;quivalent zu {@code plus(other.getIntervals())}. </p>
     *
     * @param   other       another interval collection whose intervals
     *                      are to be added to this instance
     * @return  new merged interval collection
     * @since   2.0
     */
    public IntervalCollection<T> union(IntervalCollection<T> other) {

        if (this == other) {
            return this;
        }

        return this.plus(other.getIntervals());

    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof IntervalCollection) {
            IntervalCollection<?> that = IntervalCollection.class.cast(obj);

            return (
                this.getTimeLine().equals(that.getTimeLine())
                && this.intervals.equals(that.intervals)
            );
        }

        return false;

    }

    @Override
    public int hashCode() {

        return this.intervals.hashCode();

    }

    /**
     * <p>For debugging purposes. </p>
     *
     * @return  String
     */
    /*[deutsch]
     * <p>F&uuml;r Debugging-Zwecke. </p>
     *
     * @return  String
     */
    @Override
    public String toString() {

        int n = this.intervals.size();
        StringBuilder sb = new StringBuilder(n * 30);
        sb.append('{');

        for (int i = 0; i < n; i++) {
            sb.append(this.intervals.get(i));

            if (i < n - 1) {
                sb.append(',');
            }
        }

        return sb.append('}').toString();

    }

    /**
     * <p>Definiert ein Vergleichsobjekt zum Sortieren der Intervalle
     * zuerst nach dem Start und dann nach dem Ende. </p>
     *
     * @return  Comparator for intervals
     */
    abstract Comparator<ChronoInterval<T>> getComparator();

    /**
     * <p>Erzeugt eine neue ge&auml;nderte Kopie dieser Instanz. </p>
     *
     * @param   intervals   new sorted list of intervals
     * @return  IntervalCollection
     */
    abstract IntervalCollection<T> create(List<ChronoInterval<T>> intervals);

    /**
     * <p>Liefert die zugeh&ouml;rige Zeitachse. </p>
     *
     * @return  TimeLine
     */
    abstract TimeLine<T> getTimeLine();

    /**
     * <p>Erzeugt ein Intervall zwischen den angegebenen Grenzen. </p>
     *
     * @return  new interval
     */
    abstract ChronoInterval<T> newInterval(
        Boundary<T> start,
        Boundary<T> end
    );

    /**
     * <p>Kalendarische Intervalle sind bevorzugt geschlossen und m&uuml;ssen
     * diese Methode so &uuml;berschreiben, da&szlig; sie {@code true}
     * zur&uuml;ckgeben. </p>
     *
     * @return  boolean
     */
    boolean isCalendrical() {

        return false;

    }

    private Boundary<T> createStartBoundary(T start) {

        if (start == null) {
            return Boundary.infinitePast();
        } else {
            return Boundary.ofClosed(start);
        }

    }

    private void addLeft(
        List<ChronoInterval<T>> gaps,
        T min
    ) {

        T e = this.getTimeLine().stepBackwards(min);

        if (e != null) {
            Boundary<T> be;

            if (this.isCalendrical()) {
                be = Boundary.ofClosed(e);
            } else {
                be = Boundary.ofOpen(min);
            }

            Boundary<T> bs = Boundary.infinitePast();
            gaps.add(this.newInterval(bs, be));
        }

    }

    private void addLeft(
        List<ChronoInterval<T>> gaps,
        T start,
        T min
    ) {

        if (start.isBefore(min)) {
            Boundary<T> be;

            if (this.isCalendrical()) {
                be = Boundary.ofClosed(this.getTimeLine().stepBackwards(min));
            } else {
                be = Boundary.ofOpen(min);
            }

            Boundary<T> bs = Boundary.ofClosed(start);
            gaps.add(this.newInterval(bs, be));
        }

    }

}
