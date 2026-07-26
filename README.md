# 航班資訊

高雄國際航空站的國內線到達看板，附即時匯率換算與計算機。

- **Demo 影片**：https://www.youtube.com/watch?v=n8gBh9hrWUc
- **資料來源**：[高雄航空站即時時刻表](https://www.kia.gov.tw/Announce/NewsArea/InstantSchedule_DOMARR.json)、[freecurrencyapi](https://freecurrencyapi.com/)

### 關於資料來源網址

題目給的是 `…/API/InstantSchedule.ashx?AirFlyLine=2&AirFlyIO=2`，本專案請求的是 `…/Announce/NewsArea/InstantSchedule_DOMARR.json`。兩者指向同一份資料：前者目前回傳的是一頁以 JavaScript 轉址的 HTML，其對應規則為

```js
if (line === "2" && io === "2") target = "InstantSchedule_DOMARR.json";
window.location.href = "/Announce/NewsArea/" + target
```

`AirFlyLine=2` 為國內線、`AirFlyIO=2` 為到達，因此對應到的正是本專案請求的檔案。由於轉址發生在瀏覽器執行 JS 之後、而非 HTTP 3xx，HTTP client 無法跟隨，直接請求目標檔案是唯一可行的方式。

題目文字寫「桃園機場」，但網址屬高雄航空站（`kia.gov.tw`），此處以端點為準。

## 執行

需要 JDK 17 與 Android SDK 37。

匯率 API 需要金鑰，請在專案根目錄的 `local.properties` 加入：

```properties
free_currency_api_key=你的金鑰
```

金鑰可於 [freecurrencyapi.com](https://freecurrencyapi.com/) 免費註冊取得。未填仍可建置與執行，但匯率頁會顯示錯誤（航班頁不受影響，該 API 不需金鑰）。

```bash
./gradlew assembleDebug   # 建置
./gradlew test            # 單元測試
```

## 需求對照

**基本**

- [x] 使用 Kotlin
- [x] MVVM
- [x] 專案上傳至 GitHub 並公開
- [x] 螢幕錄製上傳 YouTube（非 Shorts、非公開）

**必要**

- [x] 顯示航班資訊、狀態、時間
- [x] 每十秒更新一次
- [x] 錯誤處理
- [x] 顯示六種以上幣別匯率並預設某種幣別

**加分**

- [x] Coding Style 與架構分層
- [x] 客製化計算機：點選幣別開啟，輸入金額後即時換算並在清單同步更新
- [x] UI/UX：Material 3、共享元素轉場、下拉刷新、骨架載入
- [x] 滑動動畫與畫面特效
- [x] 使用第三方 library
- [x] 單元測試（59 項）與 UI 測試（4 項）
- [x] 支援螢幕轉向與深色模式

## 架構

三層，依賴指向內層，DTO 不越過資料層。

```
presentation/                UI、UiState、ViewModel
├─ screen/ component/        Composable
├─ state/                    UiState（sealed）與 UI model
├─ mapper/                   domain → UI：格式化、文案、顏色
├─ viewmodel/
├─ navigation/               兩頁的切換與狀態保存
└─ theme/

domain/                      業務語彙，不依賴任何外層
├─ model/                    FlightArrival、FlightStatus、Currencies
├─ repository/               Repository 介面（實作在 data）
├─ error/                    LoadError
└─ value/                    值物件與型別別名

data/                        I/O 與格式收斂
├─ datasource/               單一來源存取（api / dto / url）
├─ network/                  HttpClient 封裝與錯誤分類
└─ repository/               DTO → domain 映射

feature/calculator/          狀態驅動的運算式計算機
di/                          Hilt 綁定
util/                        純工具
```

**資料流**

```
DTO ──(data mapper)──▶ domain model ──(presentation mapper)──▶ UI model
   狀態正規化                 業務事實              格式化、文案、顏色
```

`airFlyStatus` 回傳的是中英混雜的字串（`"抵達"`、`"ARRIVED"`、`"延誤"`…），在資料層收斂成 `FlightStatus` 這個封閉集合，未知值以 `Unknown(raw)` 保留原文而非丟棄。時間欄位為 `LocalTime?`：來源只給 `"HH:mm"`、不帶日期與時區，而看板顯示的本就是機場當地時間，不做換算。

### 抓取時機

抓取由一個條件決定，而非由事件觸發：

> **有人在看 ∧ 資料已失效**

```kotlin
val state = flow {
    while (true) {
        emit(load())
        userInitiated = withTimeoutOrNull(FRESHNESS_MILLIS) { invalidated.receive() } != null
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)
```

`WhileSubscribed` 表達「有人在看」——收集停止就不再抓，所以 app 進入背景會自動停止輪詢、回到前景自動恢復，兩者都不需要額外處理。迴圈內的等待表達「已失效」：新鮮期屆滿，或使用者主動下拉刷新。手動刷新後重新計時是迴圈重新進入的自然結果。

等待的回傳值還帶著第二個訊息：是誰讓資料失效的。使用者下拉時顯示刷新指示器，十秒一次的自動更新則安靜進行——否則指示器會固定閃爍，反而變成雜訊。

畫面另外顯示這批資料的抓取時刻。刷新失敗時保留的是舊內容，時刻也維持舊值，使用者因此分得出眼前是新資料還是上一次成功的結果。即時看板顯示過期資料卻不標示，比顯示不出來更糟。

### 錯誤處理

失敗在資料層就收斂成 `LoadError`（`NoNetwork` / `Timeout` / `Server(code)` / `Malformed` / `Unknown`）。狀態碼會顯式檢查，因為錯誤回應也可能帶著合法 JSON——例如金鑰失效時 freecurrencyapi 回的就是；只靠「解析失敗」判斷會把它當成資料。原始例外不會流到畫面上，文案由 presentation 依錯誤類別對應字串資源。

刷新失敗時保留既有內容而非清空畫面，只有從頭就無資料可顯示時才進入錯誤畫面。

### 計算機

輸入由 `CurrentState` 的狀態機驅動：八個狀態決定當下哪些按鍵合法，UI 依此停用不可按的鍵。運算採中綴轉後綴（Shunting-yard），支援四則運算、優先序、括號巢狀與負號。

## 測試

59 項單元測試與 4 項 Compose UI 測試，皆不觸及網路，毫秒級完成。

| 範圍 | 內容 |
|---|---|
| `KtorHttpRequesterImplTest` | 錯誤分類；含「4xx 但 body 可解析」的迴歸測試，以及「原始例外不得外洩」的契約測試 |
| `FlightsRepositoryImplTest` | DTO → domain 映射、狀態正規化、空值與無法解析的時間 |
| `FlightsViewModelTest` | 抓取條件：無人收集時不抓、新鮮期內不重抓、手動刷新重新計時、收集停止即停；資料時刻的更新與保留；指示器只在手動刷新時出現 |
| `CurrencyRepositoryImplTest` | 空幣別清單不發請求、CSV 組裝、base 預設值、未知幣別與無法解析的匯率 |
| `CurrencyMappersTest` | 換算公式、基準幣別的優先序、除以零與缺匯率的退場、小數位上限與進位 |
| `CurrencyViewModelTest` | 載入與失敗的狀態轉換、重複載入只請求一次、選取切換、刷新不引入人為延遲 |
| `FlightsScreenTest` | 載入、內容、無網路、空清單四種畫面 |
| DTO 測試 | 反序列化 |

測試以 fake 而非 mock 驅動，斷言的是行為契約而非特定實作，因此更換計時或載入方式時不需要改測試。

有幾條斷言是刻意挑的。`CurrencyViewModelTest` 用 `testScheduler.currentTime == 0` 斷言刷新不消耗虛擬時間——比檢查「原始碼裡沒有 `delay`」可靠，因為它管的是行為而非寫法。`CurrencyMappersTest` 斷言基準幣別在多次輸入之間存活，這條抓出過一個真實缺陷：狀態物件是重建而非 `copy()`，漏寫的欄位會靜默退回預設值 `null`，而編譯器不會有任何抱怨。「自動更新不顯示指示器」則是用 `expectNoEvents()` 斷言請求飛行期間沒有任何中間狀態送出——只檢查最終值會漏掉閃爍。

編譯器警告視為錯誤（`allWarningsAsErrors`），CI 於每次推送與 PR 執行建置與測試。

## 技術

| 分類 | 使用 |
|---|---|
| 語言／建置 | Kotlin 2.3.21、AGP 9.3.1、Gradle 9.6.1、JDK 17 |
| UI | Jetpack Compose（BOM 2026.06.01）、Material 3、Adaptive |
| 非同步 | Coroutines、Flow |
| 網路 | Ktor 3.5.1（OkHttp 引擎）、kotlinx.serialization |
| DI | Hilt 2.60.1（KSP） |
| 圖片 | Coil 3 |
| 測試 | JUnit4、kotlinx-coroutines-test、Turbine、Ktor MockEngine |

所有依賴皆為穩定版，無 alpha、beta 或 rc。

## 決策紀錄

架構上的取捨記於 [`docs/adr/`](docs/adr/)。
