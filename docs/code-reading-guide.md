# 程式碼導讀

> 這份文件是**地圖與註解**，不是程式碼的複述。目的是讓你自己讀 code 時知道
> 從哪裡開始、哪些設計是刻意的、哪些是壞的。
>
> 每個斷言都可以在 code 裡驗證，看到 `檔案:行號` 就去對。若你發現對不上，
> 以 code 為準，這份文件是錯的。
>
> **本文對應的版本**：`7edb57b`（master）。若你之後合併了 `origin/test/currency`
> 或設計分支，其中幾節會需要更新——文中會標明是哪幾節。

---

## 規模

```
app/src/main/java/   4,374 行 / 58 檔

  domain/              191 行   模型、錯誤型別、repository 介面
  data/                418 行   網路、DTO、repository 實作
  di/                  137 行   Hilt 綁定
  presentation/      3,007 行   ViewModel、mapper、畫面、元件、主題
  feature/             529 行   計算機（獨立元件）
  util/                 10 行
```

小到可以在一兩個小時內讀完全部。建議真的讀完，而不是抽樣。

---

## 建議閱讀順序

由內而外，每一層都比前一層知道得更少：

| # | 路徑 | 行數 | 為什麼先讀 |
|---|---|---|---|
| 1 | `domain/` | 191 | 沒有任何相依，最快建立詞彙表 |
| 2 | `data/network/` | ~90 | 錯誤分類的收斂點，整個 app 的失敗語意在這裡定義 |
| 3 | `data/datasource/` + `data/repository/` | ~330 | DTO → domain 的轉換邊界 |
| 4 | `presentation/viewmodel/` | ~250 | 核心機制在這（見「值得慢讀的地方」） |
| 5 | `presentation/mapper/` + `state/` | ~300 | domain → UI 的最後一哩 |
| 6 | `presentation/screen/` + `component/` | ~1,700 | 畫面本身 |
| 7 | `feature/calculator/` | 529 | 獨立元件（**目前的實作是壞的**，見下） |

---

## 分層

三層，相依一律指向內層，DTO 不越過資料層。

```
presentation/  ──依賴──▶  domain/  ◀──依賴──  data/
```

`domain/` 不 import 任何 `data`／`presentation`／Android／Ktor 的東西——
repository 介面定義在 `domain/repository/`，實作在 `data/repository/`，
用 Hilt `@Binds` 綁定。這是實質的依賴反轉，不是命名上的。

**驗證方式**（全部應該是 0）：

```bash
grep -r "import moozy.flightinformation.data"          app/src/main/java/moozy/flightinformation/domain/
grep -r "import moozy.flightinformation.presentation"  app/src/main/java/moozy/flightinformation/domain/
grep -r "import io.ktor"                               app/src/main/java/moozy/flightinformation/presentation/
grep -rn "Dto"                                         app/src/main/java/moozy/flightinformation/presentation/
```

最後一條是真正要守的界線：DTO 不能出現在 presentation。

**結構例外**：`feature/calculator/` 是第四個頂層目錄，不屬於三層任何一層。

---

## 值得慢讀的地方：抓取條件 = 有人在看 ∧ 資料已過期

`presentation/viewmodel/FlightsViewModel.kt`

```kotlin
val state: StateFlow<...> = flow {
    while (true) {
        emit(load())
        // 等使用者作廢資料，最多等一個新鮮期
        withTimeoutOrNull(FRESHNESS_MILLIS) { invalidated.receive() }
    }
}.stateIn(scope, SharingStarted.WhileSubscribed(5_000), Loading)
```

兩個條件各由一個機制表達：

- **「有人在看」** ← `WhileSubscribed`。收集停止就不再抓，所以 app 進背景會自動停止
  輪詢、回前景自動恢復，兩者都不需要額外處理。
- **「已過期」** ← 迴圈內的等待。新鮮期屆滿、或使用者主動下拉刷新，兩條路都是
  「資料失效了」。手動刷新後重新計時，是迴圈重新進入的自然結果。

