# Show Chip Bounds 与 Show Page Bounds 使用说明

> 适用于最新版 NeXAS SPM Viewer（阶段 1 预览优化）。

## Switch：Show Chip Bounds

- 位置：顶部工具栏 > Debug Controls 组。
- 功能：在画布上为每一个 chip（`chipData[].dstRect`）绘制半透明的蓝色矩形轮廓，便于确认 chip 的实际摆放位置、尺寸与层级。
- 使用场景：
  - 页面中存在多个尺寸非常接近的 chip，需要验证是否出现错位或被覆盖；
  - 动画帧之间对比 chip 的移动；
  - 编辑阶段核对 chip 的 `dstRect` 是否正确。
- 效果：勾选后，画布上将出现淡蓝色的矩形框（带轻微透明度），移动/切换页面时会自动刷新。

```
补充说明

Show Page Bounds：显示当前页面的“基准边界”，取自 pageRect（若无则用 pageWidth/pageHeight，再不行就 fallback 到所有 chip 的包围盒）。这个橙色框代表整张页面的逻辑范围，相当于“本体图片/画布”的边界，用来确认页面是否居中、是否被裁切。

Show Chip Bounds：显示每一个 chip 的 dstRect（贴到页面上的矩形），因此当一个页面由多块图片拼合（多层装饰、前景/背景、光效等）时，就会看到多个淡蓝框，每个框对应一块贴图在页面中的位置与大小。

```

## Switch：Show Page Bounds

- 位置：同上，与 Show Chip Bounds 并列。
- 功能：勾选后，将绘制页面边界（橙色轮廓）。优先使用 `pageRect`；如果缺失，则回退 `pageWidth/pageHeight`；若两者均为空，会使用芯片的最小包围盒。
- 使用场景：
  - 页面数据缺乏 `pageRect` 字段时，快速确认真实可视范围；
  - 辅助选择 Origin Mode（Center / Top-Left）时查看页面在画布上的定位；
  - 分析大尺寸 UI（如地图、界面背景），确保页面整体没有被裁切。
- 效果：勾选后，画布中心会显示橙色带透明的矩形边框，标记当前页面的理论范围。

## 搭配 Origin Mode

- Origin 选择器位于 toolbar，在 Center 模式下页面以旋转中心居中；Top-Left 模式则将页面左上角对齐画布中部。
- Show Page Bounds 对应 origin 计算，方便观察哪种模式更符合原始 UI。

## 使用建议

1. 开启 Show Page Bounds，观察当前页面的橙色边框是否与实际内容一致；
2. 切换 Show Chip Bounds，对不同 chip 的分布、尺寸一目了然；
3. 配合动画步进（Step < / Step >）逐帧查看，快速定位异常；
4. 遇到页面加载缺图或 chip 缺失时，可结合“中心缺图提示”定位问题。

> 若在某些 SPM 中未见明显变化，可能是页面数据本身缺乏 `pageRect`，或所有 chip 重叠在同一位置。可尝试 `crowd.spm.json`、`effect.spm.json` 等包含复杂布局的项目验证效果。

## 示例数据建议

| 用途 | 文件 | 查看方式 |
|------|------|----------|
| Show Chip Bounds | `spmBheJson/akusa_a101ak.spm.json` | 选择动画 `[AKUSA_A101KT]` 的任意帧（如第 11 页）。角色主体与前景部件分属不同 chip，开启开关后可看到两个蓝色外框分别包住身体和前景装饰。 |
| Show Chip Bounds | `spmBheJson/benetta_u101kt.spm.json` | 页面索引 1（动画 `笑顔`）包含背景 + 前景灯板两个 chip，勾选后画面会显示两块淡蓝矩形堆叠。 |
| Show Page Bounds | `spmBheJson/427_aki_docment.spm.json` | 任意页面。该文档类 UI 设置了 `pageRect`，橙色边界与内容等大，适合确认页面对齐。 |
| Show Page Bounds + Origin 切换 | `spmBsdxJson/crowd_023.spm.json` | 页面尺寸很大（背景 + 装饰），切换 Origin 为 `Top-Left` 后，橙色边框会移动至画布中心左上，有助于理解页面原点。 |
| 两者结合 | `spmBheJson/bomb.spm.json` | 部分页含多个 chip（主体/爆炸层），同时也设置了 pageRect。勾选两个开关可同时看到蓝色 chip 框与橙色页面边界。 |

> 若在列表中未看到上述文件，可通过“Open Directory...” 选择 `src/main/resources/spmBheJson` 或 `spmBsdxJson` 目录进行浏览。