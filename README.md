# Helpful Tweake

基于 [MaLiLib](https://github.com/sakura-ryoko/malilib) 的客户端端 Minecraft 辅助模组，提供实用工具与优化。

## 依赖

Fabric API、MaLiLib

## 功能

### 更好的自动跳跃

- **效果**：让玩家平滑走上方≤1.25格高的方块（原版玩家跳跃高度），保持水平速度。与原版自动跳跃（触发真实跳跃并重置水平速度）不同，本功能仅抬高玩家的台阶高度，因此不损失速度。
- **默认状态**：关闭
- **默认热键**：无
- **灵感来源**：[Accessible Step](https://github.com/SecretOnline/accessible-step)（作者 SecretOnline）。本实现为基于相同原理的独立重写，未照搬原代码。

### 全局经验修补

- **效果**：玩家拾取经验球时，装备栏、主副手、快捷栏、背包内所有附有经验修补且已损坏的物品都会参与修复，不需要拿在手上。
- **默认状态**：关闭
- **默认热键**：无

### 更好的不死图腾

- **效果**：玩家受到致命伤害触发图腾时，装备栏、主副手、快捷栏、背包内所有带"死亡保护"组件的物品（即不死图腾）都会参与触发，不需要拿在手上。与原版一致，带 `BYPASSES_INVULNERABILITY` 标签的伤害（如 `/kill`）仍不可被图腾抵抗，保证不超模。
- **默认状态**：关闭
- **默认热键**：无

## 许可证

MIT — Copyright (c) 2026 Tanjikun. 见 [LICENSE.txt](LICENSE.txt)。