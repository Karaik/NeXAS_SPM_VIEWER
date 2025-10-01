# SPM 结构说明

> 适用于 `com.karaik.spmviewer.spm.Spm` 模型，对应 BHE / BSDX / V202 三种方言的公共字段。

## 顶层结构

| 字段 | 说明 | 关联 |
|------|------|------|
| `spmVersion` | SPM 文件版本号，例如 `"VER-2.00"`、`"VER-2.02"` | 决定采用的解析方言 (`SpmParserFactory`) |
| `numPageData` | 页面数量，与 `pageData` 数组长度一致 | 每个页面描绘一帧或一个 UI 子画面 |
| `pageData` | `List<SPMPageData>`，包含页面布局、命中框、芯片列表 | 渲染引擎的核心输入 |
| `numImageData` | 素材表数量 | 与 `imageData` 对应 |
| `imageData` | `List<SPMImageData>`，记录 spritesheet 文件名 | 与磁盘上的 PNG 对应，加载后供 chip 引用 |
| `patPageNum` | 动画帧序列长度 (每个 `patData` 中 pageNo 的槽位数量) | 用于解析 `SPMPatData` |
| `numAnimData` | 动画条目数量 | 与 `animData` 对应 |
| `animData` | `List<SPMAnimData>`，记录动画名称、帧序列表 | `MainViewController` 的动画选择器依赖此列表 |

### 示例：`kou.spm.json`

- `spmVersion`: `"VER-2.00"`
- `numPageData`: `90`，意味着存在 90 个不同姿态/帧。
- `numImageData`: `1`，所有 chip 均来源于 `"kou_0.png"`（示例名称）。
- `numAnimData`: `10`，例如动画 `"[0] 立ち"` 代表站立帧序。

## `SPMPageData`

描述单个页面（frame）的布局。

| 字段 | 说明 | 关联 |
|------|------|------|
| `numChipData` | 当前页面包含的 chip 数量 | 与 `chipData` 长度一致 |
| `pageWidth` / `pageHeight` | 页面逻辑尺寸（可能为 0，依赖 `pageRect`） | 计算页面边界与缩放 |
| `pageRect` | 左上角为 (left, top)、右下角为 (right, bottom) 的矩形 | 渲染器用来绘制 Page Bounds |
| `pageOption` | 页面选项标志（常见为 0） | 后续功能可读取特殊标志 |
| `rotateCenterX` / `rotateCenterY` | 页面旋转中心（也是中心原点） | 渲染时决定 chip 的位置对齐 |
| `hitFlag` | 32-bit 位掩码，指示激活的命中框数量 | 控制 `hitRects` 解析 |
| `hitRects` | `List<SPMHitArea>`，根据 `hitFlag` 与方言解析 | 用于“Show Hitboxes” 叠加显示 |
| `chipData` | `List<SPMChipData>`，按顺序叠放 | 画布绘制时逐个组合出页面 |

**示例：** `kou.spm.json` 的第 3 页（索引 2）

```json
{
  "numChipData": 4,
  "pageRect": {"left": -128, "top": -128, "right": 128, "bottom": 128},
  "rotateCenterX": 0,
  "rotateCenterY": 0,
  "chipData": [ ... ]
}
```

- 页面以中心 (0,0) 为原点，`pageRect` 对应棋盘背景的橙色框。
- `numChipData = 4`，代表该姿态由 4 个贴片组合而成（例如角色身体、阴影）。
- 当勾选“Show Page Bounds” 时可看到 `[-128,-128 → 128,128]` 的橙色矩形。

## `SPMChipData`

描述单个贴片（chip）的来源和目标区域。

| 字段 | 说明 |
|------|------|
| `imageNo` | 指向 `imageData` 中的索引，决定使用哪张 spritesheet |
| `dstRect` | 贴片放置在页面坐标系中的矩形范围（左、上、右、下） |
| `chipWidth` / `chipHeight` | 贴片原始宽高（可能与 `dstRect` 大小一致） |
| `srcRect` | 在 spritesheet 上截取的区域 |
| `drawOption` / `drawOptionValue` | 绘制标志与参数，当前版本作为透明传递 |
| `option` | 其他选项值（示例中常为 0） |

**示例：** `kou.spm.json` 页面的第一块 chip：

```json
{
  "imageNo": 0,
  "dstRect": {"left": -64, "top": -128, "right": 64, "bottom": 32},
  "srcRect": {"left": 0, "top": 0, "right": 128, "bottom": 160}
}
```

- `imageNo = 0` 对应 spritesheet `kou_0.png`。
- `dstRect` 表示贴片在页面上的位置（左上角 (-64,-128)，宽 128，高 160）。
- `srcRect` 从 spritesheet 截取角色身体区域；勾选 “Show Chip Bounds” 可看到淡蓝外框。

## `SPMHitArea`

命中区域（Hitbox），根据方言存在不同实现。

- BHE (多态)：使用 `shapeType` 与 `HitboxFactory` 创建对应形状，如 `DefaultHitArea` / `CRotatableRect` 等。
- BSDX (固定结构)：`LegacyRectHitArea`，字段顺序不同。

**示例：** `kou.spm.json` 部分页面 `hitFlag` 为 0，表示不含命中框；`bomb.spm.json` 则存在多个 `DefaultHitArea`。

## `SPMAnimData` 与 `SPMPatData`

| 字段 | 说明 |
|------|------|
| `animName` | 动画名称，用于 UI 列表（如 `"[1] 步き1"`） |
| `numPat` | 帧段数量 |
| `patData` | `List<SPMPatData>`，每个段包含 `waitFrame` 与 `pageNo[]` |
| `waitFrame` | 每帧持续时间（单位：帧），在 Viewer 中结合 FPS 计算时长 |
| `pageNo` | 指向 `pageData` 的索引，决定播放顺序 |

**示例：** `kou.spm.json` 的动画 `[1] 步き1`：

- `patData` 中 `pageNo` 依次为 `[1, 2, 3, 4, ...]`，`waitFrame` 均为 `2`。
- Viewer 中选择该动画并点击 “Auto Play”/“Play” 即可看到动态效果；`animFrameSlider` 允许逐帧查看。

## 结构关联总结

```
SPM
├── imageData[n] --> spritesheet 文件名
├── pageData[m]
│   ├── chipData[k] --> 引用 imageData / srcRect / dstRect
│   └── hitRects[]  --> 根据 hitFlag 与方言解析
└── animData[a]
    └── patData[p] (waitFrame, pageNo[] 引用 pageData)
```

- `pageData` 与 `imageData` 决定静态页面的视觉呈现；
- `animData` 将若干 `pageData` 串联成动画序列；
- `hitRects` 提供交互检测（碰撞、点击等）；
- `Settings` 中的 Origin Mode / Debug Switch 辅助调试这些数据。

## 推荐验证数据

| 文件 | 关键特性 |
|------|----------|
| `kou.spm.json` | 大量角色姿态、动画序列，`pageRect`/`dstRect` 配置完整 |
| `bomb.spm.json` | 命中框丰富，可搭配 “Show Hitboxes” 检查 |
| `effect.spm.json` | 动画帧较多，适合测试动画步进与帧滑块 |

使用 Viewer 打开这些示例，可直观看到本文描述的字段作用。
