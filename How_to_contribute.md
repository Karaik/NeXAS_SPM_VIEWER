# How to Contribute: 新增游戏 / 引擎 / 方言 支持指南

本项目用于解析并可视化 NeXAS 系列的 `.spm` 资源。当前支持 BHE、BSDX 两类引擎，并按文件头自动识别 2.00/2.02 版本。若需要接入新的游戏/引擎/方言，请按以下步骤进行。

## 1. 了解现有架构
- 数据模型：`src/main/java/com/karaik/spmviewer/spm/Spm.java`（嵌套 Page/Chip/HitArea/Anim 数据类）。
- 解析入口：`spm/parser/SpmParser.java`，先读取文件头，随后通过 `SpmParserFactory` 根据“引擎模式 + 版本字符串”选择具体方言解析器。
- 现有方言解析器：`spm/parser/version/BheV200Parser/BheV202Parser/BsdxV200Parser/BsdxV202Parser`。
- 命中体工厂：`spm/parser/HitboxFactory`（BHE 多态，根据 shapeType 创建）；BSDX 由解析器直接构造 `LegacyRectHitArea`。
- UI/渲染：基于 `Spm` 嵌套类的字段（Page/Chip/HitArea/Anim）进行展示与绘制。

## 2. 新增引擎/方言的基本步骤
1) **确定解析模式标识**  
   - 在 `Settings.ParsingMode` 中增加新的引擎枚举值（如果与现有 BHE/BSDX 不同）。  
   - 若仅是现有引擎的新版本，可沿用同一枚举，解析器内部根据文件头识别版本。

2) **实现方言解析器**  
   - 在 `spm/parser/version/` 下新增解析器类（例如 `NewEngineV300Parser`）。  
   - 实现 `SpmDialectParser` 接口：`Spm parse(BinaryReader reader, String spmVersion)`。  
   - 按文件结构读取并填充 `Spm` 的嵌套数据类（Page/Chip/HitArea/Anim）；必要时扩展 `Spm` 的字段或嵌套类。  
   - 若命中体有多态结构，考虑复用 `HitboxFactory` 或新增工厂方法；若是固定结构，可直接构造自定义 `SPMHitArea` 子类。

3) **在工厂中注册**  
   - 更新 `SpmParserFactory`：根据新的 `ParsingMode`（或引擎+版本的组合规则）返回新解析器实例。  
   - 若需要自动区分子版本（类似 2.00/2.02），在工厂内根据 `spmVersion` 字符串判断。

4) **补充 UI 选择项**  
   - `MainViewController` 的解析模式下拉会自动读取 `Settings.ParsingMode`，确保新增枚举即可出现在下拉列表。  
   - 若有特殊 UI 展示需求（例如新的命中体类型绘制），在 `spm/hitarea` 增加对应的 `SPMHitArea` 子类，并实现 `drawSelf/getDisplayInfo`。

5) **添加/更新测试**  
   - 为新方言添加单元/集成测试，类似 `SpmParserSmokeTest` 与 `SampleParseIntegrationTest`。  
   - 如有样本目录，可在集成测试中遍历解析，确保无异常。测试需可在样本缺失时自动跳过。

6) **更新文档**  
- README：增加新解析模式说明、方言解析器列表；更新编辑模式的新行为（导出默认写入 `tmp/` 目录）。  
- AGENT.md：补充目录结构、解析模式说明。  
- 若扩展 `Spm` 嵌套类字段，在 `Spm结构说明.md` 中注明。  
- 编辑模式相关：若扩展新工具或快捷键，请在 README/AGENT 记录；新增保存/导出流程时，保持基础校验（图号范围、rect 合法）以防数据损坏，且默认写入 `tmp/` 避免覆盖原始资源。

## 3. 解析器实现提示
- **版本识别**：通常文件头形如 `"SPM VER-2.00"`/`"SPM VER-2.02"`，可用 `String.contains("2.02")` 之类的简易判断；若新格式不同，请解析头部字段并在工厂内处理。
- **矩形/命中体**：  
  - BHE 多态命中体使用 `HitboxFactory.createHitbox(shapeType)`，返回 `SPMHitArea` 子类并调用 `readInfo`。  
  - BSDX 采用固定矩形结构 `LegacyRectHitArea`，字段顺序需与实际格式匹配。  
  - 新方言可按需要新增命中体类，保持 `drawSelf` 用于渲染叠加。
- **动画数据**：保持 `SPMAnimData`/`SPMPatData` 的读取顺序与字段宽度一致（部分方言的 `numPat` 需要与 `0xFFFF` 与操作）。
- **健壮性**：遇到异常建议 catch 后返回默认空对象，避免 GUI 崩溃（参见现有解析器的日志处理）。

## 4. 验证清单
- [ ] 新增方言解析器可编译，并被 `SpmParserFactory` 选中。  
- [ ] 样本 `.spm` 解析无异常，生成的 `Spm` 数据可在 GUI 中正确展示页面/动画/命中体。  
- [ ] `mvn -Dmaven.test.skip=false -DskipTests=false test` 通过（默认有 1 条跳过测试）。  
- [ ] README/AGENT/Spm结构说明 已更新相关描述。  

## 5. 常用路径
- 解析入口：`src/main/java/com/karaik/spmviewer/spm/parser/SpmParser.java`
- 方言工厂：`src/main/java/com/karaik/spmviewer/spm/parser/SpmParserFactory.java`
- 命中体工厂：`src/main/java/com/karaik/spmviewer/spm/parser/HitboxFactory.java`
- 命中体实现：`src/main/java/com/karaik/spmviewer/spm/hitarea/`
- UI 入口：`src/main/java/com/karaik/spmviewer/controller/MainViewController.java`
- 渲染：`src/main/java/com/karaik/spmviewer/controller/render/SpmRenderer.java`

欢迎提交 PR 或 issue，一起扩展更多引擎/版本支持！ 
