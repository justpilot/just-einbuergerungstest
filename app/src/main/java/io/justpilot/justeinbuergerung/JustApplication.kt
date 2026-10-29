package io.justpilot.justeinbuergerung

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JustApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Room observers emit once the import transaction commits, so screens need no extra signal.
        applicationScope.launch(Dispatchers.IO) { container.catalogImporter.importIfNeeded() }
    }
}
