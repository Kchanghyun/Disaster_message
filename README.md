# Disaster Alert Translator App (Client)

> 외국인을 위한 위치 기반 재난 문자 번역 및 알림 앱 (Android)

## 📌 소개

한국에 거주하는 외국인들을 위해, 재난문자를 **자동 번역**하고 **알림으로 수신**하는 앱입니다.  
위치 기반 필터링을 통해 사용자에게 **해당되는 재난문자만** 전달합니다.

## 🧩 주요 기능

- 위치 기반 재난문자 수신
- FCM 푸시 알림
- GPT API를 통한 재난문자 자동 번역 (14개 언어 지원)
- 7일간의 재난문자 수신 내역 확인
- 앱 초기 설정 시 언어/위치 동의 및 저장

## 🛠 사용 기술

| 기술 | 설명 |
|------|------|
| Kotlin | Android 앱 개발 |
| Firebase FCM | 푸시 알림 수신 |
| SharedPreferences | 로컬 데이터 저장 (언어/위치) |
| Retrofit | 서버와 REST 통신 |
| GPT API | 텍스트 번역 |

## 📸 앱 화면 예시
> ![Screenshot_20241210_170217_Permission controller](https://github.com/user-attachments/assets/c3193804-0e1e-4357-9849-b17e6c090815)
![Screenshot_20241210_170221_Disaster_message](https://github.com/user-attachments/assets/4fe0f055-11b3-445a-b626-e5d60e5b3797)
![Screenshot_20241210_170226_Permission controller](https://github.com/user-attachments/assets/5f54cce2-0a76-4b0f-a516-19b3dea237fa)
![Screenshot_20241210_170235_Disaster_message](https://github.com/user-attachments/assets/da36b825-36b4-4fef-8e71-e9e1bdf25f5f)
![Screenshot_20241210_170257_Disaster_message](https://github.com/user-attachments/assets/3913f017-70db-46f9-9999-99d590dfb0d5)
![Screenshot_20241210_170330_One UI Home](https://github.com/user-attachments/assets/dd4870a5-39ac-43cd-8430-9677ab077207)
![Screenshot_20241210_170333_One UI Home](https://github.com/user-attachments/assets/f3a3f7b1-09c9-4595-93b9-1973ace60d51)
![Screenshot_20241210_170339_Disaster_message](https://github.com/user-attachments/assets/903095ae-7d45-43c4-beb7-f63fea6d6676)