這個設計是這個專案裡最漂亮的一段，`docs/adr/0001` 有完整論證。

### ⚠️ 但它目前被一行殘留碼抵銷了

`MainActivity.kt:22-23`

```kotlin
val viewModel: FlightsViewModel = hiltViewModel()
val uiState by viewModel.state.collectAsStateWithLifecycle()   // uiState 沒有被用到

//                FlightScreen(flightArrivalsUiState = uiState)
//                CurrencyScreen()

AppNavDisplay()
```

`uiState` 完全沒有被使用——底下兩行消費它的程式碼都被註解掉了，真正渲染的是
`AppNavDisplay()`。但**這個收集是活的**，而且與 `AppNavDisplay` 取到同一個 VM 實例
（同一個 `ViewModelStoreOwner`），所以 `WhileSubscribed` 永遠看得到訂閱者。

實際後果：**使用者停在匯率頁時，航班 API 仍然每 10 秒被打一次**，ADR 0001 整篇
論證的核心不變量在執行期不成立。ViewModel 本身寫得完全正確，問題只在這一行。

刪掉它，不變量就成立。

---

## 錯誤處理

失敗在資料層就收斂成 `domain/error/LoadError.kt` 的五種：
`NoNetwork` / `Timeout` / `Server(code)` / `Malformed` / `Unknown`。

`data/network/KtorHttpRequesterImpl.kt` 有三個容易被寫錯、而這裡寫對了的細節：

1. **狀態碼在解析之前檢查**。錯誤回應也可能帶著合法 JSON——金鑰失效時
   freecurrencyapi 回的就是。只靠「解析失敗」判斷會把它當成資料。
2. **`CancellationException` 在寬 catch 之前重新拋出**。吞掉它會破壞結構化並行，
   把協程取消變成假的 `LoadError`。
3. **timeout 的比對排在 `IOException` 之前**，否則永遠走不到 `Timeout` 分支。

原始例外不會流到畫面上，文案由 `presentation/mapper/LoadErrorMessages.kt`
依錯誤類別對應字串資源。

刷新失敗時航班頁保留既有內容（`FlightsViewModel` 的 `load(previous)`），
但**匯率頁沒有這樣做**——`CurrencyViewModel.getLatestCurrencies` 直接
`_state.value = fold(...)`，任何刷新失敗都會清空已載入的匯率。兩邊政策不一致。

---

## 測試

30 個 @Test（26 個 JVM + 4 個 instrumented）。

| 檔案 | @Test |
|---|---|
| `FlightsViewModelTest` | 10 |
| `FlightsRepositoryImplTest` | 7 |
| `KtorHttpRequesterImplTest` | 6 |
| `FlightsScreenTest`（instrumented） | 4 |
| DTO / API 測試 ×3 | 3 |

測試以 fake 而非 mock 驅動（`testing/FakeFlightsRepository.kt`），斷言的是行為契約
而非特定實作。`KtorHttpRequesterImplTest` 裡那個「4xx 但 body 可解析」的迴歸測試
值得看，它守的是上面錯誤處理第 1 點。

> `origin/test/currency` 分支上還有 122 個測試沒合併進來（含 `FlightUiMapperTest`
> 35 個、`CurrencyViewModelTest`、`CurrencyMappersTest` 等）。master 目前只有 30 個。

---

## 讀到這幾個地方時要知道它們是壞的

這些不是風格問題，是實際會產生錯誤結果的缺陷。健康檢查時逐一驗證過，
部分已在 `fix/health-check` 分支修好但尚未合併。

### `feature/calculator/Calculator.kt` — 算術結果是錯的

實際執行結果（不是推論）：

