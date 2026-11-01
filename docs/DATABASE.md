# Database (Room / SQLite) - schema version 1

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
