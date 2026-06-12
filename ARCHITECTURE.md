# CorePlugin Architecture Rules

## Project Vision

CorePlugin is not a traditional Bukkit plugin.

CorePlugin is a platform/framework for Paper/Folia servers.

The project is divided into three layers:

```text
Platform
│
├── Database
├── Scheduler
├── EventBus
├── State
├── Filesystem
├── Messaging
└── Configuration

Framework
│
├── Regions
├── Commands
├── Menus
├── Items
├── Permissions
└── Shared Utilities

Features
│
├── Chat
├── Spawn
├── Economy
├── Warps
├── Kits
├── Trails
├── Shop
├── Scoreboards
└── Holograms
```

External plugins consume Platform and Framework APIs.

Features are internal consumers of the same APIs.

---

# Golden Rule

Every feature must be implementable using only public APIs.

If a feature requires access to internal classes, the API is incomplete.

Bad:

```kotlin
RegionManager
ShopManager
SpawnModule
```

Good:

```kotlin
RegionService
MenuFactory
CommandService
DatabaseService
```

---

# Module Responsibilities

## core-api

Contains only:

```text
Interfaces
Enums
Contracts
DTOs
Shared Events
```

Never:

```text
Managers
Implementations
Listeners
Modules
Services implementations
```

---

## core-infra

Contains infrastructure implementations:

```text
Database
Config
Message
Filesystem
DI
State
Lifecycle
```

---

## core-platform-api

Platform abstractions:

```text
TaskScheduler
RegionTaskScheduler
ThreadAssertion
```

No Paper-specific code.

---

## core-platform-paper

Paper/Folia implementations.

Contains all Bukkit/Paper integration.

---

## core-framework-*

Reusable framework modules.

Examples:

```text
core-framework-regions
core-framework-command
core-framework-menu
core-framework-item
```

---

## core-feature-*

Gameplay functionality.

Examples:

```text
core-feature-chat
core-feature-spawn
core-feature-kits
core-feature-warps
```

---

# Service Registry Rules

## Allowed

Register only public contracts.

Example:

```kotlin
registry.register(
    RegionService::class.java,
    DefaultRegionService(...)
)
```

---

## Forbidden

```kotlin
registry.register(
    RegionManager::class.java,
    regionManager
)
```

```kotlin
registry.register(
    SpawnModule::class.java,
    spawnModule
)
```

```kotlin
registry.register(
    ChatModule::class.java,
    chatModule
)
```

---

# API Exposure Rules

## Expose

Only abstractions.

Example:

```kotlin
DatabaseService
RegionService
MenuFactory
ItemBuilderFactory
CommandService
```

---

## Never Expose

```text
RegionManager
SpatialRegionIndex
CompiledRegion internals
Database implementation
Hikari internals
Feature implementations
```

---

# Database Rules

## Objective

Plugins must never manage connections directly.

Forbidden:

```kotlin
databaseService.getConnection()
```

---

Plugins must not see:

```text
Connection
DataSource
DriverManager
HikariDataSource
```

---

Allowed:

```text
PreparedStatement
ResultSet
SQLException
```

---

Expected usage:

```kotlin
database.query(...)
database.executeUpdate(...)
database.transaction(...)
```

---

# Menu Framework Rules

## Use Bukkit Types

Allowed:

```text
Player
ItemStack
Inventory
Adventure Component
Audience
```

---

Do not create wrappers:

Forbidden:

```text
CorePlayer
CoreItem
CoreInventory
```

---

# Item Framework Rules

Must support:

```text
MiniMessage
Lore
Enchantments
ItemFlags
PersistentDataContainer
CustomModelData
Player Heads
Texture Heads
```

from the beginning.

---

# Command Framework Rules

Cloud V2 is the standard command engine.

No feature may register commands manually.

Forbidden:

```kotlin
plugin.getCommand(...)
```

```kotlin
CommandExecutor
```

---

Required:

```kotlin
CommandService
```

---

# Region Framework Rules

Regions are framework-level systems.

Regions are NOT protection-only systems.

Regions may be used by:

```text
Spawn
PvP
Shops
Menus
NPCs
Particles
Music
Events
Scoreboards
```

---

Protection systems consume RegionService.

RegionService must not depend on protection logic.

---

# Feature Rules

Features are consumers.

Features are not infrastructure.

Features are not framework.

Examples:

```text
Spawn
Chat
Trails
Warps
Kits
```

must consume:

```text
DatabaseService
RegionService
MenuFactory
CommandService
MessageService
```

---

# Dependency Rules

Allowed:

```text
Feature -> Framework
Feature -> Platform
Feature -> Infrastructure APIs
```

---

Forbidden:

```text
Framework -> Feature
Infrastructure -> Feature
API -> Feature
```

---

# Lifecycle Rules

Every feature must implement:

```kotlin
interface Feature {

    fun load()

    fun enable()

    fun disable()

    fun reload()
}
```

---

Features are managed by:

```kotlin
FeatureManager
```

---

Never:

```kotlin
onEnable() {
    registerChat()
    registerSpawn()
    registerWarps()
}
```

---

Use:

```kotlin
featureManager.enableAll()
```

---

# Configuration Rules

Every feature owns its files.

Example:

```text
plugins/CorePlugin/

features/

  spawn/
    config.conf
    messages.conf

  regions/
    config.conf
    messages.conf

  warps/
    config.conf
    messages.conf
```

---

No giant global config files.

---

# Messaging Rules

Messages must never be hardcoded.

Forbidden:

```kotlin
player.sendMessage("Warp created")
```

Required:

```kotlin
messageService.send(
    player,
    WarpMessages.CREATED
)
```

---

# Performance Rules

Hotpaths:

```text
PlayerMoveEvent
Damage Events
Region Queries
Packet Handlers
```

must avoid:

```text
Streams
Lambdas in loops
Repeated allocations
Reflection
```

Prefer:

```text
Primitive collections
FastUtil
Index-based loops
Precompiled structures
```

when profiling justifies it.

---

# Threading Rules

Paper/Folia compatibility is mandatory.

Never assume:

```kotlin
Bukkit.isPrimaryThread()
```

is sufficient.

Use:

```kotlin
TaskScheduler
RegionTaskScheduler
ThreadAssertion
```

abstractions.

---

# External Plugin Goal

A plugin such as:

```text
Skyblock
Prisons
ProtectionBlocks
Survival
```

must be able to operate using only:

```text
DatabaseService
ConfigService
MessageService
TaskScheduler
PlayerStateService

RegionService
CommandService
MenuFactory
ItemBuilderFactory

EconomyService
WarpService
KitService
```

without touching internal CorePlugin implementations.

---

# Final Decision Rule

Before introducing a new class, ask:

1. Is this infrastructure?
2. Is this reusable framework functionality?
3. Is this gameplay-specific?

Classification:

```text
Infrastructure -> core-infra/core-api
Reusable -> framework
Gameplay -> feature
```

If uncertain, prefer framework over feature.

Frameworks scale.
Features consume frameworks.
