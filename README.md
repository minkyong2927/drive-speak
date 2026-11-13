# 드라이브 스픽 (Drive Speak)

매일 달리는 출퇴근길을 교재로 쓰는 차량용 AI 어학 튜터. 오늘 운전에서 생긴 일을 그 자리에서 상황극으로 만들고, 지난 주행 기록으로 바쁜 구간을 미리 알아 말을 마치며, 틀린 표현은 틀린 자리에서 다시 묻는다. 2026 현대자동차그룹 차량용 앱 개발 공모전(Pleos Connect) 출품작.

[![android](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml/badge.svg)](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml)

## 개요

- 문제: 폰 AI 음성 대화는 방향지시등·회전·내비 안내 시점을 모름 → 차선 변경 중에도 계속 말함
- 해결 1 (무엇을): **상황 엔진**이 차량 신호 변화를 상황으로 묶어 그 숫자 그대로 상황극을 만듦 → 오늘 운전이 오늘 수업
- 해결 2 (미리): **길 예측**이 지난 주행 기록으로 늘 바쁜 구간·오래 서는 신호를 미리 앎 → 문장이 중간에 끊기지 않음
- 해결 3 (어디서): **장소 복습**이 틀린 표현을 틀린 자리에 묶어, 같은 자리를 지날 때 다시 물음
- 해결 4 (안전): **안전 되먹임**이 말하는 동안 조향 흔들림을 재서, 부담이면 스스로 듣기로 낮춤
- 해결 5 (언제·어떻게): 차량 신호로 **운전 여유도**를 0~3단계로 판단하고, 튜터의 말하기·질문·난이도를 그에 맞춤
- 화면: 주행 중에는 상태 표시만, 교정 카드와 학습 기록은 정차·주차 후에만

## 상황 엔진 (오늘 운전 → 오늘 수업)

| 신호 | 상황 | 연습 |
|---|---|---|
| NaviHelper 도착 예정 +5분 이상 | LATE (값: 지연 분) | "팀장님께 12분 늦는다고 전화하기" 상황극 |
| Vehicle 와이퍼 | RAIN (값: 저속/고속) | 비 오는 날 스몰톡, 고속이면 듣기 버전 |
| Vehicle 연료 15% 이하 | LOW_FUEL (값: %) | 주유소에서 가득 채워 달라고 하기 |
| Vehicle 좌석 착석 | PASSENGER | 함께하는 단어 퀴즈 |
| FusedLocation 주차장 진입 | PARKING | 주차 정산 표현 |

- 같은 상황은 운전 1회에 한 번 (`SituationDetector`)
- 연습 시점은 여유도가 정함: 바쁨이면 대기, 보통이면 듣기 버전, 정차면 길게 말하기
- 임계값(지연 분, 연료 %)은 `SituationConfig`로 분리

## 길 예측 · 장소 복습 · 안전 되먹임

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

1. 오늘 운전 상황극: 상황 엔진이 만든 상황을 회화 연습으로, 주차 후 '오늘 운전에서 생긴 상황 → 연습' 목록
2. 바쁜 순간 멈춤: 여유도 '바쁨'이면 말을 끊고 질문 보류, 안정되면 끊긴 문장 처음부터
3. 여유도별 난이도: 신호 대기·직진 구간에 말하기, 복잡한 구간은 듣기
4. 도착 시간 맞춤 수업: 도착 예정 시간으로 수업 길이 구성, 도착 2분 전 오늘 표현 복습
5. 말하면 고쳐주는 회화: STT → LLM(지금 상황·여유도 전달) 한 문장 교정 → 따라 말하기
6. 동승자 모드: 좌석 착석 정보로 함께하는 단어 퀴즈 전환
7. 상황 표현 한 마디: 주차장·주유소·드라이브스루 진입 시 표현 1개

## Pleos Connect SDK/API 사용 계획

| 모듈 | 쓰는 데이터 | 용도 |
|---|---|---|
| Vehicle | 방향지시등, 브레이크, 조향각, 와이퍼, 연료 잔량 | 여유도 판단, 멈춤·재개, 비·연료 상황 |
| Vehicle | 좌석 착석 정보 | 동승자 모드 |
| NaviHelper | 목적지, 도착 예정 시간, 남은 거리, 안내 시점 | 수업 길이, 도착 지연 → 지각 상황극, 도착 전 복습, 안내 음성과 겹침 방지 |
| FusedLocation | 위치, 속도 | 정차 판단, 주차장·주유소 진입 감지 |
| STT / TTS | 운전자 발화, 튜터 문장 | 대화 입출력 (내비 음성 우선) |
| LLM | 발화 텍스트, 수업 문맥 | 회화 생성, 문장 교정 |

