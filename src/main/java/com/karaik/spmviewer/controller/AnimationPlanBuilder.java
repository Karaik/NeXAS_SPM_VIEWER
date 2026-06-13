package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.spm.Spm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 根据动画数据生成播放帧序列（不涉及时间轴，只统计 page / waitFrame）。
 */
public final class AnimationPlanBuilder {

    private AnimationPlanBuilder() {
    }

    public static List<AnimationFrame> build(Spm.SPMAnimData anim, int patPageCount) {
        if (anim == null || anim.getPatData() == null || anim.getPatData().isEmpty()) {
            return Collections.emptyList();
        }
        int effectiveCount = Math.max(0, patPageCount);
        List<AnimationFrame> frames = new ArrayList<>();
        List<Spm.SPMPatData> pats = anim.getPatData();
        for (int patIndex = 0; patIndex < pats.size(); patIndex++) {
            Spm.SPMPatData pat = pats.get(patIndex);
            int wait = Optional.ofNullable(pat.getWaitFrame()).orElse(1);
            List<Integer> pageNos = Optional.ofNullable(pat.getPageNo()).orElse(List.of());
            if (pageNos.isEmpty()) {
                continue;
            }
            int limit = effectiveCount > 0 ? Math.min(effectiveCount, pageNos.size()) : pageNos.size();
            for (int i = 0; i < limit; i++) {
                frames.add(new AnimationFrame(pageNos.get(i), wait, patIndex, i));
            }
        }
        return frames;
    }

    public record AnimationFrame(int pageNo, int waitFrames, int patIndex, int slotIndex) {
        public int effectiveWaitFrames() {
            return waitFrames <= 0 ? 1 : waitFrames;
        }
    }
}
