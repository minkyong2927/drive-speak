# 드라이브 스픽 (Drive Speak)

가는 곳에서 쓸 말을, 가는 길에 연습하는 차량용 AI 회화 에이전트. 목적지와 일정을 읽어 도착해서 쓸 말을 예행연습으로 짜고, 차량 신호로 말해도 되는 순간만 골라 말을 건다. 2026 현대자동차그룹 차량용 앱 개발 공모전(Pleos Connect) 출품작.

[![android](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml/badge.svg)](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml)

## 제출 정보

| 항목 | 내용 |
|---|---|
| 앱 이름 | 드라이브 스픽 (Drive Speak) |
| 한 줄 소개 | 가는 곳에서 쓸 말을, 가는 길에 연습하는 차량용 AI 회화 에이전트 |
| APK | [apk/이스트_DriveSpeak_release.apk](apk/) (main push 마다 CI가 빌드해 갱신) |
| Pleos Connect SDK | Vehicle 2.3.2, NaviHelper 3.0.5 (`https://nexus-playground.pleos.ai/repository/maven-releases/`) |
| 대상 에뮬레이터 | Pleos Connect Emulator 2.1.0 (Android API 34, x86_64 시스템 이미지) |
| 화면 규격 | 1920x1080 기준으로 만들고, 다른 해상도에서는 비율을 지켜 자동 맞춤. 위 1920x720 이 차량 화면, 아래는 시연용 신호 패널 |
| 에뮬레이터 실행 확인 | 미확인. SDK 연결 코드는 CI 빌드(컴파일)까지 확인했고, Pleos Connect 에뮬레이터에서 실제 신호 수신은 아직 돌려 보지 못함 |

## 실행 방법

1. APK 설치: `adb install apk/이스트_DriveSpeak_release.apk` → '드라이브 스픽' 실행
2. Pleos Connect 에뮬레이터·차량: 앱이 차량 서비스에 연결해 Vehicle·NaviHelper 신호를 받아 동작. 화면 아래 패널에 'Pleos Connect 신호 수신 중' 표시
3. 그 외 기기(폰, 일반 에뮬레이터): 화면 아래 시연용 신호 버튼으로 같은 흐름을 재현. '자동 시연' 버튼은 전체 시나리오를 1분 30초로 보여 줌
4. 브라우저만으로 보기: [prototype/index.html](prototype/index.html) 을 크롬으로 열기 (앱 화면과 같은 파일)

## 테스트 환경과 필요한 설정

- 빌드: JDK 17, Android SDK 35, Gradle 8.11, minSdk 29 (Pleos 요구 28 이상)
- 권한: `pleos.car.permission.CAR_LIGHTS` · `CAR_DRIVE` · `CAR_WINDOWS` · `CAR_ENERGY` · `CAR_SEATS` · `NAVI_ROUTE` (읽기 전용, 차량 제어 권한 없음)
- 확인한 환경: GitHub Actions(ubuntu) 단위 테스트 74개 + release 빌드, 크롬에서 웹 프로토타입 자동·수동 시연
- 라이브 모드(선택): '라이브(마이크·AI)' → Claude API 키 입력(탭 메모리에만 보관) → 실제 음성 인식·합성 + Claude 교정·되돌아보기. 키 없이 시작하면 음성만 켜지고 교정은 시연 응답. 인터넷과 마이크 권한 필요
- 시연 데이터: 자동 시연의 운전자 발화, 지난 12회 주행 기록, 일정(09:00 회의), 학습자 기록은 시연용 값

## 개요

- 문제: 폰 AI 회화는 어디로 가는지, 지금 말을 걸어도 되는지 모름 → 차선 변경 중에도 말하고, 연습 내용은 오늘 일과 무관
- 무엇을 (목적지 예행연습): 에이전트가 목적지·도착 시간·일정을 읽어 도착해서 쓸 말을 가는 길에 연습시킴. 정체로 늦으면 계획을 바꿔 "회의에 12분 늦는다"부터 연습
- 누가 정하나 (에이전트 3단계): 출발 전 계획 → 주행 중 대화 → 주차 후 되돌아보기. 무엇을 읽고 왜 이 연습을 골랐는지 화면에 기록으로 남김
- 안전 (안전 게이트): 말해도 되는지는 LLM 이 아니라 규칙 엔진이 정함. 에이전트가 고른 행동이 허용 범위를 넘으면 낮춤 → LLM 이 틀려도 운전을 방해하지 못함
- 기억 (학습자 기록·장소 복습): 반복되는 오류 유형을 쌓아 다음 연습 문형을 고르고, 틀린 표현은 틀린 자리에서 다시 물음
- 화면: 주행 중에는 상태 표시만, 교정 카드와 되돌아보기는 주차 후에만

