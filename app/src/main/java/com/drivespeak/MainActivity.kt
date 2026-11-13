package com.drivespeak

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.drivespeak.load.LoadEngine
import com.drivespeak.load.NavGuidance
import com.drivespeak.load.TurnSignal
import com.drivespeak.load.VehicleSignals
import com.drivespeak.situation.PlaceType
import com.drivespeak.situation.SituationDetector
import com.drivespeak.situation.SituationSignals
import com.drivespeak.situation.Wiper
import org.json.JSONArray
import org.json.JSONObject

/**
 * 1차 버전: 웹 프로토타입 화면을 띄우고, 여유도 판단만 Kotlin 엔진(LoadEngine)이 맡음.
 * 프로토타입의 시뮬레이션 신호 -> DriveSpeak.evaluate() -> 단계·근거 -> 화면.
 * 2차에서 시뮬레이션 대신 Pleos Connect SDK 신호를 VehicleSignalSource로 연결.
 */
class MainActivity : Activity() {

    private val engine = LoadEngine()
    private val situations = SituationDetector()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.addJavascriptInterface(Bridge(), "DriveSpeak")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(BRIDGE_JS, null)
            }
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        /** 프로토타입 상태 S(JSON) -> {"level":n,"reason":"..."} */
        @JavascriptInterface
        fun evaluate(stateJson: String): String {
            val r = engine.evaluate(fromPrototype(JSONObject(stateJson)))
            return JSONObject().put("level", r.load.level).put("reason", r.reason.name).toString()
        }

        /** 프로토타입 상태 S(JSON) -> 새로 생긴 상황 [{"id":"LATE","value":12}] */
        @JavascriptInterface
        fun detect(stateJson: String, reset: Boolean): String {
            if (reset) situations.reset()
            val out = JSONArray()
            for (e in situations.detect(situationsFromPrototype(JSONObject(stateJson)))) {
                out.put(JSONObject().put("id", e.situation.name).put("value", e.value))
            }
            return out.toString()
        }
    }

    companion object {
        fun fromPrototype(s: JSONObject): VehicleSignals = VehicleSignals(
            parked = s.optBoolean("park"),
            speedKmh = s.optDouble("speed", 0.0),
            turnSignal = when (s.optString("turn")) {
                "left" -> TurnSignal.LEFT
                "right" -> TurnSignal.RIGHT
                else -> TurnSignal.OFF
            },
            steeringAngleDeg = s.optDouble("steer", 0.0),
            navGuidance = when (s.optString("nav")) {
                "voice" -> NavGuidance.VOICE
                "near" -> NavGuidance.TURN_AHEAD
                else -> NavGuidance.NONE
            },
            wiperHigh = s.optString("wiper") == "high",
        )

        fun situationsFromPrototype(s: JSONObject): SituationSignals {
            val plan = s.optString("etaPlan").split(":").let {
                if (it.size == 2) it[0].toInt() * 60 + it[1].toInt() else null
            }
            return SituationSignals(
                plannedArrivalMin = plan,
                expectedArrivalMin = plan?.plus(s.optInt("delayMin")),
                wiper = when (s.optString("wiper")) {
                    "high" -> Wiper.HIGH
                    "low" -> Wiper.LOW
                    else -> Wiper.OFF
                },
                fuelPercent = s.optInt("fuel", 100),
                passengerSeated = s.optBoolean("rear"),
                place = if (s.optString("place") == "parking") PlaceType.PARKING else PlaceType.NONE,
            )
        }

        /** 프로토타입의 computeLoad()를 Kotlin 엔진 호출로 바꿔 끼움 */
        private val BRIDGE_JS = """
            (function(){
              if (!window.DriveSpeak) return;
              var WHY = {
                PARKED:'<b>주차</b> · 오늘 수업 정리를 보여줍니다',
                TURN_SIGNAL:'<b>방향지시등 켜짐</b> → 말을 멈추고 기다립니다',
                LARGE_STEERING:'<b>큰 조향</b> → 회전이 끝날 때까지 멈춥니다',
                HARD_BRAKING:'<b>급브레이크</b> → 말을 멈춥니다',
                NAV_VOICE:'<b>내비 안내 음성 출력 중</b> → 내비가 먼저 말합니다',
                NAV_TURN_NEAR:'<b>회전 안내 300m 이내</b> → 질문 없이 듣기만',
                WIPER_HIGH:'<b>와이퍼 고속</b> → 질문 없이 듣기만',
                STOPPED:'<b>정차 중</b> → 긴 문장 말하기 연습',
                CRUISING:'<b>직진 · 안내 없음</b> → 짧게 답하는 질문'
              };
              computeLoad = function(){
                var r = JSON.parse(DriveSpeak.evaluate(JSON.stringify(S)));
                return { l: r.level, why: (WHY[r.reason] || '') };
              };
              detectSituations = function(s, fired){
                return JSON.parse(DriveSpeak.detect(JSON.stringify(s), fired.length === 0));
              };
              render();
            })();
        """.trimIndent()
    }
}
