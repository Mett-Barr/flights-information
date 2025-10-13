package moozy.flightinformation.data.remote.dto

import kotlinx.serialization.Serializable

// 到達國內航班即時時刻表
class InstantScheduleDomesticArrivalDto : ArrayList<InstantScheduleDomesticArrivalDto.InstantScheduleDomesticArrivalDtoItem>(){
    @Serializable
    data class InstantScheduleDomesticArrivalDtoItem(
        val expectTime: String? = null,
        val realTime: String? = null,
        val airLineName: String? = null,
        val airLineCode: String? = null,
        val airLineLogo: String? = null,
        val airLineUrl: String? = null,
        val airLineNum: String? = null,
        val upAirportCode: String? = null,
        val upAirportName: String? = null,
        val airPlaneType: String? = null,
        val airBoardingGate: String? = null,
        val airFlyStatus: String? = null,
        val airFlyDelayCause: String? = null
    )
}