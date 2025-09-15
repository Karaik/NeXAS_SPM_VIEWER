package com.karaik.spmviewer.spm;

import com.karaik.spmviewer.io.BinaryReader;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;

import java.util.List;

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
        private byte unk5;
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
        private byte unk3;
        private List<SPMHitArea> hitRects;
        private List<SPMChipData> chipData;
    }

    @Data
    public static abstract class SPMHitArea {
        private short id;
        private short shapeType;

        public abstract void readInfo(BinaryReader reader) throws Exception;

        public abstract String getDisplayInfo();

        /**
         * 在Canvas上绘制自身的形状。
         *
         * @param g           JavaFX的绘图上下文
         * @param pageOriginX 页面原点在画布上的X坐标
         * @param pageOriginY 页面原点在画布上的Y坐标
         */
        public abstract void drawSelf(GraphicsContext g, double pageOriginX, double pageOriginY);
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