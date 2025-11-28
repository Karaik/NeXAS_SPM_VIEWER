package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.io.BinaryWriter;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.parser.SpmParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpmParserSmokeTest {

    @Test
    void parseBheSampleWithHitboxAndAnimation() throws Exception {
        byte[] data = buildSampleSpm(true);
        Spm spm = new SpmParser().parse(data, "sample_bhe.spm", StandardCharsets.UTF_8.name(), Settings.ParsingMode.BHE);

        assertEquals("VER-2.00", spm.getSpmVersion());
        assertEquals(1, spm.getPageData().size());
        Spm.SPMPageData page = spm.getPageData().get(0);
        assertEquals(1, Optional.ofNullable(page.getChipData()).map(List::size).orElse(0));
        assertEquals(1, Optional.ofNullable(page.getHitRects()).map(List::size).orElse(0));
        assertEquals(1, spm.getImageData().size());
        assertEquals("sheet.png", spm.getImageData().get(0).getImageName());
        assertEquals(1, spm.getAnimData().size());
        var anim = spm.getAnimData().get(0);
        assertEquals("demo", anim.getAnimName());
        assertEquals(1, Optional.ofNullable(anim.getPatData()).map(List::size).orElse(0));
        Spm.SPMPatData pat = Optional.ofNullable(anim.getPatData()).orElse(List.of()).get(0);
        assertEquals(2, pat.getWaitFrame());
        assertEquals(List.of(0), pat.getPageNo());
    }

    @Test
    void parseBsdxSampleWithoutHitbox() throws Exception {
        byte[] data = buildSampleSpm(false);
        Spm spm = new SpmParser().parse(data, "sample_bsdx.spm", StandardCharsets.UTF_8.name(), Settings.ParsingMode.BSDX);

        assertEquals("VER-2.00", spm.getSpmVersion());
        assertEquals(1, spm.getPageData().size());
        Spm.SPMPageData page = spm.getPageData().get(0);
        assertEquals(1, Optional.ofNullable(page.getChipData()).map(List::size).orElse(0));
        List<? extends Spm.SPMHitArea> hits = Optional.ofNullable(page.getHitRects()).orElse(List.of());
        assertTrue(hits.isEmpty());
        assertEquals("sheet.png", spm.getImageData().get(0).getImageName());
        assertEquals(1, spm.getAnimData().size());
    }

    private byte[] buildSampleSpm(boolean includeHitbox) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(bos, StandardCharsets.UTF_8.name())) {
            writer.writeNullTerminatedString("VER-2.00");

            writer.writeInt(1); // numPageData
            writer.writeInt(1); // numChipData
            writer.writeInt(128); // pageWidth
            writer.writeInt(128); // pageHeight
            writeRect(writer, -64, -64, 64, 64); // pageRect
            writer.writeInt(0); // pageOption
            writer.writeInt(0); // rotateCenterX
            writer.writeInt(0); // rotateCenterY
            writer.writeInt(includeHitbox ? 0b1 : 0); // hitFlag
            if (includeHitbox) {
                writer.writeShort((short) 1); // id
                writer.writeShort((short) 0); // shapeType = DefaultHitArea
                writer.writeInt(-10); // xMin
                writer.writeInt(10);  // xMax
                writer.writeInt(-5);  // yMin
                writer.writeInt(5);   // yMax
                writer.writeInt(0);   // zMin
                writer.writeInt(0);   // zMax
            }

            writer.writeInt(0); // imageNo
            writeRect(writer, -16, -16, 16, 16); // dstRect
            writer.writeInt(32); // chipWidth
            writer.writeInt(32); // chipHeight
            writeRect(writer, 0, 0, 32, 32); // srcRect
            writer.writeInt(0); // drawOption
            writer.writeInt(0); // drawOptionValue
            writer.writeInt(0); // option

            writer.writeInt(1); // numImageData
            writer.writeNullTerminatedString("sheet.png");

            writer.writeInt(1); // patPageNum
            writer.writeInt(1); // numAnimData
            writer.writeNullTerminatedString("demo");
            writer.writeInt(1); // numPat
            writer.writeInt(0); // animRotateDirection
            writer.writeInt(0); // animReverseDirection
            writer.writeInt(2); // waitFrame
            writer.writeInt(0); // page index for pat
        }
        return bos.toByteArray();
    }

    private void writeRect(BinaryWriter writer, int left, int top, int right, int bottom) throws Exception {
        writer.writeInt(left);
        writer.writeInt(top);
        writer.writeInt(right);
        writer.writeInt(bottom);
    }
}