## 에이전트 구조

| 시점 | 에이전트가 하는 일 | 읽고 쓰는 것 |
|---|---|---|
| 출발 전 (계획) | 목적지·일정·지난 주행 기록·밀린 복습·학습자 약점을 읽고 오늘 예행연습을 짬. 긴 말하기는 오래 서는 신호에 예약 | 내비 목적지·도착 예정, 일정, `RouteMemory`, `PlaceReview`, `LearnerModel` |
| 주행 중 (대화) | 상황극 상대를 하며 교정하고 다음 행동(긴 말하기·짧은 질문·듣기만·대기)을 고름. 도착 지연 같은 변화가 생기면 계획을 바꿈 | 차량 상태, `SituationDetector`, 오류 기록 쓰기 |
| 주차 후 (되돌아보기) | 오늘 대화 기록을 읽고 반복 오류와 내일 계획을 고쳐 씀 | 대화 기록, `LearnerModel`, 복습 예약 |

- 안전 게이트(`SafetyGate`): 여유도 단계·조향 흔들림·곧 바쁜 구간으로 '지금 허용되는 가장 요구가 큰 행동'을 정하고, 에이전트가 고른 행동을 그 아래로만 통과시킴. 게이트는 행동을 낮출 뿐 올리지 않음
- 학습자 기록(`LearnerModel`): 최근 7일 오류를 유형별로 세고, 3회 이상 반복된 유형을 다음 긴 말하기 문형으로 고름
- 라이브 모드에서는 주행 중 대화와 주차 후 되돌아보기를 Claude API 가 맡고(다음 행동·오류 유형을 구조화 출력으로 받음), 키가 없으면 시연 응답으로 같은 흐름을 보여 줌

## 상황 엔진 (운전 중 생긴 변화 → 계획 변경)

| 신호 | 상황 | 연습 |
|---|---|---|
| NaviHelper 도착 예정 +5분 이상 | LATE (값: 지연 분) | "회의에 12분 늦는다고 알리기" 상황극 |
| Vehicle 와이퍼 | RAIN (값: 저속/고속) | 비 오는 날 스몰톡, 고속이면 듣기 버전 |
| Vehicle 연료 15% 이하 | LOW_FUEL (값: %) | 주유소에서 가득 채워 달라고 하기 |
| Vehicle 좌석 착석 | PASSENGER | 함께하는 단어 퀴즈 |
| FusedLocation 주차장 진입 | PARKING | 주차 정산 표현 |

- 같은 상황은 운전 1회에 한 번 (`SituationDetector`)
- 연습 시점은 여유도가 정함: 바쁨이면 대기, 보통이면 듣기 버전, 정차면 길게 말하기
- 임계값(지연 분, 연료 %)은 `SituationConfig`로 분리

## 길 예측 · 장소 복습 · 조향 흔들림

| 엔진 | 입력 | 동작 |
|---|---|---|
| `RouteMemory` | 주행마다 200m 구간별 여유도·정차 시간 | 30초 안에 늘 바쁜 구간(바빴던 비율 70% 이상) → 새 질문 보류. 평균 30초 이상 서는 신호 → 긴 말하기 예약. 같은 길 3회 이상부터 |
| `PlaceReview` | 교정받은 표현 + 경로 위치 | 150m 안으로 다시 지날 때 물음. 맞히면 1 → 2 → 4 → 7일 간격, 틀리면 다음 날 |
| `StabilityMonitor` | 조향각 시계열 | 6초 안에 조향 반전 5회 이상이면 듣기로 낮춤. 한 방향 큰 회전은 세지 않음 |

- '바쁘면 멈춤'은 일이 벌어진 뒤의 반응, 길 예측은 벌어지기 전에 비켜 주는 것
- 주행 기록과 복습 위치는 차량 안에만 저장

## 운전 여유도

