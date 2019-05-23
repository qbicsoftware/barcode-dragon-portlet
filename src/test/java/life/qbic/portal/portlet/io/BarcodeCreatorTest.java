package life.qbic.portal.portlet.utilTest.utilFunctionsTest;

import org.junit.Test;

import static life.qbic.portal.portlet.util.Functions.escapeLatexCharacters;
import static org.junit.Assert.assertEquals;

public class EscapeLatexCharactersTest {

    @Test
    public void simpleCharacterTest() {
        assertEquals("test\\$", escapeLatexCharacters("test$"));
    }

    @Test
    public void differentPositionsTest() {
        assertEquals("s\\$o\\$\\$me\\$S\\$t\\$rin\\$g\\$Bl\\$a", escapeLatexCharacters("s$o$$me$S$t$rin$g$Bl$a"));
    }

    @Test
    public void manyDifferentLatexCharactersTest() {
        assertEquals("t\\%h>i<sIsA\\$notherWeirdString", escapeLatexCharacters("t%h>i<sIsA$notherWeirdString"));
    }

    @Test
    public void spacesIncludedTest() {
        assertEquals("test \\$", escapeLatexCharacters("test $"));
    }
}
