package de.ph1b.audiobook.data.repo.internals.migrations

import android.arch.persistence.db.SupportSQLiteDatabase

class Migration43to44 : IncrementalMigration(43) {
  override fun migrate(database: SupportSQLiteDatabase) {
    //empty because of initial room migration
  }
}
