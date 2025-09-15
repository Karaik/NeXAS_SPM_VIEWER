package com.karaik.spmviewer.spm.parser;

import com.karaik.spmviewer.io.BinaryReader;
import com.karaik.spmviewer.spm.Spm;
import com.karaik.spmviewer.spm.parser.version.SpmVersionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * SPM 文件主解析器。
 * 负责整体的解析流程控制，具体的版本相关解析逻辑则委托给
 * 通过 {@link SpmParserFactory} 获取的特定“方言”解析器来完成。
 */
public class SpmParser {

    public Spm parse(byte[] data, String filename, String charset) {
        var reader = new BinaryReader(data, charset);
        var spm = new Spm();

        // 1. 读取版本号，并据此获取对应的“方言”解析器
        String spmVersion = reader.readNullTerminatedString();
        spm.setSpmVersion(spmVersion);
        SpmVersionParser versionParser = SpmParserFactory.getParserForVersion(spmVersion, filename);

        // 2. 解析页面数据块
        spm.setNumPageData(reader.readInt());
        List<Spm.SPMPageData> pageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumPageData(); i++) {
            pageDataList.add(versionParser.parsePageData(reader)); // 委托
        }
        spm.setPageData(pageDataList);

        // 3. 解析图像数据块
        spm.setNumImageData(reader.readInt());
        List<Spm.SPMImageData> imageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumImageData(); i++) {
            imageDataList.add(parseImageData(reader));
        }
        spm.setImageData(imageDataList);

        // 4. 解析动画数据块
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