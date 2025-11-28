package com.karaik.spmviewer.spm.parser.version;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class BheV202Parser extends BheV200Parser {

    @Override
    protected Spm.SPMPageData parsePage(BinaryReader reader) {
        Spm.SPMPageData page = super.parsePage(reader);
        page.setUnk3(reader.readByte());
        return page;
    }

    @Override
    protected Spm.SPMChipData parseChip(BinaryReader reader) {
        var chip = new Spm.SPMChipData();
        chip.setImageNo(reader.readInt());
        chip.setDstRect(parseRect(reader));
        chip.setChipWidth(reader.readInt());
        chip.setChipHeight(reader.readInt());
        chip.setSrcRect(parseRect(reader));
        chip.setDrawOption(reader.readInt() & 0xFFFFFFFFL);
        chip.setUnk5(reader.readByte());
        chip.setDrawOptionValue(reader.readInt() & 0xFFFFFFFFL);
        chip.setOption(reader.readInt());
        return chip;
    }
}
