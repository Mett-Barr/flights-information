package moozy.flightinformation.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class LayeringTest {

    @Test
    fun `domain 不得匯入 data 或 UI modules`() {
        domainFiles().assertTrue(
            additionalMessage =
                "domain 不得依賴 data 或 UI modules；內層必須保持獨立，讓資料來源與 UI 可以替換。",
        ) { file ->
            file.imports.none { importDeclaration ->
                importDeclaration.name.startsWith("moozy.flightinformation.data.") ||
                    importDeclaration.name.startsWith("moozy.flightinformation.feature.") ||
                    importDeclaration.name.startsWith("moozy.flightinformation.core.ui.")
            }
        }
    }

    @Test
    fun `domain 不得匯入 Android API`() {
        domainFiles().assertTrue(
            additionalMessage =
                "domain 不得依賴 Android API；領域模型與 repository 介面必須可在純 JVM 環境使用與測試。",
        ) { file ->
            file.imports.none { importDeclaration ->
                importDeclaration.name.startsWith("android.") ||
                    importDeclaration.name.startsWith("androidx.")
            }
        }
    }

    @Test
    fun `domain 不得匯入 Ktor`() {
        domainFiles().assertTrue(
            additionalMessage =
                "domain 不得依賴 Ktor；HTTP 實作屬於 data 層，避免網路細節滲入商業規則。",
        ) { file ->
            file.imports.none { importDeclaration ->
                importDeclaration.name.startsWith("io.ktor.")
            }
        }
    }

    @Test
    fun `UI modules 不得匯入 DTO`() {
        // NavRoute 的 Serializable 是 Navigation 3 保存 back stack 所需，並非 DTO 外洩。
        uiFiles().assertTrue(
            additionalMessage =
                "UI modules 不得接觸 DTO；DTO 必須在 data 層轉換為 domain model，避免網路契約滲入 UI。",
        ) { file ->
            file.imports.none { importDeclaration ->
                importDeclaration.name.endsWith("Dto")
            }
        }
    }

    @Test
    fun `data 不得匯入 UI modules`() {
        dataFiles().assertTrue(
            additionalMessage =
                "data 不得依賴 UI modules；資料取得與轉換必須可在沒有 UI 的情況下重複使用。",
        ) { file ->
            file.imports.none { importDeclaration ->
                importDeclaration.name.startsWith("moozy.flightinformation.feature.") ||
                    importDeclaration.name.startsWith("moozy.flightinformation.core.ui.") ||
                    importDeclaration.name.startsWith("moozy.flightinformation.navigation.") ||
                    importDeclaration.name.startsWith("moozy.flightinformation.component.")
            }
        }
    }

    private fun domainFiles(): List<KoFileDeclaration> = filesIn("domain")

    private fun uiFiles(): List<KoFileDeclaration> = Konsist.scopeFromProject().files.filter { file ->
        val packageName = file.packagee?.name ?: return@filter false
        packageName.startsWith("moozy.flightinformation.feature.") ||
            packageName.startsWith("moozy.flightinformation.core.ui.") ||
            packageName.startsWith("moozy.flightinformation.navigation.") ||
            packageName.startsWith("moozy.flightinformation.component.")
    }

    private fun dataFiles(): List<KoFileDeclaration> = filesIn("data")

    // scopeFromProject 會讀取所有 Gradle module，讓分模組後的規則仍檢查完整分層。
    private fun filesIn(layer: String): List<KoFileDeclaration> =
        Konsist.scopeFromProject().files.filter { file ->
            val packageName = "moozy.flightinformation.$layer"
            // packagee 可為 null（沒有 package 宣告的檔案），這種檔案不屬於任何一層。
            val filePackage = file.packagee?.name ?: return@filter false
            filePackage == packageName || filePackage.startsWith("$packageName.")
        }
}
