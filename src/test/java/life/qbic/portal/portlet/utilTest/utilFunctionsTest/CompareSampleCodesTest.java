package life.qbic.portal.portlet.utilTest.utilFunctionsTest;

import org.junit.Test;

import static life.qbic.portal.portlet.util.Functions.compareSampleCodes;
import static org.junit.Assert.assertEquals;

public class CompareSampleCodesTest {

    @Test
    public void twoEqualSampleCodes() {
        assertEquals(0, compareSampleCodes("QABCD002AC", "QABCD002AC"));
    }

    @Test
    public void twoDifferentSampleCodes() {
        assertEquals(1, compareSampleCodes("QABCD003AB", "QABCD002AC"));
    }
}
