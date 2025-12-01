# GUI 手工验证清单（Edit 模式）

使用 BHE/BSDX/Clarias 样本，构建后按下列步骤检查（`mvn -q test` 已通过）：

1) 启动与打开样本  
   - 运行应用，打开含 `.spm` 的目录。  
   - 确认窗口图标/标题正常；选择文件浏览页/图像，无崩溃（缺图允许）。

2) 打开 Editor  
   - 右键 SPM → Edit，窗口约 1080x720，图标存在。  
   - 切换 Chip/Page/Hit 复选框：高亮即时显示/隐藏。

3) 缩放/平移一致性  
   - Ctrl+滚轮缩放；+/− 调整；中键拖动画布。  
   - 点击 Fit：视图居中当前页。

4) 精灵导入流程（每种引擎至少一次）  
   - 选中 Chip → 双击（确认对话）→ “Import Sprite...” 选择 PNG。  
   - 预览对话显示旧/新叠加、左上角信息；点击 Apply。  
   - 确认 Chip 显示更新；用 “Match Bounds to Sprite” 同步 dstRect 尺寸（左上角不变）。  
   - Undo/Redo 验证撤销栈。

5) 精灵/图像编辑（笔刷）  
   - 选 Chip → Edit Sprite 涂改并保存，确认变化。  
   - Ctrl+E 打开图片编辑器，绘制/撤销/保存，dirty 标记与渲染正常。

6) Bounds/状态栏  
   - 勾选时显示 Chip/Page 左上角标签与尺寸信息，状态栏显示选中尺寸。  
   - 取消勾选后高亮消失。

7) 保存/导出  
   - Ctrl+S 或 Save SPM：校验通过，文件写入 `tmp/`。  
   - Save Image/Export JSON 也写入 `tmp/`；Export Bounds (SVG) 导出矢量（Chip/Page 边框）。

8) 多模式重复  
   - BHE、BSDX、Clarias 各挑一例：打开 Editor，执行导入/预览/匹配/保存，确保无异常。
