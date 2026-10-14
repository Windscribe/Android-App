package de.blinkt.openvpn.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.regex.Matcher;

/**
 * The flags field of a --machine-readable-output line decides the level every message from the
 * OpenVPN process is logged at.
 */
public class TestOpenVPNLogParsing {

    private static VpnStatus.LogLevel levelOf(String logline) {
        Matcher m = OpenVPNThread.LOG_PATTERN.matcher(logline);
        assertTrue("did not match: " + logline, m.matches());
        return OpenVPNThread.levelFromFlags(Integer.parseInt(m.group(3), 16));
    }

    private static int verbosityOf(String logline) {
        Matcher m = OpenVPNThread.LOG_PATTERN.matcher(logline);
        assertTrue("did not match: " + logline, m.matches());
        return Integer.parseInt(m.group(3), 16) & 0x0F;
    }

    @Test
    public void flagsAboveTheLowNibbleSurviveTheCapture() {
        assertEquals(VpnStatus.LogLevel.WARNING,
                levelOf("1757386423.905123 41 WARNING: this configuration may cache passwords"));
        assertEquals(VpnStatus.LogLevel.WARNING,
                levelOf("1757386423.905123 21 Options error: option 'tcp-nodelay' cannot be used here"));
        assertEquals(VpnStatus.LogLevel.ERROR,
                levelOf("1757386423.905123 11 Cannot open TUN device"));
        assertEquals(VpnStatus.LogLevel.VERBOSE,
                levelOf("1757386423.905123 83 MANAGEMENT: CMD 'state on'"));
    }

    @Test
    public void anOrdinaryMessageStaysInfo() {
        assertEquals(VpnStatus.LogLevel.INFO,
                levelOf("1380308330.240114 18000002 Send to HTTP proxy: 'X-Online-Host: bla.com'"));
    }

    @Test
    public void verbosityIsTheLowNibble() {
        assertEquals(2, verbosityOf("1380308330.240114 18000002 Send to HTTP proxy: 'X-Online-Host: bla.com'"));
        assertEquals(1, verbosityOf("1757386423.905123 41 WARNING: this configuration may cache passwords"));
    }
}
