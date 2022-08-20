package com.github.colebennett.abbacaving.worldgen;

import org.bukkit.Chunk;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.SimplexNoiseGenerator;

public class GiantCaveRandom {

    private final Chunk chunk;
    private final int caveBandMin;
    private final int caveBandMax;
    private final int cutoff;

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
    private final NoiseGenerator noiseGen1;
    private final NoiseGenerator noiseGen2;
    private final NoiseGenerator noiseGen3;

    GiantCaveRandom(final Chunk chunk, final int caveBandMin, final int caveBandMax, final double sxz, final double sy, final int cutoff) {
        this.chunk = chunk;
        this.caveBandMin = caveBandMin;
        this.caveBandMax = caveBandMax;
        this.cutoff = cutoff;

        this.subtractForLessThanCutoff = this.amplitude1 - cutoff;
        this.f1xz = 1.0 / sxz;
        this.f1y = 1.0 / sy;
        if (caveBandMax - caveBandMin > 128) {
            this.caveBandBuffer = 32;
        } else {
            this.caveBandBuffer = 16;
        }
        this.noiseGen1 = new SimplexNoiseGenerator(chunk.getWorld());
        this.noiseGen2 = new SimplexNoiseGenerator((long) this.noiseGen1.noise(chunk.getX(), chunk.getZ()));
        this.noiseGen3 = new SimplexNoiseGenerator((long) this.noiseGen1.noise(chunk.getX(), chunk.getZ()));
    }

    public boolean isInGiantCave(final int x, final int y, final int z) {
        final double xx = (this.chunk.getX() << 4) | (x & 0xF);
        final double zz = (this.chunk.getZ() << 4) | (z & 0xF);

        final double n1 = this.noiseGen1.noise(xx * this.f1xz, y * this.f1y, zz * this.f1xz) * this.amplitude1;
        final double n2 = this.noiseGen2.noise(xx * this.f2xz, y * this.f2y, zz * this.f2xz) * this.amplitude2;
        final double n3 = this.noiseGen3.noise(xx * this.f3xz, y * this.f3y, zz * this.f3xz) * this.amplitude3;
        final double lc = this.linearCutoffCoefficient(y);

        return n1 + n2 - n3 - lc > this.cutoff; // isInCave
    }

    private double linearCutoffCoefficient(final int y) {
        // Out of bounds
        if (y < this.caveBandMin || y > this.caveBandMax) {
            return this.subtractForLessThanCutoff;
            // Bottom layer distortion
        } else if (y <= this.caveBandMin + this.caveBandBuffer) {
            // In case of issue, revert above line to:
            // else if (y >= this.caveBandMin && y <= this.caveBandMin + this.caveBandBuffer)
            final double yy = y - this.caveBandMin;
            return (-this.subtractForLessThanCutoff / (double) this.caveBandBuffer) * yy + this.subtractForLessThanCutoff;
            // Top layer distortion
        } else if (y >= this.caveBandMax - this.caveBandBuffer) {
            // In case of issue, revert above line to:
            // else if (y <= this.caveBandMax && y >= this.caveBandMax - this.caveBandBuffer)
            final double yy = y - this.caveBandMax + this.caveBandBuffer;
            return (this.subtractForLessThanCutoff / (double) this.caveBandBuffer) * yy;
            // In bounds, no distortion
        } else {
            return 0;
        }
    }

}
