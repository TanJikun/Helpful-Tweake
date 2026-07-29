# Helpful Tweake

基于 [MaLiLib](https://github.com/sakura-ryoko/malilib) 的客户端端 Minecraft 辅助模组，提供实用工具与优化。

## 依赖

Fabric API、MaLiLib

## 功能

### 更好的自动跳跃

- **效果**：自动走上≤1.25格高的方块时不会损失速度。原版自动跳跃会触发真实跳跃导致减速，本功能改为直接抬脚迈上去，走过去时速度不变。
- **默认状态**：关闭
- **默认热键**：无
- **灵感来源**：[Accessible Step](https://github.com/SecretOnline/accessible-step)（作者 SecretOnline）。本实现为基于相同原理的独立重写，未照搬原代码。

### 全局经验修补

- **效果**：拾取经验球时，背包里所有带经验修补且损坏的物品都会一起修复，不需要拿在手上。潜影盒等容器内的物品不受影响。
- **默认状态**：关闭
- **默认热键**：无

### 更好的不死图腾

- **效果**：受到致命伤害时，背包里所有不死图腾都有机会触发救命，不需要拿在手上。`/kill` 等强制伤害仍无法被图腾抵抗，与原版一致。
- **默认状态**：关闭
- **默认热键**：无

### 更好的攀爬

- **效果**：在梯子、藤蔓等可攀爬方块上时，根据视角方向改变攀爬速度。向下看会越爬越快，向上看会越爬越快。持续爬行 3 秒后速度会进一步提升。
- **默认状态**：关闭
- **默认热键**：无
- **灵感来源**：[better-climbing](https://github.com/artemisSystem/better-climbing)（作者 artemisSystem）。速度阈值与分阶段计时数据来源于该项目。

### 更好的船

- **效果**：开启后船只可以越过一定高度的障碍（如台阶、半砖）。在配置界面中点击功能左侧的 +/- 按钮可展开设置抬升高度，高度越高能越过的障碍越高。
- **默认状态**：关闭
- **默认热键**：无
- **灵感来源**：[Better Boat Movement](https://modrinth.com/mod/better-boat-movement)（作者 btwonion）。

## 许可证

MIT — Copyright (c) 2026 Tanjikun. 见 [LICENSE.txt](LICENSE.txt)。