package com.astryxion.chaospersists.world.dimension.chunkgenerator;

import net.minecraft.util.Mth;

record ChaosPlainsNoiseSlider(double target, int size, int offset) {

    double applySlide(double density, double y) {
        if (this.size <= 0) {
            return density;
        }

        double slide = (y - (double) this.offset) / (double) this.size;
        return Mth.clampedLerp(this.target, density, slide);
    }
}
