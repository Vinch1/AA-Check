package com.aacheck.travel

import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*

data class CreateTravelRequest(
    @field:NotBlank val name: String? = null,
    val note: String? = null,
)

data class JoinRequest(@field:NotBlank val code: String? = null)

data class UpdateTravelRequest(val name: String? = null, val note: String? = null)

data class TravelDto(
    val id: Long,
    val name: String,
    val note: String?,
    val status: String,
    val inviteCode: String,
    val amCreator: Boolean,
    val createdAt: java.time.Instant,
    val endedAt: java.time.Instant?,
    val members: List<MemberBrief>,
)

@RestController
@RequestMapping("/api/travels")
class TravelController(private val service: TravelService) {

    @PostMapping
    fun create(@RequestBody req: CreateTravelRequest): TravelDto {
        val t = service.create(req.name!!, req.note)
        return service.detail(t.id).toDto()
    }

    @PostMapping("/join")
    fun join(@RequestBody req: JoinRequest): TravelDto {
        val t = service.join(req.code!!)
        return service.detail(t.id).toDto()
    }

    @GetMapping
    fun myTravels(): List<TravelDto> =
        service.myTravels().map { (travel, members) ->
            TravelDto(
                travel.id, travel.name, travel.note, travel.status, travel.inviteCode,
                amCreator = travel.creatorId == currentUserId(), travel.createdAt, travel.endedAt, members,
            )
        }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): TravelDto = service.detail(id).toDto()

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: UpdateTravelRequest): TravelDto {
        service.update(id, req.name, req.note)
        return service.detail(id).toDto()
    }

    @PostMapping("/{id}/invite-code/regenerate")
    fun regenerate(@PathVariable id: Long): TravelDto {
        service.regenerateInviteCode(id)
        return service.detail(id).toDto()
    }

    @PostMapping("/{id}/end")
    fun end(@PathVariable id: Long): TravelDto {
        service.end(id)
        return service.detail(id).toDto()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): Map<String, String> {
        service.delete(id)
        return mapOf("deleted" to "true")
    }

    private fun TravelDetail.toDto() = TravelDto(
        travel.id, travel.name, travel.note, travel.status, travel.inviteCode,
        amCreator, travel.createdAt, travel.endedAt, members,
    )

    private fun currentUserId(): Long =
        com.aacheck.security.currentUser() ?: throw com.aacheck.common.unauthorized("请先登录")
}
