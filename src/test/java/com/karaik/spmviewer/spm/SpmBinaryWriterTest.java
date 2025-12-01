package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.parser.SpmBinaryWriter;
import com.karaik.spmviewer.spm.parser.SpmParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpmBinaryWriterTest {

    @Test
    void writeAndParseBheRoundtrip() throws Exception {
        Spm spm = buildSimpleSpm();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SpmBinaryWriter.write(spm, baos, Settings.ParsingMode.BHE);
        byte[] data = baos.toByteArray();
        // 头部应包含版本字符串
        String header = new String(data, 0, Math.min(data.length, 12), StandardCharsets.US_ASCII);
        assertNotNull(header);

        SpmParser parser = new SpmParser();
        Spm parsed = parser.parse(data, "roundtrip.spm", Settings.getCharsetName(), Settings.ParsingMode.BHE);
        assertNotNull(parsed);
        assertEquals(1, parsed.getPageData().size());
        assertEquals(1, parsed.getPageData().get(0).getChipData().size());
    }

    private Spm buildSimpleSpm() {
        Spm spm = new Spm();
        spm.setSpmVersion("SPM VER-2.00");
        Spm.SPMPageData page = new Spm.SPMPageData();
        Spm.SPMChipData chip = new Spm.SPMChipData();
        Spm.SPMRect dst = new Spm.SPMRect();
        dst.setLeft(0); dst.setTop(0); dst.setRight(32); dst.setBottom(32);
        chip.setDstRect(dst);
        Spm.SPMRect src = new Spm.SPMRect();
        src.setLeft(0); src.setTop(0); src.setRight(32); src.setBottom(32);
        chip.setSrcRect(src);
        chip.setImageNo(0);
        chip.setChipWidth(32);
        chip.setChipHeight(32);
        chip.setDrawOption(0L);
        chip.setDrawOptionValue(0L);
        chip.setOption(0);
        page.setNumChipData(1);
        page.setChipData(List.of(chip));
        page.setPageWidth(64);
        page.setPageHeight(64);
        page.setRotateCenterX(32);
        page.setRotateCenterY(32);
        page.setHitRects(List.of());
        spm.setPageData(List.of(page));

        Spm.SPMImageData img = new Spm.SPMImageData();
        img.setImageName("dummy.png");
        spm.setImageData(List.of(img));
        spm.setPatPageNum(0);
        spm.setAnimData(List.of());
        return spm;
    }
}
