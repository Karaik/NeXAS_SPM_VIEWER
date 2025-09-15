package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.parser.version.BheSpmParser;
import com.karaik.spmviewer.spm.parser.version.BsdxSpmParser;
import com.karaik.spmviewer.spm.parser.version.SpmV202Parser;
import com.karaik.spmviewer.spm.parser.version.SpmVersionParser;

/**
 * SPM 解析器工厂。
 * 根据用户选择的解析模式（方言），决定并创建合适的版本解析器实例。
 */
public class SpmParserFactory {

    /**
     * 根据版本字符串和用户选择的模式获取对应的解析器。
     *
     * @param spmVersion 从文件中读取的版本字符串 (e.g., "SPM VER-2.02")
     * @param mode       用户在UI上选择的 {@link Settings.ParsingMode}
     * @return 合适的 SpmVersionParser 实例
     */
    public static SpmVersionParser getParserForMode(String spmVersion, Settings.ParsingMode mode) {
        return switch (mode) {
            // 方言A (bhe): 使用支持多态Hitbox的默认解析器
            case VER_2_00_BHE -> new BheSpmParser();

            // 方言C (bsdx): 使用专门处理其Hitbox字段顺序的解析器
            case VER_2_00_BSDX -> new BsdxSpmParser();

            // 方言B (2.02): 使用专门处理其Hitbox结构和额外字节的解析器
            case VER_2_02 -> new SpmV202Parser(spmVersion);
        };
    }
}