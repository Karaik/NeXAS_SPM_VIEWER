# 测试补充说明

- `SpmParserSmokeTest`：构造最小 SPM 二进制数据，验证 BHE / BSDX 两类方言关键字段（页面、图片、动画、HitArea）。
- `CanvasControllerTest`：
  1. 检查图片缓存与索引顺序保持；
  2. 断言画布在不同页面尺寸之间不会收缩；
  3. 校验 `PageExtents` 计算结果；
  4. 重用原有缺图提示、文本换行等回归检查。
- `ImageRepositoryTest`：覆盖额外搜索路径、缓存复用两个场景。
- `AnimationPlanBuilderTest`：验证动画帧序生成及 `waitFrame` 逻辑。
- `MainViewControllerUiTest`：
  1. “Auto Play” 偏好同步；
  2. FlowPane 工具栏、原点切换、帧滑块等新控件完整呈现；
  3. 首次加载时帧滑块保持禁用状态（0/0）。
- `SpmFileHandlerIntegrationTest`：使用 `D:/BDY/bsdx_bhe/bsdx_resources` 与 `D:/BDY/bsdx_bhe/bhe_resources` 模拟快速切换目录，确保结果列表只保留最后一次加载。

执行：`mvn -q test`

> 集成测试依赖本地资源目录，请确认路径可访问。
