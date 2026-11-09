package com.drivespeak.load

/** 운전 여유도 0~3단계. 숫자가 클수록 바쁨. */
enum class DrivingLoad(val level: Int, val label: String) {
    STOPPED(0, "정차"),
    RELAXED(1, "여유"),
    MODERATE(2, "보통"),
    BUSY(3, "바쁨");

    /** 튜터가 이 단계에서 할 수 있는 일 */
    val tutorMode: TutorMode
        get() = when (this) {
            STOPPED -> TutorMode.FREE_TALK
            RELAXED -> TutorMode.SHORT_QUESTION
            MODERATE -> TutorMode.LISTEN_ONLY
            BUSY -> TutorMode.PAUSE
        }
}

enum class TutorMode { FREE_TALK, SHORT_QUESTION, LISTEN_ONLY, PAUSE }

/** 판단 근거. 화면 문구와 로그에 씀. */
enum class LoadReason {
    PARKED, TURN_SIGNAL, LARGE_STEERING, HARD_BRAKING, NAV_VOICE,
    NAV_TURN_NEAR, WIPER_HIGH, STOPPED, CRUISING
}

data class LoadResult(val load: DrivingLoad, val reason: LoadReason)
