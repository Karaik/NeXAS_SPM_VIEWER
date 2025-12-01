# Edit 模式优化待办（按顺序执行）

> 目标：围绕汉化工作流，强化精灵导入/替换、左上角对齐与 bounds 调整，优化视口与操作提示，确保 GUI 可用且编译通过。

## 阶段 0：基线与回归
- [x] `mvn -q test` 保持通过；Editor FXML 可加载，应用可启动（含 icon）。
- [x] 初始窗口尺寸与 Viewer 一致（1080x720 左右），Fit/缩放不放大到全屏。

## 阶段 1：精灵导入/替换流程
- [x] “Edit Sprite” 保持现有裁剪+涂改；“Import Sprite...” 专用于外部 PNG 覆盖选中 Chip 的 `srcRect`，自动更新 `srcRect`/`dstRect`/`chipWidth/Height`。
- [x] 导入前显示预览叠加（左上角为原点），展示源/目标尺寸，确认后写入；导入后支持 Undo/Redo。
- [x] 导入时可选“Align to top-left”开关，明确以左上角原点对齐；超出边界时阻止写入并提示。
- [x] 导入失败/越界的提示需包含目标尺寸与源界限；提供一次性“Cancel/Apply”对话，避免误写。

## 阶段 2：Bounds 调整辅助（汉化重点）
- [x] 勾选 Chip/Page bounds 时，渲染左上角原点、边框和尺寸标签；导入或修改后高亮变化区域。
- [x] 提供“一键 Match bounds to sprite size”按钮：保持左上角不变，将 dstRect 宽高同步为当前 sprite 尺寸；必要时提示同步 page bounds。
- [x] 状态栏展示当前选中对象的 rect 信息、原点位置，便于汉化后精确覆盖。

## 阶段 3：视口与缩放体验
- [x] Fit 基于当前页面 extents 或选中 Chip 居中缩放，并更新滚动位置；显示缩放百分比。
- [x] 支持 +/- 热键调整缩放；保持中键拖动平移；恢复与 Viewer 一致的 Ctrl+滚轮缩放手势。
- [x] 取消勾选 Hit/Chip bounds 时，高亮即时隐藏，不影响画布渲染。

## 阶段 4：提示与引导
- [x] 状态栏/Overlay 提示主要操作：拖拽、导入、撤销、保存到 `tmp/`。
- [x] 双击 Chip 前弹出当前 `srcRect`/目标尺寸提示，防止误操作；错误/越界提示使用英文。
- [x] 增加汉化流程提示：选 Chip → 预览/导入 → Match bounds → 导出到 `tmp/`。

## 阶段 5：稳定性与测试
- [x] 新增/更新单测覆盖：sprite 导入补丁写入、bounds 匹配逻辑（保持 Editor FXML 测试）。
- [x] GUI 烟雾测试：加载 FXML + 最小 Spm 初始化，确认列表选择正常（`EditorWorkflowSmokeTest`）。
- [ ] 每步完成后本地手工验证：加载 BHE/BSDX/Clarias 样本，打开 Editor，执行导入/预览/拖拽/保存（写入 `tmp/`），确保 GUI 可正常打开，无崩溃。（参见 `MANUAL_TESTS.md` 检查清单）

## 阶段 6：文档同步
- [x] README/AGENT 更新：描述精灵导入/对齐流程、左上角原点、bounds 调整、保存到 `tmp/`、新快捷键。
- [x] 如有新控件/按钮/快捷键，补充到界面说明与贡献指南。
