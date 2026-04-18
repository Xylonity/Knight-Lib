# 1.5.0
- License changed from GPLv3 to LGPL
- Updated wiki
- The internal configuration system has been completely rewritten. In addition to specifying more information per se in the configuration entries, now KnightLib automatically creates a configuration screen/menu for each configuration file that a mod (that uses KnightLib) has in use
- The forge version no longer replicates its own configuration system, avoiding irrelevant alerts in the logs, among other issues
- Added a common event bus abstraction that automatically derives registration and execution calls to their loader-specific derivatives, with sticky system applicability (mainly for fabric)
- Added an automaton system (finite state machine) for handling complex entity logic.
- Added an experimental OBB (Oriented Bounding Box) system that allows assigning dynamic hitboxes to bones in geckolib entities
- The networking system has been generalized so that each mod represents its own communication channel instead of using knightlib to register and send its own packets, which led to untimely crashes and packets that never arrived
- Added utilities to work with post shaders (and accumulating multitarget post shaders) more uniformly
- Added utilities to manage the spawning of entities in specific biomes in a simpler and more centralized way
- Added utilities to manage entity loot tables in a simpler and more centralized way
- Added utilities to manage persistent sounds
- Added utilities to manage persistent entity data
- Added extra additional utilities, such as Math or EasingTypes
- Enhanced the registrar system and added various extra abstractions
- Completely rewritten the camera shaking system to make it more configurable and less limited to a simple randomized "vibration"
- Added some convenience S2C packets to facilitate the use of certain utilities
- Added a config option to prevent the filled grail from getting yeeted when using the empty grail in the great chalice, and to automatically add it to the player's inventory.
- Added a config option to allow entities to be added to a whitelist or blacklist, in order to avoid unnecessary essence drops on all entities.
- Potentially fixed issues with config file locks that caused the game to crash
- Refactored some of the internal package distribution
- Small and Big Essences now have a new item icon, made by joosh_7889
- The knightlib icon can now be seen in the ingame configuration menu
- Fixed a bug in forge where too much essence was dropped when an explosion occurred, such as when a creeper exploded. This happened because all blocks in the explosion were automatically taken into account, each of which also calculated the drop probability separately.
- Fixed a crash due to an illegal thread access that could randomly occur when generating camera shake
- Fixed a crash that occurred when rendering the starset ring in a JEI menu when the mod lazyyyy was present. This occurs due to an incompatibility with geckolib, not with knightlib, so now the ring causing the crash is not rendered if lazyyyy is in the modpack.
- Fixed icons texture breaking mipmaps (fix by pokesmells)

# 1.4.3
- Fixed a crash when booting with the mod Immersive Engineering in the same pack
- Successfully added geckolib as a mandatory dependency
- Fixed non-capitalized item names

# 1.4.2
- Fixed a crash caused by the networking stack

# 1.4.1
- Fixed a crash using the camera shake utility

# 1.4.0
- Added a brand-new registrar system
- Updated license assets
- Now the content that's not used by any mod won't be obtainable ingame
- Fixed config composer creating multiple .bak files in the forge version
- Added a brand-new ARGB wrapper and extra utilities
- Added a brand-new networking API along with a custom tracking bossbar event
- Refactor internal package distribution
- Added ja_jp translation keys by elinka47
- Added ru_ru translation keys by Tefnya
- Added pt_br translation keys by Bea-CEO
- Added zh_cn translation keys by Junnaturefox