# NeXAS .spm Viewer / Reverse Tool

> 读取与分析 NeXAS 引擎的 `.spm` sprite 资源：动作组、动画与图像信息的可视化与调试。

## 特性
- 解析 `.spm` 内的页面/图像/动画结构
- 图像预览、缩放/平移
- 按页面/图像维度浏览与定位
- 动画播放与逐帧检视
- 导出当前画布到 PNG
- 字符集、解析方言（VER-2.00/2.02）快速切换

## 运行演示（占位）
- 应用总览  
  ![应用总览](docs/images/overview.png)
- images 列表与预览区  
  ![images 列表与预览区](docs/images/images-list-and-preview.png)

## 安装与构建
- 要求：JDK 17+、Maven、JavaFX（Windows 21 运行时通过依赖提供）
- 构建：
```bash
mvn clean package -DskipTests
```
- 运行：
```bash
java -jar target/NeXAS_SPM_VIEWER-1.0-0.jar
```
（使用 shade 插件生成的 `*-FULL.jar` 可独立运行）

## 使用说明
1. 打开含 `.spm` 的目录；
2. 在左侧文件列表选择目标文件；
3. 使用下方 images 列表或页面树查看图像 / 页；
4. 通过工具栏切换背景、缩放、动画等选项；
5. 需要导出当前画布，可使用 `Export PNG`。

## 目录结构（节选）
```
src/
  main/
    java/
      com/karaik/spmviewer/spm/        # .spm 结构与解析
      com/karaik/spmviewer/controller/ # JavaFX 控制器
    resources/
      bug报告.md                       # 故障分析报告
      fxml/MainView.fxml                # 主界面布局
      images/                           # 应用图标等
docs/
  images/                              # 截图占位
```

## `.spm` 简要结构
- `SPMPageData`：页面尺寸、贴图、碰撞等信息；
- `SPMImageData`：引用到的原始位图名称；
- `SPMAnimData`：动作组、帧序列以及对应的页面索引；
- HitArea 支持多态/legacy 两种结构（按解析模式区分）。

更多细节参见 `src/main/java/com/karaik/spmviewer/spm` 目录。

## 已知问题与限制
- 仅验证 Windows 平台运行；
- 未对超大位图进行内存占用优化；
- 动画播放依赖 `.spm` 中的帧引用，缺失数据时无法自动补齐。

## Roadmap
- [ ] 增强 hitbox 编辑与导出能力
- [ ] 支持更多 `.spm` 方言自动检测
- [ ] 引入搜索/过滤快捷面板
- [ ] 增加跨平台打包脚本（macOS/Linux）

## 贡献指南
- 提交前执行 `mvn -q -DskipTests verify`（或根据需求取消跳测）
- 代码注释写在语句上方，日志使用 SLF4J
- 提交信息建议格式：`fix(view): align image preview with selection`
- 新增特性请更新 `README.md` 与相关文档

## 许可证
- 待补充（License 占位）
