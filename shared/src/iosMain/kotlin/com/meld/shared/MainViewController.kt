import androidx.compose.ui.window.ComposeUIViewController
import com.meld.shared.ui.MeldApp
import platform.UIKit.UIViewController

/**
 * iOS entry point consumed from Swift as `MainViewControllerKt.mainViewController()`.
 * The `MainViewControllerKt` facade name follows the Kotlin/Native export rule
 * (file name + `Kt`, no package declared in this file on purpose); CI verifies
 * the symbol against the generated `MeldShared.h` header.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { MeldApp() }
