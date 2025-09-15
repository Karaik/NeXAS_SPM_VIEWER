package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.model.Settings;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.parser.version.SpmVersionParser;

import java.util.ArrayList;
import java.util.List;

public class SpmParser {

    /**
     * 解析SPM文件字节数据。
     *
     * @param data     文件内容的字节数组
     * @param filename 文件名
     * @param charset  文件内字符串所使用的字符集
     * @param mode     用户选择的解析模式（方言）
     * @return 解析完成的 {@link Spm} 数据对象
     */
    public Spm parse(byte[] data, String filename, String charset, Settings.ParsingMode mode) {
        var reader = new BinaryReader(data, charset);
        var spm = new Spm();

        String spmVersion = reader.readNullTerminatedString();
        spm.setSpmVersion(spmVersion);

        // 将版本号和用户选择的模式一起传递给工厂，以获取正确的“方言”解析器
        SpmVersionParser versionParser = SpmParserFactory.getParserForMode(spmVersion, mode);

        spm.setNumPageData(reader.readInt());
        List<Spm.SPMPageData> pageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumPageData(); i++) {
            pageDataList.add(versionParser.parsePageData(reader));
        }
        spm.setPageData(pageDataList);

        spm.setNumImageData(reader.readInt());
        List<Spm.SPMImageData> imageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumImageData(); i++) {
            imageDataList.add(parseImageData(reader));
        }
        spm.setImageData(imageDataList);

        spm.setPatPageNum(reader.readInt());
        spm.setNumAnimData(reader.readInt());
        List<Spm.SPMAnimData> animDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumAnimData(); i++) {
            animDataList.add(parseAnimData(reader, spm.getPatPageNum()));
        }
        spm.setAnimData(animDataList);

        return spm;
    }

    private Spm.SPMImageData parseImageData(BinaryReader reader) {
        var imageData = new Spm.SPMImageData();
        imageData.setImageName(reader.readNullTerminatedString());
        return imageData;
    }

    private Spm.SPMAnimData parseAnimData(BinaryReader reader, int patPageNum) {
        var animData = new Spm.SPMAnimData();
        animData.setAnimName(reader.readNullTerminatedString());
        animData.setNumPat(reader.readInt());
        animData.setAnimRotateDirection(reader.readInt());
        animData.setAnimReverseDirection(reader.readInt());

        int numPat = animData.getNumPat() & 0xFFFF;
        List<Spm.SPMPatData> patDataList = new ArrayList<>();
        for (int i = 0; i < numPat; i++) {
            patDataList.add(parsePatData(reader, patPageNum));
        }
        animData.setPatData(patDataList);
        return animData;
    }

    private Spm.SPMPatData parsePatData(BinaryReader reader, int patPageNum) {
        var patData = new Spm.SPMPatData();
        patData.setWaitFrame(reader.readInt());
        List<Integer> pageNos = new ArrayList<>();
        for (int i = 0; i < patPageNum; i++) {
            pageNos.add(reader.readInt());
        }
        patData.setPageNo(pageNos);
        return patData;
    }
}