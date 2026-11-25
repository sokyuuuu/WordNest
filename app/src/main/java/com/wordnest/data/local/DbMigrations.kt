package com.wordnest.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Các bước migration tường minh của WordNest Room database (không xoá dữ liệu khi nâng phiên bản).
 */
object DbMigrations {

    const val DEFAULT_MIGRATED_UPDATED_AT = "2026-01-01T00:00:00Z"

    /**
     * v2 -> v3:
     * - Deck: thêm cột updated_at (ISO-8601 UTC), deleted (0/1), index (user_id, deleted).
     * - Word: đổi tên last_review_at -> last_reviewed_at, thêm updated_at, deleted,
     *   chuẩn hoá status về chữ thường (egg|chick|bird), thêm index (deck_id, deleted) & (status).
     *   Dùng kỹ thuật dựng lại bảng tạm (_new_Word) để tương thích hoàn toàn từ minSdk 26 (SQLite 3.19.4),
     *   không phụ thuộc ALTER TABLE ... RENAME COLUMN (chỉ có từ SQLite 3.25 / Android 10).
     * - Tạo mới bảng study_history kèm FK CASCADE -> Word và index (word_id), (created_at).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Deck: thêm updated_at, deleted và index (user_id, deleted)
            db.execSQL(
                "ALTER TABLE `Deck` ADD COLUMN `updated_at` TEXT NOT NULL DEFAULT '$DEFAULT_MIGRATED_UPDATED_AT'"
            )
            db.execSQL(
                "ALTER TABLE `Deck` ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_Deck_user_id_deleted` ON `Deck` (`user_id`, `deleted`)"
            )

            // 2. Word: dựng bảng mới với last_reviewed_at, updated_at, deleted & status chữ thường
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_Word` (
                    `word_id` TEXT NOT NULL,
                    `deck_id` TEXT NOT NULL,
                    `term` TEXT NOT NULL,
                    `meaning` TEXT NOT NULL,
                    `phonetic` TEXT,
                    `example` TEXT,
                    `note` TEXT,
                    `status` TEXT NOT NULL,
                    `correct_streak` INTEGER NOT NULL,
                    `wrong_count` INTEGER NOT NULL,
                    `interval_days` INTEGER NOT NULL,
                    `next_review_at` TEXT,
                    `last_reviewed_at` TEXT,
                    `create_at` TEXT NOT NULL,
                    `updated_at` TEXT NOT NULL DEFAULT '$DEFAULT_MIGRATED_UPDATED_AT',
                    `deleted` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`word_id`),
                    FOREIGN KEY(`deck_id`) REFERENCES `Deck`(`deck_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `_new_Word` (
                    `word_id`, `deck_id`, `term`, `meaning`, `phonetic`, `example`, `note`,
                    `status`, `correct_streak`, `wrong_count`, `interval_days`,
                    `next_review_at`, `last_reviewed_at`, `create_at`, `updated_at`, `deleted`
                )
                SELECT
                    `word_id`, `deck_id`, `term`, `meaning`, `phonetic`, `example`, `note`,
                    LOWER(`status`), `correct_streak`, `wrong_count`, `interval_days`,
                    `next_review_at`, `last_review_at`, `create_at`, '$DEFAULT_MIGRATED_UPDATED_AT', 0
                FROM `Word`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `Word`")
            db.execSQL("ALTER TABLE `_new_Word` RENAME TO `Word`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Word_deck_id` ON `Word` (`deck_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Word_term` ON `Word` (`term`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Word_deck_id_deleted` ON `Word` (`deck_id`, `deleted`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Word_status` ON `Word` (`status`)")

            // 3. Tạo mới bảng study_history và index
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `study_history` (
                    `entry_id` TEXT NOT NULL,
                    `word_id` TEXT NOT NULL,
                    `result` TEXT NOT NULL,
                    `mode` TEXT NOT NULL,
                    `created_at` TEXT NOT NULL,
                    PRIMARY KEY(`entry_id`),
                    FOREIGN KEY(`word_id`) REFERENCES `Word`(`word_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_study_history_word_id` ON `study_history` (`word_id`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_study_history_created_at` ON `study_history` (`created_at`)"
            )
        }
    }

    val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_2_3)
}
