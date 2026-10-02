# Merchantry

**A server-side shop, economy and player market — no client install needed.**

Merchantry adds a complete, easy-to-configure economy to your server: a GUI shop, an optional currency with fair ways to earn it, a player market, repairs, homes, permanent utility commands and more. Everything runs on the server, so players can join with a plain vanilla client — only the server needs the mod.

Available for **NeoForge 1.21.1**, **Fabric 1.21.1** and **Fabric 1.20.1**.

***

## ✨ Features

### 🛒 Shop (`/shop`)

*   Chest-style GUI with **Buy**, **Sell** and **Market** tabs
*   Confirmation screen for every purchase, with a **quantity selector** for items and charges
*   Offer types:
    *   **Items** — sell any item stack (with enchantments, names, etc.)
    *   **Permanent commands** — `/craft`, `/anvil`, `/enderchest` (`/ec`), `/stonecutter`, `/smithing`, `/grindstone`, `/loom`, `/cartography`
    *   **Extra homes** — each next home costs more (configurable multiplier)
    *   **Repairs** — `/repair` (item in hand) and `/repairall` (whole inventory), priced automatically by damage and material tier
    *   **keepInventory charges** — one charge protects your inventory and XP on one death (the gamerule is never touched)
    *   **One-time commands** — any server command, e.g. a random teleport
*   **Two payment modes:** pay with **$** when the currency is enabled, or with **items** (e.g. diamonds) when it's disabled — each offer can have both prices

### 💰 Currency (optional)

*   Disabled by default — turn it on with `enableCurrency = true`
*   `/balance`, `/pay <player> <amount>`, `/cheque <amount>` (signed paper cheques players can trade and redeem)
*   Personal **sidebar** showing your balance, time to the next payout and payouts left today (`/sidebar` to toggle)
*   Balance mirrored to a scoreboard objective for datapacks
*   **Hard to automate earnings:**
    *   one-time rewards for advancements (task / goal / challenge)
    *   discovering new biomes and dimensions
    *   active playtime with AFK detection and a daily limit

### 🏪 Sell to the server (`/sell`)

*   Sell resources like iron, gold, diamonds or emeralds for $
*   Left-click sells 1, right-click 64, shift-click everything
*   Fully configurable list and prices

### 🤝 Player market (`/market`)

*   `/market sell <price> [amount]` lists the item in your hand
*   Categories: weapons, armor, tools, food, blocks, magic & potions, other
*   Configurable sales fee and listing limit per player
*   Sellers get paid even while offline; admins can remove listings

### 🏠 Homes

*   `/sethome`, `/home`, `/delhome`, `/homes`
*   One home by default, more can be bought in the shop (up to a configurable limit)

### 🪙 Create: Numismatics compatibility (NeoForge 1.21.1 and Fabric 1.20.1)

*   `/exchange` — convert **$ ↔ Numismatics bank balance** and deposit coins from your inventory
*   Configurable exchange rate and fee
*   Not available on Fabric 1.21.1 — Create: Numismatics has no Fabric release for that version

### 🧩 Mod compatibility

*   **Default shop and sell offers** added automatically the first time the server starts with the mod (removed offers never come back):
    *   **Create** — andesite, andesite alloy, zinc, copper, brass
    *   **Create Crafts & Additions** — electrum · **Create: New Age** — magnetite · **Create: Crafts & (More) Additions** — silver
    *   **Applied Energistics 2** — certus quartz, charged certus, fluix, silicon, sky stone, blank patterns and the **Mysterious Cube** (inscriber presses) for a higher price
*   **FTB Ultimine** — ultimine can be a permanent unlock bought in the shop (`ultimineRequiresUnlock`)
*   **keepInventory charges** work with:
    *   **Trinkets** (Fabric) and **Curios** (NeoForge) — accessory slots are kept too
    *   **Graves** — no grave when a charge saved your items (`blockGraves`): Universal Graves and You're in Grave Danger (Fabric); Gravestone, Corpse and You're in Grave Danger (NeoForge)
    *   **FTB Chunks** — no death waypoint; **Xaero's Minimap** and **JourneyMap** too when the player has Merchantry on the client (`hideDeathWaypoints`)
*   **Leaderboards** — balance, payouts left today and seconds to the next payout are scoreboard objectives, ready for leaderboard and hologram mods or datapacks

Create and AE2 only exist on NeoForge 1.21.1 and Fabric 1.20.1, so their offers and `/exchange` are not available on Fabric 1.21.1.

### 🛠️ Admin tools

*   **Graphical offer editor** — just type `/shopconfig`
    *   set products, icons and prices by clicking with an item on your cursor
    *   +/- buttons for prices, reordering, deleting, creating new offers
*   **JEI / EMI drag & drop** into the editor (needs the mod on the admin's client — optional)
*   Plenty of `/shopconfig` subcommands for scripting and quick edits
*   `/eco give|take|set`, `/keepinv give|set`

### 🌍 Languages

*   **English** and **Polish**, selected **automatically per player** based on their game language

***

## ⚙️ Configuration

| File                          |What it contains                                                          |
| ----------------------------- |------------------------------------------------------------------------- |
| <code>config/merchantry-server.toml</code> |currency, earnings, homes, repairs, market, exchange (NeoForge), language |
| <code>config/merchantry_offers.json</code> |shop offers (edit in-game with <code>/shopconfig</code>)                  |
| <code>config/merchantry_sell.json</code> |items the server buys and their prices                                    |
| <code>config/merchantry_compat_offers.json</code> |which mods already got their default offers  |

Admin commands require permission level 2 (operator).

## 📦 Requirements

*   Minecraft **1.21.1** — **NeoForge** 21.1, or **Fabric** Loader 0.16+ with [Fabric API](https://modrinth.com/mod/fabric-api)
*   Minecraft **1.20.1** — **Fabric** Loader 0.15+ with [Fabric API](https://modrinth.com/mod/fabric-api)
*   Server-side. The client mod is **optional** (only needed for JEI/EMI drag & drop in the editor)

**Optional compatibility:** JEI, EMI, FTB Ultimine, FTB Chunks, Xaero's Minimap, JourneyMap, You're in Grave Danger (all versions); Create and addons, Create: Numismatics, Applied Energistics 2 (NeoForge 1.21.1, Fabric 1.20.1); Curios, Gravestone, Corpse (NeoForge); Trinkets, Universal Graves (Fabric)

## 📜 License

MIT — feel free to use Merchantry in your modpacks.

Source code: [github.com/KociakTV/merchantry](https://github.com/KociakTV/merchantry) — bug reports and suggestions welcome in [Issues](https://github.com/KociakTV/merchantry/issues).
