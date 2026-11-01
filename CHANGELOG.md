# Changelog

## 0.3.0 - Phase 3 (part 1): Purchases and Sales / POS
- Database v2 with migration 1 -> 2 (suppliers, customers, purchases, sales and their items)
- Purchases: multi-item invoice, item and invoice discount, extra expenses, cash/partial/credit, supplier quick-add
- Sales / POS: cart, quantity +/-, item and invoice discount, cash/partial/credit, customer quick-add
- Stock rises on purchase and falls on sale in one database transaction; stock history records both
- Weighted average cost (see docs/ACCOUNTING.md); cost of goods stored per sale
- Duplicate-sale protection (unique checkout token), no selling above available stock
- Sequential invoice numbers (S-000001, P-000001), history lists, invoice view, text share (WhatsApp etc.)
- Dashboard: sales, profit, receivables, payables, cash balance and recent invoices from real records
- Unit tests: calculators, weighted average, invoice text

## 0.2.0 - Phase 2: Database and Products
- Room database (SQLite) with foreign keys, exported schema, migration scaffold
- Entities: categories, products, stock movements
- Money stored as paisa (Long), quantities as thousandths (Long); BigDecimal in the app
- Product CRUD: add, edit, detail, delete (archive; history kept)
- Categories: add, rename, delete (products kept)
- Search (name/SKU/barcode), category filter, sorting
- Stock value and expected profit; stock history (opening stock, adjustments)
- Dashboard product figures now come from the database
- Unit tests: validator, minor-unit conversions

## 0.1.0 - Phase 1: Project Foundation
- Android Studio project, Gradle Kotlin DSL, version catalog
- Material 3 theme (light/dark), adaptive app icon, splash screen
- Navigation drawer, top toolbar, Navigation Compose graph
- Dashboard UI with date filters
- Fixed developer information (Burhan Nexus) in footer and About
