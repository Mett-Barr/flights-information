# Git 慣例

這份文件描述本專案實際在用的分支與提交慣例。它是把既有做法寫下來，
不是新規定——`git log` 上的每一個 commit 都符合這裡寫的規則。

---

## 提交訊息：Conventional Commits

```
<type>(<scope>)<!>: <subject>
```

- **type** 必填，小寫，見下表
- **scope** 選填，指模組或功能區（`flights`、`currency`、`network`、`deps`）
- **`!`** 標記破壞性變更，放在冒號前
- **subject** 祈使句、小寫開頭、不加句號，描述**這個 commit 做了什麼**

### type

| type | 用於 | 本專案實例 |
|---|---|---|
| `feat` | 新增功能 | `feat(network)!: classify failures instead of surfacing raw exceptions` |
| `fix` | 修正缺陷 | `fix(currency): handle categorized load errors` |
| `refactor` | 不改變行為的結構調整 | `refactor(flights)!: introduce a domain model so the DTO stops leaking` |
| `test` | 只動測試 | `test(flights): make the repository test actually assert something` |
| `docs` | 只動文件 | `docs: record the three decisions a reviewer would question` |
| `build` | 建置設定、依賴 | `build(deps): upgrade remaining libraries and add Turbine` |
| `ci` | CI 設定 | `ci: verify Android changes before they reach master` |
| `chore` | 其他雜項 | `chore: delete two declarations nothing referenced` |

### subject 寫「為什麼」而不只是「什麼」

比較這兩個寫法：

```
❌ refactor: change FlightsViewModel
✅ refactor(flights)!: derive fetching from demand and staleness
```

第二個讓人不必打開 diff 就知道發生了什麼概念上的改變。這是本專案 history
一貫的標準，維持它。

---

## 分支模型

短命的主題分支，`--no-ff` 併回 `master`。**沒有 `develop` 分支。**

```
master  ──●────────────●────────────●──
           \          / \          /
            ●──●──●──●   ●──●──●──●
         refactor/domain-model  test/compose-ui
```

### 分支命名：與 commit type 同一套詞彙

```
<type>/<短描述>
```

| 分支 | 對應的 commit |
|---|---|
| `refactor/domain-model` | `refactor(flights)!: introduce a domain model…` |
| `chore/zero-warnings` | `build: fail the build on compiler warnings` |
| `docs/readme` | `docs: rewrite the README` |
| `test/compose-ui` | `test: cover flight screen states` |

前綴共用讓分支名與它產出的 commit 互相印證，回頭讀 history 時不必猜這個分支
當初在做什麼。

### 額外兩種前綴

| 前綴 | 用於 | 生命週期 |
|---|---|---|
| `fix/` | 修缺陷 | 併回後刪除 |
| `spike/` | 探索、原型、設計比較 | **可能不會被併回**，驗證完就丟 |

`spike/` 是刻意與其他前綴區隔的：它的產出預期是「知識」而不是「程式碼」。
設計評選、技術可行性驗證放這裡，不要污染 `feat/`。

### 為什麼用 `--no-ff`

fast-forward 會把主題分支的存在抹掉，history 變成一條直線，看不出哪幾個 commit
屬於同一件事。`--no-ff` 保留 merge commit，`git log --graph` 上一眼就能看出
每個主題的範圍。

```bash
git merge --no-ff refactor/domain-model
```

## 合併前的檢查

`master` 上的每個 commit 都應該可建置、測試全綠。合併前在本地跑：

```bash
./gradlew assembleDebug testDebugUnitTest lint
```

專案設定 `allWarningsAsErrors = true`，所以編譯警告會直接讓建置失敗。
CI（`.github/workflows/build.yml`）在推送到 master 與 PR 時執行相同的 gate。

---

## 一個 commit 一件事

`refactor/domain-model` 這個分支是好例子——它拆成四個 commit：

```
ec710bb refactor(flights)!: introduce a domain model so the DTO stops leaking
2a65d24 test(flights): make the repository test actually assert something
83f841d refactor(flights)!: derive fetching from demand and staleness
ecef975 refactor: delete ReTimer, now that nothing needs it
```

每一個都可以獨立 review、獨立 revert。最後那個「刪掉 ReTimer」之所以是獨立
commit 而不是併進前一個，是因為它是前一個變更的**後果**——分開才能讓
history 說出「因為抓取改成推導式，這個類別就沒有存在理由了」這件事。
