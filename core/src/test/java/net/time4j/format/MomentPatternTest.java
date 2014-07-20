package net.time4j.format;

import net.time4j.Iso8601Format;
import net.time4j.Moment;
import net.time4j.PatternType;
import net.time4j.tz.TZID;
import net.time4j.tz.ZonalOffset;
import net.time4j.tz.olson.AMERICA;
import net.time4j.tz.olson.ASIA;
import net.time4j.tz.olson.EUROPE;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static net.time4j.tz.ZonalOffset.Sign.BEHIND_UTC;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(Parameterized.class)
public class MomentPatternTest {

 	@Parameterized.Parameters
        (name= "{index}: "
            + "(pattern={0},locale={1},timezone={2},value={3},text={4})")
 	public static Iterable<Object[]> data() {
 		return Arrays.asList(
            new Object[][] {
                {"uuuuMMdd'T'HHmmssSSSSSSSSSx",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123456789Z",
                        "20120630T235960123456789+00"},
                {"uuuuMMdd'T'HHmmssSSSSSSSSSxx",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123456789Z",
                        "20120630T235960123456789+0000"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSxxx",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123456789Z",
                        "2012-06-30T23:59:60.123456789+00:00"},
                {"uuuuMMdd'T'HHmmssSSSSSSSSSXX",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123456789Z",
                        "20120630T235960123456789Z"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123456789Z",
                        "2012-06-30T23:59:60.123456789Z"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSX",
                        "",
                        ASIA.TOKYO,
                        "2012-06-30T23:59:60,123456789Z",
                        "2012-07-01T08:59:60.123456789+09"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX",
                        "",
                        ASIA.TOKYO,
                        "2012-06-30T23:59:60,123456789Z",
                        "2012-07-01T08:59:60.123456789+09:00"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123456789Z",
                        "2012-07-01T01:59:60.123456789+02:00"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123456000Z",
                        "2012-07-01T01:59:60.123456+02:00"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSXXX",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123+02:00"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSX",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123+02"},
                {"uuuu-MM-dd'T'HH:mm:ss.SSSX",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123+0530"},
                {"d. MMMM uuuu HH:mm:ss.SSSSSSSSSXXX",
                        "de",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123456789Z",
                        "1. Juli 2012 01:59:60.123456789+02:00"},
                {"d. MMMM uuuu HH:mm[:ss]XXX",
                        "de",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60Z",
                        "1. Juli 2012 01:59:60+02:00"},
                {"d. MMMM uuuu HH:mm[:ss.SSSSSSSSS]XXX",
                        "de",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123456789Z",
                        "1. Juli 2012 01:59:60.123456789+02:00"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123+05:30"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123+02:00"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSxxxxx",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123+00:00"},
                {"d. MMMM uuuu HH:mm:ss'['VV']'",
                        "de",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60Z",
                        "1. Juli 2012 01:59:60[Europe/Berlin]"},
                {"d. MMMM uuuu HH:mm'['VV']'",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-10T23:59Z",
                        "11. März 2012 00:59[Europe/Berlin]"},
                {"d. MMM uuuu HH:mm'['VV']'",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-10T23:59Z",
                        "11. Mrz 2012 00:59[Europe/Berlin]"},
                {"dd.MM.uuuu HH:mmVV",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-10T23:59Z",
                        "11.03.2012 00:59Europe/Berlin"},
                {"MMMM/dd/uuuu HH:mm:ssXXX",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-06-30T23:59:60Z",
                        "June/30/2012 16:59:60-07:00"},
                {"MMMM/dd/uuuu HH:mm:ss '['VV']'",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-06-30T23:59:60Z",
                        "June/30/2012 16:59:60 [America/Los_Angeles]"},
                {"MMMM/dd/uuuu HH:mm:ss z",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-06-30T23:59:60Z",
                        "June/30/2012 16:59:60 PDT"},
                {"MMMM/dd/uuuu HH:mm:ss zzzz",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-06-30T23:59:60Z",
                        "June/30/2012 16:59:60 Pacific Daylight Time"},
                {"d. MMM uuuu HH:mm z",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-31T23:59Z",
                        "1. Apr 2012 01:59 MESZ"},
                {"d. MMM uuuu HH:mm zzzz",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-31T23:59Z",
                        "1. Apr 2012 01:59 Mitteleuropäische Sommerzeit"},
                {"d. MMMM uuuu HH:mm z",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-01T23:59Z",
                        "2. März 2012 00:59 MEZ"},
                {"d. MMMM uuuu HH:mm zzzz",
                        "de",
                        EUROPE.BERLIN,
                        "2012-03-01T23:59Z",
                        "2. März 2012 00:59 Mitteleuropäische Zeit"},
                {"d. MMM uuuu HH:mm z",
                        "fr",
                        EUROPE.PARIS,
                        "2012-03-31T23:59Z",
                        "1. avr. 2012 01:59 CEST"},
                {"d. MMM uuuu HH:mm zzzz",
                        "fr",
                        EUROPE.PARIS,
                        "2012-03-31T23:59Z",
                        "1. avr. 2012 01:59 Heure d'été d'Europe centrale"},
                {"d. MMMM uuuu HH:mm z",
                        "fr",
                        EUROPE.PARIS,
                        "2012-03-01T23:59Z",
                        "2. mars 2012 00:59 CET"},
                {"d. MMMM uuuu HH:mm zzzz",
                        "fr",
                        EUROPE.PARIS,
                        "2012-03-01T23:59Z",
                        "2. mars 2012 00:59 Heure d'Europe centrale"},
                {"MMMM/dd/uuuu HH:mmXXX",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-02-21T14:30Z",
                        "February/21/2012 06:30-08:00"},
                {"MMMM/dd/uuuu hh:mm a z",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-02-21T14:30Z",
                        "February/21/2012 06:30 AM PST"},
                {"MMMM/dd/uuuu hh:mm a zzzz",
                        "us",
                        AMERICA.LOS_ANGELES,
                        "2012-02-21T14:30Z",
                        "February/21/2012 06:30 AM Pacific Standard Time"},
                {"d. MMMM uuuu HH:mm z",
                        "en",
                        EUROPE.LONDON,
                        "2012-03-01T23:59Z",
                        "1. March 2012 23:59 GMT"},
                {"d. MMMM uuuu HH:mm z",
                        "en",
                        EUROPE.LONDON,
                        "2012-03-31T23:59Z",
                        "1. April 2012 00:59 BST"},
                {"d. MMMM uuuu HH:mm zzzz",
                        "en",
                        EUROPE.LONDON,
                        "2012-03-01T23:59Z",
                        "1. March 2012 23:59 Greenwich Mean Time"},
                {"d. MMMM uuuu HH:mm zzzz",
                        "en",
                        EUROPE.LONDON,
                        "2012-03-31T23:59Z",
                        "1. April 2012 00:59 British Summer Time"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSS z",
                        "in",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123 IST"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSS zzzz",
                        "in",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123 India Standard Time"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZ",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123+0530"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZ",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123+0200"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZ",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123+0000"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZ",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123GMT+05:30"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZ",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123GMT+02:00"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZ",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123GMT"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZZ",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123+05:30"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZZ",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123+02:00"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSZZZZZ",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123Z"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSOOOO",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123GMT+05:30"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSOOOO",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123GMT+02:00"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSOOOO",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123GMT"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSO",
                        "",
                        ASIA.KOLKATA,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T05:29:60.123GMT+5:30"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSO",
                        "",
                        EUROPE.BERLIN,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-07-01T01:59:60.123GMT+2"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSO",
                        "",
                        ZonalOffset.UTC,
                        "2012-06-30T23:59:60,123000000Z",
                        "2012-06-30T23:59:60.123GMT"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXXXX",
                        "",
                        ZonalOffset.atLongitude(BEHIND_UTC, 14, 30, 0),
                        "2012-06-30T12:00Z",
                        "2012-06-30T11:02:00.000000000-00:58"},
                 {"uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXXXX",
                        "",
                        ZonalOffset.atLongitude(BEHIND_UTC, 14, 30, 20),
                        "2012-06-30T12:00Z",
                        "2012-06-30T11:01:58.666666667-00:58:01.333333333"}
           }
        );
    }

    private ChronoFormatter<Moment> formatter;
    private Moment value;
    private String text;

    public MomentPatternTest(
        String pattern,
        String locale,
        TZID tzid,
        String value,
        String text
    ) throws ParseException {
        super();

        this.formatter =
            Moment.formatter(
                pattern,
                PatternType.CLDR,
                toLocale(locale))
            .withTimezone(tzid);
        this.value = Iso8601Format.EXTENDED_DATE_TIME_OFFSET.parse(value);
        this.text = text;
    }

    @Test
    public void print() {
        assertThat(
            this.formatter.format(this.value),
            is(this.text));
    }

    @Test
    public void parse() throws ParseException {
        assertThat(
            this.formatter.parse(this.text),
            is(this.value));
    }

    private static Locale toLocale(String locale) {
        if (locale.equals("en")) {
            return Locale.UK;
        } else if (locale.equals("us")) {
            return Locale.US;
        } else if (locale.equals("in")) {
            return new Locale("en", "IN");
        }
        return new Locale(locale, locale.toUpperCase());
    }

}