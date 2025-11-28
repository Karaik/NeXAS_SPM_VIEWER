package com.karaik.spmviewer.spm.hitarea.clarias;

import com.karaik.spmviewer.spm.Spm;
import lombok.extern.slf4j.Slf4j;

/**
 * Clarias 专用命中体工厂（与 BHE 映射一致，便于独立维护）。
 */
@Slf4j
public class ClariasHitboxFactory {

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
                log.error("unknown Clarias Hitbox type: " + shapeType + " parse with fallback");
                yield new CRect();
            }
        };
    }
}
