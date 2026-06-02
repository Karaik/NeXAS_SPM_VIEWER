package com.karaik.spmviewer.spm.writer;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.writer.version.BheV200Writer;
import com.karaik.spmviewer.spm.writer.version.BheV202Writer;

/**
 * SPM 写入器工厂，根据方言和版本返回对应的 writer。
 */
public class SpmWriterFactory {

    public static SpmWriter getWriter(Settings.ParsingMode mode, String spmVersion) {
        boolean is202 = spmVersion != null && spmVersion.contains("2.02");
        return switch (mode) {
            case BHE -> is202 ? new BheV202Writer() : new BheV200Writer();
            case BSDX -> is202 ? new BheV202Writer() : new BheV200Writer();
            case CLARIAS -> is202 ? new BheV202Writer() : new BheV200Writer();
        };
    }
}
