# 드라이브 스픽 (Drive Speak)

운전 상황에 맞춰 말하고 멈추는 차량용 AI 어학 튜터. 2026 현대자동차그룹 차량용 앱 개발 공모전(Pleos Connect) 출품작.

[![android](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml/badge.svg)](https://github.com/minkyong2927/drive-speak/actions/workflows/android.yml)

## 개요

- 문제: 폰 AI 음성 대화는 방향지시등·회전·내비 안내 시점을 모름 → 차선 변경 중에도 계속 말함
- 해결: 차량 신호로 **운전 여유도**를 0~3단계로 판단하고, 튜터의 말하기·질문·난이도를 그에 맞춤
- 화면: 주행 중에는 상태 표시만, 교정 카드와 학습 기록은 정차·주차 후에만

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

1. 바쁜 순간 멈춤: 여유도 '바쁨'이면 말을 끊고 질문 보류, 안정되면 끊긴 문장 처음부터
2. 여유도별 난이도: 신호 대기·직진 구간에 말하기, 복잡한 구간은 듣기
3. 도착 시간 맞춤 수업: 도착 예정 시간으로 수업 길이 구성, 도착 2분 전 오늘 표현 복습
4. 말하면 고쳐주는 회화: STT → LLM 한 문장 교정 → 따라 말하기
5. 동승자 모드: 좌석 착석 정보로 함께하는 단어 퀴즈 전환
6. 상황 표현 한 마디: 주차장·주유소·드라이브스루 진입 시 표현 1개

## Pleos Connect SDK/API 사용 계획

| 모듈 | 쓰는 데이터 | 용도 |
|---|---|---|
| Vehicle | 방향지시등, 브레이크, 조향각, 와이퍼 | 여유도 판단, 멈춤·재개 |
| Vehicle | 좌석 착석 정보 | 동승자 모드 |
| NaviHelper | 목적지, 도착 예정 시간, 남은 거리, 안내 시점 | 수업 길이, 도착 전 복습, 안내 음성과 겹침 방지 |
| FusedLocation | 위치, 속도 | 정차 판단, 주차장·주유소 진입 감지 |
| STT / TTS | 운전자 발화, 튜터 문장 | 대화 입출력 (내비 음성 우선) |
| LLM | 발화 텍스트, 수업 문맥 | 회화 생성, 문장 교정 |

## 구현 현황

| 구분 | 1차 (~11/15) | 2차 준비 (~12/2) |
|---|---|---|
| 여유도 엔진 | Kotlin 구현 + 단위 테스트 18개 | 실차 신호로 임계값 조정 |
| 차량 신호 | 웹 프로토타입 시뮬레이션 → 엔진 | `VehicleSignalSource`에 Pleos SDK 구현체 연결 |
| 화면 | 웹 프로토타입을 WebView로 표시 | 네이티브 상태 화면 |
| 대화 | 프로토타입 시나리오 | STT·LLM·TTS 연동 |

- 앱 화면의 여유도 단계와 근거는 프로토타입 JS가 아니라 Kotlin `LoadEngine`이 계산 (`MainActivity` 브리지)

## 시연

- 시연 영상: [docs/demo.webm](docs/demo.webm) (1분 14초, 무음)
- 웹 프로토타입: [prototype/index.html](prototype/index.html) → 브라우저로 열면 바로 동작, '자동 시연' 버튼

## 빌드·실행

필요: JDK 17, Android SDK 35, Gradle 8.11

```bash
gradle :app:testDebugUnitTest   # 여유도 엔진 테스트
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
    VehicleSignalSource.kt   신호 공급원 인터페이스 (Pleos SDK 연결 지점)
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
