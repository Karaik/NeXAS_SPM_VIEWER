# 重构与验证待办清单

> 目标：拆分 SPM 顶层模型与解析器，使 BHE / BSDX / V202 版本各自独立实现；每个阶段完成后确保项目能编译、测试通过（无编译错误）。

- [x] 阶段 0：基线确认  
  - 记录当前分支状态（未改代码）。  
  - 跑一次构建：`mvn -Dmaven.test.skip=false -DskipTests=false clean test`，确认现状（pom 默认跳测，需显式关闭）。

- [x] 阶段 1：核心抽象搭建  
  - 在 `com.karaik.spmviewer.spm.core` 下新增 `SpmBase`（抽象基类，含版本号、动画/页面/图片访问器），`PageDataBase`、`ChipDataBase`、`HitAreaBase`、`AnimDataBase` 接口（含渲染需要的 getter、`drawSelf`/`getDisplayInfo`）。  
  - 为渲染用字段定义最小访问契约（page rect、中心点、chip src/dst、hitbox 绘制）。  
  - 编译校验。

- [x] 阶段 2：BHE 模型与解析器拆分  
  - 按 `src/main/resources/example/bhe/spm/Spm.java` 拆出 `bhe.v200`、`bhe.v202` 模型（各自 extends SpmBase），命中体沿用现有形状类实现 `HitAreaBase`。  
  - 基于 example 的 parser 逻辑落地 `Bhe200Parser`、`Bhe202Parser`（独立文件，不继承彼此），保留 hitbox 工厂、V202 额外字段读取。  
  - 本阶段编译/测试。

- [x] 阶段 3：BSDX 模型与解析器拆分  
  - 按 `src/main/resources/example/bsdx/spm/Spm.java` 拆出 `bsdx.v200`、`bsdx.v202` 模型，命中体使用新版 `LegacyRect` 实现 `HitAreaBase`。  
  - 实现 `Bsdx200Parser`、`Bsdx202Parser`（固定矩形结构，V202 读 `unk3/unk5`）。  
  - 本阶段编译/测试。

- [x] 阶段 4：解析工厂与设置接线  
  - 更新 `Settings.ParsingMode` 为四档（BHE 2.00、BHE 2.02、BSDX 2.00、BSDX 2.02），同步 UI 下拉显示。  
  - `SpmParserFactory` 改为返回新 parser；移除旧的基类复用。  
  - `SpmParser` 改为持有 `SpmBase` 泛型，调用 `AbstractSpmParser` 模板完成顶层读取。  
  - 编译/测试。

- [x] 阶段 5：UI/渲染适配  
  - `CanvasController`、`SpmRenderer`、`UiStateController`、`MainViewController` 使用接口类型 (`SpmBase`/`PageDataBase`/`ChipDataBase`/`HitAreaBase`) 取数与绘制，移除对旧内嵌类的直接依赖。  
  - 确认 hitbox `drawSelf` 仍按原逻辑工作；表格列绑定更新为接口 getter。  
  - 本阶段编译/测试。

- [x] 阶段 6：兼容性收尾  
  - 检查 `HitboxFactory` 适配新接口；必要时提供 BSDX 专用绘制（若需区分样式）。  
  - 清理不再使用的旧类/字段；更新结构说明文档。  
  - 编译/测试。

- [ ] 阶段 7：示例与验证  
  - 用 `src/main/resources/example` 下的样例跑 4 种模式，确认页面/动画/命中体显示正常。  
  - 记录手工验证要点（截图或描述）。  
  - 最终 `mvn -Dmaven.test.skip=false -DskipTests=false clean test`。

- [x] 阶段 8：文档与交付  
  - 更新 README/结构说明，简述新包结构与模式选择。  
  - 确认根目录 Agent 文档同步最新信息。  
  - 再次编译/测试，准备提交。
