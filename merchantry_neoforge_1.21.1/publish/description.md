# Merchantry

**A server-side shop, economy and player market — no client install needed.**

Merchantry adds a complete, easy-to-configure economy to your server: a GUI shop, an optional currency with fair ways to earn it, a player market, repairs, homes, permanent utility commands and more. Everything runs on the server, so players can join with a plain NeoForge client — only the server needs the mod.

---

## ✨ Features

### 🛒 Shop (`/shop`)
- Chest-style GUI with **Buy**, **Sell** and **Market** tabs
- Confirmation screen for every purchase, with a **quantity selector** for items and charges
- Offer types:
  - **Items** — sell any item stack (with enchantments, names, etc.)
  - **Permanent commands** — `/craft`, `/anvil`, `/enderchest` (`/ec`), `/stonecutter`, `/smithing`, `/grindstone`, `/loom`, `/cartography`
  - **Extra homes** — each next home costs more (configurable multiplier)
  - **Repairs** — `/repair` (item in hand) and `/repairall` (whole inventory), priced automatically by damage and material tier
  - **keepInventory charges** — one charge protects your inventory and XP on one death (the gamerule is never touched)
  - **One-time commands** — any server command, e.g. a random teleport
- **Two payment modes:** pay with **$** when the currency is enabled, or with **items** (e.g. diamonds) when it's disabled — each offer can have both prices

### 💰 Currency (optional)
- Disabled by default — turn it on with `enableCurrency = true`
- `/balance`, `/pay <player> <amount>`, `/cheque <amount>` (signed paper cheques players can trade and redeem)
- Personal **sidebar** showing your balance, time to the next payout and payouts left today (`/sidebar` to toggle)
- Balance mirrored to a scoreboard objective for datapacks
- **Hard to automate earnings:**
  - one-time rewards for advancements (task / goal / challenge)
  - discovering new biomes and dimensions
  - active playtime with AFK detection and a daily limit

### 🏪 Sell to the server (`/sell`)
- Sell resources like iron, gold, diamonds or emeralds for $
- Left-click sells 1, right-click 64, shift-click everything
- Fully configurable list and prices

### 🤝 Player market (`/market`)
- `/market sell <price> [amount]` lists the item in your hand
- Categories: weapons, armor, tools, food, blocks, magic & potions, other
- Configurable sales fee and listing limit per player
- Sellers get paid even while offline; admins can remove listings

### 🏠 Homes
- `/sethome`, `/home`, `/delhome`, `/homes`
- One home by default, more can be bought in the shop (up to a configurable limit)

### 🪙 Create: Numismatics compatibility (optional)
- `/exchange` — convert **$ ↔ Numismatics bank balance** and deposit coins from your inventory
- Configurable exchange rate and fee

### 🛠️ Admin tools
- **Graphical offer editor** — just type `/shopconfig`
  - set products, icons and prices by clicking with an item on your cursor
  - +/- buttons for prices, reordering, deleting, creating new offers
- **JEI / EMI drag & drop** into the editor (needs the mod on the admin's client — optional)
- Plenty of `/shopconfig` subcommands for scripting and quick edits
- `/eco give|take|set`, `/keepinv give|set`

### 🌍 Languages
- **English** and **Polish**, selected **automatically per player** based on their game language

---

## ⚙️ Configuration
| File | What it contains |
|---|---|
| `config/merchantry-server.toml` | currency, earnings, homes, repairs, market, exchange, language |
| `config/merchantry_offers.json` | shop offers (edit in-game with `/shopconfig`) |
| `config/merchantry_sell.json` | items the server buys and their prices |

Admin commands require permission level 2 (operator).

## 📦 Requirements
- Minecraft **1.21.1**, **NeoForge**
- Server-side. The client mod is **optional** (only needed for JEI/EMI drag & drop in the editor)

**Optional compatibility:** [Create: Numismatics](https://www.curseforge.com/minecraft/mc-mods/numismatics), [JEI](https://www.curseforge.com/minecraft/mc-mods/jei), [EMI](https://www.curseforge.com/minecraft/mc-mods/emi)

## 📜 License
MIT — feel free to use Merchantry in your modpacks.
