# 程式碼導讀

這份文件是**地圖與註解**，不是程式碼的複述。目的是讓讀的人知道從哪裡開始、
每一層負責什麼、以及哪幾個設計決定值得慢下來看。

每個斷言都可以在 code 裡驗證。若發現對不上，以 code 為準。

---

## 規模

```
app/src/main/java/   6,830 行 / 60 檔

  domain/              191 行   模型、錯誤型別、repository 介面
  data/                373 行   網路、DTO、repository 實作
  di/                  137 行   Hilt 綁定
  presentation/      5,153 行   ViewModel、mapper、畫面、導覽、元件、主題
  feature/             937 行   計算機（獨立元件）
  util/                 10 行
```

兩個畫面各約 1,400 行，是行數的大宗——它們的版面（時間軸的軌道與節點、
匯率的卡片網格）都在 Compose 裡直接繪製，沒有拆成一堆小元件。

---

## 建議閱讀順序

由內而外，每一層都比前一層知道得更少：

| # | 路徑 | 為什麼先讀 |
|---|---|---|
| 1 | `domain/` | 零相依，最快建立詞彙表 |
| 2 | `data/network/` | 錯誤分類的收斂點，整個 app 的失敗語意在此定義 |
| 3 | `data/datasource/` + `data/repository/` | DTO → domain 的轉換邊界 |
| 4 | `presentation/viewmodel/` | 核心機制在這（見下節） |
| 5 | `presentation/mapper/` + `state/` | domain → UI 的最後一哩 |
| 6 | `presentation/screen/` + `component/` | 畫面本身 |
| 7 | `presentation/navigation/` | Navigation 3：back stack、deep link、window size class 自適應 |
| 8 | `feature/calculator/` | 獨立元件，可最後看 |

---

## 分層

三層，相依一律指向內層，DTO 不越過資料層。

```
presentation/  ──依賴──▶  domain/  ◀──依賴──  data/
```

repository 介面定義在 `domain/repository/`，實作在 `data/repository/`，
以 Hilt `@Binds` 綁定。依賴反轉是實質的，不是命名上的。

可驗證：

```bash
grep -r "import moozy.flightinformation.data"          .../domain/        # 0
grep -r "import moozy.flightinformation.presentation"  .../domain/        # 0
grep -r "import io.ktor"                               .../presentation/  # 0
grep -rn "Dto"                                         .../presentation/  # 0
```

最後一條是真正要守的界線：DTO 不出現在 presentation。

這些規則由 `app/src/test/java/moozy/flightinformation/architecture/LayeringTest.kt`
的 Konsist 架構測試強制，跑在一般的 `./gradlew test` 裡。違規會讓 CI 失敗，
不再只靠作者紀律。

**結構例外**：`feature/calculator/` 是第四個頂層目錄，不屬於三層任何一層。
它是自足的元件，對外只暴露 `Calculator` 與 `CalculatorUI`。

---

## 值得慢讀的地方：抓取條件 = 有人在看 ∧ 資料已過期

`presentation/viewmodel/FlightsViewModel.kt`

```kotlin
val state: StateFlow<...> = flow {
    while (true) {
        current = load(current)
        emit(current)
        // 等使用者作廢資料，最多等一個新鮮期
        refreshWasUserInitiated =
            withTimeoutOrNull(FRESHNESS_MILLIS) { invalidated.receive() } != null
    }
}.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), Loading)
```

兩個條件各由一個機制表達：

- **「有人在看」** ← `WhileSubscribed`。收集停止就不再抓，所以進背景自動停止輪詢、
  回前景自動恢復，兩者都不需要額外處理。
- **「已過期」** ← 迴圈內的等待。新鮮期屆滿、或使用者主動下拉刷新，兩條路都是
  「資料失效了」。手動刷新後重新計時，是迴圈重新進入的自然結果。

回傳值區分刷新來源：只有使用者主動刷新才顯示指示器，背景輪詢不打擾畫面。

`docs/adr/0001` 記錄了完整論證。

### 這個設計的前提

收集必須發生在「使用者實際在看那個畫面」的作用域內。把 `collectAsStateWithLifecycle()`
提升到畫面之外的共用作用域，輪詢就永遠不會停——而且畫面上看不出任何異狀。
改動導覽層時要留意這點。

---

## 值得慢讀的地方：錯誤在資料層就收斂

失敗在 `data/network/KtorHttpRequesterImpl.kt` 收斂成 `domain/error/LoadError.kt`
的五種：`NoNetwork` / `Timeout` / `Server(code)` / `Malformed` / `Unknown`。

三個容易寫錯而這裡寫對的細節：

1. **狀態碼在解析之前檢查。** 錯誤回應也可能帶著合法 JSON——金鑰失效時
   freecurrencyapi 回的就是。只靠「解析失敗」判斷會把它當成資料。
   `KtorHttpRequesterImplTest` 有一個「4xx 但 body 可解析」的迴歸測試守這條。
2. **`CancellationException` 在寬 catch 之前重新拋出。** 吞掉它會破壞結構化並行，
   把協程取消變成假的 `LoadError`。
3. **timeout 的比對排在 `IOException` 之前**，否則永遠走不到 `Timeout` 分支。

原始例外不會流到畫面上，文案由 `presentation/mapper/LoadErrorMessages.kt`
依錯誤類別對應字串資源。

刷新失敗時保留既有內容而非清空畫面，只有從頭就無資料可顯示時才進錯誤畫面。

---

## 測試

94 個 JVM 單元測試 + 11 個 instrumented。全部不觸及網路，毫秒級完成。

| 檔案 | @Test | 涵蓋 |
|---|---|---|
| `FlightUiMapperTest` | 35 | 狀態正規化、空值、無法解析的時間 |
| `FlightsViewModelTest` | 15 | 抓取條件：無人收集不抓、新鮮期內不重抓、手動刷新重新計時 |
| `CurrencyMappersTest` | 13 | 換算數學、捨入邊界 |
| `CurrencyViewModelTest` | 9 | 狀態轉換 |
| `FlightsRepositoryImplTest` | 7 | DTO → domain 映射 |
| `KtorHttpRequesterImplTest` | 6 | 錯誤分類，含 4xx-可解析的迴歸測試 |
| `CurrencyRepositoryImplTest` | 6 | repository 契約 |
| `CurrencyScreenTest`（instrumented） | 6 | 載入／錯誤／內容狀態 |
| `FlightsScreenTest`（instrumented） | 4 | 同上 |
| DTO / API / 導覽測試 | 4 | 反序列化、URL、分頁切換 |

以 fake 而非 mock 驅動（`testing/FakeFlightsRepository.kt`），斷言的是行為契約
而非特定實作，所以更換計時或載入方式時不需要改測試。

時鐘是注入的（`FlightsViewModel` 的 `clock: () -> LocalDateTime`），測試中固定，
因此沒有時間相依的不穩定測試。

CI 執行 `assembleDebug test`；instrumented 測試需要裝置，目前不在 CI 內。

---

## 讀的時候可以帶著的問題

- `domain/` 真的沒有相依嗎？（用上面的 grep 驗）
- 輪詢在什麼情況下會停？
- 如果 API 回 200 但 body 是錯誤訊息，會發生什麼？
- 哪些測試在測行為契約，哪些其實只是在測 kotlinx.serialization？

---

## 相關文件

- `docs/adr/` — 架構決策紀錄
- `docs/git-conventions.md` — 分支與提交慣例
