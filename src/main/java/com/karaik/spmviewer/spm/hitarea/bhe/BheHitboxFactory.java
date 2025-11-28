package com.karaik.spmviewer.spm.hitarea.bhe;

import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.hitarea.bhe.c.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Hitbox（碰撞区域）工厂。
 * 根据给定的形状类型ID，创建对应的 HitArea 实现。
 */
@Slf4j
public class BheHitboxFactory {

    public static Spm.SPMHitArea createHitbox(short shapeType) {
        return switch (shapeType) {
            case 0 -> new DefaultHitArea();
            case 1 -> new CRotatableRect();
            case 2 -> new CCircle();
            case 7 -> new C2DLineSegment();
            case 8 -> new C2DDot();
            case 9 -> new CBox();
            case 10 -> new CRotatableBox();
            case 11 -> new CSphere();
            default -> {
                log.error("unknown Hitbox type: " + shapeType + " parse with original");
                yield new CRect();
            }
        };
    }
}
