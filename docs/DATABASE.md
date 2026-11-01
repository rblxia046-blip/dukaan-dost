# Database (Room / SQLite) - schema version 2

File: `dukaan_dost.db`, local to each installation. Foreign keys are enforced.

| Table | Purpose |
|---|---|
| categories | id, name (unique, case-insensitive) |
| products | prices in paisa, stock in thousandths, `isActive` (false = archived), FK categoryId -> categories (SET NULL) |
| stock_movements | OPENING / ADJUSTMENT (later PURCHASE, SALE, returns); FK productId -> products (RESTRICT) |

## Rules
- Business values are never Float/Double. Money = Long paisa, quantity = Long thousandths.
- "Delete product" archives it; stock history is never lost.
- SKU and barcode must be unique among active products (checked inside the save transaction).
- Saving a product and its stock movement happen in one transaction.
- Schema changes need a version bump plus a Migration. Destructive migration is not used.
- Exported schemas go to `app/schemas/` and should be committed.

## Version 2 tables (Phase 3)
| Table | Purpose |
|---|---|
| suppliers, customers | name, phone, opening balance (full screens and ledgers in Phase 4) |
| purchases, purchase_items | invoice header (discount, extra expenses, total, paid) and lines (incl. landed cost) |
| sales, sale_items | invoice header (`clientToken` unique = duplicate protection, `cogsMinor`) and lines (name and cost snapshot) |

Migration 1 -> 2 only creates tables; existing data is untouched.
Invoices and items are never deleted (foreign keys use RESTRICT).
