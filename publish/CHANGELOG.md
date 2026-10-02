# Changelog

## 1.1.0 (Fabric 1.20.1) — Fabric 1.20.1 release with mod compatibility
First version for **Minecraft 1.20.1** (Fabric, requires Fabric API). Includes everything from 1.0.0 plus:

**Create**
- Create: Numismatics exchange is back (`/exchange`): $ ↔ Numismatics bank and coins from your inventory
- Default shop and sell offers: andesite, andesite alloy, zinc, copper, brass
- Addons: electrum (Create Crafts & Additions), magnetite (Create: New Age), silver (Create: Crafts & (More) Additions)

**Applied Energistics 2**
- Default shop and sell offers: certus quartz, charged certus, fluix, silicon, sky stone
- Blank patterns and the **Mysterious Cube** (inscriber presses) for a higher price

Default offers are added once, the first time the server starts with the mod — also to existing offer files. Offers you remove never come back (`config/merchantry_compat_offers.json`).

**FTB Ultimine**
- Ultimine can be a permanent unlock bought in the shop (`ultimineRequiresUnlock`, on by default)

**keepInventory charges**
- Trinkets slots are kept too
- No grave when a charge saved your items: Universal Graves, You're in Grave Danger (`blockGraves`)
- No death waypoint: FTB Chunks, plus Xaero's Minimap and JourneyMap when the player has Merchantry on the client (`hideDeathWaypoints`)

**Leaderboards**
- Payouts left today (`merchantry_payouts_left`) and seconds to the next payout (`merchantry_next_payout`) are scoreboard objectives, next to the balance (`money`) — ready for leaderboard and hologram mods or datapacks

**Notes**
- Minecraft 1.20.1 cannot hide scoreboard numbers, so the personal sidebar shows small numbers on the right
- All compatibility is optional — Merchantry works without any of these mods

## 1.0.0 (Fabric) — Fabric release
- First Fabric version for Minecraft 1.21.1 (requires Fabric API)
- Same features as NeoForge 1.0.0, except the Create: Numismatics exchange (`/exchange`), as Numismatics is not available on Fabric 1.21.1

## 1.0.0 — Initial release
- GUI shop with Buy / Sell / Market tabs and purchase confirmation
- Offer types: items, permanent utility commands, extra homes, repairs, keepInventory charges, one-time commands
- Optional currency with cheques, sidebar and fair earnings (advancements, exploration, active playtime)
- Server buy-back list (`/sell`)
- Player market with categories, fees and offline payouts (`/market`)
- Homes (`/sethome`, `/home`, `/delhome`, `/homes`)
- Create: Numismatics exchange (`/exchange`)
- Graphical offer editor (`/shopconfig`) with optional JEI / EMI drag & drop
- English and Polish, selected automatically per player
