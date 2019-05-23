package life.qbic.portal.portlet.io;

import org.junit.Test;

import life.qbic.portal.portlet.io.BarcodeConfig;
import life.qbic.portal.portlet.io.BarcodeCreator;

import static org.junit.Assert.assertEquals;

public class BarcodeCreatorTest {

  BarcodeCreator b = new BarcodeCreator(new BarcodeConfig("", "", "", ""));

  @Test
  public void simpleCharacterTest() {
    assertEquals("test\\$", BarcodeCreator.escapeLatexCharacters("test$"));
  }

  @Test
  public void differentPositionsTest() {
    assertEquals("s\\$o\\$\\$me\\$S\\$t\\$rin\\$g\\$Bl\\$a",
        BarcodeCreator.escapeLatexCharacters("s$o$$me$S$t$rin$g$Bl$a"));
  }

  @Test
  public void manyDifferentLatexCharactersTest() {
    assertEquals("t\\%h>i<sIsA\\$notherWeirdString",
        BarcodeCreator.escapeLatexCharacters("t%h>i<sIsA$notherWeirdString"));
  }

  @Test
  public void spacesIncludedTest() {
    assertEquals("test \\$", BarcodeCreator.escapeLatexCharacters("test $"));
  }

  @Test
  public void simpleCharacterRemoveTest() {
    assertEquals("test", BarcodeCreator.removeLatexCharacters("test$"));
  }

  @Test
  public void differentPositionsRemoveTest() {
    assertEquals("someStringBla", BarcodeCreator.removeLatexCharacters("s$o$$me$S$t$rin$g$Bl$a"));
  }

  @Test
  public void manyDifferentLatexCharactersRemoveTest() {
    assertEquals("thisIsAnotherWeirdString",
        BarcodeCreator.removeLatexCharacters("t%h>i<s&IsA$no{therW}_eirdS\\trin\\g"));
  }

  @Test
  public void spacesIncludedRemoveTest() {
    assertEquals("test", BarcodeCreator.removeLatexCharacters("test $"));
  }
}
