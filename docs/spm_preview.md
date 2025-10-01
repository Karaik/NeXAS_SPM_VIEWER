# 阶段 1：预览体验强化记录

> 目标：改善预览交互体验，包括资源加载、坐标调试与动画调试工具。

## 实现内容

- **图像搜索与缓存**
  - 新增 `ImageRepository`，优先在 SPM 所在目录查找图片，不足时遍历可配置的额外搜索路径。
  - 引入缓存，避免同一图片重复读取 IO，配合 `Settings` 记住额外搜索目录。
- **坐标/页面显示**
  - Canvas 支持 `OriginMode`（中心 / 左上角）切换，利用 `PageExtents` 统一计算页面宽高。
  - 渲染新增“Chip Bounds”、“Page Bounds”调试开关，便于定位芯片与页面外延。
- **动画调试工具**
  - FlowPane 重构顶部工具栏，腾出空间后加入帧步进、时间轴滑块与帧序标签。
  - `AnimationPlanBuilder` 生成帧序列，支持单帧步进、滑块跳转及 Auto Play 偏好。

## 关键代码

- `com.karaik.spmviewer.controller.ImageRepository`
- `com.karaik.spmviewer.controller.CanvasController` / `SpmRenderer`
- `com.karaik.spmviewer.controller.MainViewController`（FlowPane 布局、动画控制逻辑）
- `com.karaik.spmviewer.model.Settings`（图像搜索路径、原点模式偏好）

## 自动化测试

- `CanvasControllerTest`：覆盖缓存逻辑、页面外延计算、缺图提示。
- `ImageRepositoryTest`：验证额外搜索路径与缓存复用。
- `AnimationPlanBuilderTest`：校验帧序生成规则。
- `MainViewControllerUiTest`：确保新控件（FlowPane、帧滑块、偏好）渲染成功。

执行：`mvn -q test`

## 后续

- 阶段 2 将引入图层面板与 chip 编辑交互。
- 图像搜索路径的 UI 配置仍待补齐，可在后续阶段集成。
