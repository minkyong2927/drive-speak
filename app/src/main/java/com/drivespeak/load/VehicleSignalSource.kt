package com.drivespeak.load

/**
 * 차량 신호 공급원. 2차 준비 기간에 Pleos Connect SDK 구현체를 붙임.
 * 지금은 웹 프로토타입(시뮬레이션) 값을 받는 구현만 있음.
 */
interface VehicleSignalSource {
    fun start(listener: (VehicleSignals) -> Unit)
    fun stop()
}
