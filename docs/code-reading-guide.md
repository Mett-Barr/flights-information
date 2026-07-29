# 程式碼導讀

這份文件是**地圖與註解**，不是程式碼的複述。目的是讓讀的人知道從哪裡開始、
每一層負責什麼、以及哪幾個設計決定值得慢下來看。

每個斷言都可以在 code 裡驗證。若發現對不上，以 code 為準。

---

## 規模

| 模組 | 檔 | 行 |
|---|---:|---:|
| `:app` | 10 | 460 |
| `:core:data` | 16 | 326 |
| `:core:domain` | 10 | 176 |
| `:core:ui` | 4 | 376 |
| `:feature:flights` | 10 | 2,076 |
| `:feature:currency` | 10 | 2,213 |
| `:feature:calculator` | 3 | 827 |

`:app` 只保留組裝責任：`di/`、`navigation/` 與 `component/`。航班與匯率的畫面、ViewModel、mapper、UI state 與 previews 已分別在 `:feature:flights` 與 `:feature:currency`。

兩個畫面仍是行數的大宗——它們的版面（時間軸的軌道與節點、
匯率的卡片網格）都在 Compose 裡直接繪製，沒有拆成一堆小元件。

---

## 建議閱讀順序

由內而外，每一層都比前一層知道得更少：

| # | 路徑 | 為什麼先讀 |
|---|---|---|
| 1 | `:core:domain` | 零相依，最快建立詞彙表 |
| 2 | `:core:data` 的 `network/` | 錯誤分類的收斂點，整個 app 的失敗語意在此定義 |
| 3 | `:core:data` 的 `datasource/` + `repository/` | DTO → domain 的轉換邊界 |
| 4 | `:feature:flights/FlightsViewModel.kt` | 核心抓取機制在這（見下節） |
| 5 | `:feature:flights/FlightUiMapper.kt` + UI state | domain → UI 的最後一哩 |
| 6 | `:feature:flights/FlightScreen.kt`、`FlightTimeline*.kt` 與 `:feature:currency` | 兩個功能畫面本身 |
| 7 | `:app` 的 `navigation/AppNavDisplay.kt` | Navigation 3、nav entry 的收集作用域、back stack、deep link、window size class 自適應 |
| 8 | `:core:ui` 與 `:feature:calculator` | 共享呈現資源與獨立元件，可最後看 |

---

## 分層

核心仍分三層，相依一律指向內層，DTO 不越過資料層；共享 UI 與三個功能各自是獨立模組。

```
:app  ──依賴──▶  :core:data ──依賴──▶ :core:domain
 :app  ──依賴──▶  :core:ui ───依賴──▶ :core:domain
 :app  ──依賴──▶  :feature:flights ──依賴──▶ :core:ui、:core:domain
 :app  ──依賴──▶  :feature:currency ──依賴──▶ :core:ui、:core:domain、:feature:calculator
```

repository 介面定義在 `:core:domain` 的 `repository/`，實作在 `:core:data` 的 `repository/`，
以 Hilt `@Binds` 綁定。依賴反轉是實質的，不是命名上的。

可驗證：

```bash
rg -n "import moozy.flightinformation.data" core/domain/src/main          # 無結果
rg -n "import moozy.flightinformation.feature" core/domain/src/main       # 無結果
rg -n "import io.ktor|Dto" feature/flights/src/main feature/currency/src/main core/ui/src/main # 無結果
```

最後一條是真正要守的界線：DTO 與 Ktor 不出現在 feature 或共享 UI；`:app` 的 DI 組裝可持有 Ktor client。

最內層的 `:core:domain` 現在是純 Kotlin JVM 模組；Android 與 Ktor 型別不在它的編譯 classpath，
因此這條邊界由模組結構與編譯器強制，而不只靠目錄約定。
`app/src/test/java/moozy/flightinformation/architecture/LayeringTest.kt` 的 Konsist 架構測試仍然必要：
它守 DTO 邊界，以及模組邊界本身無法表達的規則；測試跑在一般的 `./gradlew test` 裡，違規會讓 CI 失敗。

**計算機模組**：`:feature:calculator` 是獨立的 Android library 模組，不再是 `:app` 下的第四個頂層目錄。
它是自足的元件，對外只暴露 `Calculator` 與 `CalculatorUI`。

---

## 值得慢讀的地方：抓取條件 = 有人在看 ∧ 資料已過期

`:feature:flights` 的 `FlightsViewModel.kt`。它的 state 由 `:app` 的
`navigation/AppNavDisplay.kt` 中 `entry<NavRoute.Flights>` 收集，再把已收集的
`FlightArrivalsUiState` 傳給 `FlightsScreen`。

