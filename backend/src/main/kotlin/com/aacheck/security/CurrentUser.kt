package com.aacheck.security

import com.aacheck.common.unauthorized

/** 获取当前登录用户 id，未登录抛 401 */
fun requireUser(): Long = currentUser() ?: throw unauthorized("请先登录")
