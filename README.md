# Helpful Tweake

---

基于 [MaLiLib](https://github.com/sakura-ryoko/malilib) 的客户端端 Minecraft 辅助模组，提供实用工具与优化。

## 依赖

---

Fabric API、MaLiLib

## 功能

---

### 更好的自动跳跃

- **效果**：让玩家平滑走上方≤1.25格高的方块（原版玩家跳跃高度），保持水平速度。与原版自动跳跃（触发真实跳跃并重置水平速度）不同，本功能仅抬高玩家的台阶高度（step height），由引擎内置的碰撞抬升逻辑完成上台阶，因此不损失速度。
- **默认状态**：关闭
- **默认热键**：无
- **灵感来源**：[Accessible Step](https://github.com/SecretOnline/accessible-step)（作者 SecretOnline）。本实现为基于相同原理的独立重写，未照搬原代码。

## 许可证

---

MIT — Copyright (c) 2026 Tanjikun. 见 [LICENSE.txt](LICENSE.txt)。