```kotlin
val state: StateFlow<...> = flow {
    var refreshAttempt = 0L
    while (true) {
        if (current is Content) emit(current.copy(isRefreshing = true))
        current = load(current)
        if (current is Content) current = current.copy(refreshAttempt = ++refreshAttempt)
        emit(current)
        // 等資料失效，最多等一個新鮮期
        withTimeoutOrNull(FRESHNESS) { invalidated.receive() }
    }
}.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE), Loading)
```

每次完成嘗試都會遞增 `refreshAttempt`；倒數環以此重啟，失敗時也不會停住。

兩個條件各由一個機制表達：

- **「有人在看」** ← `WhileSubscribed`。收集停止就不再抓，所以進背景自動停止輪詢、
  回前景自動恢復，兩者都不需要額外處理。
- **「已過期」** ← 迴圈內的等待。新鮮期屆滿、或使用者主動刷新，兩條路都是
  「資料失效了」。手動刷新後重新計時，是迴圈重新進入的自然結果。

每次抓取既有內容時都會顯示同一個刷新指示器，無論資料是因為新鮮期屆滿或使用者主動作廢。

`docs/adr/0001` 記錄了完整論證。

### 這個設計的前提

收集必須發生在「使用者實際在看那個畫面」的作用域內。這裡是
`entry<NavRoute.Flights>`；`SinglePaneSceneStrategy` 只組合最上層 entry，因此離開航班
entry 時 collector 會停止，`WhileSubscribed` 才真正表示使用者正在看航班板。把收集提升到
共用作用域，輪詢就不會停——而且畫面上看不出任何異狀。

---

## 值得慢讀的地方：錯誤在資料層就收斂

失敗在 `:core:data` 的 `network/KtorHttpRequesterImpl.kt` 收斂成 `:core:domain` 的 `error/LoadError.kt`
的五種：`NoNetwork` / `Timeout` / `Server(code)` / `Malformed` / `Unknown`。

三個容易寫錯而這裡寫對的細節：

1. **狀態碼在解析之前檢查。** 錯誤回應也可能帶著合法 JSON——金鑰失效時
   freecurrencyapi 回的就是。只靠「解析失敗」判斷會把它當成資料。
   `KtorHttpRequesterImplTest` 有一個「4xx 但 body 可解析」的迴歸測試守這條。
2. **`CancellationException` 在寬 catch 之前重新拋出。** 吞掉它會破壞結構化並行，
   把協程取消變成假的 `LoadError`。
3. **timeout 的比對排在 `IOException` 之前**，否則永遠走不到 `Timeout` 分支。

原始例外不會流到畫面上，文案由 `:core:ui` 的 `LoadErrorMessages.kt`
依錯誤類別對應字串資源。

刷新失敗時保留既有內容而非清空畫面，只有從頭就無資料可顯示時才進錯誤畫面。

---

## 測試

158 個 JVM 單元測試 + 11 個 instrumented 測試。全部不觸及網路，毫秒級完成。

| 模組 | 檔案 | @Test | 涵蓋 |
|---|---|---:|---|
| `:app` | `LayeringTest` | 5 | 完整專案的分層規則 |
| `:core:data` | data 測試 | 22 | DTO、URL、HTTP 錯誤與 repository 映射 |
| `:feature:flights` | `FlightUiMapperTest`、`FlightsViewModelTest` | 51 | 呈現映射與抓取條件 |
| `:feature:currency` | `CurrencyMappersTest`、`CurrencyViewModelTest` | 29 | 換算、捨入與狀態轉換 |
| `:feature:calculator` | 計算機測試 | 51 | 運算式、運算子優先序、括號、負號與輸入狀態矩陣 |
| `:app` | `AppNavigationTest`（instrumented） | 1 | deep link 與 nav entry |
| `:feature:flights` | `FlightsScreenTest`（instrumented） | 4 | 航班畫面狀態 |
| `:feature:currency` | `CurrencyScreenTest`（instrumented） | 6 | 匯率畫面狀態 |

以 fake 而非 mock 驅動（`:feature:flights` 的 `FakeFlightsRepository.kt`），斷言的是行為契約
而非特定實作，所以更換計時或載入方式時不需要改測試。

時鐘是注入的（`FlightsViewModel` 的 `clock: () -> LocalDateTime`），測試中固定，
因此沒有時間相依的不穩定測試。

CI 執行 `./gradlew build detekt lint`。

---

## 讀的時候可以帶著的問題

- `:core:domain` 真的沒有相依嗎？（用上面的 grep 驗）
- 輪詢在什麼情況下會停？
- 如果 API 回 200 但 body 是錯誤訊息，會發生什麼？
- 哪些測試在測行為契約，哪些其實只是在測 kotlinx.serialization？

---

## 相關文件

- `docs/adr/` — 架構決策紀錄
- `docs/git-conventions.md` — 分支與提交慣例
- `README.md` — 模組相依圖（由 `./gradlew createModuleGraph` 產生）
