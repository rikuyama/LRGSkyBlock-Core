# Bazaar Hypixel-Compatible Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the flat Bazaar inventory with a Japanese Hypixel-style category, product, detail, quantity, confirmation, and search flow while preserving the existing database and admin functions.

**Architecture:** Store navigation state in `BazaarHolder`, render every screen through `BazaarGui`, and route clicks through `BazaarListener`. `BazaarManager` remains responsible for transactions and inventory-safe item movement.

**Tech Stack:** Java 25, Paper 26.2 API, Bukkit inventories, MySQL-backed existing Bazaar repository.

## Global Constraints

- Deliver only changed and new files in the final ZIP.
- All player-facing text is Japanese and configurable through `messages.yml`.
- Bazaar unlock remains Player Level 7.
- v0.7.4 order-book functionality is not included.

---

### Task 1: Stateful GUI navigation
- [x] Expand `BazaarHolder` with screen, category, item, page, query, action, and amount state.
- [x] Render category, product list, item detail, quantity, and confirmation inventories.

### Task 2: Hypixel-compatible click flow
- [x] Route category selection, paging, detail navigation, buy/sell quantity selection, and confirmation.
- [x] Preserve admin screen behavior.

### Task 3: Search and Japanese copy
- [x] Add chat-based product search from the search icon.
- [x] Add all new GUI labels and transaction messages to `messages.yml`.

### Task 4: Package verification
- [x] Check Java source structure and ZIP integrity.
- [x] Produce a diff-only ZIP.
