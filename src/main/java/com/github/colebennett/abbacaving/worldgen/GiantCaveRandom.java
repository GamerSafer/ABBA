package com.github.colebennett.abbacaving.worldgen;

import org.bukkit.Chunk;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

public class GiantCaveRandom {

    private final Chunk chunk;
    private final int caveBandMin, caveBandMax, cutoff;

    // Note: Smaller frequencies yield slower change (more stretched out)
    //   Larger amplitudes yield greater influence on final void
    // Frequency
    private final double f1xz;
    private final double f1y;

    // Density
    private final int amplitude1 = 100;
    private final double subtractForLessThanCutoff;

    // Second pass - small noise
    private final double f2xz = 0.25;
    private final double f2y = 0.05;
    private final int amplitude2 = 2;

    // Third pass - vertical noise
    private final double f3xz = 0.025;
    private final double f3y = 0.005;
    private final int amplitude3 = 20;

    // Position
    private final int caveBandBuffer;

    // Noise
    private final NoiseGenerator noiseGen1, noiseGen2, noiseGen3;

    GiantCaveRandom(Chunk chunk, int caveBandMin, int caveBandMax, double sxz, double sy, int cutoff) {
        this.chunk = chunk;
        this.caveBandMin = caveBandMin;
        this.caveBandMax = caveBandMax;
        this.cutoff = cutoff;

        subtractForLessThanCutoff = amplitude1 - cutoff;
        f1xz = 1.0 / sxz;
        f1y = 1.0 / sy;
        if (caveBandMax - caveBandMin > 128) {
            caveBandBuffer = 32;
        } else {
            caveBandBuffer = 16;
        }
        noiseGen1 = new SimplexNoiseGenerator(chunk.getWorld());
        noiseGen2 = new SimplexNoiseGenerator((long) noiseGen1.noise(chunk.getX(), chunk.getZ()));
        noiseGen3 = new SimplexNoiseGenerator((long) noiseGen1.noise(chunk.getX(), chunk.getZ()));
    }

    public boolean isInGiantCave(int x, int y, int z) {
        double xx = (chunk.getX() << 4) | (x & 0xF);
        double zz = (chunk.getZ() << 4) | (z & 0xF);

        double n1 = (noiseGen1.noise(xx * f1xz, y * f1y, zz * f1xz) * amplitude1);
        double n2 = (noiseGen2.noise(xx * f2xz, y * f2y, zz * f2xz) * amplitude2);
        double n3 = (noiseGen3.noise(xx * f3xz, y * f3y, zz * f3xz) * amplitude3);
        double lc = linearCutoffCoefficient(y);

        return n1 + n2 - n3 - lc > cutoff; // isInCave
    }

    private double linearCutoffCoefficient(int y) {
        // Out of bounds
        if (y < caveBandMin || y > caveBandMax) {
            return subtractForLessThanCutoff;
            // Bottom layer distortion
        } else if (y >= caveBandMin && y <= caveBandMin + caveBandBuffer) {
            double yy = y - caveBandMin;
            return (-subtractForLessThanCutoff / (double) caveBandBuffer) * yy + subtractForLessThanCutoff;
            // Top layer distortion
        } else if (y <= caveBandMax && y >= caveBandMax - caveBandBuffer) {
            double yy = y - caveBandMax + caveBandBuffer;
            return (subtractForLessThanCutoff / (double) caveBandBuffer) * yy;
            // In bounds, no distortion
        } else {
            return 0;
        }
    }
}
