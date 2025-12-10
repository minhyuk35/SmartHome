"""
음성 인식 → Java 서버 → Jupyter 연동 메인 스크립트
문 열림 이벤트 또는 Java GUI 버튼으로 STT 실행
Whisper 사용 (한국어 인식 최적화)
"""

import socket
import time
import whisper
import sounddevice as sd
import numpy as np
from pathlib import Path
import threading


# ================================
# 🔥 자바로 명령 보내기
# ================================
def send_to_java(cmd):
    """Java TcpServer에 명령 전송 (자동 재시도)"""
    max_retries = 3
    for attempt in range(max_retries):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(2)
            print(f"[SEND] Java 서버 연결 시도 ({attempt+1}/{max_retries})...")
            s.connect(("127.0.0.1", 39186))  # 로컬호스트
            s.sendall((cmd + "\n").encode())
            s.close()
            print(f"[SEND] ✅ JAVA로 전송됨: {cmd}")
            return True
        except Exception as e:
            print(f"[SEND] ❌ 시도 {attempt+1} 실패: {e}")
            if attempt < max_retries - 1:
                time.sleep(0.5)  # 재시도 전 대기
    
    print(f"[SEND] ⚠️  모든 재시도 실패: {cmd}")
    return False
# ================================
# 🔥 1) Whisper STT 모델 로드
# ================================

print("📢 Whisper 모델 로딩 중...")
print("⚠️  첫 실행 시 모델 다운로드 (약 1-2GB, 시간 소요)")

# base 모델 사용 (small보다 정확, medium보다 빠름)
model = whisper.load_model("base", device="cpu")

print("✅ Whisper 모델 로드 완료")


# ================================
# 🔥 음성 인식 플래그 (토글 모드)
# ================================
# `running_event`가 set 상태면 계속 녹음/인식 모드
running_event = threading.Event()


# ================================
# 🔥 2) 도어락 이벤트 수신 (별도 스레드)
# ================================
def listen_door_events():
    """도어락 이벤트 수신 - 문이 열리면 계속 음성 인식"""
    SERVER_IP = "127.0.0.1"
    SERVER_PORT = 39189

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((SERVER_IP, SERVER_PORT))
        print("[PY] ✅ 자바 이벤트 서버 접속 완료")

        while True:
            try:
                data = sock.recv(1024).decode().strip()
                if not data:
                    continue

                print(f"[PY] 도어락 상태: {data}")

                if data == "UNLOCKED":
                    print("🚪 문 열림! 자동 음성 인식 모드 시작")
                    start_recording()
            except Exception as e:
                print(f"도어락 수신 오류: {e}")
                break

        sock.close()
    except ConnectionRefusedError:
        print("[PY] ⚠️  도어락 서버 미연결 (GUI 버튼으로만 진행)")
    except Exception as e:
        print(f"[PY] ⚠️  도어락 오류: {e}")


# 도어락 이벤트 리스너 스레드 시작
door_thread = threading.Thread(target=listen_door_events, daemon=True)
door_thread.start()


# ================================
# 🔥 3) Java GUI 음성 인식 서버 (별도 스레드)
# ================================
def listen_voice_server():
    """Java GUI에서 음성 인식 요청을 받음"""
    server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_sock.bind(("127.0.0.1", 40191))
    server_sock.listen(5)
    
    print("[PY] ✅ 음성 인식 서버 시작 (포트 40191)")

    while True:
        try:
            client_sock, addr = server_sock.accept()
            print(f"[PY] Java GUI 연결: {addr}")

            data = client_sock.recv(1024).decode().strip()
            if data == "START_RECORDING":
                print("[PY] Java GUI에서 START 요청 — 녹음 시작")
                start_recording()
                running_event.set()
            elif data == "STOP_RECORDING":
                print("[PY] Java GUI에서 STOP 요청 — 녹음 중지 및 인식 시작")
                running_event.clear()
                stop_recording_and_process()

            client_sock.close()
        except Exception as e:
            print(f"음성 서버 오류: {e}")


# 음성 인식 서버 스레드 시작
voice_server_thread = threading.Thread(target=listen_voice_server, daemon=True)
voice_server_thread.start()


# ================================
# 🔥 오디오 설정 및 녹음
# ================================
SAMPLE_RATE = 16000

# 녹음 제어 플래그 (START~STOP 사이 계속 녹음)
is_recording = False
audio_chunks = []
stream = None


