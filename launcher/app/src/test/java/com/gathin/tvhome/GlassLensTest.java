package com.gathin.tvhome;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public class GlassLensTest {
    @Test public void centerIsNotMagnifiedOrBent() {
        assertEquals(0, GlassLens.bend(10, 8, 4), 0);
        assertEquals(0, GlassLens.bend(8, 8, 4), 0);
    }
    @Test public void edgeLensingIsBoundedAndSmooth() {
        float previous = 5;
        for (float d = 0; d <= 8; d += .1f) {
            float value = GlassLens.bend(d, 8, 4);
            assertTrue(value <= previous && value >= 0 && value <= 4); previous = value;
        }
    }
    @Test public void focusedGlassCannotWhitenAUniformBackdrop() {
        int[] source = new int[100 * 80]; Arrays.fill(source, 0xff143246);
        for (float amount : new float[]{1.1f, 4f}) {
            int[] rendered = GlassLens.render(source, source, 100, 80, 60, 40, 20, 20, 1, 12, 8, amount);
            for (int pixel : rendered) assertEquals(0xff143246, pixel);
        }
    }
    @Test public void samplesAtScreenEdgesClampWithoutBlackSeams() {
        int[] pixels = {0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffffff};
        assertEquals(0xffff0000, GlassLens.sample(pixels, 2, 2, -100, -100));
        assertEquals(0xffffffff, GlassLens.sample(pixels, 2, 2, 100, 100));
        assertEquals(0xff808080, GlassLens.sample(pixels, 2, 2, .5f, .5f));
    }
    @Test public void focusChangesEdgeSamplingButLeavesInteriorIdentical() {
        int[] pixels = new int[100 * 80];
        for (int y=0; y<80; y++) for (int x=0; x<100; x++) pixels[y*100+x]=0xff000000 | (x*2)<<16 | y*3;
        int[] rest=GlassLens.render(pixels,pixels,100,80,60,40,20,20,1,12,8,1);
        int[] focus=GlassLens.render(pixels,pixels,100,80,60,40,20,20,1,12,8,4);
        assertEquals(rest[20*60+30],focus[20*60+30]);
        assertNotEquals(rest[20*60+1],focus[20*60+1]);
    }
}
