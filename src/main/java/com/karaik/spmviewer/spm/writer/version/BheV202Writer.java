package com.karaik.spmviewer.spm.writer.version;

import com.karaik.spmviewer.io.BinaryWriter;
import com.karaik.spmviewer.spm.Spm;

public class BheV202Writer extends BheV200Writer {

    @Override
    protected void writePage(BinaryWriter writer, Spm.SPMPageData page) throws Exception {
        super.writePage(writer, page);
        writer.writeByte(page.getUnk3());
    }

    @Override
    protected void writeChip(BinaryWriter writer, Spm.SPMChipData chip) throws Exception {
        writer.writeInt(val(chip.getImageNo()));
        writeRect(writer, chip.getDstRect());
        writer.writeInt(val(chip.getChipWidth()));
        writer.writeInt(val(chip.getChipHeight()));
        writeRect(writer, chip.getSrcRect());
        writer.writeInt((int) (valL(chip.getDrawOption()) & 0xFFFFFFFFL));
        writer.writeByte(chip.getUnk5());
        writer.writeInt((int) (valL(chip.getDrawOptionValue()) & 0xFFFFFFFFL));
        writer.writeInt(val(chip.getOption()));
    }
}
