<h1 align="center">📱 Room Acoustic</h1>
<p align="center"><i>룸 어쿠스틱 환경 조성을 위한 스마트 어플리케이션</i></p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white"/>
  <img src="https://img.shields.io/badge/ARCore-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/YOLOv8-FF5252?style=for-the-badge&logo=OpenCV&logoColor=white"/>
  <img src="https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white"/>
</p>

---

## 🎯 프로젝트 개요

`Room Acoustic`은 **YOLOv8**, **Google ARCore**, **OpenAI API** 등의 최신 기술을 활용하여 사용자의 실내 공간을 정밀하게 분석하고, 최적의 스피커 배치 및 음향 환경을 조성할 수 있도록 돕는 스마트 어플리케이션입니다.

---

## ⚙️ 주요 기능

| 기능 번호 | 기능명 | 설명 |
|-----------|--------|------|
| 1️⃣ | **방 관리 및 측정 기록** | 사용자가 여러 방을 생성, 관리하고 각 방에 대한 측정 및 대화 기록을 저장합니다. |
| 2️⃣ | **AR 기반 정밀 방 크기 측정** | Google ARCore를 활용하여 방의 폭, 깊이, 높이를 단계별로 측정합니다. 6점 측정 방식을 통해 더욱 정확한 3D 공간 데이터를 확보합니다. |
| 3️⃣ | **YOLOv8 기반 실시간 스피커 탐지** | 학습된 YOLOv8 모델을 통해 실시간으로 스피커를 인식하고, 3D 공간 내 스피커의 위치를 추적 및 시각화합니다. |
| 4️⃣ | **3D 공간 시각화 및 배치 시뮬레이션** | 측정된 방 크기와 탐지된 스피커 위치를 기반으로 3D 공간을 렌더링하여 시각적으로 스피커 배치를 확인하고 최적화합니다. |
| 5️⃣ | **AI 챗봇을 통한 음향 컨설팅** | OpenAI API 기반 챗봇이 측정된 방 데이터와 스피커 배치 정보를 바탕으로 사용자에게 맞춤형 음향 분석 피드백 및 개선 방안을 제공합니다. |
| 6️⃣ | **사운드 테스트 및 분석 가이드** | 실내 음향 테스트를 위한 가이드를 제공하고, 녹음된 사운드 데이터를 기반으로 음향 특성을 분석하여 사용자에게 유용한 정보를 전달합니다. |

---

## 📅 진행 상황 (2025.09.25 기준)

### ✅ 완료된 작업
- ✅ YOLOv8 학습 완료 및 Android용 `.tflite` 모델 변환 및 적용: 어플리케이션 내 스피커 탐지 기능 정상 작동.
- ✅ Google ARCore 기반 방 크기 측정 기능 구현: 폭, 깊이, 높이 측정 및 6점 측정 방식 도입.
- ✅ OpenAI API 기반 챗봇 통합: 초기 프롬프트 구성 및 대화 기능 구현.
- ✅ 측정된 방 및 스피커 3D 시각화 기능 구현.
- ✅ 방 관리 (추가, 이름 변경, 삭제) 및 측정/채팅 기록 관리 기능 구현.

### ⚠️ 진행 중 / 개선 필요
- ⚠️ 챗봇 프롬프트 정교화: 더 자연스러운 대화 흐름, 정확한 정보 제공 및 오류 감소를 위한 프롬프트 엔지니어링 진행 중.
- ⚠️ 사운드 분석 기능 고도화: 스마트폰 마이크를 활용한 실내 녹음 및 전문적인 음향 분석 알고리즘 통합 작업 진행 중.
- ⚠️ AR 측정 정밀도 및 사용자 경험 개선: 다양한 환경에서의 안정적인 AR 측정 및 직관적인 UI/UX 개선.

---

## 🧪 앞으로의 계획

- [ ] 🔧 **챗봇 프롬프트 최적화**
  사용자 질의에 대한 심층적인 이해와 전문적인 음향 지식을 결합하여, 사람과 유사한 톤으로 일관되고 유용한 응답을 제공하도록 개선.

- [ ] 🔉 **고급 사운드 분석 기능 개발**
  스마트폰 내장 마이크를 통해 수집된 실내 녹음 데이터를 기반으로 잔향 시간(RT60), 주파수 응답 등 상세한 음향 특성을 분석하고, 이를 시각적으로 표현하는 기능 구현.

- [ ] 📈 **측정 데이터 기반 맞춤형 솔루션 제안**
  측정된 방의 물리적 특성과 음향 분석 결과를 종합하여, 사용자에게 최적의 스피커 배치, 흡음재/확산재 배치 등 구체적인 룸 어쿠스틱 개선 솔루션 제안 기능 개발.

---

## 📂 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin (Android), Python (YOLOv8 모델 학습 및 변환) |
| AI / ML | YOLOv8 (객체 탐지), OpenAI GPT (챗봇) |
| AR | Google ARCore (공간 측정 및 3D 데이터 확보) |
| 데이터베이스 | Room Persistence Library (로컬 데이터 저장) |
| UI 프레임워크 | Jetpack Compose |
| API 통신 | Retrofit |
| 기타 | TFLite, Depth API, Plane Detection, 프롬프트 엔지니어링, 3D 렌더링 라이브러리 (추정) |

---

## 📌 참고 이미지 (추후 추가 예정)

> 📸 YOLO 탐지 결과, ARCore 측정 화면, 챗봇 UI, 3D 렌더링 화면 등 추가 예정