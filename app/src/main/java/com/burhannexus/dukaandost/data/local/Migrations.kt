package com.burhannexus.dukaandost.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2 (Phase 3): suppliers, customers, purchases, sales. Existing data is untouched. */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `suppliers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `phone` TEXT, `address` TEXT, `openingBalanceMinor` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_name` ON `suppliers` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `phone` TEXT, `address` TEXT, `openingBalanceMinor` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `purchases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` INTEGER NOT NULL, `supplierId` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "`subtotalMinor` INTEGER NOT NULL, `discountMinor` INTEGER NOT NULL, " +
                "`extraExpensesMinor` INTEGER NOT NULL, `totalMinor` INTEGER NOT NULL, " +
                "`paidMinor` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_supplierId` ON `purchases` (`supplierId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchases_number` ON `purchases` (`number`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_createdAt` ON `purchases` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `purchase_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`purchaseId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `quantityMilli` INTEGER NOT NULL, " +
                "`unitCostMinor` INTEGER NOT NULL, `discountMinor` INTEGER NOT NULL, " +
                "`lineTotalMinor` INTEGER NOT NULL, `landedTotalMinor` INTEGER NOT NULL, " +
                "FOREIGN KEY(`purchaseId`) REFERENCES `purchases`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_items_purchaseId` ON `purchase_items` (`purchaseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_items_productId` ON `purchase_items` (`productId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` INTEGER NOT NULL, `clientToken` TEXT NOT NULL, `customerId` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `subtotalMinor` INTEGER NOT NULL, `discountMinor` INTEGER NOT NULL, " +
                "`totalMinor` INTEGER NOT NULL, `paidMinor` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, " +
                "`cogsMinor` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_number` ON `sales` (`number`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_clientToken` ON `sales` (`clientToken`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_createdAt` ON `sales` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, " +
                "`quantityMilli` INTEGER NOT NULL, `unitPriceMinor` INTEGER NOT NULL, " +
                "`unitCostMinor` INTEGER NOT NULL, `discountMinor` INTEGER NOT NULL, " +
                "`lineTotalMinor` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
    }
}

/** v2 -> v3 (Phase 3, part 2): sales returns and purchase returns. Existing data is untouched. */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_returns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` INTEGER NOT NULL, `saleId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`valueMinor` INTEGER NOT NULL, `refundMinor` INTEGER NOT NULL, `cogsMinor` INTEGER NOT NULL, " +
                "`paymentMethod` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_returns_saleId` ON `sale_returns` (`saleId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sale_returns_number` ON `sale_returns` (`number`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_returns_createdAt` ON `sale_returns` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_return_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`returnId` INTEGER NOT NULL, `saleItemId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                "`quantityMilli` INTEGER NOT NULL, `valueMinor` INTEGER NOT NULL, " +
                "FOREIGN KEY(`returnId`) REFERENCES `sale_returns`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`saleItemId`) REFERENCES `sale_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_items_returnId` ON `sale_return_items` (`returnId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_items_saleItemId` ON `sale_return_items` (`saleItemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_return_items_productId` ON `sale_return_items` (`productId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `purchase_returns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` INTEGER NOT NULL, `purchaseId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`valueMinor` INTEGER NOT NULL, `refundMinor` INTEGER NOT NULL, " +
                "`paymentMethod` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "FOREIGN KEY(`purchaseId`) REFERENCES `purchases`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_returns_purchaseId` ON `purchase_returns` (`purchaseId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_returns_number` ON `purchase_returns` (`number`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_returns_createdAt` ON `purchase_returns` (`createdAt`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `purchase_return_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`returnId` INTEGER NOT NULL, `purchaseItemId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                "`quantityMilli` INTEGER NOT NULL, `valueMinor` INTEGER NOT NULL, " +
                "FOREIGN KEY(`returnId`) REFERENCES `purchase_returns`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`purchaseItemId`) REFERENCES `purchase_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_items_returnId` ON `purchase_return_items` (`returnId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_items_purchaseItemId` ON `purchase_return_items` (`purchaseItemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_return_items_productId` ON `purchase_return_items` (`productId`)")
    }
}
