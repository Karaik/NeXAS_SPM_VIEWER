package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.parser.version.SpmDialectParser;

/**
 * 解析器入口，根据模式选择具体实现。
 */
public class SpmParser {

    public Spm parse(byte[] data, String filename, String charset, Settings.ParsingMode mode) {
        BinaryReader reader = new BinaryReader(data, charset);
        String version = reader.readNullTerminatedString();
        SpmDialectParser parser = SpmParserFactory.getParserForMode(mode, version);
        return parser.parse(reader, version);
    }
}
