package life.qbic.portal.portlet.utilTest.utilFunctionsTest;

import org.junit.Test;

import static life.qbic.portal.portlet.util.Functions.removeLatexCharacters;
import static org.junit.Assert.assertEquals;

public class RemoveLatexCharactersTest {

    @Test
    public void simpleCharacterTest() {
        assertEquals("test", removeLatexCharacters("test$"));
    }

    @Test
    public void differentPositionsTest() {
        assertEquals("someStringBla", removeLatexCharacters("s$o$$me$S$t$rin$g$Bl$a"));
    }

    @Test
    public void manyDifferentLatexCharactersTest() {
        assertEquals("thisIsAnotherWeirdString", removeLatexCharacters("t%h>i<s&IsA$no{therW}_eirdS\\trin\\g"));
    }

    @Test
    public void spacesIncludedTest() {
        assertEquals("test", removeLatexCharacters("test $"));
    }
}
