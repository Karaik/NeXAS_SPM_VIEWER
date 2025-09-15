package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.spm.parser.version.DefaultSpmParser;
import com.karaik.spmviewer.spm.parser.version.SpmVersionParser;

/**
 * SPM 解析器工厂。
 * 根据版本字符串和文件名，决定并创建合适的“方言”解析器实例。
 */
public class SpmParserFactory {

    /**
     * 根据版本和文件名获取对应的解析器。
     *
     * @param versionString SPM 版本字符串
     * @param filename      SPM 文件名
     * @return 合适的 SpmVersionParser 实例
     */
    public static SpmVersionParser getParserForVersion(String versionString, String filename) {
        // 在这里添加基于文件名或版本号的特殊“方言”规则
        // 例如:
        // if (filename.toLowerCase().endsWith("_adv.spm")) {
        //     return new AdvSpmParser();
        // }

        // 默认情况下，返回基础版本的解析器
        return new DefaultSpmParser();
    }
}