| 단계 | 판단 신호 | 튜터 동작 |
|---|---|---|
| 0 정차 | 주차, 속도 0 | 자유 회화, 긴 문장 말하기 |
| 1 여유 | 직진, 조향각 작음, 내비 안내 없음 | 짧은 답 하는 질문 |
| 2 보통 | 회전 안내 300m 이내, 와이퍼 고속 | 듣기만 (질문 없음) |
| 3 바쁨 | 방향지시등, 조향각 30° 이상, 급브레이크, 내비 음성 출력 중 | 즉시 멈춤, 안정되면 이어서 |

- 규칙은 위에서부터 먼저 맞는 것이 우선 (`LoadEngine`)
- 바빠질 때는 즉시, 덜 바빠질 때는 1.5초 안정 후 반영 (`LoadTracker`) → 지시등 깜빡임에 흔들리지 않음
- 임계값(조향각, 안내 거리, 감속도, 안정 시간)은 `LoadConfig` 설정값으로 분리

## 주요 기능

1. 목적지 예행연습: 목적지·일정에 맞춰 도착해서 쓸 말을 가는 길에 연습
2. 다시 계획: 도착 지연·비·연료 같은 변화가 생기면 그 숫자 그대로 상황극으로 바꿈 (상황 엔진)
3. 안전 게이트: 바쁜 순간 멈춤, 곧 바쁜 구간이면 새 질문 보류, 조향이 흔들리면 듣기로 낮춤
4. 학습자 기록: 반복되는 오류 유형을 다음 연습 문형에 넣음
5. 장소 복습: 틀린 자리에서 다시 묻기, 1 → 2 → 4 → 7일 간격
6. 주차 후 되돌아보기: 반복 오류, 교정 카드, 내일 계획과 수업 지도
7. 동승자 모드: 좌석 착석 정보로 함께하는 단어 퀴즈 전환

## Pleos Connect SDK 연동

`PleosSignalSource` 가 SDK 리스너로 받은 값을 화면 상태에 넣고, 같은 값이 Kotlin 엔진(`LoadEngine`, `SituationDetector`, `StabilityMonitor`)으로 들어감.

| 모듈 | 쓰는 API | 용도 | 상태 |
|---|---|---|---|
| Vehicle 2.3.2 | `TurnSignal.registerTurnSignalState` | 여유도 '바쁨' 판단, 멈춤·재개 | 연결 코드 있음 |
| Vehicle | `SteeringWheel.registerSteeringWheelAngle` | 큰 조향 판단, 조향 반전 횟수(안전 게이트) | 연결 코드 있음 |
| Vehicle | `Brake.registerBrakePedalStatus` | 정차 보조 신호 | 연결 코드 있음 |
| Vehicle | `Wiper.registerWiperState` + `getCurrentWiperSwitch` | 비 상황, 고속이면 듣기만 | 연결 코드 있음 |
| Vehicle | `Fuel.registerFuelPercent` | 연료 부족 상황 | 연결 코드 있음 |
| Vehicle | `Gear.registerGearSelection` | 주차 판단 → 되돌아보기 화면 | 연결 코드 있음 |
| Vehicle | `Seat.registerSeatOccupancy` (2열) | 동승자 모드 | 연결 코드 있음 |
| NaviHelper 3.0.5 | `onRouteStarted`, `onDestinationInfo`, `onDrivingInfo` | 목적지 이름, 도착 예정, 도착 지연 분 | 연결 코드 있음 |
| NaviHelper | `onTBTInfo` | 회전 안내 300m 이내 판단 | 연결 코드 있음 |
| 속도 | Vehicle SDK 에 없음 | 정차 판단 | 미연결 (2차에 차량 속도 속성 또는 위치 속도로 연결) |
| 일정 | Pleos SDK 범위 밖 | 목적지 예행연습의 일정 | 시연용 값 (2차에 휴대폰 캘린더 연동) |
| STT / TTS / LLM | Pleos SpeechToText·TextToSpeech·LLM | 대화 입출력 | 1차는 브라우저 음성 + Claude API, 2차에 Pleos 모듈로 교체 |

- '연결 코드 있음'은 공개된 SDK 소스의 시그니처에 맞춰 작성하고 CI에서 컴파일까지 확인했다는 뜻. 에뮬레이터에서 값이 실제로 들어오는지는 미확인
- NaviHelper 의 시간 값(`duration`, `remainTime`)은 초 단위로 가정함. 에뮬레이터에서 확인 필요
- 차량 서비스가 없는 기기에서는 SDK 연결을 건너뛰고 시연용 신호 패널로 동작

