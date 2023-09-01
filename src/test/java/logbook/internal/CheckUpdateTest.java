package logbook.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;

public class CheckUpdateTest {
    @Test
    public void tagRegexTest() {
        String tag = "v20.12.31";
        Version expected = new Version(20, 12, 31);
        this.assertVersionMatches(expected, tag);

        tag = "v23.8.1-rsky-20230809";
        expected = new Version(23, 8, 1);
        this.assertVersionMatches(expected, tag);
    }

    private void assertVersionMatches(Version expected, String tag) {
        Matcher m = CheckUpdate.TAG_REGEX.matcher(tag);
        Assert.assertTrue(m.find());
        Assert.assertEquals(expected, new Version(m.group(CheckUpdate.TAG_REGEX_GROUP_VERSION)));
    }
}
