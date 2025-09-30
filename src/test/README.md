# 测试补充说明

- `CanvasControllerTest` 新增三项用例：
  1. 验证画布在不同页面尺寸之间切换时不会收缩，确保动画播放时中心稳定。
  2. 通过反射测试 `wrapLine` 的换行逻辑，防止缺图提示被裁切。
  3. 采样预览画布像素，确认缺图提示框宽度不超过画布宽度的 80%。
- `SpmFileHandlerIntegrationTest` 使用实际资源目录（`D:/BDY/bsdx_bhe/bsdx_resources` 与 `D:/BDY/bsdx_bhe/bhe_resources`）模拟用户快速切换目录，验证结果列表只保留最后一次加载的目录并且进度条正确收起。
- `MainViewControllerUiTest`：
  1. 校验“Auto Play”复选框与用户偏好同步，切换后立即写回设置。
  2. 确认工具栏使用带换行能力的 `FlowPane`，缓解顶部控件拥挤问题。

执行集成测试前，请确保上述资源目录存在且可访问。