## 구현 현황

| 구분 | 1차 (~11/15) | 2차 준비 (~12/2) |
|---|---|---|
| 여유도 엔진 | Kotlin 구현 + 단위 테스트 18개 | 실차 신호로 임계값 조정 |
| 상황 엔진 | Kotlin 구현 + 단위 테스트 11개 | 상황 종류 추가(드라이브스루, 장거리 휴게소) |
| 길 기억·장소 복습·안정성 | Kotlin 구현 + 단위 테스트 23개, 화면 연결은 프로토타입 JS(시연용 12회 기록) | 실주행 기록 저장, NaviHelper 경로 위치 연결 |
| 차량 신호 | 웹 프로토타입 시뮬레이션 → 엔진 | `VehicleSignalSource`에 Pleos SDK 구현체 연결 |
| 화면 | 웹 프로토타입을 WebView로 표시 | 네이티브 상태 화면 |
| 대화 | 프로토타입 라이브 모드: 브라우저 STT·TTS + Claude API 교정 | 차량 STT·TTS 모듈로 교체 |

- 앱 화면의 여유도 단계와 상황 감지는 프로토타입 JS가 아니라 Kotlin `LoadEngine`·`SituationDetector`가 계산 (`MainActivity` 브리지)

## 시연

- 시연 영상: [docs/demo.webm](docs/demo.webm) (1분 20초, 무음, 자동 시연 녹화 — 운전자 발화와 지난 12회 주행 기록은 시연용 데이터)
- 웹 프로토타입: [prototype/index.html](prototype/index.html) → 브라우저로 열면 바로 동작, '자동 시연' 버튼
- 라이브 모드: '라이브(마이크·AI)' → Claude API 키 입력(탭 메모리에만 보관) → 실제 음성 인식·합성 + Claude 교정. 키 없이 시작하면 음성만 켜지고 교정은 시연 응답. 크롬 권장

## 빌드·실행

필요: JDK 17, Android SDK 35, Gradle 8.11

```bash
gradle :app:testDebugUnitTest   # 엔진 5종 단위 테스트 52개
gradle :app:assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

- Android Studio: 저장소 폴더 열기 → `app` 실행 (Automotive 에뮬레이터 또는 일반 안드로이드 기기)
- release APK: [Releases](https://github.com/minkyong2927/drive-speak/releases) 또는 Actions 실행 결과의 `release-apk`
- CI(GitHub Actions): push마다 테스트 → release 빌드, `v*` 태그 push 시 Releases에 APK 첨부
- 서명키: CI가 빌드마다 새로 생성 (저장소에 키 없음, 로컬 빌드는 debug 키)

## 구조

```
app/src/main/java/com/drivespeak/
  MainActivity.kt            WebView + 여유도 엔진 브리지
  load/
    VehicleSignals.kt        차량 신호 값, 임계값 설정(LoadConfig)
    LoadEngine.kt            규칙 기반 여유도 판단
    LoadTracker.kt           시간축 처리 (즉시 멈춤, 안정 후 재개)
    DrivingLoad.kt           단계·근거·튜터 동작
    StabilityMonitor.kt      말하는 동안 조향 흔들림 측정 (안전 되먹임)
    VehicleSignalSource.kt   신호 공급원 인터페이스 (Pleos SDK 연결 지점)
  situation/
    SituationDetector.kt     차량 신호 변화 → 상황(지각·비·연료·동승자·주차장)
  route/
    RouteMemory.kt           구간별 주행 기록 → 바쁜 구간·긴 신호 예측
  review/
    PlaceReview.kt           틀린 자리에 묶은 간격 복습
app/src/test/...             단위 테스트
prototype/index.html         웹 프로토타입 (앱 assets로도 사용)
docs/demo.webm               시연 영상
```

## 팀 역할

| 역할 | 담당 |
|---|---|
| 기획 | 문제 정의, 시나리오, 기획서 |
| 개발 | 여유도 엔진, 앱, 프로토타입 |
| 디자인 | 화면, 시연 영상 |
| 검증 | 선행 사례 조사, 테스트 |
