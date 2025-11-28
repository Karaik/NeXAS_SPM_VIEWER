# NeXAS .spm Viewer / Reverse Tool

> 读取与分析 NeXAS 引擎的 `.spm` 资源：按方言展示页面/动画/命中体，可视化与调试。

## 特性
- 解析 `.spm` 内的页面/图像/动画结构（BHE/BSDX，2.00/2.02）
- 图像预览、缩放/平移，按页面/图像维度浏览
- 动画播放与逐帧检视
- 导出当前画布到 PNG
- 字符集、解析方言快速切换

## 安装与构建
- 要求：JDK 17+、Maven、JavaFX（Windows 21 运行时通过依赖提供）
- 构建：`mvn clean package -DskipTests`
- 运行：`java -jar target/NeXAS_SPM_VIEWER-1.0-0.jar`（`*-FULL.jar` 可独立运行）
- 开启测试：`mvn -Dmaven.test.skip=false -DskipTests=false test`

## 使用说明
1. 打开含 `.spm` 的目录。
2. 选择文件，使用 images 列表或页面树查看图像/页。
3. 工具栏切换背景、缩放、动画等选项。
4. 导出当前画布：`Export PNG`。

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
- 命中体工厂：`spm/parser/HitboxFactory`（BHE/Clarias 多态）；BSDX 由解析器直接构造 `LegacyRectHitArea`。

## 已知问题与限制
- 主要验证 Windows 平台运行。
- 未对超大位图进行内存优化。
- 动画播放依赖 `.spm` 中的帧引用，缺失数据时无法自动补齐。

## Roadmap
- [ ] 增强 hitbox 编辑与导出能力
- [ ] 支持更多 `.spm` 方言自动检测 / 新引擎增量接入
- [ ] 引入搜索/过滤快捷面板
- [ ] 增加跨平台打包脚本（macOS/Linux）

## 贡献指南
- 提交前执行 `mvn -q -DskipTests verify`（或按需开启测试）
- 代码注释写在语句上方，日志使用 SLF4J
- 提交信息建议格式：`fix(view): align image preview with selection`
- 新增特性请更新 `README.md` 与相关文档
