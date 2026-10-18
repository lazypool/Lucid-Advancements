## v1.3.0

* Removed the manual tracked/untracked state. The HUD now automatically displays every incomplete, visible advancement.
* HUD entries use the advancement tree's iteration order.
* The HUD shows up to four advancements per page, reduced automatically to fit the HUD height; press `Ctrl` plus your configured advancements key to cycle pages, with an `x/y` page indicator.
* Press `Alt` plus your configured advancements key to toggle HUD visibility.
* Removed the Tracked filter, track toggles, the separate Tracked progress bar, and the per-world tracking cache.
* Category data is preloaded when advancements are received instead of during the first HUD render.
* HUD boxes now refresh immediately after config changes or hot reloads.
* Pressing the advancements key again now closes the advancements screen.
* Replaced advancement completion and recipe unlock toasts with centered top notifications styled like the HUD overlay.
* Notifications now share a single countdown that refreshes whenever a new notification appears, so they disappear together.
* Moved persistent active effect icons to a left-side grid and restyled them to match the overlay.
* The effect grid keeps clear of centered notifications and the right pinned HUD.
* Added clamping for new notification and effect HUD settings to prevent invalid layouts or crashes.

---

## v1.2.2

* Add multilingual support for config section titles #36
* Fix category titles and descriptions not following the selected language by storing keys instead of resolved text #36
* Add vanilla advancement criteria translations for all supported languages #36
* Resolve armor trim criteria via trim_pattern.* and add names for animal variants #36

---

## v1.2.1

* Added ScrollSpeed Options to the Config Screen #32
* Fixed Configs Sidebar Category Selection not Updating While Scrolling Through Content #32
* Added Color Picker to the Config Screen #32
* Add Haptic Feedback to Config Screen Interactions #32
* Added Alphabetical Sorting to the Categories Sidebar #31

* Updated NeoForge version from `21.1.230` to `21.1.245`.

---

## v1.2.0.1

* Hotfix > Fixed categories not following their respective data file

---

## v1.2.0

* Improved config screen by creating more concise entries #27
* Implemented auto-release scripts for Curseforge, Modrinth & Github
* Changed config file locations to `/config/lucid-advancements/{cache,data,assets}` #28
* Added per-mod category customization via `/config/lucid-advancements/data/{MOD_ID}.json` (title, description, icon, enabled) #28
* Added support for custom texture icons in categories, stored in `/config/lucid-advancements/assets/{MOD_ID}/` #28
* Added a Reload button in the config screen to refresh config, category data and icon cache #28
* Added a scrollbar for the advancements roots sidebar #28

---

## v1.1.9

* Implemented a comprehensive config menu GUI to modify mod settings directly in-game.
* Added full translation support for Spanish (`es_es`), Russian (`ru_ru`), German (`de_de`), French (`fr_fr`), and Simplified Chinese (`zh_cn`).
* Removed the legacy 'Clear Tracked' button. Replaced it with a new action button positioned adjacent to the configuration gear icon in the main UI.
* Added an option to toggle the config watcher (hot-reload) off.
* Changed default config values `screenSidebarRowHeight=32` and `screenSidebarItemHeight=28`

* Revised the underlying config file comments to be more concise and straightforward for manual edits.

---

## v1.1.8

* Fixed the mod trying to load on the server side. #15
* Fixed GUI missing 1 pixel on the right/bottom edges. #19
* Fixed the search box suggestion placeholder never rendering.
* Fixed a visual glitch where the advancement screen would repeatedly open/close when Clickable Advancements was also installed. #16

* Changed the GUI Scale button to a dropdown, supporting `Auto` and `1` - `8`.
* Added support for filtering by multiple mods in the search bar (e.g. `@create @farmersdelight`). #18
* Added support for remembering the search query across menu reopens. #17

* Updated NeoForge version from `21.0.37-beta` to `21.1.230`.

---
