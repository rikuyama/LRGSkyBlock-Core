# Bazaar Order System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a persistent Hypixel-style Bazaar order book with buy/sell orders, price-time matching, instant trades, cancellation, and claimable escrow rewards.

**Architecture:** Orders persist in MySQL and use escrow: buy orders reserve coins and sell orders reserve items. Matching uses best price then FIFO; offline counterpart rewards are stored as claimable coins/items and collected from the Bazaar GUI.

**Tech Stack:** Java 25, Paper 26.2 API, Bukkit inventories, MySQL, Maven.

## Global Constraints

- Preserve Player Level 7 Bazaar unlock.
- Keep all player-facing copy configurable in `messages.yml`.
- Avoid depending on an online PlayerData instance for offline settlement.
- Package the complete project, not a diff-only archive.

---

### Task 1: Persistence and domain model
- [x] Add order side/status/order models.
- [x] Add order, coin claim, and item claim tables.
- [x] Add repository CRUD, best-order lookup, and claim storage.

### Task 2: Trading engine
- [x] Add escrow-backed order creation and cancellation.
- [x] Add price-time matching and partial fills.
- [x] Add instant buy/sell execution and claim collection.

### Task 3: GUI and interaction
- [x] Add order actions to product detail.
- [x] Add chat-based custom order input.
- [x] Add personal order and claim screens.

### Task 4: Integration and packaging
- [x] Wire repository/manager/listener into plugin startup.
- [x] Update version and messages.
- [x] Run available static verification and package the full project.
