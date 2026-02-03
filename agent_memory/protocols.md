# Turbo Core Protocols

## Memory and Persistence
- This folder acts as the persistent knowledge base for the agent.
- DO NOT FORGET: V206 "Chimera" protocol is the current standard.
- Thermal Protection at 39°C and 40°C is critical.

## Current State (V206 ZUEIRA Update)
- V140 is officially retired/ignored.
- New Standard: Exactly 25 specialized modes across 7 categories (CPU, GPU, MIRA, REDE, CHIMERA, DEBLOAT, POWER).
- Taxonomy: Agressive/Brazilian Humor ("Bixby no Vasco", "Poco Bomba").
- Architecture: Logic moved to `SmartCoreLogic.kt`.
- ICE BREAKER: Emergency cooling script runs on app launch.
- Baseline Profiles: `androidx.profileinstaller` added to mitigate Android 16 dex2oat loops.
- Gradle: Parallel execution disabled and workers limited to 1 for memory efficiency.
- Manifest: `ShizukuProvider` removed to prevent installation conflicts on Android 16.
- Theme: Fire (CHIMERA, CPU, GPU), Ice (POWER, MIRA), Water (REDE), Purple (DEBLOAT).
- Hardware Intelligence: Samsung/Xiaomi specialized filtering implemented in `SmartCoreLogic.kt`.