## 구현 현황

| 구분 | 1차 (~11/15) | 2차 준비 (~12/2) |
|---|---|---|
| 여유도 엔진 | Kotlin 구현 + 단위 테스트 18개, 앱 화면에 연결 | 실차 신호로 임계값 조정 |
| 상황 엔진 | Kotlin 구현 + 단위 테스트 11개, 앱 화면에 연결 | 상황 종류 추가 |
| 길 기억·장소 복습·안정성 | Kotlin 구현 + 단위 테스트 23개. 안정성은 Pleos 조향각에 연결, 길 기억·장소 복습의 화면 동작은 프로토타입 JS(시연용 12회 기록) | 실주행 기록 저장, 경로 위치 연결 |
| 안전 게이트·학습자 기록 | Kotlin 구현 + 단위 테스트 13개. 화면 동작은 프로토타입 JS 의 같은 규칙 | 엔진을 대화 루프에 연결, 기록 저장 |
| Pleos 신호 | `PleosSignalSource` (Vehicle·NaviHelper) + 값 변환 단위 테스트 9개 | 에뮬레이터·실차에서 값 확인, 속도 연결 |
| 화면 | 웹 프로토타입을 WebView로 표시 | 네이티브 상태 화면 |
| 대화 | 프로토타입 라이브 모드: 브라우저 STT·TTS + Claude API (교정, 다음 행동, 되돌아보기) | 차량 STT·TTS·LLM 모듈로 교체 |

## 시연

- 시연 영상: [docs/demo.webm](docs/demo.webm) (1분 30초, 무음, 자동 시연 녹화)
- 순서: 출발 전 계획 → 목적지 예행연습 → 장소 복습 → 안전 게이트(길 예측) → 다시 계획(지각) → 안전 게이트(조향 흔들림) → 예약해 둔 신호에서 긴 말하기 → 주차 후 되돌아보기

## 빌드

```bash
gradle :app:testDebugUnitTest   # 단위 테스트 74개
gradle :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

- CI(GitHub Actions): main push 마다 테스트 → release 빌드 → `apk/` 폴더 갱신. `v*` 태그 push 시 Releases에도 APK 첨부
- 서명키: CI가 빌드마다 새로 생성 (저장소에 키 없음, 로컬 빌드는 debug 키)

## 구조

```
app/src/main/java/com/drivespeak/
  MainActivity.kt            WebView + 엔진 브리지 + Pleos 신호 연결
  pleos/
    PleosSignalSource.kt     Pleos Connect SDK(Vehicle·NaviHelper) 리스너 → 화면 상태
    PleosMapper.kt           SDK 값 → 앱이 쓰는 값 (방향지시등, 와이퍼 단계, 도착 지연 분)
  agent/
    SafetyGate.kt            에이전트가 고른 행동을 허용 범위로 낮추는 규칙
    LearnerModel.kt          오류 유형별 기록, 다음 연습 문형 선택
  load/
    VehicleSignals.kt        차량 신호 값, 임계값 설정(LoadConfig)
    LoadEngine.kt            규칙 기반 여유도 판단
    LoadTracker.kt           시간축 처리 (즉시 멈춤, 안정 후 재개)
    DrivingLoad.kt           단계·근거·튜터 동작
    StabilityMonitor.kt      조향 반전 횟수 측정
  situation/
    SituationDetector.kt     차량 신호 변화 → 상황(지각·비·연료·동승자·주차장)
  route/
    RouteMemory.kt           구간별 주행 기록 → 바쁜 구간·긴 신호 예측
  review/
    PlaceReview.kt           틀린 자리에 묶은 간격 복습
app/src/test/...             단위 테스트
prototype/index.html         웹 프로토타입 (앱 assets로도 사용)
apk/                         release APK (CI가 갱신)
docs/demo.webm               시연 영상
```

## 팀 역할

| 역할 | 담당 |
|---|---|
| 기획 | 문제 정의, 시나리오, 기획서 |
| 개발 | 여유도 엔진, 앱, 프로토타입 |
| 디자인 | 화면, 시연 영상 |
| 검증 | 선행 사례 조사, 테스트 |
