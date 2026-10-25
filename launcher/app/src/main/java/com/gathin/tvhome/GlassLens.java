package com.gathin.tvhome;

/** Platform-independent edge lens. Focus changes sampling coordinates, never the surface's RGB fill. */
final class GlassLens {
    static float bend(float inwardDistance, float band, float strength) {
        if (band <= 0 || inwardDistance >= band) return 0;
        float t = Math.max(0, Math.min(1, 1 - inwardDistance / band));
        return strength * (1 - (float) Math.sqrt(Math.max(0, 1 - t * t)));
    }

    static int sample(int[] pixels, int width, int height, float x, float y) {
        x = Math.max(0, Math.min(width - 1, x));
        y = Math.max(0, Math.min(height - 1, y));
        int x0 = (int) x, y0 = (int) y;
        int x1 = Math.min(width - 1, x0 + 1), y1 = Math.min(height - 1, y0 + 1);
        float fx = x - x0, fy = y - y0;
        return mix(mix(pixels[y0 * width + x0], pixels[y0 * width + x1], fx),
                mix(pixels[y1 * width + x0], pixels[y1 * width + x1], fx), fy);
    }

    static int mix(int a, int b, float amount) {
        float t = Math.max(0, Math.min(1, amount));
        int r = Math.round(((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int g = Math.round(((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xff000000 | r << 16 | g << 8 | bl;
    }

    static int[] render(int[] sharp, int[] soft, int sourceWidth, int sourceHeight,
                        int width, int height, float left, float top, float scale,
                        float radius, float band, float strength) {
        int[] output = new int[width * height];
        float halfW = width * .5f, halfH = height * .5f;
        radius = Math.min(radius, Math.min(halfW, halfH));
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            float cx = x + .5f - halfW, cy = y + .5f - halfH;
            float qx = Math.abs(cx) - (halfW - radius), qy = Math.abs(cy) - (halfH - radius);
            float ox = Math.max(0, qx), oy = Math.max(0, qy);
            float length = (float) Math.hypot(ox, oy);
            float distance = radius - length - Math.min(Math.max(qx, qy), 0);
            float nx, ny;
            if (length > .001f) { nx = Math.signum(cx) * ox / length; ny = Math.signum(cy) * oy / length; }
            else if (qx > qy) { nx = Math.signum(cx); ny = 0; }
            else { nx = 0; ny = Math.signum(cy); }
            float displacement = bend(distance, band, strength);
            float sx = left + (x + .5f - nx * displacement) * scale;
            float sy = top + (y + .5f - ny * displacement) * scale;
            int blurred = sample(soft, sourceWidth, sourceHeight, sx, sy);
            // Only a narrow perimeter transmits sharper detail. The center stays calm and legible.
            float edge = Math.max(0, 1 - Math.max(0, distance) / Math.max(1, band));
            output[y * width + x] = mix(blurred, sample(sharp, sourceWidth, sourceHeight, sx, sy), edge * .65f);
        }
        return output;
    }
    private GlassLens() { }
}
