package moozy.flightinformation.data.datasource.flights

import moozy.flightinformation.data.datasource.flights.api.FlightsApi
import moozy.flightinformation.data.datasource.flights.dto.InstantScheduleDomesticArrivalDto
import javax.inject.Inject
import javax.inject.Singleton

// 當前只有直通方法，未來可以將錯誤處理或重試邏輯放在這塊
@Singleton
class FlightsNetworkDataSource @Inject constructor(
    private val api: FlightsApi
) : FlightsDataSource {
    override suspend fun fetchArrivals(): Result<List<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>> {
        return api.instantDomesticArrivals()
    }
}