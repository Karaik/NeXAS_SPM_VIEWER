# Agent Briefing

快速给新接手的代码代理的项目概览与常用命令。

## 项目概况
- 名称：NeXAS_SPM_VIEWER（Java 17 + JavaFX 21，Maven）。
- 目标：读取/渲染多种方言的 SPM（二进制资源），展示页面、动画、命中体并预览贴图。
- 入口：`com.karaik.spmviewer.MainApplication`（JavaFX），视图控制在 `controller/MainViewController.java`。
- 关键数据流：`SpmFileHandler` 读取目录 → `SpmParser` 按 `Settings.ParsingMode` 选择版本解析器 → 结果封装为 SPM 模型 → UI 树/列表/表格展示 → `CanvasController` + `SpmRenderer` 绘制画布（贴片/命中体/坐标系）。

## 当前代码骨架
- `controller/`：主界面逻辑、文件加载、画布渲染、动画播放、状态面板填充。
- `spm/Spm.java`：通用视图模型（嵌套 Page/Chip/HitArea/Anim 数据类）。
- `spm/parser`：统一入口 `SpmParser`，方言 parser（Bhe/Bsdx v200/v202）、`SpmParserFactory`。
- `spm/hitarea`：命中体绘制实现；Clarias 独立目录与 BHE 对照（见 `spm/hitarea/clarias/README.md`），BSDX 也有独立的 `spm/hitarea/bsdx/LegacyRectHitArea.java`。
- 编辑模式：右键 SPM → Edit，独立窗口；Chip/Hit 高亮拖拽、表单修改；精灵替换/导入（从文件覆盖 Chip 源区域并自动调整大小）、图片笔刷/撤销，PNG 另存，SPM 导出 JSON/另存为二进制（基础校验）；快捷键 Ctrl+S/Ctrl+Z/Ctrl+Y/Ctrl+E，滚轮+Ctrl 缩放。所有导出/保存默认写到 SPM 同目录的 `tmp/` 目录，避免覆盖原文件。
- `io/`：`BinaryReader`/`BinaryWriter`。
- `model/Settings`：全局配置与持久化（包含解析模式、背景、原点、自动播放、图片搜索路径等）。
- 资源：`src/main/resources/fxml` 界面、`images` 图标；`example` 目录提供 BHE/ BSDX 样例代码与结构参考；`spmBheJson` 为解包出的 JSON 样本。

## 构建与运行
- 默认 pom 跳过测试；需要执行测试时显式覆盖：  
  `mvn -Dmaven.test.skip=false -DskipTests=false clean test`
- 打包（含 shaded）：`mvn clean package`（生成 `target/*-FULL.jar`）；JavaFX 依赖通过 shade + `javapackager`。
- 运行 jar（需本地 JavaFX 依赖或使用 FULL 版）：`java -jar target/NeXAS_SPM_VIEWER-1.0-0-FULL.jar`
- 快速验证（不带样本）：`mvn -q test` 包含解析/Writer/Editor FXML/几何与图像拷贝基础用例。

## 解析模式（现状）
- `Settings.ParsingMode`：`BHE`、`BSDX`、`CLARIAS`（版本自动识别 2.00/2.02）。
- `SpmParserFactory` 根据引擎+版本返回方言 parser；`HitboxFactory` 按 `shapeType` 创建多态命中体（BHE/Clarias），BSDX 使用 `LegacyRectHitArea`。

## 重要依赖/假设
- 字符集默认 `windows-31j`，可在设置对话框更改；`BinaryReader` 支持大小端切换。
- 画布绘制基于 `pageRect` 或 `pageWidth/Height` + `rotateCenterX/Y` 计算坐标系；命中体调用各自 `drawSelf`。
- 资源查找：`ImageRepository` 使用 `Settings.getImageSearchRoots()` 并支持环境变量 `SPM_IMAGE_PATHS`。

## 已完成的重构要点（见 `todolist.md`）
- 方言解析输出统一 `Spm` 模型；解析器按引擎/版本拆分。
- UI/渲染/文件加载基于 `Spm` 嵌套类；`SpmEntry` 保存 `Spm`。
- 测试默认跳过（pom），需要时：`mvn -Dmaven.test.skip=false -DskipTests=false test`。

## 快速定位
- 画布/渲染：`controller/CanvasController.java`, `controller/render/SpmRenderer.java`
- 文件加载：`controller/SpmFileHandler.java`
- 解析器入口：`spm/parser/SpmParser.java` 与 `parser/version/*`（Bhe/Bsdx v200/v202）
- 命中体实现：`spm/hitarea/*`
- 示例参考：`src/main/resources/example`（BHE / BSDX 版本对照）
