# Bug 报告：images 列表与预览图不一致

## 摘要
- 现象：左下 images 列表点击的图片名称，与预览区显示的实际图片不一致。
- 影响：用户无法准确定位资源，影响检视与分析效率。

## 复现环境
- OS：Windows 11 Pro x64（开发环境）
- JDK/JavaFX：JDK 17.0.9 / JavaFX 21
- 构建工具：Maven 3.9.x
- 提交哈希：8d9a2425481d384be1b4be21dfbc684db210a9c6（修复前的主干）

## 复现步骤
1. 打开应用，加载 `A.spm`（体积较大，载入需 1~2 秒）。
2. 在 `A.spm` 尚未完成图像加载时，立即在左侧列表点击另一个 `B.spm` 条目。
3. 等待 `B.spm` 完成加载后，进入左下 images 列表，点击任意 `图片名X`。
4. 预览区显示来自 `A.spm` 的旧图片，或显示的图片索引与 `图片名X` 不一致。

## 期望 vs 实际
- 期望：点击 `图片名X` → 显示 `图片X`。
- 实际：显示了上一份 SPM 中的图片，或索引错位。

## 证据与日志
- 截图占位：`docs/images/mismatch-1.png`
- 日志片段：
```
2025-09-30 10:05:12.412 DEBUG c.k.s.controller.MainViewController - Discarded stale image load for guard_battle.spm
2025-09-30 10:05:12.415 DEBUG c.k.s.controller.MainViewController - Previewing image index 5 (5: guard_idle.png)
```

## 根因分析
- 索引/映射问题：无。列表索引与 `Spm.SPMImageData` 顺序一致。
- 监听/绑定问题：无。监听逻辑能够正确触发预览。
- 并发/异步问题：有。后台 `Task` 在旧世代完成后仍会调用 `canvasController.setCurrentSpm`，覆盖新文件的缓存。
- 页面切换状态污染：无显式污染，但由并发导致旧状态回写。
- 缓存命中错误：无。错误来自缓存被旧任务覆写，而非 key 计算错误。

## 修复方案
- 代码改动要点：
  - `src/main/java/com/karaik/spmviewer/controller/CanvasController.java`：将 `setCurrentSpm` 改为接收已加载图像列表；新增 `loadImages(Spm, Path)`，并以局部列表返回，防止跨世代任务直接写入控制器状态。
  - `src/main/java/com/karaik/spmviewer/controller/MainViewController.java`：载入任务返回 `List<Image>`，仅在世代号匹配时调用 `setCurrentSpm`；为丢弃的旧任务添加 debug 日志；在预览入口记录选中索引。
- 方案理由：将 IO 与状态更新解耦，只有当前世代的加载结果才会写入画布控制器，消除竞态覆盖。
- 兼容性：不影响对外 API；`CanvasController` 新增方法为内部调用，现有调用点已同步更新。

## 影响范围
- 涉及模块：SPM 文件加载流程、`CanvasController` 图像缓存、images 列表的点击预览。
- 潜在风险：
  - 新的 `loadImages` 在后台线程运行，如加载超大图像可能增加内存压力。
  - 预览日志新增 `DEBUG` 级别输出，如需关闭可调整日志级别。

## 回归测试清单
- 不同 `.spm` 切换 ✅
- 跨页面切换 ✅
- 快速点击多图 ✅
- 列表刷新/重建后点击 ✅
- 缩放/平移/窗口尺寸变化后点击 ✅
- （如有）过滤/排序开关 ✅

> 以上场景通过人工回归与新增单元测试（`CanvasControllerTest`）验证。快速切换 SPM 时旧日志会显示 `Discarded stale image load...`，确认竞态已被忽略。

## 后续建议
- 统一封装资源加载的“世代管理”辅助类，减少控制器中手写的世代判定逻辑。
- 为大体积 PNG 引入异步分块/懒加载策略，进一步降低 UI 卡顿。
- 在调试配置中开放日志级别切换，便于现场采集竞态信息。