def start_recording():
    """녹음 시작 (연속 녹음)"""
    global is_recording, stream, audio_chunks
    is_recording = True
    audio_chunks = []
    
    print("🎤 마이크 녹음 시작 (STOP 버튼을 누를 때까지 계속 녹음)...")
    
    # 오디오 스트림 시작
    stream = sd.InputStream(channels=1, samplerate=SAMPLE_RATE, dtype=np.float32)
    if stream is not None:
        stream.start()
    
    # 녹음 데이터 수집 스레드
    def recording_thread():
        global stream
        while is_recording:
            try:
                if stream is not None:
                    chunk, _ = stream.read(SAMPLE_RATE // 10)  # 100ms씩 읽기
                    if chunk is not None and len(chunk) > 0:
                        audio_chunks.append(chunk)
            except Exception as e:
                print(f"녹음 오류: {e}")
                break
        
        if stream is not None:
            stream.stop()
            stream.close()
    
    import threading
    rec_thread = threading.Thread(target=recording_thread, daemon=True)
    rec_thread.start()


def stop_recording():
    """녹음 중지 및 오디오 반환"""
    global is_recording, audio_chunks
    is_recording = False
    
    # 스레드가 정리되도록 잠시 대기
    time.sleep(0.5)
    
    if len(audio_chunks) == 0:
        print("⚠️  녹음된 오디오가 없습니다")
        return None
    
    # 모든 청크를 합치기
    audio = np.concatenate(audio_chunks, axis=0)
    print(f"✅ 녹음 완료 ({len(audio) / SAMPLE_RATE:.2f}초)")
    return audio


def audio_to_file(audio, sample_rate=SAMPLE_RATE):
    """녹음된 오디오를 임시 파일로 저장"""
    temp_file = Path(__file__).parent / "temp_audio.wav"
    import soundfile as sf
    sf.write(str(temp_file), audio, sample_rate)
    return str(temp_file)


# ================================
# 🔥 음성 명령 처리
# ================================
def process_command(text):
    """Whisper 인식 결과를 명령으로 변환"""
    print(f"[STT] 인식: {text}")

    text_lower = text.lower()

    # LED 제어
    if "불 켜" in text_lower or "불켜" in text_lower or "라이트 온" in text_lower:
        send_to_java("LED_ON")

    elif "불 꺼" in text_lower or "불꺼" in text_lower or "라이트 오프" in text_lower:
        send_to_java("LED_OFF")

    # 선풍기 제어
    elif "선풍기 켜" in text_lower or "팬 온" in text_lower:
        send_to_java("FAN_ON")

    elif "선풍기 꺼" in text_lower or "팬 오프" in text_lower:
        send_to_java("FAN_OFF")

    # 수면 모드
    elif "수면" in text_lower or "잠자기" in text_lower or "자기" in text_lower:
        send_to_java("LIGHT_SLEEP")

    # 따뜻한 조명
    elif "따뜻한" in text_lower or "따뜻해" in text_lower or "웜" in text_lower:
        send_to_java("LIGHT_WARM")

    # RGB 제어
    elif "화이트" in text_lower or "하얀" in text_lower:
        send_to_java("RGB_ON")

    elif "rgb 꺼" in text_lower or "색 꺼" in text_lower:
        send_to_java("RGB_OFF")

    # 도어
    elif "문 열어" in text_lower or "도어 열어" in text_lower or "열어" in text_lower:
        send_to_java("UNLOCK")

    else:
        print("❓ 인식된 명령을 찾을 수 없음")


# ================================
# 🔥 4) 녹음 완료 후 인식 처리
# ================================
def stop_recording_and_process():
    """녹음 중지, 파일 저장, Whisper 인식, 명령 전송"""
    global audio_chunks
    
    audio = stop_recording()
    if audio is None:
        return
    
    # 임시 파일로 저장
    audio_file = audio_to_file(audio)
    
    # Whisper로 인식
    try:
        print("🔄 Whisper로 인식 중...")
        result = model.transcribe(audio_file, language="ko", verbose=False)
        text = str(result["text"]).strip()
        
        if text:
            process_command(text)
        else:
            print("⚠️  음성이 인식되지 않음")
    
    except Exception as e:
        print(f"❌ 인식 오류: {e}")
    
    # 임시 파일 삭제
    import os
    try:
        os.remove(audio_file)
    except:
        pass


# ================================
# 🔥 5) Whisper STT 메인 루프
# ================================
print("\n" + "="*60)
print("🎤 실시간 음성 인식 준비 완료!")
print("="*60)
print("명령어 예시:")
print("  - '불 켜', '불 꺼'")
print("  - '선풍기 켜', '선풍기 꺼'")
print("  - '수면 모드', '따뜻한 모드'")
print("  - '문 열어'")
print("="*60)
print("👉 Java GUI의 '🎤 음성 인식' 버튼을 누르거나")
print("👉 도어락이 열리면 자동으로 시작됩니다\n")

try:
    # 모든 리스너 스레드가 준비될 때까지 대기
    time.sleep(1)
    print("[PY] ✅ 모든 서버 준비 완료. 이제 START 신호를 기다립니다...")
    
    # 이 주 루프는 특별히 할 일이 없으므로 계속 대기
    while True:
        time.sleep(1)

except KeyboardInterrupt:
    print("\n\n👋 프로그램 종료")
    is_recording = False