import core.appModule
import org.koin.core.context.startKoin

// starts the koin DI. Must be called from swift also on app start!
// see https://insert-koin.io/docs/quickstart/kmp/
fun initKoin() {
    startKoin {
        modules(appModule)
    }
}