package com.aacheck.expense

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class SettlementController(private val service: SettlementService) {

    /** 任意时刻可看实时结算；旅行结束后即为最终方案 */
    @GetMapping("/api/travels/{travelId}/settlement")
    fun settlement(@PathVariable travelId: Long): SettlementDto = service.settlement(travelId)
}
