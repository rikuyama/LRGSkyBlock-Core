# Fortune Category GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace per-block mandatory Fortune rules with automatic categories and an in-game administrative GUI.

**Architecture:** `FortuneTargetSettings` owns mutable category switches and material overrides persisted to YAML. `FortuneCategoryResolver` classifies unconfigured materials. `FortuneGui` and its listener edit settings, while `FortuneManager` consumes the resolved rule.

**Tech Stack:** Paper 26.2 API, Java 25, Maven, Bukkit YAML.

## Global Constraints

- Keep Maven.
- Preserve LRGMine reflection API: `getMiningFortune(Player, Material)` and `calculateDropAmount(int, double)`.
- No block needs a manually written rule for default operation.
- GUI command permission defaults to OP.

---

### Task 1: Category settings and resolver

**Files:**
- Modify: `src/main/java/me/lrg/skyblock/core/config/FortuneTargetSettings.java`
- Create: `src/main/java/me/lrg/skyblock/core/model/FortuneCategoryResolver.java`
- Modify: `src/main/resources/fortune-targets.yml`

- [x] Load category switches and material overrides.
- [x] Resolve missing materials automatically.
- [x] Save changes immediately.

### Task 2: Fortune manager integration

**Files:**
- Modify: `src/main/java/me/lrg/skyblock/core/manager/FortuneManager.java`

- [x] Use resolved category rules rather than explicit-only rules.
- [x] Remove temporary debug logging.
- [x] Preserve public reflection methods used by LRGMine.

### Task 3: Administration GUI

**Files:**
- Create: `src/main/java/me/lrg/skyblock/core/gui/FortuneGui.java`
- Create: `src/main/java/me/lrg/skyblock/core/listener/FortuneGuiListener.java`
- Create: `src/main/java/me/lrg/skyblock/core/command/FortuneGuiCommand.java`
- Modify: `src/main/java/me/lrg/skyblock/core/LRGSkyBlockCore.java`
- Modify: `src/main/resources/plugin.yml`

- [x] Display category switches.
- [x] Toggle category by clicking.
- [x] Assign the held block to a selected category.
- [x] Remove a held block override.

### Task 4: Latest Paper metadata

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/plugin.yml`
- Modify: LRGMine `pom.xml`
- Modify: LRGMine `src/main/resources/plugin.yml`

- [x] Target Paper 26.2 and Java 25.
- [x] Keep LRGMine soft dependency and reflection integration.
