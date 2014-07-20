package net.time4j.tz;

import net.time4j.PlainTimestamp;
import net.time4j.tz.olson.AMERICA;
import net.time4j.tz.olson.EUROPE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnit4.class)
public class TZIDTest {

    @Test
    public void canonicalEnum() {
        TZID tzid = EUROPE.LISBON;
        assertThat(tzid.canonical(), is("Europe/Lisbon"));
    }

    @Test
    public void canonicalOffsetUTC() {
        TZID tzid = ZonalOffset.UTC;
        assertThat(tzid.canonical(), is("Z"));
    }

    @Test
    public void canonicalOffsetPlus02() {
        TZID tzid = ZonalOffset.ofHours(ZonalOffset.Sign.AHEAD_OF_UTC, 2);
        assertThat(tzid.canonical(), is("UTC+02:00"));
    }

    @Test
    public void equalsByObject() {
        TZID tzid = new TZID() {
            @Override
            public String canonical() {
                return EUROPE.BERLIN.canonical();
            }
        };
        assertThat(tzid.equals(EUROPE.BERLIN), is(false));
    }

    @Test
    public void equalsByCanonical() {
        TZID tzid = new TZID() {
            @Override
            public String canonical() {
                return EUROPE.BERLIN.canonical();
            }
        };
        assertThat(
            tzid.canonical().equals(EUROPE.BERLIN.canonical()),
            is(true));
    }

    @Test
    public void brazilRoundtrip() {
        PlainTimestamp ts = PlainTimestamp.of(2014, 7, 1, 12, 0);
        assertThat(
            ts.at(Timezone.of("Brazil/Acre"))
              .inZonalView(AMERICA.RIO_BRANCO),
            is(ts));
    }

    @Test
    public void predefinedTZ() {
        TZID tzid = AMERICA.ARGENTINA.BUENOS_AIRES;
        assertThat(
            Timezone.of(tzid).getID(),
            is(tzid));
        assertThat(
            Timezone.of(tzid).getID() == tzid,
            is(true));
    }

}