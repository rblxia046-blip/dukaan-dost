# Changelog

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
