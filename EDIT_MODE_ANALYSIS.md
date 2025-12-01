# Edit 模式分析与实施流程（草案）

## 目标与范围
- 在现有 Viewer 外，提供独立的 Edit 界面：右键列表中的 SPM → “Edit” 打开新窗口。
- Edit 界面加载当前 SPM 及关联图片，支持查看与编辑：
  - 画布中展示与 Viewer 一致的 page/anim/chip/hitbox 切换。
  - 可编辑 chip bounds（dst/src）、page bounds/option/中心点、命中体位置/尺寸（按方言字段）。
  - 简易贴图编辑（基础绘制/擦除/填充），编辑结果可保存为新的图片/覆盖原图。
- 与 MainView 逻辑解耦，复用底层模型/渲染能力，避免相互回归。

## 现状概览（相关组件）
- 数据模型：`spm/Spm.java`（嵌套 Page/Chip/HitArea/Anim）；方言解析器：`spm/parser/version/*`。
- 渲染/播放：`controller/CanvasController` + `controller/render/SpmRenderer` + `AnimationPlanBuilder`。
- UI/状态：`MainViewController`、`UiStateController`、`SpmFileHandler`，FXML `MainView.fxml`。
- 图片加载：`ImageRepository`，支持额外搜索路径。

## 设计思路
1) **入口与隔离**
   - 在 MainView 的 SPM 列表添加右键菜单项“Edit”，触发打开新窗口（新 FXML + Controller），传入选中 SPM 数据与所在目录。
   - Edit 界面独立控制器（例如 `EditorViewController`），不复用 MainView 控件，避免状态耦合。

2) **共享基座**
   - 复用 `Spm` 模型、`ImageRepository`、`CanvasController`/`SpmRenderer`（可提炼渲染到共享服务，避免重复绘制代码）。
   - 提取共用的列表/树构建逻辑到可复用的 helper（如 `UiStateBuilder`），MainView 与 Editor 共用。

3) **编辑能力分层**
   - **选择/定位层**：和 Viewer 一样的 page/anim/image 列表，点击后在画布上高亮对应元素。
   - **变换层（SPM 元素）**：
     - Chip：拖拽/缩放 dstRect，编辑 srcRect，编辑 drawOption/option。
     - Page：调整 pageRect、width/height、rotateCenter、pageOption。
     - Hitbox：按 shapeType 展示控件并支持拖拽/输入字段；方言差异用各自的类字段。
   - **图像层（贴图）**：
     - 基础画笔/橡皮/填充/取色，作用于当前 image（WritableImage）；支持撤销栈。
     - 变更后标记“dirty”，保存时导出为 PNG 并替换引用。
   - **属性编辑面板**：表单 + 数值输入框，支持同步画布操作。

4) **保存与导出**
   - SPM 变更：序列化回原格式（新增 Writer，与各方言匹配字段顺序）。可先支持 “导出为 JSON” 过渡，再实现二进制写回。
   - 图片变更：写回文件或另存为；SPM 引用保持文件名。
   - 支持“另存为”以避免破坏原文件；保存前做必需字段校验。

5) **交互与体验**
   - 工具栏：选择工具/移动/缩放/画笔/橡皮/填充/取色，撤销/重做，网格/对齐开关，吸附（整像素/自定义步长）。
   - 状态栏：坐标、缩放、未保存提示。
   - 快捷键：Ctrl+Z/Y 撤销重做，Ctrl+S 保存，空格拖动画布，鼠标滚轮缩放。

6) **测试与验证**
   - 单元：新建 Writer/编辑操作的纯逻辑测试；撤销栈；坐标换算。
   - 集成：加载 BHE/BSDX/Clarias 样本，打开 Edit 界面，进行基本拖拽/保存不抛异常。
   - GUI：保留现有 Viewer 测试，新增 Editor FXML 的加载测试。

## 实施步骤（建议迭代）
1) **准备阶段**
   - 抽取渲染/列表构建的共用 helper，清理 MainView 的硬编码依赖。
   - 为 SPM 模型补充写入器草稿接口（不改现有解析）。
2) **骨架搭建**
   - 新增 `EditorView.fxml` + `EditorViewController`，能打开窗口、加载 SPM+图片、浏览 page/anim。
   - MainView 列表添加右键“Edit”入口（仅当解析成功）。
3) **编辑工具（SPM 结构）**
   - Chip/page/hitbox 的选中、属性表单、画布拖拽同步。
   - 撤销/重做栈（命令模式），保存 dirty 状态。
4) **图片编辑基础**
   - 将当前 image 转 WritableImage，提供画笔/橡皮/填充/取色，附带撤销。
5) **保存与导出**
   - SPM Writer（按方言/版本写回）；图片写回/另存为；保存冲突提醒。
6) **验证与文档**
   - 增加 Editor 使用说明与已知限制；跑全套测试。

## 开放问题（需确认）
- 图片编辑深度：是只需要简易像素级操作，还是需图层/滤镜级别？（当前建议先做基础绘制）
- SPM 写回格式：允许先导出 JSON 过渡，还是必须直接写二进制覆盖原文件？
- 命中体编辑范围：是否需要支持新建/删除 hitbox，还是仅调整现有的几何字段？
- 数据安全：是否需要自动备份原始 SPM/图片到指定目录？

## 预估风险
- SPM Writer 需要严格匹配各方言字段顺序，易引入兼容性问题；建议先以 JSON 导出验证，再写二进制。
- 画布编辑与渲染公用代码耦合，需谨慎抽象以避免影响现有 Viewer。
- 大批量图片转 WritableImage 可能占用内存，需懒加载与释放策略。*** End Patch
