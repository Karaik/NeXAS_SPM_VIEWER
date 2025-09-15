package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.io.BinaryReader;
import lombok.Data;

import java.util.List;

/**
 * SPM 文件的顶层数据模型。
 * 包含了所有解析出的数据块。
 */
@Data
public class Spm {

    private String spmVersion;
    private Integer numPageData;
    private List<SPMPageData> pageData;
    private Integer numImageData;
    private List<SPMImageData> imageData;
    private Integer patPageNum;
    private Integer numAnimData;
    private List<SPMAnimData> animData;

    @Data
    public static class SPMRect {
        private Integer left;
        private Integer top;
        private Integer right;
        private Integer bottom;
    }

    @Data
    public static class SPMChipData {
        private Integer imageNo;
        private SPMRect dstRect;
        private Integer chipWidth;
        private Integer chipHeight;
        private SPMRect srcRect;
        private Long drawOption;
        private byte unk5; // 仅在特定版本中由解析器填充
        private Long drawOptionValue;
        private Integer option;
    }

    @Data
    public static class SPMPageData {
        private Integer numChipData;
        private Integer pageWidth;
        private Integer pageHeight;
        private SPMRect pageRect;
        private Long pageOption;
        private Integer rotateCenterX;
        private Integer rotateCenterY;
        private Long hitFlag;
        private byte unk3; // 仅在特定版本中由解析器填充
        private List<SPMHitArea> hitRects;
        private List<SPMChipData> chipData;
    }

    /**
     * 碰撞区域（HitArea）的抽象基类。
     * 不同的形状（矩形、圆形等）继承自此类。
     */
    @Data
    public static abstract class SPMHitArea {
        /** 碰撞区域的 ID */
        private short id;
        /** 形状类型，用于决定具体的子类 */
        private short shapeType;

        /**
         * 从二进制流中读取该形状特有的数据。
         * 每个子类都需要实现此方法。
         *
         * @param reader 二进制读取器
         * @throws Exception 读取时可能发生错误
         */
        public abstract void readInfo(BinaryReader reader) throws Exception;

        /**
         * 获取用于在UI中显示的描述信息。
         *
         * @return 描述性字符串
         */
        public abstract String getDisplayInfo();
    }

    @Data
    public static class SPMImageData {
        private String imageName;
    }

    @Data
    public static class SPMPatData {
        private Integer waitFrame;
        private List<Integer> pageNo;
    }

    @Data
    public static class SPMAnimData {
        private String animName;
        private Integer numPat;
        private Integer animRotateDirection;
        private Integer animReverseDirection;
        private List<SPMPatData> patData;
    }
}