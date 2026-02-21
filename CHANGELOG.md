# 1.5.0
- The internal configuration system has been completely rewritten. In addition to specifying more information per se in the configuration entries, now KnightLib automatically creates a configuration screen/menu for each configuration file that a mod (that uses KnightLib) has in use
- The forge version no longer replicates its own configuration system, avoiding irrelevant alerts in the logs
- Added a common event bus abstraction that automatically derives registration and execution calls to their loader-specific derivatives, with sticky system applicability (mainly for fabric)
- The networking system has been generalized so that each mod represents its own communication channel instead of using knightlib to register and send its own packets, which led to untimely crashes and packets that never arrived
- Added utilities to work with post shaders (and accumulating multitarget post shaders) more uniformly
- Completely rewritten the camera shaking system to make it more configurable and less limited to a simple randomized "vibration"
- Potentially fixed issues with config file locks that caused the game to crash
- Fixed a crash that could occur when generating camera shake due to an illegal thread access

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