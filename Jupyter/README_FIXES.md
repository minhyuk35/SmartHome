# Jupyter 연동 메모

이 노트북들은 POP 하드웨어가 있는 라즈베리 파이(또는 호환 보드)에서 실행을 전제로 함. PC/Spring/Python과 연결할 때 필요한 설정만 정리.

## 네트워크/포트
- PC(Spring) IP: `127.0.0.1` 로 통일. 다른 IP를 쓸 경우 노트북의 `JAVA_IP` 값을 PC 주소로 바꿔야 함.
- 포트는 기존과 동일:  
  - 명령 39186, 센서 39187, 도어 39188

## 각 노트북 체크 사항
- `Data_TCP.ipynb`: 가스/먼지/온습도/PIR를 읽어 1초마다 `SENSOR ...` 패킷을 39187로 송신. POP 라이브러리(`Gas`, `Dust`, `TempHumi`, `Pir`)와 센서 배선이 필요.
- `Door.ipynb`: 도어락/키패드/PIR/부저/서보 제어.  
  - 명령 수신: 39186(TCP)에서 `UNLOCK`, `LOCK`, `LED_ON`, `LED_OFF`, `REQ_FACE_UNLOCK` 처리.  
  - 도어 이벤트 송신: 39188(TCP)로 `LOCKED`/`UNLOCKED`/`ALERT_FAIL_3`.  
  - 센서 전송: 39187(TCP)로 `SENSOR ... PIR=...` 포함.  
  - 임시 비밀번호: `[10,2,10,4]` (키패드 채널 기준) — 웹 비밀번호(1234)와 별개.
- `AIot_LED.ipynb`: CDS(조도) 기반 PWM LED 자동 밝기.
- `Smart_Home.ipynb`: 미리 작성된 홈 제어 샘플(POP 하드웨어 필요).

## 동작 순서(라즈베리 파이 쪽)
1. PC에서 Spring Boot 실행(`server.port=8081`), Python `main.py` 실행.  
2. 라즈베리 파이에서 `Data_TCP.ipynb` 실행 → 센서 데이터가 Spring으로 흐름.  
3. 도어락/키패드까지 쓸 경우 `Door.ipynb` 실행 → 명령 수신/도어 이벤트 송신/PIR 포함.  
4. 웹 대시보드에서 PIR 감지 시 인증 배너 표시, `UNLOCK` 수신 시 제어 버튼 활성화.

## 참고
- POP 라이브러리/센서가 없는 환경에서는 노트북이 동작하지 않음(하드웨어 의존).  
- PC에서 “값이 안 온다”면 ① PC IP/포트 확인 ② 노트북에서 TCP 연결 성공 로그 확인 ③ 방화벽 허용 확인. 
