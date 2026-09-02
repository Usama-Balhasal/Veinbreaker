<div align="center">

# ⛏️ VeinBreaker

**Mine entire ore veins, fell whole trees, and harvest crop patches — all with a single block break.**

[![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)](https://github.com/Usama-Balhasal/Veinbreaker/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.x-green?style=for-the-badge)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/license-MIT-purple?style=for-the-badge)](LICENSE)
[![API](https://img.shields.io/badge/API-Spigot%20%2F%20Paper-orange?style=for-the-badge)](https://hub.spigotmc.org/)

*A lightweight, highly configurable quality-of-life plugin built for survival servers.*

[🌐 Website](https://www.iceforge.world/) · [🐛 Report a Bug](https://github.com/Usama-Balhasal/Veinbreaker/issues) · [💡 Request a Feature](https://github.com/Usama-Balhasal/Veinbreaker/issues)

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| ⛏️ **Vein Mining** | Break an entire connected ore vein with one block break |
| 🌲 **Tree Felling** | Chop down whole trees — logs and leaves — instantly |
| 🌾 **Crop Harvesting** | Harvest entire patches of fully-grown crops in one swing |
| 🌱 **Auto-Replant** | Optionally replant crops automatically after harvest |
| ✨ **XP Drops** | Vanilla-accurate XP orbs dropped per ore block (configurable) |
| 🔧 **Tool Enforcement** | Require the correct tool (pickaxe/axe/hoe) for each mode |
| ⏱️ **Cooldowns** | Per-feature cooldowns to prevent spam |
| 🌍 **World Blacklist** | Disable VeinBreaker in specific worlds |
| 🎨 **Color Messages** | Fully customisable messages with `&` colour code support |
| 🔊 **Sound Feedback** | Configurable toggle sounds |
| 🔑 **Multi-Permission** | Supports multiple configurable permission nodes |
| ♻️ **Live Reload** | Reload the config without restarting the server |
| 🧠 **Safe BFS Engine** | Crash-proof block scanning with configurable size limits |
| 🌿 **Fortune & Silk Touch** | Full enchantment support — drops respect all tool enchants |
| 🔨 **Tool Durability** | Tool takes proper durability damage (Unbreaking respected) |

---

## 📦 Installation

1. Download the latest `VeinBreaker-x.x.x.jar` from the [Releases](https://github.com/Usama-Balhasal/Veinbreaker/releases) page.
2. Drop it into your server's `/plugins/` folder.
3. Restart or reload your server.
4. Edit `plugins/VeinBreaker/config.yml` to your liking.
5. Run `/vb reload` to apply changes without a restart.

**Requirements:**
- Minecraft **1.21.x**
- Spigot or Paper (Paper recommended for best performance)
- Java 21+

---

## 🎮 Commands

All commands use `/veinbreaker` or the alias `/vb`.

| Command | Permission | Description |
|---|---|---|
| `/vb` | `veinbreaker.use` | Toggle VeinBreaker on/off |
| `/vb toggle` | `veinbreaker.use` | Alias for toggle |
| `/vb help` | `veinbreaker.help` | Show all commands and usage |
| `/vb status` | `veinbreaker.use` | Show current state and feature settings |
| `/vb reload` | `veinbreaker.reload` | Reload `config.yml` live |
| `/vb permission add <node>` | `veinbreaker.admin` | Add a permission node to the use list |
| `/vb permission remove <node>` | `veinbreaker.admin` | Remove a permission node from the use list |

---

## 🔑 Permissions

| Permission | Default | Description |
|---|---|---|
| `veinbreaker.use` | OP | Toggle and use VeinBreaker |
| `veinbreaker.admin` | OP | All admin commands |
| `veinbreaker.reload` | OP | `/vb reload` |
| `veinbreaker.help` | Everyone | `/vb help` |

> **Tip:** You can add any custom permission node to the `permissions.use` list in `config.yml` so VeinBreaker integrates with your existing permission system.

---

## ⚙️ Configuration

The full `config.yml` is generated automatically on first run. Every option is documented inline.

```yaml
# ─── General ────────────────────────────────────────────────────────────────
general:
  debug: false                   # Extra console logging for troubleshooting
  default-toggle-state: false    # Whether VeinBreaker starts ON for new players

# ─── Features ────────────────────────────────────────────────────────────────
features:
  ore-vein-mining: true
  tree-felling: true
  crop-harvesting: true
  crop-replant: true             # Auto-replant crops after harvest
  xp-drops: true                 # Drop XP when vein-mining (Silk Touch suppresses)

# ─── Permissions ─────────────────────────────────────────────────────────────
permissions:
  use:
    - veinbreaker.use            # Add extra nodes to support custom perm plugins
  admin:
    - veinbreaker.admin

# ─── Limits ──────────────────────────────────────────────────────────────────
limits:
  max-vein-size: 64
  max-tree-size: 400             # Raised high enough for giant spruce/jungle trees
  max-crop-size: 128

# ─── XP Drops ────────────────────────────────────────────────────────────────
xp:
  diamond:        { min: 3, max: 7 }
  emerald:        { min: 3, max: 7 }
  coal:           { min: 0, max: 2 }
  lapis:          { min: 2, max: 5 }
  redstone:       { min: 1, max: 5 }
  quartz:         { min: 2, max: 5 }
  iron:           { min: 0, max: 0 }   # XP comes from smelting in vanilla
  gold:           { min: 0, max: 0 }
  copper:         { min: 0, max: 0 }
  nether_gold:    { min: 0, max: 1 }
  ancient_debris: { min: 0, max: 0 }

# ─── Tool Requirements ───────────────────────────────────────────────────────
tools:
  ore-requires-pickaxe: true
  tree-requires-axe: true
  crop-requires-hoe: false

# ─── Cooldowns (seconds) ─────────────────────────────────────────────────────
cooldowns:
  ore: 0
  tree: 0
  crop: 0

# ─── Blacklisted Worlds ──────────────────────────────────────────────────────
blacklisted-worlds: []

# ─── Messages ────────────────────────────────────────────────────────────────
messages:
  prefix:         "&8[&bVeinBreaker&8]&r "
  enabled:        "&aVeinBreaker &2enabled&a. Break a vein to mine it all!"
  disabled:       "&cVeinBreaker &4disabled&c."
  no-permission:  "&cYou don't have permission to use VeinBreaker."
  reload-success: "&aConfiguration reloaded successfully."
  cooldown:       "&cPlease wait &e{time}s &cbefore using VeinBreaker again."

# ─── Sounds ──────────────────────────────────────────────────────────────────
sounds:
  enabled:    true
  toggle-on:  BLOCK_NOTE_BLOCK_PLING
  toggle-off: BLOCK_NOTE_BLOCK_BASS
```

---


## ❓ FAQ

**Q: Does VeinBreaker work with Fortune and Silk Touch?**  
A: Yes. Drops are calculated using Bukkit's native `getDrops(tool, player)` method, which handles all enchantments correctly.

**Q: Will it cause lag on large servers?**  
A: VeinBreaker uses configurable size limits (`max-vein-size`, `max-tree-size`, `max-crop-size`) to prevent runaway scans. All operations run synchronously on the main thread, which is required for block manipulation in Bukkit. For typical survival gameplay the operations complete in microseconds.

**Q: Can I use a custom permission plugin like LuckPerms?**  
A: Yes. Add your custom permission node to `permissions.use` in `config.yml` and grant it via LuckPerms (or any other permission plugin).

**Q: Does it work with custom trees from other plugins?**  
A: VeinBreaker detects trees by log material type and connected leaf blocks. As long as the tree is built from vanilla log and leaf materials, it will work.

**Q: Can I disable tree felling and keep only ore mining?**  
A: Yes — set `features.tree-felling: false` in `config.yml`.

**Q: Is the plugin compatible with Paper?**  
A: Yes, and Paper is recommended for best performance.

---

## 📊 Performance Notes

- Block scanning uses an iterative BFS, not recursion — no stack overflow risk.
- Each scan terminates as soon as the configured limit is reached.
- Tool durability is applied with Unbreaking chance correctly — no double-damage.
- Leaf removal is batched into a single shared scan to avoid redundant work.
- The plugin has zero external dependencies.

---

## 📜 Changelog

### v1.0.0
- Initial public release
- Ore vein mining with Fortune/Silk Touch support
- Tree felling with improved large-tree detection (max 400 logs)
- Crop harvesting with optional auto-replant
- Full `config.yml` with live reload
- XP drops matching vanilla Minecraft amounts
- Multi-permission system via config
- `/vb help`, `/vb status`, `/vb reload`, `/vb permission add/remove`
- Tool requirement enforcement (pickaxe / axe / hoe)
- Per-feature cooldowns
- Blacklisted world support
- Color-coded messages
- Sound feedback on toggle
- Tool durability damage (Unbreaking respected)

---

## Author

**Usama Balhasal**

- LinkedIn: [Usama Balhasal](https://www.linkedin.com/in/usama-balhasal/)
- Instagram: [@vlx_soma](https://www.instagram.com/vlx_soma/)
- Facebook: [Usama Balhasal](https://www.facebook.com/usama.balhasal.05/)
- GitHub: [@vlxb](https://github.com/Usama-Balhasal)

---

## ⚖️ License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
MIT License — Copyright (c) 2026 Usama Balhasal
```

---

<div align="center">

Made with ❤️ for the Minecraft community · [IceForge](https://www.iceforge.world/)

</div>
