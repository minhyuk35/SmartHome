# SmartHome

PC와 POP 보드로 구성된 스마트홈 과제용 프로젝트입니다. 음성(STT/TTS), 얼굴 인식, Java GUI, Jupyter 노트북을 TCP로 연동합니다. (Spring Boot 대시보드는 선택 요소입니다.)

## 구성 개요
- Python (`Python/`): Whisper STT + Google TTS, 얼굴 인식(`face_recognition`), POP/Java와 TCP 통신. `main.py`가 통합 런타임.
- Java GUI (`Java/`): PC용 제어판. 음성 녹음 트리거, LED/팬/RGB 제어, 얼굴 등록·인증 버튼, 센서 표시.
- Jupyter (`Jupyter/`): POP 보드 테스트 노트북. 센서 송신(`Data_TCP.ipynb`), 도어락 시뮬레이터(`Door.ipynb`), LED PWM 제어(`AIot_LED.ipynb`), 스마트홈 예제(`Smart_Home.ipynb`).
- (옵션) Spring Boot (`spring-app/`): TCP 브리지를 통해 들어오는 데이터를 WebSocket으로 중계하는 웹 대시보드.

## 포트 매핑
- 명령 브로드캐스트: `39186`
- 센서 피드: `39187`
- 도어 이벤트: `39189`
- 음성 서버(PC Whisper trigger): `40191`

## 실행 순서 예시 (Spring 없이)
1) Python: `pip install -r requirements.txt` 후 `cd Python && python main.py` 실행. (웹캠/마이크 필요, `owner_face.npy` 없으면 얼굴 등록 필요)
2) Java GUI: `cd Java && javac -encoding UTF-8 *.java && java Main` 실행.
3) (선택) POP/Jupyter: `Jupyter/Data_TCP.ipynb`로 센서를 39187에 송신, `Jupyter/Door.ipynb`로 39186 명령 수신 + 39189 도어 이벤트 송신.

## 주요 기능 요약
- 음성: GUI 버튼이 `START_RECORDING`/`STOP_RECORDING`을 40191로 보내면 Python(Whisper)이 STT → 명령 파싱(`LED_ON/OFF`, `FAN_ON/OFF`, `UNLOCK`) → 39186으로 명령을 다시 전송 → TTS 응답.
- 얼굴: Java GUI의 `REQ_FACE_UNLOCK` 버튼 → Python `main.py`에서 10초간 얼굴 인증 → 성공 시 `UNLOCK` 송신. `REGISTER_FACE` 버튼으로 `owner_face.npy` 갱신.
- 센서/문 이벤트: POP 보드가 `SENSOR ... PIR=...`(39187) / `LOCKED`·`UNLOCKED` 등 도어 이벤트(39189)를 보내면 Java GUI가 즉시 갱신한다.

## 디렉터리 참고
- `Python/main.py` : 통합 런타임, Whisper/TTS/얼굴 인식, TCP 클라이언트/서버.
- `Java/SmartHomeGUI.java` : Swing 기반 제어 UI, RGB 슬라이더, 음성 토글, 얼굴 등록/인증 버튼.
- `Jupyter/README_FIXES.md` : POP 환경 설정 및 포트/메시지 예시 정리.
- (옵션) `spring-app/` : 필요 시 WebSocket 대시보드를 위한 TCP 브리지.
