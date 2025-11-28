# hitarea 结构字段说明

本目录下的 `SPMHitArea` 子类用于把 SPM 文件中的碰撞区域数据映射成可视化对象。字段含义的确认基于：

1. 对照 `src/main/resources/spmBheJson` 目录下解包得到的 JSON 样本；
2. 结合 `com.karaik.spmviewer.spm.hitarea.bhe.BheHitboxFactory` 的 shapeType 映射与各解析器的读写顺序；
3. 对尚未在样本中出现的类型记录推断依据与当前统计结果。

> **样本统计**：遍历 `spmBheJson` 共发现 shapeType=1 的记录 235,801 条、shapeType=10 的记录 5,002 条、缺省（无 shapeType 字段，对应旧版默认矩形）的记录 136 条，其余 shapeType 目前均为 0 条。

## shapeType = 0 → `DefaultHitArea`
- JSON 示例：`feiselect.spm.json` `pageData[0].hitRects[0]`
  ```json
  {"xMin":0,"xMax":-239,"yMin":4,"yMax":-235,"zMin":0,"zMax":0,...}
  ```
- 按顺序读取 `xMin → xMax → yMin → yMax → zMin → zMax`，构成轴对齐包围盒。
- 在 GUI 中绘制 XY 投影与 `dstRect` 完全重合，说明字段解释正确。

## shapeType = 1 → `CRotatableRect`
- JSON 示例：`sou.spm.json` `pageData[0].hitRects[1]`
  ```json
  {"int1":-22,"int2":6,"int3":4,"int4":4,...}
  ```
- `int1/int2` 随动画帧平移，可视为矩形中心；`int3/int4` 恒正，对应宽/高。
- `attrU16` 目前均为 0，推测用于旋转或额外标志。

## shapeType = 7 → `C2DLineSegment`
- 当前 JSON 样本中暂未出现（统计 0 条）。
- 解析流程读取 `(x1,y1) → (x2,y2)` 后跟保留位，预计用于射线或拖尾碰撞。

## shapeType = 8 → `C2DDot`
- 当前 JSON 样本中暂未出现（统计 0 条）。
- 结构为单点坐标 + 16 字节保留位，用于标记单点检测。

## shapeType = 9 → `CBox`
- 当前 JSON 样本中暂未出现（统计 0 条）。
- 连续读取 6 个整数，推断为 3D 包围盒的 `min/max` 六个坐标（与 LegacyRectHitArea 的扩展形式一致）。

## shapeType = 10 → `CRotatableBox`
- JSON 示例：`freja.spm.json` `pageData[1210].hitRects[0]`
  ```json
  {"int1":0,"int2":48,"int3":88,"int4":96,"int5":96,"int6":208,"int7":0,"int8":0}
  ```
- 与其它帧对比：`int1/2/3` 为中心 `(centerX, centerY, centerZ)`，`int4/5/6` 为尺寸 `sizeX/sizeY/sizeZ`。
- GUI 中绘制 `sizeX/sizeY` 的投影矩形，并在旁标注 `z±sizeZ/2`，与原作护罩范围吻合。

## shapeType = 11 → `CSphere`
- 当前 JSON 样本中暂未出现（统计 0 条）。
- 解析顺序为三坐标 + 半径，与 `CCircle` 的二维版本完全对应。

## 旧版结构 → `LegacyRectHitArea`
- 用于 VER-2.02、bsdX 等旧格式，解析器中已验证字段顺序。
- GUI 直接采用 `hitRect` 边界绘制矩形。

## 汇总表

| shapeType | Java 类              | 核心字段解释                        | 例证来源 / 条数 |
|-----------|----------------------|-------------------------------------|-----------------|
| none/0    | `DefaultHitArea`     | `xMin/xMax/yMin/yMax/zMin/zMax`     | `feiselect.spm.json`（136 条） |
| 1         | `CRotatableRect`     | `center(x,y)` + `size(width,height)`| `sou.spm.json`（235,801 条） |
| 2         | `CCircle`            | `center(x,y)` + `radius`            | 读取顺序推断（当前 0 条） |
| 7         | `C2DLineSegment`     | `(x1,y1)` → `(x2,y2)`               | 读取顺序推断（当前 0 条） |
| 8         | `C2DDot`             | 单点坐标                            | 读取顺序推断（当前 0 条） |
| 9         | `CBox`               | `min/max` 三维角点                   | 结构推断（当前 0 条） |
| 10        | `CRotatableBox`      | `center(x,y,z)` + `size(x,y,z)`      | `freja.spm.json`（5,002 条） |
| 11        | `CSphere`            | `center(x,y,z)` + `radius`           | 读取顺序推断（当前 0 条） |

当前数据中 shapeType 仅出现 none/0、1 与 10，其余类型尚未在 `spmBheJson` 目录中观测到；随着更多 SPM 样本加入，可继续补充截图及数值比对，逐步确认尚未验证的类型。

