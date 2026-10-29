package com.aacheck.expense

import org.springframework.web.bind.annotation.*

@RestController
class ExpenseController(private val service: ExpenseService) {

    @PostMapping("/api/travels/{travelId}/expenses")
    fun create(@PathVariable travelId: Long, @RequestBody req: ExpenseUpsertRequest): ExpenseView =
        service.detail(service.create(travelId, req).id)

    @GetMapping("/api/travels/{travelId}/expenses")
    fun list(@PathVariable travelId: Long): List<ExpenseView> = service.list(travelId)

    /** 作者的待追认弹窗队列 */
    @GetMapping("/api/travels/{travelId}/approvals")
    fun pendingApprovals(@PathVariable travelId: Long): List<PendingApproval> =
        service.myPendingApprovals(travelId)

    @GetMapping("/api/expenses/{id}")
    fun detail(@PathVariable id: Long): ExpenseView = service.detail(id)

    @PatchMapping("/api/expenses/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: ExpenseUpsertRequest): ExpenseView =
        service.update(id, req)

    @PostMapping("/api/expenses/{id}/approve")
    fun approve(@PathVariable id: Long): Map<String, String> {
        service.approve(id)
        return mapOf("ok" to "true")
    }

    /** 作者不同意：标记争议，提示线下沟通 */
    @PostMapping("/api/expenses/{id}/disagree")
    fun disagree(@PathVariable id: Long): Map<String, String> {
        service.dispute(id)
        return mapOf("ok" to "true")
    }

    /** 修改人撤回：恢复到作者认可的基线版本 */
    @PostMapping("/api/expenses/{id}/withdraw")
    fun withdraw(@PathVariable id: Long): Map<String, String> {
        service.withdraw(id)
        return mapOf("ok" to "true")
    }

    @DeleteMapping("/api/expenses/{id}")
    fun delete(@PathVariable id: Long): Map<String, String> {
        service.delete(id)
        return mapOf("ok" to "true")
    }
}
