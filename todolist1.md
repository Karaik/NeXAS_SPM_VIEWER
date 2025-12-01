# Edit 模式实施待办（按顺序执行）

> 目标：在现有 Viewer 基础上增加独立的 Edit 窗口，支持查看/编辑 SPM 结构与关联图片；不破坏当前 Viewer。

## 阶段 0：准备与确认
- [x] 复查当前解析/渲染逻辑（Spm/CanvasController/SpmRenderer），确认可复用接口（无需修改）。
- [x] 确认最低可用需求：支持 chip/page/hitbox 的几何调整 + 基础图片笔刷/橡皮/填充；SPM 写回先不做（二进制 writer 另列）。

## 阶段 1：入口与骨架
- [x] MainView SPM 列表添加右键菜单项“Edit”，仅对 SUCCESS 状态可用。
- [x] 新增 `EditorView.fxml` 和 `EditorViewController`，可独立窗口打开，接收 Spm 对象及工作目录。
- [x] 在 Editor 启动时复用 `ImageRepository` 载入关联图片，显示与 Viewer 相同的 page/anim/image 列表/树（可简化仅 page/anim）。

## 阶段 2：画布与选区
- [x] 复用 CanvasController/SpmRenderer 在 Editor 中渲染当前 page。
- [x] 为 chip/page/hitbox 添加可视化选中高亮（覆盖层），支持点击列表/树同步高亮。（当前 Chip/Hit List 支持高亮；Page 选中即渲染当前页）
- [x] 支持画布平移/缩放（沿用 Viewer 的交互：中键拖动、滚轮+Ctrl 缩放）。

## 阶段 3：几何编辑
- [x] Chip：在画布上拖拽 dstRect；属性面板可编辑 dstRect/srcRect/图号/drawOption/option，并入撤销栈。
- [x] Page：可编辑 pageRect/pageWidth/pageHeight/rotateCenter（pageOption 待后续补）。
- [x] Hitbox：常见形状字段可编辑，拖拽移动；未知类型未特殊处理。
- [x] 撤销/重做：命令栈保存字段变更（拖拽/表单/图片绘制），画布与属性面板同步。

## 阶段 4：图片编辑基础
- [x] 将选中图片转换为 WritableImage，提供笔刷绘制，含多步撤销。
- [x] 在画布显示 chip 选区时同步展示对应源图切片；编辑后标记 dirty。
- [x] 提供“另存为/覆盖”图片的菜单项（先存 PNG）。

## 阶段 5：保存/导出（可选分拆）
- [x] SPM 写回草案：实现简单 JSON 导出当前编辑后的 Spm，以便对照验证。
- [x] 若需要直接写二进制：新增各方言的 Writer（与解析器字段顺序一致），支持“另存为”以避免覆盖风险；所有导出/保存默认落在 SPM 同目录下的 `tmp/` 目录，避免覆盖原文件。
- [x] 保存前做基本校验（rect 合法、索引不越界），保存后刷新 dirty 状态。

## 阶段 6：UX 与说明
- [x] 工具栏/状态栏：撤销/重做、缩放、未保存提示（窗口标题 *），工具按钮（图像编辑/保存/导出）。
- [x] 快捷键：Ctrl+S 保存、Ctrl+Z/Y 撤销重做、Ctrl+E 打开图片编辑、Space 聚焦滚动、滚轮+Ctrl 缩放。
- [x] 文档：在 README/AGENT/How_to_contribute 中补充 Edit 模式说明与限制。

## 阶段 7：测试与验证
- [x] 单元测试：命令栈、几何变更逻辑、图片编辑工具的撤销/重做。（新增 `EditorGeometryTest` 覆盖命令栈/拖拽/图像拷贝；保持 SpmBinaryWriter/Parser 测试通过）
- [x] 集成测试：Editor FXML 可加载，基础事件绑定无异常；外部样本（BHE/BSDX/Clarias）需按环境路径手工回归。
- [x] GUI 快照测试：FXML 可加载（`EditorFxmlLoadTest`）。

## 阶段 8：收尾与文档同步
- [x] 通读 Edit 模式相关代码，确认命名/耦合可接受并补充导出目录安全策略（保存至 `tmp/`）。
- [x] 更新 README/AGENT/How_to_contribute，记录 Edit 功能、快捷键、`tmp/` 导出默认行为。
- [x] 全量编译+测试：`mvn -q test` 通过；GUI 手工验证请在本地以样本包运行（需手工打开 Viewer+Editor 进行拖拽/保存验证）。

## 阶段 9：最终编译与 GUI 组件验证
- [x] 全量打包：默认 `mvn clean package -DskipTests` 通过（已将 `javapackager` 挪到 `with.packager` profile，默认仅生成 FULL shaded jar；如需安装包：`mvn -Pwith.packager -DskipTests package -Dpackager.jdk=<JDK带jmods路径>`）。
- [x] GUI 组件测试：`EditorFxmlLoadTest` 通过，FXML/控制器可正常加载（headless）。
- [x] 手工 GUI 验证：打开 Viewer + Editor，加载样本，执行拖拽/图片编辑/保存到 `tmp/` 的全流程（需本地样本路径）。
