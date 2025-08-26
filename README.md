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

`Room Acoustic`은 **YOLOv8**, **ARCore**, **OpenAI API** 등의 기술을 활용하여 사용자의 실내 공간을 분석하고, 스피커 위치 및 음향 환경을 개선할 수 있도록 돕는 어플리케이션입니다.  

---

## ⚙️ 주요 기능

| 기능 번호 | 기능명 | 설명 |
|-----------|--------|------|
| 1️⃣ | YOLOv8 기반 스피커 탐지 | 학습된 YOLOv8 모델을 통해 실시간으로 스피커를 인식하고 위치를 시각화 |
| 2️⃣ | Google ARCore 기반 방 크기 측정 | AR 기반 거리 감지를 통해 공간의 폭, 깊이, 높이 등을 계산 |
| 3️⃣ | OpenAI 기반 챗봇 | 사용자의 질문에 따라 공간 분석 피드백 또는 추천을 제공 |

---

## 📅 진행 상황 (2025.07.01 기준)

### ✅ 완료된 작업
- ✅ YOLOv8 학습 완료 및 Android용 `.tflite` 모델로 변환 후 적용 → 어플리케이션 내 스피커 탐지 정상 작동
- ✅ 챗봇 초안 구성 완료 → OpenAI API 기반으로 작동하며, 초기 프롬프트 테스트 중
- ✅ ARCore 기반 방 크기 측정으로 전환 시도 중  
  (MiDaS Depth estimation은 거리 측정 정밀도 부족으로 보류)

### ⚠️ 진행 중 / 개선 필요
- ⚠️ 챗봇 프롬프트 개선  
  → 더 자연스러운 문장 표현, 오류 감소를 위한 정제 필요  
- ⚠️ 방 크기 측정과 스피커 좌표 정합성 확보  
  → 정확한 위치 데이터를 AR 기반으로 확보 중

---

## 🧪 앞으로의 계획

- [ ] 🔧 **챗봇 프롬프트 개선**  
  사람과 유사한 톤, 일관된 응답 설계를 목표로 개선

- [ ] 🔉 **사운드 분석 기능 개발**  
  스마트폰 내장 마이크를 활용해 실내 녹음을 수집하고, 분석 결과를 기반으로 룸 어쿠스틱 피드백 도출

---

## 📂 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin (Android), Python (YOLOv8, 모델 학습) |
| AI / ML | YOLOv8, OpenAI GPT |
| AR | Google ARCore |
| 음향 분석 예정 | Android 녹음 기능 + 커스텀 분석 알고리즘 |
| 기타 | TFLite, Depth API, Plane Detection, 프롬프트 엔지니어링 등 |

---

## 📌 참고 이미지 (추후 추가 예정)

> 📸 YOLO 탐지 결과, ARCore 측정 화면, 챗봇 UI 등 추가 예정

---
