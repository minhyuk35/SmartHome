# SmartHome

PC와 POP 보드로 구성된 스마트홈 과제용 프로젝트입니다. 음성(STT/TTS), 얼굴 인식, Java GUI, Spring Web 대시보드, Jupyter 노트북을 연동합니다.

## 구성 개요
- Python (`Python/`): Whisper STT + Google TTS, 얼굴 인식(`face_recognition`), POP/Java와 TCP 통신. `main.py`가 통합 런타임.
- Java GUI (`Java/`): PC용 토스 스타일 제어판. 음성 녹음 트리거, LED/팬/RGB 제어, 얼굴 등록·인증 버튼, 센서 표시.
- Spring Boot (`spring-app/`): TCP 브리지를 통해 들어오는 센서/문 이벤트/명령을 WebSocket으로 중계해 주는 웹 대시보드(기본 포트 8080).
- Jupyter (`Jupyter/`): POP 보드 테스트 노트북. 센서 송신(`Data_TCP.ipynb`), 도어락 시뮬레이터(`Door.ipynb`), LED PWM 제어(`AIot_LED.ipynb`), 스마트홈 예제(`Smart_Home.ipynb`).

## 포트 매핑
- 명령: `39186`
- 센서 피드: `39187`
- 문 상태(POP→PC): `39188`
- 도어 이벤트 브로드캐스트(PC→Spring/기타): `39189`
- 음성 서버(PC Whisper): `40191`

## 실행 순서 예시
1) Python: `pip install -r requirements.txt` 후 `cd Python && python main.py` 실행. (웹캠/마이크 필요, `owner_face.npy` 없으면 얼굴 등록 필요)
2) Java GUI: `cd Java && javac *.java && java Main` 실행.
3) Spring 대시보드: `cd spring-app && mvn spring-boot:run` (포트 8080, WebSocket `/ws`).
4) 스마트홈: `Jupyter/Data_TCP.ipynb` 등 노트북으로 센서값을 주기적으로 `SENSOR ...` 포맷으로 39187 포트에 송신. 도어락 이벤트는 39188/39189 포트 사용.

## 주요 기능 요약
- 음성: GUI 버튼 또는 Spring WebSocket 메시지로 `START_RECORDING`/`STOP_RECORDING`을 보내면 Whisper STT → 명령 파싱(`LED_ON/OFF`, `FAN_ON/OFF`, `UNLOCK`) → TCP로 전송 → TTS 응답.
- 얼굴: Java GUI의 `REQ_FACE_UNLOCK` 버튼 → Python `main.py`에서 10초간 얼굴 인증 → 성공 시 `UNLOCK` 송신. `REGISTER_FACE` 버튼으로 `owner_face.npy` 갱신.
- 센서/문 이벤트: POP 보드가 `SENSOR ... PIR=...`/`LOCKED`/`UNLOCKED` 등을 송신하면 Java GUI와 Spring 대시보드가 실시간 갱신.

## 디렉터리 참고
- `Python/main.py` : 통합 런타임, Whisper/TTS/얼굴 인식, TCP 클라이언트/서버.
- `Java/SmartHomeGUI.java` : Swing 기반 제어 UI, RGB 슬라이더, 음성 토글, 얼굴 등록/인증 버튼.
- `spring-app/` : `TcpCommandServer`, `SensorTcpServer`, `DoorEventTcpServer`로 TCP 수신 후 WebSocket 브로드캐스트. `application.properties`로 포트(8080) 설정.
- `Jupyter/README_FIXES.md` : POP 환경 설정 및 포트/메시지 예시 정리.