```
10 - 2 + 3        = 5                      應為 11
10 - 3 - 4        = 11                     應為 3
8 ÷ 4 ÷ 2         = 4                      應為 1
2 × 3 × 4 + 1     = 26                     應為 25
0.1 + 0.2         = 0.30000000000000004
100000 × 100000   = 2147483647             Int 飽和
1 ÷ 0             = Infinity
1 + 2 )           → EmptyStackException 逃出 cal()
```

根因在 `infixToPostfix` 的 `operatorCheck`：pop 條件只看堆疊頂是不是 `×`/`÷`，
一次只 pop 一個；而 `×`/`÷` 自己完全不 pop。所以左結合性沒有實作，連續兩個 `×`
之後遇到 `+` 連優先序都會崩。

`+` 和 `×` 因為滿足結合律看不出來，所以人工試按很容易漏掉。

整個檔案零測試，而且**結構上無法測試**：`cal()` 內散布 `android.util.Log` 呼叫，
而專案沒設 `testOptions.unitTests.isReturnDefaultValues`，JVM 測試會直接拋
"not mocked"。

### `CurrencyViewModel.kt:45` — 幣別清單是隨機的

```kotlin
codes = CurrencyCode.entries.shuffled().take(15).toSet()
```

33 個幣別隨機取 15 個，沒有 seed。每次冷啟動顯示的清單都不同，bug report 無法重現。

更嚴重的是它同時要求 `base = USD`，但 USD 有 **55% 機率不在那 15 個裡**。USD 不在
回應中時，`CurrencyMappers.nextContent` 的基準會靜默 fallback 到排序後的第一列，
於是**所有換算數字都是錯的，而每列標籤仍寫「1 USD = …」**。

### `CurrencyScreen.kt` — 轉螢幕會把使用者鎖死

開著計算機轉螢幕 → 導覽列永久消失、返回鍵直接退出 app。實機驗證過。

根因是 `showCalculator` 用普通 `remember`（Activity 重建後歸零），而導覽列的
scaffold 狀態用 `rememberSaveable`（正確還原成 Hidden）。兩者失去同步後，唯一能
呼叫 `onCalculatorDismiss()` 的 `BackHandler(showCalculator)` 也一併失效。

更根本的解法是計算機改用 `ModalBottomSheet`——它本來就畫在導覽列之上，
根本不需要隱藏任何東西，那整類 bug 會消失而不是被補起來。

### 其他

| 位置 | 問題 |
|---|---|
| `FlightScreen.kt` | `AnimatedContent` 用整個 `Content`（含每 10 秒變的 `updatedAt`）當 key，捲動位置每次輪詢彈回頂端 |
| `FlightScreen.kt` | `items.isEmpty()` 分支沒吃 `innerPadding` 也沒有下拉刷新 |
| `FlightArrivalCard.kt:150-155` | 六組寫死的 hex 色碼配 `isDarkTheme` 手動分支，沒用 `colorScheme` |
| `CurrencyScreen.kt` | 刷新時彈出 `Dialog({})`，空的 `onDismissRequest` 讓返回鍵被吃掉最多 10 秒 |
| `CurrencyScreen.kt` | FAB 寫著 "Search" 但它是幣別選擇器，沒有任何搜尋功能；而且蓋住清單內容 |
| `CurrencyItem.kt` | 匯率顯示到小數 10 位（`316.8400616188`），且非等寬數字，刷新時位數會跳 |

---

## 讀的時候可以帶著的問題

- `domain/` 真的沒有相依嗎？（用上面的 grep 驗）
- 輪詢在什麼情況下會停？（提示：目前答案是「不會」，找出為什麼）
- 如果 API 回 200 但 body 是錯誤訊息，會發生什麼？
- 計算機的 `Double` 有沒有可能漏進 `BigDecimal` 管線？
- 哪些測試是在測行為契約，哪些其實只是在測 kotlinx.serialization？

---

## 相關文件

- `docs/adr/0001` — 抓取條件為什麼這樣設計
- `docs/adr/0002` — 為什麼要有 domain model 這一層
- `docs/adr/0003` — 匯率為什麼改成按需載入
