package eu.peppol.as2;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.regex.Matcher;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author steinar
 *         Date: 10.10.13
 *         Time: 18:50
 */
public class As2DispositionTest {

    @Test
    public void testToString() throws Exception {

        String s = As2Disposition.processed().toString();
        assertEquals(s, "automatic-action/MDN-sent-automatically; processed");

        s = As2Disposition.failed("uhada").toString();
        assertEquals(s.toLowerCase(), "automatic-action/mdn-sent-automatically; failed/failure: uhada");

        s = As2Disposition.processedWithError("Ouch!").toString();
        assertEquals(s.toLowerCase(), "automatic-action/mdn-sent-automatically; processed/error: ouch!");

    }

    @Test
    public void parseWithPattern() throws Exception {
        String s = "automatic-action/MDN-sent-automatically; processed/error: Unknown recipient";

        Matcher matcher = As2Disposition.pattern.matcher(s);
        assertTrue(matcher.matches());
        assertEquals(matcher.groupCount(), 6);

        String actionMode = matcher.group(1);
        String sendingMode = matcher.group(2);
        String dispositionType = matcher.group(3);
        String dispositionModifierPrefix = matcher.group(5);
        String dispositionModifier = matcher.group(6);

        assertEquals(actionMode, "automatic-action");
        assertEquals(sendingMode, "MDN-sent-automatically");
        assertEquals(dispositionType, "processed");
        assertEquals(dispositionModifierPrefix, "error");
        assertEquals(dispositionModifier, "Unknown recipient");

        s = "automatic-action/MDN-sent-automatically; processed";
        matcher = As2Disposition.pattern.matcher(s);
        assertTrue(matcher.matches());

        assertEquals(matcher.toMatchResult().groupCount(), 6);

        assertEquals(matcher.group(4), null);
    }

    @Test
    public void parseWithValueOf() throws Exception {
        String s = "automatic-action/MdN-sent-automatically; processed/error: Unknown recipient";
        As2Disposition as2Disposition = As2Disposition.valueOf(s);
        assertNotNull(as2Disposition);

        assertEquals(as2Disposition.getActionMode() , As2Disposition.ActionMode.AUTOMATIC);
        assertEquals(as2Disposition.getSendingMode(), As2Disposition.SendingMode.AUTOMATIC);
        assertEquals(as2Disposition.getDispositionType(), As2Disposition.DispositionType.PROCESSED);
        assertEquals(as2Disposition.getDispositionModifier().getPrefix(), As2Disposition.DispositionModifier.Prefix.ERROR);
        assertEquals(as2Disposition.getDispositionModifier().getDispositionModifierExtension(), "Unknown recipient");
    }

    @Test
    public void parseWithValueOfAdvanced() throws Exception {
        String s = "automatic-action/MDN-sent-automatically; processed/ERROR: Payload does not contain Standard Business Document Header ";
        As2Disposition as2Disposition = As2Disposition.valueOf(s);
        assertNotNull(as2Disposition);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parseLongDispositionFromRealWorldTesting() {
        String s = "automatic-action/MDN-sent-automatically; processed\\Error: unexpected-error";
        As2Disposition unimaze = As2Disposition.valueOf(s); // illegal syntax
    }

    @Test
    public void parseLongDispositionFromRealWorldTestingFixed() {
        String s = "  automatic-action/MDN-sent-automatically; processed/Error: unexpected-error  ";
        As2Disposition unimaze = As2Disposition.valueOf(s);
        assertEquals(unimaze.getDispositionModifier().getDispositionModifierExtension(), "unexpected-error");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void parseShortDispositionFromRealWorldTesting() {
        String s = "Disposition: \"disposition-mode\";  processed";
        s = s.split("Disposition:")[1];
        As2Disposition unimaze = As2Disposition.valueOf(s); // illegal syntax
    }

}
