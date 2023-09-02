package logbook.internal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.regex.Matcher;

@RunWith(Parameterized.class)
public class CheckUpdateTagRegexTest {
    private final String tag;
    private final Version expected;

    public CheckUpdateTagRegexTest(String tag, Version expected) {
        this.tag = tag;
        this.expected = expected;
    }

    @Parameters
    public static Object[][] data() {
        return new Object[][]{
                {"0.1", new Version(0, 1, 0)},
                {"1.2.3", new Version(1, 2, 3)},
                {"v20.12.31", new Version(20, 12, 31)},
                {"v23.8.1-rsky-20230809", new Version(23, 8, 1)},
        };
    }

    @Test
    public void testTagRegex() {
        Matcher m = CheckUpdate.TAG_REGEX.matcher(this.tag);
        Assert.assertTrue(m.find());
        Assert.assertEquals(this.expected, new Version(m.group(CheckUpdate.TAG_REGEX_GROUP_VERSION)));
    }
}
