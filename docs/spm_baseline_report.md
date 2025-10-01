# SPM 基线能力报告

> 阶段 0 目标：梳理现有 SPM 解析 / 预览能力，明确已覆盖的用例与缺口，为后续增量开发奠定基线。

## 模块概览

| 模块 | 作用 | 现状 |
|------|------|------|
| `spm.parser` | 解析二进制 `.spm`（VER-2.00/2.02） 并生成 `Spm` 对象 | 已支持 BHE / BSDX / V202 三种方言，含多态 HitArea 逻辑 |
| `controller.render` | 基于 JavaFX Canvas 渲染页面、chips、坐标轴、HitArea | 以页面旋转中心为原点绘制，支持坐标/HitArea toggles |
| `controller` | `MainViewController` 负责 UI 交互、文件加载、动画播放、Zoom | 已实现目录加载、图片预览、动画播放、HitArea 显示 |
| `model.Settings` | 统一存储偏好（解析模式、背景模式、颜色等） | 目前记录解析模式、背景相关配置 |

## 现有能力

- **SPM 解析**：`SpmParser` 联合各方言解析器，覆盖页面、图片、动画、HitArea 等字段。
- **预览渲染**：`SpmRenderer` 基于 Canvas 绘制 page/chip，`CanvasController` 负责画布缩放与背景。
- **动画播放**：`MainViewController` 使用 `Timeline` 根据 `patData.waitFrame` 驱动页面播放。
- **命中框可视化**：已有 `Show Hitboxes` 开关，可在渲染阶段绘制对应 `HitArea`。
- **缺图提示**：当图片丢失时，Canvas 中心展示醒目提示（红底白字）。

## 回归测试基线

新增/整理以下最小化回归用例（均通过 `mvn -q test` 验证）：

| 测试类 | 关注点 |
|--------|--------|
| `CanvasControllerTest` | 图片加载顺序、画布尺寸稳定性、缺图提示渲染 |
| `SpmFileHandlerIntegrationTest` | 实际 SPM 目录（bhe/bsdx）的并发加载与进度收敛 | 
| `MainViewControllerUiTest` | 主界面 FXML 构造、Auto Play 偏好同步、工具栏布局 |

> **缺口**：
> - 缺乏针对方言解析的单元测试（计划在增量阶段补齐）。
> - 预览层的图像缓存、动画步进等体验仍待加强（阶段 1 处理）。
> - 尚未覆盖 JSON 最小差异导出、命中框编辑、新建向导等高级功能。

## 下一步

- 进入阶段 1：完善预览体验（图像缓存、坐标模式、动画调试等）。
- 对照路线图更新 `docs/spm_editor_roadmap.md` 进度标记。

