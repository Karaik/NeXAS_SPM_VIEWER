package com.karaik.spmviewer.model;

import com.karaik.spmviewer.spm.Spm;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@Data
@RequiredArgsConstructor
public class SpmEntry {
    private final Path path;
    private Status status = Status.PENDING;
    private Spm spm;
    private String errorMessage;

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    @Override
    public String toString() {
        return path.getFileName().toString();
    }
}