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
        assertEquals(0, frames.get(0).patIndex());
        assertEquals(0, frames.get(0).slotIndex());
        assertEquals(0, frames.get(1).patIndex());
        assertEquals(1, frames.get(1).slotIndex());
    }

    @Test
    void buildFramesPreservesPatAndSlotIndexes() {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        Spm.SPMPatData pat0 = new Spm.SPMPatData();
        pat0.setWaitFrame(1);
        pat0.setPageNo(List.of(3, 4));
        Spm.SPMPatData pat1 = new Spm.SPMPatData();
        pat1.setWaitFrame(2);
        pat1.setPageNo(List.of(9));
        anim.setPatData(List.of(pat0, pat1));

        List<AnimationFrame> frames = AnimationPlanBuilder.build(anim, 2);
        assertEquals(3, frames.size());
        assertEquals(0, frames.get(0).patIndex());
        assertEquals(0, frames.get(0).slotIndex());
        assertEquals(0, frames.get(1).patIndex());
        assertEquals(1, frames.get(1).slotIndex());
        assertEquals(1, frames.get(2).patIndex());
        assertEquals(0, frames.get(2).slotIndex());
    }

    @Test
    void buildFramesHandlesEmptyData() {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        anim.setPatData(List.of());
        assertTrue(AnimationPlanBuilder.build(anim, 1).isEmpty(), "Empty animation should produce empty frame list");
    }
}
