package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.controller.AnimationPlanBuilder.AnimationFrame;
import com.karaik.spmviewer.spm.Spm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimationPlanBuilderTest {

    @Test
    void buildFramesRespectsPatPageCount() {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        Spm.SPMPatData pat = new Spm.SPMPatData();
        pat.setWaitFrame(3);
        pat.setPageNo(List.of(0, 1, 2));
        anim.setPatData(List.of(pat));

        List<AnimationFrame> frames = AnimationPlanBuilder.build(anim, 2);
        assertEquals(2, frames.size(), "Should truncate to patPageCount entries");
        assertEquals(0, frames.get(0).pageNo());
        assertEquals(1, frames.get(1).pageNo());
        assertEquals(3, frames.get(0).effectiveWaitFrames());
    }

    @Test
    void buildFramesHandlesEmptyData() {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        anim.setPatData(List.of());
        assertTrue(AnimationPlanBuilder.build(anim, 1).isEmpty(), "Empty animation should produce empty frame list");
    }
}
