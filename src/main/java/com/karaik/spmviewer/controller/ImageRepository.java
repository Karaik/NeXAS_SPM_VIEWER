package com.karaik.spmviewer.controller;

import com.karaik.spmviewer.spm.Spm;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片加载与缓存仓库：
 * <ul>
 *   <li>优先在 SPM 所在目录查找图片；</li>
 *   <li>若失败则遍历额外搜索路径列表；</li>
 *   <li>缓存成功加载的 {@link Image}，避免重复 IO；</li>
 *   <li>在多线程环境下安全使用（后台加载任务会调用）。</li>
 * </ul>
 */
@Slf4j
public class ImageRepository {

    private final Map<Path, Image> imageCache = new ConcurrentHashMap<>();
    private volatile List<Path> extraRoots = List.of();

    public void setExtraSearchRoots(List<Path> roots) {
        if (roots == null || roots.isEmpty()) {
            this.extraRoots = List.of();
        } else {
            List<Path> normalized = new ArrayList<>();
            for (Path root : roots) {
                if (root != null) {
                    normalized.add(root.toAbsolutePath().normalize());
                }
            }
            this.extraRoots = Collections.unmodifiableList(normalized);
        }
    }

    public void clearCache() {
        imageCache.clear();
    }

    public List<Image> loadImages(Spm spm, Path baseDir) {
        if (spm == null || spm.getImageData() == null) {
            return List.of();
        }
        List<Image> images = new ArrayList<>(spm.getImageData().size());
        for (Spm.SPMImageData imageData : spm.getImageData()) {
            images.add(loadImage(baseDir, imageData == null ? null : imageData.getImageName()));
        }
        return images;
    }

    public Image loadImage(Path baseDir, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Path candidate = locateCandidate(baseDir, name);
        if (candidate == null) {
            log.debug("Image asset {} not found under {} nor extra roots", name, baseDir);
            return null;
        }
        Path key = candidate.toAbsolutePath().normalize();
        return imageCache.computeIfAbsent(key, this::readImageSilently);
    }

    private Path locateCandidate(Path baseDir, String name) {
        List<Path> searchOrder = new ArrayList<>();
        if (baseDir != null) {
            searchOrder.add(baseDir);
            // 部分资源会放在 images 子目录，再尝试一次
            searchOrder.add(baseDir.resolve("images"));
        }
        searchOrder.addAll(extraRoots);

        for (Path root : searchOrder) {
            if (root == null) continue;
            Path candidate = root.resolve(name).normalize();
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Image readImageSilently(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return new Image(input);
        } catch (Exception ex) {
            log.warn("Failed to load image {}", path, ex);
            return null;
        }
    }
}
