package com.wordnest

import android.app.Application
import com.wordnest.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application gốc của WordNest: khởi tạo [AppContainer] dùng chung và nạp dữ liệu mẫu
 * lần đầu (idempotent) trên [Dispatchers.IO].
 */
class WordNestApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applicationScope.launch {
            container.seeder.seedIfEmpty()
        }
    }
}
