package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.parser.SpmParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析外部样本目录，确保没有异常（若目录不存在则跳过）。
 */
class SampleParseIntegrationTest {

    private static final Path BHE_DIR = Paths.get("D:\\BaiduNetdiskDownload\\bsdx_bhe\\bheAll");
    private static final Path BSDX_DIR = Paths.get("D:\\BaiduNetdiskDownload\\bsdx_bhe\\bsdxAll");
    private static final Path CLARIAS_DIR = Paths.get("D:\\BaiduNetdiskDownload\\bsdx_bhe\\clarias_resources");

    @Test
    void parseAllBheSamples() throws Exception {
        if (!Files.isDirectory(BHE_DIR)) {
            return; // skip silently if samples not present
        }
        List<Path> files = collectSpmFiles(BHE_DIR);
        assertTrue(!files.isEmpty(), "bheAll 应包含 .spm 文件");
        for (Path spmFile : files) {
            parseWithMode(spmFile, Settings.ParsingMode.BHE);
        }
    }

    @Test
    void parseAllBsdxSamples() throws Exception {
        if (!Files.isDirectory(BSDX_DIR)) {
            return; // skip silently if samples not present
        }
        List<Path> files = collectSpmFiles(BSDX_DIR);
        assertTrue(!files.isEmpty(), "bsdxAll 应包含 .spm 文件");
        for (Path spmFile : files) {
            parseWithMode(spmFile, Settings.ParsingMode.BSDX);
        }
    }

    @Test
    void parseAllClariasSamples() throws Exception {
        if (!Files.isDirectory(CLARIAS_DIR)) {
            return;
        }
        List<Path> files = collectSpmFiles(CLARIAS_DIR);
        assertTrue(!files.isEmpty(), "clarias_resources 应包含 .spm 文件");
        for (Path spmFile : files) {
            parseWithMode(spmFile, Settings.ParsingMode.CLARIAS);
        }
    }

    private List<Path> collectSpmFiles(Path dir) throws Exception {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(dir)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".spm"))
                    .forEach(files::add);
        }
        return files;
    }

    private void parseWithMode(Path spmPath, Settings.ParsingMode mode) throws Exception {
        byte[] data = Files.readAllBytes(spmPath);
        BinaryReader reader = new BinaryReader(data, Settings.getCharsetName());
        String version = reader.readNullTerminatedString();

        SpmParser parser = new SpmParser();
        Spm spm = parser.parse(data, spmPath.getFileName().toString(), Settings.getCharsetName(), mode);
        assertNotNull(spm, "解析结果不能为空: " + spmPath);
        assertNotNull(spm.getPageData(), "PageData 不能为空: " + spmPath);
    }

    private Settings.ParsingMode pickMode(String version, boolean isBhe) {
        return isBhe ? Settings.ParsingMode.BHE : Settings.ParsingMode.BSDX;
    }
}
