# Clarias 命中体字段说明

本目录复制了 BHE 的命中体实现，便于 Clarias 方言独立维护。shapeType 映射与 BHE 保持一致（见 `ClariasHitboxFactory`），但读取细节有以下差异：

1) **shapeType=2 → CCircle**：根据逆向，中心点后只跳过 4 字节占位，再读取半径；BHE 版本跳过 8 字节。当前实现已按 Clarias 格式覆写 `readInfo`。  
2) **其余 shapeType（0/1/7/8/9/10/11）**：字段顺序与 BHE 相同，渲染逻辑复用父类。若后续样本显示不同布局，可仅在本目录内调整对应类。  
3) **未知 shapeType**：`ClariasHitboxFactory` 记录错误并回退为 `CRect`，保证解析不中断。

| shapeType | Java 类（Clarias）            | 与 BHE 的差异                      |
|-----------|--------------------------------|-----------------------------------|
| 0 / none  | `DefaultHitArea`               | 相同                               |
| 1         | `CRotatableRect`               | 相同                               |
| 2         | `CCircle`                      | 跳过 4 字节占位（BHE 为 8 字节）    |
| 7         | `C2DLineSegment`               | 相同                               |
| 8         | `C2DDot`                       | 相同                               |
| 9         | `CBox`                         | 相同                               |
| 10        | `CRotatableBox`                | 相同                               |
| 11        | `CSphere`                      | 相同                               |

其他参考：BHE 版本的详细字段样本与统计见 `../bhe/README.md`，Clarias 若出现新差异，请在本文件补充。
