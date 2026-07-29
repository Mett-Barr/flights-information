package moozy.flightinformation.feature.flights

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * 抓取的不變式是「有需求 ∧ 資料失效」：
 *  - 需求 = 有人在收集 state（訂閱結束就該停）
 *  - 失效 = TTL 到期（自然）或使用者主動刷新（宣告）
 *
 * 這些測試釘的是那條不變式，而不是任何特定的計時器實作。
 * 用 UnconfinedTestDispatcher：collector 立即啟動，不必手動泵送；
 * 時間仍由 advanceTimeBy 精準控制，足以驗證「不該發生的事沒發生」。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlightsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `does not fetch until something collects the state`() = runTest {
        val repository = FakeFlightsRepository()

        FlightsViewModel(repository)

        // 沒有需求就沒有請求：建構本身不該是觸發條件。
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `fetches once when collection starts`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertEquals(1, repository.callCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `starts from loading before anything is collected`() = runTest {
        // StateFlow 會 conflate，一旦開始收集，Loading 可能已被 Content 取代，
        // 所以初始態直接看 value，載入結果才用收集斷言。
        val viewModel = FlightsViewModel(FakeFlightsRepository())

        assertTrue(viewModel.state.value is FlightArrivalsUiState.Loading)
    }

    @Test
    fun `settles on content once loaded`() = runTest {
        val viewModel = FlightsViewModel(FakeFlightsRepository())

        viewModel.state.test {
            assertTrue(expectMostRecentItem() is FlightArrivalsUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refetches once the data goes stale`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertEquals(1, repository.callCount)

            advanceTimeBy(FlightsViewModel.FRESHNESS + 1.milliseconds)
            assertEquals(2, repository.callCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not refetch while the data is still fresh`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertEquals(1, repository.callCount)

            advanceTimeBy(FlightsViewModel.FRESHNESS - 1.milliseconds)
            assertEquals(1, repository.callCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a manual refresh restarts the freshness window`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertEquals(1, repository.callCount)

            // 過了半個 TTL 後手動刷新：資料重新變新鮮，計時應從此刻重算。
            advanceTimeBy(FlightsViewModel.FRESHNESS / 2)
            viewModel.refresh()
            assertEquals(2, repository.callCount)

            // 從刷新起算再過 TTL-1，還不該自動抓。
            advanceTimeBy(FlightsViewModel.FRESHNESS - 1.milliseconds)
            assertEquals(2, repository.callCount)

            advanceTimeBy(2.milliseconds)
            assertEquals(3, repository.callCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manual refresh shows its indicator until the reload completes`() = runTest {
        val repository = FakeFlightsRepository(responseDelay = 1.milliseconds)
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertTrue(awaitItem() is FlightArrivalsUiState.Loading)
            advanceTimeBy(1)
            assertTrue(awaitItem() is FlightArrivalsUiState.Content)

            repository.responseDelay = 1_000.milliseconds
            viewModel.refresh()

            val refreshing = awaitItem() as FlightArrivalsUiState.Content
            assertTrue(refreshing.isRefreshing)

            advanceTimeBy(1_000)
            val completed = awaitItem() as FlightArrivalsUiState.Content
            assertTrue(!completed.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `freshness expiry reload shows a refresh indicator until it completes`() = runTest {
        val repository = FakeFlightsRepository(responseDelay = 1.milliseconds)
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            assertTrue(awaitItem() is FlightArrivalsUiState.Loading)
            advanceTimeBy(1)
            assertTrue(awaitItem() is FlightArrivalsUiState.Content)

            repository.responseDelay = 1_000.milliseconds
            advanceTimeBy(FlightsViewModel.FRESHNESS)
            val refreshing = awaitItem() as FlightArrivalsUiState.Content
            assertTrue(refreshing.isRefreshing)

            advanceTimeBy(1_000)
            val reloaded = awaitItem() as FlightArrivalsUiState.Content
            assertTrue(!reloaded.isRefreshing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stops fetching once nothing is collecting`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            cancelAndIgnoreRemainingEvents()
        }
        val afterUnsubscribe = repository.callCount

        // 需求消失後，即使過了好幾個 TTL 也不該再打 API。
        advanceTimeBy(FlightsViewModel.FRESHNESS * 5)

        assertEquals(afterUnsubscribe, repository.callCount)
    }

    @Test
    fun `shows an error when the first load fails`() = runTest {
        val repository = FakeFlightsRepository(Result.failure(IOException("offline")))
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state is FlightArrivalsUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keeps showing the previous content when a refresh fails`() = runTest {
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(repository)

        viewModel.state.test {
            val loaded = expectMostRecentItem() as FlightArrivalsUiState.Content

            repository.result = Result.failure(IOException("offline"))
            viewModel.refresh()

            // StateFlow 會 conflate 掉「內容相同的後續發射」，所以看當前值而非發射序列。
            val afterFailure = viewModel.state.value
            assertTrue(afterFailure is FlightArrivalsUiState.Content)
            assertEquals(loaded.items, (afterFailure as FlightArrivalsUiState.Content).items)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful load records the injected clock time`() = runTest {
        val updatedAt = LocalDateTime.of(2026, 7, 26, 10, 11, 12)
        val viewModel = FlightsViewModel(
            flightsRepository = FakeFlightsRepository(),
            clock = { updatedAt },
        )

        viewModel.state.test {
            val content = expectMostRecentItem() as FlightArrivalsUiState.Content

            assertEquals(updatedAt, content.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale data reload records a new clock time`() = runTest {
        var updatedAt = LocalDateTime.of(2026, 7, 26, 10, 11, 12)
        val viewModel = FlightsViewModel(
            flightsRepository = FakeFlightsRepository(),
            clock = { updatedAt },
        )

        viewModel.state.test {
            val first = expectMostRecentItem() as FlightArrivalsUiState.Content
            updatedAt = updatedAt.plusSeconds(10)

            advanceTimeBy(FlightsViewModel.FRESHNESS + 1.milliseconds)

            val refreshed = viewModel.state.value as FlightArrivalsUiState.Content
            assertEquals(updatedAt, refreshed.updatedAt)
            assertTrue(first.updatedAt != refreshed.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed refresh keeps the previous data time`() = runTest {
        var updatedAt = LocalDateTime.of(2026, 7, 26, 10, 11, 12)
        val repository = FakeFlightsRepository()
        val viewModel = FlightsViewModel(
            flightsRepository = repository,
            clock = { updatedAt },
        )

        viewModel.state.test {
            val loaded = expectMostRecentItem() as FlightArrivalsUiState.Content
            repository.result = Result.failure(IOException("offline"))
            updatedAt = updatedAt.plusSeconds(10)

            viewModel.refresh()

            val afterFailure = viewModel.state.value as FlightArrivalsUiState.Content
            assertEquals(loaded.updatedAt, afterFailure.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
