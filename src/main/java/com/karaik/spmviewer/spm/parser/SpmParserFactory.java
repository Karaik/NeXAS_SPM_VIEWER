package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.parser.version.BheV200Parser;
import com.karaik.spmviewer.spm.parser.version.BheV202Parser;
import com.karaik.spmviewer.spm.parser.version.BsdxV200Parser;
import com.karaik.spmviewer.spm.parser.version.BsdxV202Parser;
import com.karaik.spmviewer.spm.parser.version.SpmDialectParser;

/**
 * SPM 解析器工厂。
 * 根据用户选择的解析模式（方言），决定并创建合适的版本解析器实例。
 */
public class SpmParserFactory {

    public static SpmDialectParser getParserForMode(Settings.ParsingMode mode, String spmVersion) {
        boolean is202 = spmVersion != null && spmVersion.contains("2.02");
        return switch (mode) {
            case BHE -> (is202 ? new BheV202Parser() : new BheV200Parser());
            case BSDX -> (is202 ? new BsdxV202Parser() : new BsdxV200Parser());
        };
    }
}
