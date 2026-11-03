# Changelog

## 0.4.0 - Phase 4: Customers, Suppliers, Ledger
- New menu entries: Return items, Customers, Suppliers, Ledger
- Return items menu: all sales and purchase returns in one list, start a new return against any invoice
- Customers and Suppliers: list with balances, search, add, edit (name, phone, address, opening balance)
- Ledger: every bill, payment-with-bill, return, refund and payment on account with a running balance
- Receive payment from a customer / make payment to a supplier on account
- Statement PDF (logo, shop details) with share and print
- Opening balance is locked once the party has transactions (history is never rewritten silently)
- Database v4 with migration 3 -> 4 (customer_payments, supplier_payments)
- Dashboard receivables, payables and cash balance include opening balances and payments on account

## 0.3.2 - Shop profile and PDF receipts
- First run asks for shop name, mobile number, address, owner, receipt message and logo (DataStore + private logo file)
- Shop details editable later in Settings
- PDF receipt (A5) with logo, shop name, address and phone on top; multi-page; Share PDF and Print
- Text share also starts with the shop details
- Shop name shown in the toolbar business menu

## 0.3.1 - Phase 3 (part 2): Returns
- Database v3 with migration 2 -> 3 (sale_returns, sale_return_items, purchase_returns, purchase_return_items)
- Sales return: stock goes back up, receivable falls first, only the overpayment is refunded in cash
- Purchase return: stock goes down (blocked if the goods were already sold), payable falls first, excess refunded by the supplier
- Return value includes the invoice-discount share; repeated partial returns never exceed the line value
- Invoice screens show returns, net remaining balance and a "Return items" button
- Dashboard sales, profit, receivables, payables and cash balance are net of returns and refunds
- Stock history shows SR-/PR- return numbers

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
