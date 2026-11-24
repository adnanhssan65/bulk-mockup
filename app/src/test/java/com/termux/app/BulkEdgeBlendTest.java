package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

/**
 * v133 Edge Blend — pure logic ke tests: mist ka rang har palette
 * ke liye, gradient stops ki shakal (edge par gehra, andar khatam,
 * koi hard jump nahi), aur zone heights ka hisaab.
 */
public class BulkEdgeBlendTest {

    private static int alphaOf(int argb) {
        return (argb >> 24) & 0xFF;
    }

    private static int rgbOf(int argb) {
        return argb & 0x00FFFFFF;
    }

    // ------------------------------------------------------------ mist color

    @Test
    public void mistColor_lightThemes_white() {
        for (BulkPalettes.Palette p : BulkPalettes.all()) {
            if (p.light) {
                Assert.assertEquals("light theme " + p.id + " ki mist safed honi chahiye",
                    0xFFFFFF, BulkEdgeBlend.mistRgb(p));
            }
        }
    }

    @Test
    public void mistColor_darkThemes_ownBg() {
        for (BulkPalettes.Palette p : BulkPalettes.all()) {
            if (!p.light) {
                Assert.assertEquals("dark theme " + p.id + " ki mist usi ke bg rang ki ho",
                    p.bg & 0x00FFFFFF, BulkEdgeBlend.mistRgb(p));
            }
        }
        Assert.assertEquals(0x030603,
            BulkEdgeBlend.mistRgb(BulkPalettes.byId(BulkPalettes.ID_DARK)));
        Assert.assertEquals(0x0F172A,
            BulkEdgeBlend.mistRgb(BulkPalettes.byId(BulkPalettes.ID_MIDNIGHT)));
    }

    @Test
    public void mistColor_nullPalette_whiteFallback() {
        Assert.assertEquals(0xFFFFFF, BulkEdgeBlend.mistRgb(null));
    }

    // ------------------------------------------------------------ gradient stops

    @Test
    public void edgeStops_shape_edgeOpaque_innerTransparent() {
        for (BulkPalettes.Palette p : BulkPalettes.all()) {
            int[] stops = BulkEdgeBlend.edgeStops(p);
            Assert.assertEquals(BulkEdgeBlend.ALPHA_FRACTIONS.length, stops.length);
            Assert.assertEquals("pehla stop (edge) EDGE_ALPHA ho",
                BulkEdgeBlend.EDGE_ALPHA, alphaOf(stops[0]));
            Assert.assertEquals("aakhri stop (inner end) bilkul transparent ho",
                0, alphaOf(stops[stops.length - 1]));
            for (int s : stops) {
                Assert.assertEquals("har stop ka RGB mist ka rang ho",
                    BulkEdgeBlend.mistRgb(p), rgbOf(s));
            }
        }
    }

    @Test
    public void edgeStops_alphaStrictlyDecreasing_noHardJump() {
        for (BulkPalettes.Palette p : BulkPalettes.all()) {
            int[] stops = BulkEdgeBlend.edgeStops(p);
            for (int i = 1; i < stops.length; i++) {
                Assert.assertTrue("alpha har step par kam ho (theme " + p.id + ", stop " + i + ")",
                    alphaOf(stops[i]) < alphaOf(stops[i - 1]));
                Assert.assertTrue("koi hard jump na ho — pichle stop ke adhe se zyada na ghate",
                    alphaOf(stops[i - 1]) - alphaOf(stops[i]) <= BulkEdgeBlend.EDGE_ALPHA / 2 + 1);
            }
        }
    }

    @Test
    public void edgeStops_exactValues_defaultTheme() {
        int[] stops = BulkEdgeBlend.edgeStops(BulkPalettes.byId(BulkPalettes.ID_DEFAULT));
        Assert.assertEquals((242 << 24) | 0xFFFFFF, stops[0]);
        Assert.assertEquals(0x00FFFFFF, stops[stops.length - 1]);
    }

    // ------------------------------------------------------------ zone heights

    @Test
    public void zoneHeights_insetPlusExtension() {
        Assert.assertEquals(90, BulkEdgeBlend.topZonePx(72, 18));
        Assert.assertEquals(78, BulkEdgeBlend.bottomZonePx(48, 30));
        Assert.assertEquals(0, BulkEdgeBlend.topZonePx(0, 0));
    }

    @Test
    public void zoneHeights_negativesClamped() {
        Assert.assertEquals(0, BulkEdgeBlend.topZonePx(-5, -2));
        Assert.assertEquals(10, BulkEdgeBlend.bottomZonePx(-5, 10));
    }

    @Test
    public void extensions_positiveAndBottomSofter() {
        Assert.assertTrue(BulkEdgeBlend.TOP_EXTENSION_DP > 0);
        Assert.assertTrue(BulkEdgeBlend.BOTTOM_EXTENSION_DP >= BulkEdgeBlend.TOP_EXTENSION_DP);
    }
}
