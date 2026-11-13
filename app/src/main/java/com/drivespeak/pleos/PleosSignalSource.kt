package com.drivespeak.pleos

import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.drivespeak.load.StabilityMonitor
import org.json.JSONObject
import java.util.Calendar

/**
 * Pleos Connect SDK(Vehicle 2.3.2, NaviHelper 3.0.5)에서 받은 신호를 화면 상태로 넘김.
 *
 * - Vehicle: 방향지시등, 조향각, 브레이크, 와이퍼, 연료, 기어, 뒷좌석 착석 (읽기 전용 리스너)
 * - NaviHelper: 목적지 이름, 남은 시간, 다음 회전까지 거리
 * - 조향각은 StabilityMonitor 로 넘겨 '6초 안 조향 반전 횟수'를 같이 계산
 *
 * Pleos Connect 가 아닌 기기(폰, 일반 에뮬레이터)에는 차량 서비스가 없으므로
 * [start] 가 false 를 돌려주고, 화면은 시연용 신호 패널로 동작함.
 * 속도는 Vehicle SDK 에 없어 여기서 채우지 않음.
 */
class PleosSignalSource(
    private val context: Context,
    private val onChange: (JSONObject) -> Unit,
) {
    private var vehicle: Vehicle? = null
    private var navi: NaviHelper? = null
    private val stability = StabilityMonitor()
    private val rearSeats = mutableMapOf<SeatValue, Boolean>()
    private val frontWindow = WindowArea(WindowValue.FRONT_WINDSHIELD)

    private var plannedArrivalMs: Long? = null
    private var remainSec: Int? = null

    /** 차량 서비스가 있는 기기인지 */
    fun available(): Boolean = try {
        context.packageManager
            .queryIntentServices(Intent(VEHICLE_ACTION), 0).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    /** @return Pleos 신호에 연결을 시작했으면 true */
    fun start(): Boolean {
        if (!available()) return false
        return try {
            startVehicle()
            startNavi()
            emit(JSONObject().put("source", "pleos"))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Pleos SDK 연결 실패, 시연용 신호로 동작", e)
            stop()
            false
        }
    }

    fun stop() {
        try { vehicle?.release() } catch (e: Exception) { Log.w(TAG, "vehicle release", e) }
        try { navi?.release() } catch (e: Exception) { Log.w(TAG, "navi release", e) }
        vehicle = null
        navi = null
    }

    private fun emit(o: JSONObject) = onChange(o)

    private fun startVehicle() {
        val v = Vehicle(context).also { it.initialize() }
        vehicle = v

        v.getTurnSignal().registerTurnSignalState(object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) =
                emit(JSONObject().put("turn", PleosMapper.turn(state?.name)))
            override fun onFailed(e: Exception) = warn("turnSignal", e)
        })

        v.getSteeringWheel().registerSteeringWheelAngle(object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) {
                val deg = angle?.toDouble() ?: return
                val now = SystemClock.elapsedRealtime()
                stability.add(now, deg)
                emit(JSONObject().put("steer", Math.round(deg)).put("wobble", stability.reversals(now)))
            }
            override fun onFailed(e: Exception) = warn("steering", e)
        })

        v.getBrake().registerBrakePedalStatus(object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(status: Boolean?) =
                emit(JSONObject().put("brake", status == true))
            override fun onFailed(e: Exception) = warn("brake", e)
        })

        val wiper = v.getWiper()
        wiper.registerWiperState(frontWindow, object : WiperStateListener {
            override fun onWiperStateUpdated(state: WiperState?) {
                // 켜짐/꺼짐만 오므로, 켜졌을 때 스위치 단계를 한 번 더 읽어 고속 여부를 정함
                emit(JSONObject().put("wiper", PleosMapper.wiper(state?.name, null)))
                if (state != WiperState.ON) return
                wiper.getCurrentWiperSwitch(
                    frontWindow,
                    { sw -> emit(JSONObject().put("wiper", PleosMapper.wiper("ON", sw?.name))) },
                    { e -> warn("wiperSwitch", e) },
                )
            }
            override fun onFailed(e: Exception) = warn("wiper", e)
        })

        v.getFuel().registerFuelPercent(object : FuelPercentListener {
            override fun onFuelPercentUpdated(value: Int?) {
                if (value != null) emit(JSONObject().put("fuel", value))
            }
            override fun onFailed(e: Exception) = warn("fuel", e)
        })

        @Suppress("DEPRECATION")
        v.getGear().registerGearSelection(object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) =
                emit(JSONObject().put("park", PleosMapper.parked(gear?.name)))
            override fun onFailed(e: Exception) = warn("gear", e)
        })

        val seat = v.getSeat()
        for (pos in listOf(SeatValue.ROW_2_LEFT, SeatValue.ROW_2_RIGHT)) {
            seat.registerSeatOccupancy(SeatArea(pos), object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                    rearSeats[pos] = occupancy == true
                    emit(JSONObject().put("rear", rearSeats.values.any { it }))
                }
                override fun onFailed(e: Exception) = warn("seat", e)
            })
        }
    }

    private fun startNavi() {
        val n = NaviHelper(context)
        navi = n
        n.addListener(object : NaviHelperEventListener {
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
                // duration 은 초 단위로 가정 (에뮬레이터에서 확인 필요)
                plannedArrivalMs = System.currentTimeMillis() + routeStartInfo.duration * 1000L
                remainSec = routeStartInfo.duration
                emitRoute(routeStartInfo.name)
            }

            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                remainSec = destinationInfo.remainTime
                if (plannedArrivalMs == null) {
                    plannedArrivalMs = System.currentTimeMillis() + destinationInfo.remainTime * 1000L
                }
                emitRoute(destinationInfo.name)
            }

            override fun onDrivingInfo(drivingInfo: DrivingInfo) {
                remainSec = drivingInfo.destination.duration
                emitRoute(drivingInfo.destination.name)
            }

            override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
                val d = tbtInfoList.firstOrNull()?.distance
                val o = JSONObject()
                if (PleosMapper.turnNear(d)) o.put("nav", "near").put("navDist", "${Math.round(d!!)}m")
                else o.put("nav", JSONObject.NULL)
                emit(o)
            }

            override fun onRouteCancelled() = clearRoute()
            override fun onRoutingCompleted() = clearRoute()
        })
        n.initialize(
            onConnected = {
                try {
                    n.getDestinationInfo()
                    n.getTBTInfo()
                } catch (e: Exception) {
                    warn("navi query", e)
                }
            },
            onFailed = { e -> Log.w(TAG, "NaviHelper 연결 실패", e) },
        )
    }

    private fun emitRoute(destName: String) {
        val planned = plannedArrivalMs ?: return
        val cal = Calendar.getInstance().apply { timeInMillis = planned }
        val o = JSONObject()
            .put("destName", destName)
            .put("etaPlan", PleosMapper.hhmm(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)))
            .put("delayMin", PleosMapper.delayMin(planned, System.currentTimeMillis(), remainSec))
        emit(o)
    }

    private fun clearRoute() {
        plannedArrivalMs = null
        remainSec = null
        emit(JSONObject().put("delayMin", 0).put("nav", JSONObject.NULL))
    }

    private fun warn(what: String, e: Throwable) {
        Log.w(TAG, "$what: ${e.message}")
    }

    private companion object {
        const val TAG = "DriveSpeak/Pleos"
        const val VEHICLE_ACTION = "ai.pleos.playground.vehicle.VEHICLE_START"
    }
}
