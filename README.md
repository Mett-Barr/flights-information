專案架構：
data/                                      ← 資料層：I/O、DTO、Repo 實作（不讓 DTO 外洩）   
├─ datasource/                              ← 存取外部來源（HTTP 等）；只做取得與最小轉換  
│  ├─ currency/  
│  │  ├─ api/                               ← 調用端點（Ktor 請求入口）    
│  │  ├─ dto/                               ← 遠端傳輸格式（只在 data 層存在）  
│  │  └─ url/                               ← 端點常數與路徑  
│  └─ flights/  
│     ├─ api/  
│     ├─ dto/  
│     └─ url/  
├─ network/                                 ← HttpClient/序列化/攔截器與 Requester 封裝  
└─ repository/                              ← Repository 實作：聚合來源、DTO→domain 映射、回傳 Result<domain>  
├─ currency/  
└─ flights/  
  
di/                                        ← 依賴注入（Hilt/Koin 綁定 HttpClient/DataSource/Repo/VM）  

domain/                                    ← 網域層：純模型與介面；不依賴 data/presentation  
├─ model/                                   ← 業務語彙模型（不可見 DTO、UI 細節）  
│  └─ currency/  
├─ repository/                              ← Repository 介面（穩定邊界，供 VM 依賴）  
│  ├─ currency/  
│  └─ flights/  
└─ value/                                   ← 值物件/型別別名（不可變、型別安全）  
  
feature/                                    ← 功能切片（獨立演進，利於抽取與重用）  
└─ calculator/                              ← 計算器子域（示例；可比照匯率/航班收斂為 feature）  
  
presentation/                               ← 展示層：UI/狀態/導航/VM；單向資料流  
├─ component/                               ← 可重用 UI 元件（含 modifier/）  
│  └─ modifier/                             ← 修飾器擴充（骨架、陰影、shimmer 等）  
├─ mapper/                                  ← domain → UI 的最後轉換（避免 UI 知道 domain 細節）  
├─ model/                                   ← UI 專屬模型（貼近畫面需求）  
│  └─ currency/  
├─ navigation/                              ← Route/Graph 定義（頁面切換邏輯）  
├─ screen/                                  ← 頁面組合（觀察 UiState 呈現）  
├─ state/                                   ← UiState/事件結果（少布林、以 sealed/資料類表達）  
│  ├─ currency/  
│  └─ flights/  
├─ theme/                                   ← 色彩/字體/形狀與密度  
└─ viewmodel/                               ← 協調 UI 與 domain；觸發使用案例/副作用（如輪詢）  
  
util/                                       ← 純工具（不得依賴上層；可在任何層安全使用）  
├─ collection/                              ← 集合操作與擴充（去重、分組、持久集合輔助）  
├─ number/                                  ← 數值工具（BigDecimal 轉換/格式化/保留位數）  
└─ time/                                    ← 時間工具（解析/格式化/區時/間隔計算）  
  
花三四天左右處理的，到後面真的很趕沒法弄得很完整  
不過整個思路有出來，很久沒有這樣從零開始設計一個專案了，蠻有趣的  

首先是想法，我參考 Now in Android(nia) 和六角形(應該算)設計層級與依賴  
nia 的範例還蠻有趣的，之前有人還跟官方吵過很不 clean code，因為 domain 依賴 data layer  
原因是官方覺得 domain 存在的前提是業務足夠複雜，不然本身是可選的(data ui 兩層直通)  
我後來還是決定把 repo 丟給 domain 就是了，畢竟 repo 本身就是開純業務面的 model 了沒道理不放 domain  

再來講講架構，算是 MVVM 跟 MVI 都有沾邊  
我自己對 MVVM 的定義是 View/Composable 持有 VM 才算  
我是包成 UiState 了，callback 也都是 Composable 的獨立參數   
MVI 的重點則是單向數據流與唯一可信數據源，這兩點我算是高度遵守(當然有些太趕的真的來不及弄)  
有人會覺得一定要包 sealed class Intent 才算，我自己是覺得不算  
官方本身都沒這麼做，再來 callback 和 intent instance 差異只在 call 本身是否可以傳遞與儲存  
大多數時候純 callback 已經足夠了，只有需要紀錄或回朔操作的場景我才會轉 intent instance 來用  
UiState 用 sealed class 設計區隔每種狀態  

導航的部分因為只有兩頁所以沒用庫  
我原本很想用 Nav3 的，因為 Nav2 真的太醜了，什麼 type-safe 也都講假的實際上根本沒有  
但 Nav3 沒有直接保存狀態的機制所以很難用，最耗只好自己控  
還有用上 Adaptive Layout 元件，比較好控制方向和 window insert 之類的  
但是新的庫真的感覺有點差，連 inner padding 都沒有害我自己刻  
現在要弄 edge-to-edge 真的不少工要弄  
不過也讓旋轉畫面任一一邊都不會切割到內容  
出了大量的動畫還配上 SharedTransition  
不過真的有點太新了，不是很熟

DI 用 Hilt，老實說很久沒用 DI 了，之前也是自己的專案練習過而已  
不過還蠻好玩的，可惜測試的地方還沒用上，沒發揮全部能力  

測試現在還沒寫，時間真的不夠  
不過我現在主要也是用 prompt 生，訂好規格後都還蠻準的  
看接下來幾天有沒有時間補  

這次有兩部分我覺得比較有趣  
一個是飛航每 10 秒刷新一次，另一個是計算機  

10 秒刷新我用了自己寫的重啟計時器 ReTimer，我不是直接用 delay 來做  
首先我對 10 秒刷新的理解是"只要資料超過 10 秒未更新則自動更新"  
所以他有兩種情況  
1. 手動頻繁更新的話，10 秒倒數必須重來  
2. 若超過 10 秒但已不在頁面上，此次更新應該取消直到重返頁面

所以這個場景實際上更複雜一點，同時我又要求 UI 不該直接進行網路請求等等操作，而且更新控制本就該交給 VM  
但我也不希望讓 VM 直接持有 UI 自身的狀態(非 UiState)，導致設計上調整不少  
再來是可重啟計時器 ReTimer 的設計  
因為 delay 方案在高頻下需要一直取消協程，我不太想要這種額外開銷所以改成了 delay 結束後重新判斷的方案  
但同時要考慮多執行緒狀態競爭問題，原本是用 atomic 旗標方案，但還是很不好維護  
後來改成 Channel 的設計，保證線程安全同時低開銷，也算是花了點時間想  

計算機我拿以前做過的出來用  
但那真的很久以前了，差點修不好，好險結果是好的  
這個計算機特別的點在於它是狀態驅動，而不是純字串每次更新轉換  
也就是說我可以直接觀察當前的輸入，判斷接下來是合法非法  
等於不可能產生無法計算的非法狀態，但真的有點久了，裡面的 code 很髒  

YouTube:
https://www.youtube.com/watch?v=n8gBh9hrWUc

失敗案例測試圖：
<img width="1344" height="2992" alt="Screenshot_20251017_065840" src="https://github.com/user-attachments/assets/0384ebda-90e0-48cd-9e73-32efad45063f" />
<img width="1344" height="2992" alt="Screenshot_20251017_064636" src="https://github.com/user-attachments/assets/ecede3d4-3944-4cf2-aaf8-3543ba39850d" />
