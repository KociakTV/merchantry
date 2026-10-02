# Publikacja Merchantry — lista kontrolna

Pliki w tym folderze:
- `icon.png` — ikona 512×512 (Modrinth i CurseForge)
- `description.md` — opis moda po angielsku (Markdown, działa w obu serwisach)
- `CHANGELOG.md` — lista zmian do wklejenia przy wersji
- `generate_icon.ps1` — skrypt, którym wygenerowano ikonę (do ewentualnych poprawek)

Pliki do wysłania (zwykłe jary, **bez** `-sources`):
- NeoForge: `merchantry_neoforge_1.21.1/build/libs/merchantry_neoforge_1.21.1-<wersja>.jar`
  — opcjonalne (Optional): Create, Create: Numismatics, Applied Energistics 2, FTB Ultimine, Curios, Gravestone, Corpse, You're in Grave Danger
- Fabric: `merchantry_fabric_1.21.1/build/libs/merchantry_fabric_1.21.1-<wersja>.jar`
  — loader Fabric, tylko Minecraft 1.21.1, zależność **Fabric API** (Required), bez Numismatics; opcjonalne (Optional): FTB Ultimine, Trinkets, Universal Graves, You're in Grave Danger
- Fabric 1.20.1: `merchantry_fabric_1.20.1/build/libs/merchantry_fabric_1.20.1-<wersja>.jar`
  — loader Fabric (i Quilt), tylko Minecraft 1.20.1, zależność **Fabric API** (Required); opcjonalne (Optional):
  Create Fabric, Create: Numismatics, Applied Energistics 2, FTB Ultimine, Trinkets, Universal Graves, You're in Grave Danger

(zbuduj: w IntelliJ panel Gradle → Tasks → build → **build**, albo `gradlew build` w folderze danej wersji)

---

## 1. Przed publikacją — test
- [ ] Serwer testowy (**Server** w IntelliJ) + klient **bez** Merchantry → wejście na serwer, `/shop`, `/market`, zakup
- [ ] Serwer bez Create/Numismatics → mod działa, `/exchange` wyświetla komunikat
- [ ] Klient w języku angielskim i polskim → teksty w dobrym języku
- [ ] Zrób 3–5 zrzutów ekranu: sklep, potwierdzenie z ilością, rynek, edytor `/shopconfig`, panel boczny

## 2. Modrinth (modrinth.com → Create a project)
| Pole | Wartość |
|---|---|
| Name | Merchantry |
| URL (slug) | `merchantry` (sprawdzone — wolny) |
| Summary | Server-side shop, economy and player market — no client install needed. |
| Icon | `icon.png` |
| Description | treść `description.md` |
| Categories | Economy, Management, Utility |
| Environment | **Server-side only → Works in singleplayer too** (zwykli gracze nie potrzebują moda; dodatek JEI/EMI dla admina to drobiazg) |
| License | MIT |

Wersja (Versions → Upload):
| Pole | Wartość |
|---|---|
| Version number | 1.0.0 |
| Loaders | NeoForge |
| Game versions | 1.21.1 |
| Release channel | Release (albo Beta na pierwszy raz) |
| Dependencies | Create: Numismatics — **optional**; JEI — optional; EMI — optional |
| Changelog | treść `CHANGELOG.md` |

## 3. CurseForge (authors.curseforge.com → Create Project → Minecraft → Mods)
| Pole | Wartość |
|---|---|
| Project type (klasa) | **Mods** — wybierane na starcie, nie jest kategorią |
| Name | Merchantry |
| Summary | Server-side shop, economy & player market |
| Main category | Server Utility |
| Additional categories | Utility & QoL, Miscellaneous |
| License | MIT License |
| Avatar | `icon.png` |
| Description | treść `description.md` (edytor Markdown) |

Plik (Upload file):
| Pole | Wartość |
|---|---|
| Display name | Merchantry 1.0.0 (NeoForge 1.21.1) |
| Release type | Release (albo Beta) |
| Game versions | 1.21.1, NeoForge |
| Java | Java 21 |
| Environment | **Server + Client** (singleplayer to też serwer — mod musi działać u gracza; na serwerze dedykowanym klient go nie potrzebuje) |
| Related projects | Create: Numismatics — Optional Dependency; JEI — Optional; EMI — Optional |
| Changelog | treść `CHANGELOG.md` |

CurseForge sprawdza pliki ręcznie — zatwierdzenie trwa zwykle od kilku godzin do 1–2 dni.

## 4. Po publikacji
- W `src/main/templates/META-INF/neoforge.mods.toml` odkomentuj i uzupełnij `displayURL` (link do strony moda)
- Przy każdej nowej wersji: podbij `mod_version` w `gradle.properties`, dopisz zmiany do `CHANGELOG.md`, zbuduj i wyślij nowy jar

## Uwagi
- Mod wymaga NeoForge **21.1.248 lub nowszego** (`neo_version` w `gradle.properties`). Starsze wersje 21.1.x prawdopodobnie też by działały, ale nie były testowane.
- Opcjonalnie: repozytorium na GitHubie (kod + zgłaszanie błędów) — przydaje się do `issueTrackerURL` i buduje zaufanie.
