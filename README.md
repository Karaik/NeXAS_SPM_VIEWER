# NeXAS .spm Viewer / Reverse Tool

> 读取与分析 NeXAS 引擎的 `.spm` 资源：按方言展示页面/动画/命中体，可视化与调试。

## 特性
- 解析 `.spm` 内的页面/图像/动画结构（BHE/BSDX/Clarias，2.00/2.02）
- 图像预览、缩放/平移，按页面/图像维度浏览；动画播放与逐帧检视
- 编辑模式：右键 SPM → Edit，独立窗口支持 Chip/Hit 拖拽、表单修改；精灵替换/导入（从文件直接覆盖 Chip 源区域并更新尺寸）、图像笔刷/撤销、PNG 另存、SPM JSON/二进制导出
- 保存安全：所有导出/保存默认写入 SPM 所在目录下的 `tmp/` 目录，避免覆盖原文件
- 字符集、解析方言快速切换

## 安装与构建
- 要求：JDK 17+、Maven、JavaFX（Windows 21 运行时通过依赖提供）
- 构建：`mvn clean package -DskipTests`
- 运行：`java -jar target/NeXAS_SPM_VIEWER-1.0-0.jar`（`*-FULL.jar` 可独立运行）
- 开启测试：`mvn -Dmaven.test.skip=false -DskipTests=false test`

## 使用说明
1. 打开含 `.spm` 的目录。
2. 选择文件，使用 images 列表或页面树查看图像/页。
3. 工具栏切换背景、缩放、动画等选项；导出当前画布 `Export PNG`。
4. **编辑模式**（右键列表中的 SPM → Edit）：
   - 左侧列表选择 Page/Chip/Hit；画布支持平移/缩放（中键拖动、Ctrl+滚轮）。
   - Chip/Hit 可在画布拖拽；右侧表单可编辑常用字段并自动入撤销栈（Ctrl+Z/Y）。
   - 图像编辑：选中图片后 Ctrl+E 进入笔刷模式，可撤销；保存后仅更新内存并标记 dirty。
   - 导出：PNG/JSON/SPM 二进制默认落在 `tmp/` 目录；保存前会做索引和矩形范围校验。

## 目录结构（节选）
```
src/
  main/
    java/
      com/karaik/spmviewer/spm/Spm.java   # 通用视图模型（嵌套数据类）
      com/karaik/spmviewer/spm/parser     # 解析入口与方言 parser（引擎选择，版本自动识别）
      com/karaik/spmviewer/controller/    # JavaFX 控制器
    resources/
      fxml/MainView.fxml                  # 主界面布局
      images/                             # 应用图标等
docs/
  images/                                 # 截图占位
```

## `.spm` 简要结构
- 顶层模型 `Spm`：嵌套 Page/Chip/HitArea/Anim 数据类，解析器按 BHE/BSDX 及版本填充。
- 页面：页面尺寸、中心、chip 列表、hitbox 列表。
- 贴片：图片索引、src/dst 矩形、drawOption 等。
- 命中体：BHE 为多态 shapeType，BSDX 为 legacy 矩形。
- 动画：记录帧序与等待帧。

更多细节参见 `src/main/java/com/karaik/spmviewer/spm` 与 `Spm结构说明.md`。

## 解析方言与入口
- 模式：`BHE`、`BSDX`、`CLARIAS`（UI 下拉）；版本自动从文件头判断（含“2.02”走 v202，否则 v200）。
- 入口：`spm/parser/SpmParser.java`；工厂 `SpmParserFactory` 根据引擎+版本返回方言解析器。
- 方言解析器：`BheV200Parser`/`BheV202Parser`、`BsdxV200Parser`/`BsdxV202Parser`、`ClariasV200Parser`/`ClariasV202Parser`。
- 命中体工厂：`spm/parser/HitboxFactory`（BHE/Clarias 多态）；BSDX 由解析器直接构造 `LegacyRectHitArea`（已移动到 `spm/hitarea/bsdx/`，与其他方言隔离）。Clarias 的命中体实现与 BHE 独立维护（详见 `spm/hitarea/clarias/README.md`，其中 `CCircle` 的占位字节与 BHE 不同）。
- 编辑模式：右键列表 SPM → Edit，独立窗口；支持 Chip/Hit 选中高亮、拖拽、表单编辑，图片笔刷+撤销，PNG 另存，SPM 导出 JSON/另存为二进制（保存前做基础校验）。快捷键：Ctrl+S 保存 SPM，Ctrl+Z/Y 撤销/重做，Ctrl+E 打开图片编辑，滚轮+Ctrl 缩放。

## 已知问题与限制
- 主要验证 Windows 平台运行。
- 未对超大位图进行内存优化。
- 动画播放依赖 `.spm` 中的帧引用，缺失数据时无法自动补齐。

## Roadmap
- [ ] 增强 hitbox 编辑与导出能力
- [x] 支持更多 `.spm` 方言自动检测 / 新引擎增量接入
- [x] 引入搜索/过滤快捷面板
- [ ] 增加跨平台打包脚本（macOS/Linux）

## 贡献指南
- 提交前执行 `mvn -q -DskipTests verify`（或按需开启测试）
- 代码注释写在语句上方，日志使用 SLF4J
- 提交信息建议格式：`fix(view): align image preview with selection`
- 新增特性请更新 `README.md` 与相关文档
