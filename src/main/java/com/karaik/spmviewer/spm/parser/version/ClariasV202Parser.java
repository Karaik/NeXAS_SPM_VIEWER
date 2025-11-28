package com.karaik.spmviewer.spm.parser.version;

/**
 * Clarias 方言，2.02 系列：开启 2.02 额外字段读取。
 */
public class ClariasV202Parser extends ClariasV200Parser {

    @Override
    protected boolean isV202(String spmVersion) {
        return true;
    }
}
