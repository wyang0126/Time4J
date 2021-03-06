package net.time4j.i18n;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses(
    {
        CalendricalNamesTest.class,
        DatePatternTest.class,
        DateTimePatternTest.class,
        EnumDisplayTest.class,
        LocalizedGMTOffsetTest.class,
        NumberSymbolTest.class,
        PrettyTimeTest.class,
        RootLocaleTest.class,
        IsoSanityTest.class
    }
)
public class I18nSuite {

}