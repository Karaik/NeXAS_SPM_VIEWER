# Bug 报告2：快速切换目录导致列表回滚

## 摘要
- 在加载 .spm 目录时，如果用户快速切换到另一个目录，旧的加载线程完成后仍会刷新 UI，使得列表回滚到之前的目录内容，并把状态条信息也覆盖。
- 同时，进度条的 `bind` 由于没有解绑定，会在竞争场景下抛出 `RuntimeException: A bound value cannot be set`。

## 复现步骤
1. 打开应用，选择含有多个 .spm 文件的目录 A。
2. 在加载进度尚未完成前，立即再次选择另一个目录 B。
3. 等待两个加载任务依次完成。

## 期待行为 vs 实际行为
- 期待：最终界面展示目录 B 的文件列表，状态条与进度条都对应目录 B。
- 实际：目录 A 的旧线程完成后会覆盖列表，状态条显示 A；有时控制台还会抛出绑定异常，进度条停止更新。

## 根因分析
- `SpmFileHandler.loadDirectory` 每次都会创建新 `Task` 并开线程执行，但没有世代号防护；老线程完成后仍然执行 `setOnSucceeded` 回调，将陈旧数据写回 UI。
- 在旧线程尚未解绑进度条时，新任务再次调用 `progressBar.progressProperty().bind(...)` 会触发 JavaFX 对“重复 bind”的运行时检查并抛错。

## 修复说明
- 为 `SpmFileHandler` 增加 `loadSequence` 世代号。每次调用 `loadDirectory` 时自增，并在回调里比对，不匹配即丢弃旧结果。
- 在绑定新的 `Task` 前先 `unbind` 进度条，避免竞争状态。
- 新建后台线程时设置为 daemon，并附带易读的线程名 `spm-loader-<seq>`，方便日后排查。

## 验证
- 手动按复现步骤操作：最终列表稳定停留在目录 B，状态条/进度条均正确。
- 快速连点多个目录，不再出现异常堆栈或 UI 闪回。

## 关联改动
- 代码：`src/main/java/com/karaik/spmviewer/controller/SpmFileHandler.java`
- 记录：见本文件。
