"""
[종합 PC 비서]
Whisper 음성 + Google TTS + GUI 제어 + 🔥버튼식 얼굴 인식 (안정화 버전)
"""

import socket
import time
import whisper
import sounddevice as sd
import numpy as np
from pathlib import Path
import threading
import requests       
import urllib.parse   
import os
from playsound import playsound
import cv2                 
import face_recognition    

# ================================
# ⚙️ 설정
# ================================
JAVA_IP = "127.0.0.1"    
CMD_PORT = 39186         
VOICE_SERVER_PORT = 40191
DOOR_EVENT_PORT = 39189

# 상태 플래그
is_registering_mode = False   # 등록 모드 확인
is_active_recognition = False # 인식 모드 확인 (버튼 누를 때만 True)
my_command_lock = False       # 메아리 방지

# ================================
# 🔊 TTS 및 통신
# ================================
def speak_answer(text):
    try:
        # print(f"[TTS] 💬 {text}")
        enc_text = urllib.parse.quote(text)
        url = f"https://translate.google.com/translate_tts?ie=UTF-8&q={enc_text}&tl=ko&client=tw-ob"
        headers = {"User-Agent": "Mozilla/5.0"}
        
        response = requests.get(url, headers=headers)
        filename = "pc_voice_temp.mp3"
        
        # 기존 파일 삭제 (충돌 방지)
        if os.path.exists(filename):
            try: os.remove(filename)
            except: pass
            
        with open(filename, 'wb') as f:
            f.write(response.content)
            
        playsound(filename)
        
        # 재생 후 삭제
        try: os.remove(filename)
        except: pass
    except: pass

def send_to_java(cmd):
    global my_command_lock
    my_command_lock = True
    def release_lock():
        global my_command_lock
        time.sleep(1.5)
        my_command_lock = False
    threading.Thread(target=release_lock).start()

    max_retries = 3
    for attempt in range(max_retries):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(2)
            s.connect((JAVA_IP, CMD_PORT))
            s.sendall((cmd + "\n").encode())
            s.close()
            print(f"[SEND] 📤 JAVA 전송: {cmd}")
            return True
        except:
            if attempt < max_retries - 1: time.sleep(0.5)
    return False

# ================================
# 📸 얼굴 등록 모드 (메모리 패치 적용)
# ================================
def start_face_registration():
    global is_registering_mode
    is_registering_mode = True 
    
    print("📸 [얼굴 등록] 카메라 가동...")
    speak_answer("얼굴 등록 모드입니다.")
    
    # 카메라 0번 (안 되면 1번으로 변경)
    cap = cv2.VideoCapture(0)
    
    if not cap.isOpened():
        print("❌ 카메라를 열 수 없습니다.")
        speak_answer("카메라 오류가 발생했습니다.")
        is_registering_mode = False
        return

    while True:
        ret, frame = cap.read()
        if not ret: 
            print("❌ 프레임을 읽을 수 없습니다.")
            break
        
        cv2.putText(frame, "Press 's' to Save, 'q' to Quit", (50, 50), 
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.imshow('Register Face', frame)
        
        key = cv2.waitKey(1) & 0xFF
        if key == ord('s'): 
            try:
                # 1. BGR -> RGB 변환 (OpenCV 함수 사용)
                rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                
                # 2. 메모리 강제 정렬 (dlib 오류 해결 핵심)
                rgb = np.ascontiguousarray(rgb, dtype=np.uint8)
                
                # 3. 얼굴 찾기
                boxes = face_recognition.face_locations(rgb)
                
                if boxes:
                    enc = face_recognition.face_encodings(rgb, boxes)[0]
                    np.save("owner_face.npy", enc)
                    print("✅ 얼굴 저장 완료")
                    speak_answer("얼굴이 등록되었습니다.")
                    break
                else:
                    print("❌ 얼굴 미감지")
                    speak_answer("얼굴을 찾을 수 없습니다.")
            except Exception as e:
                print(f"❌ 등록 에러: {e}")
                speak_answer("오류가 발생했습니다.")

        elif key == ord('q'):
            print("취소됨")
            speak_answer("취소했습니다.")
            break
            
    cap.release()
    cv2.destroyAllWindows()
    is_registering_mode = False
    print("👀 다시 대기 모드")

# ================================
# 👁️ [핵심] 버튼식 얼굴 인식 스레드 (안정화)
# ================================
def face_recognition_loop():
    global is_active_recognition, is_registering_mode
    print("[Face] 🙂 대기 중 (버튼을 누르면 켜집니다)")
    
    video_capture = None
    last_unlock_time = 0 # 쿨타임 계산용

    while True:
        # 1. 카메라를 꺼야 하는 조건 확인
        # (활성화 요청 없음 OR 등록 중 OR 쿨타임 10초 미만)
        current_time = time.time()
        is_cooldown = (current_time - last_unlock_time < 10)

        if not is_active_recognition or is_registering_mode or is_cooldown:
            if video_capture is not None:
                video_capture.release()
                video_capture = None
                if is_cooldown: 
                    print(f"[Face] ⏳ 쿨타임 대기 ({10 - int(current_time - last_unlock_time)}초)")
                else:
                    print("[Face] 💤 카메라 대기 모드")
            
            # 대기 중일 땐 CPU를 쉬게 해줌
            time.sleep(1) 
            continue

        # 2. 카메라 켜기
        if video_capture is None:
            video_capture = cv2.VideoCapture(0)
            if not video_capture.isOpened():
                speak_answer("카메라를 켤 수 없습니다.")
                is_active_recognition = False
                continue
            print("[Face] 📸 카메라 작동 시작! 얼굴 찾는 중...")

        # 3. 데이터 로드
        try: owner_encoding = np.load("owner_face.npy")
        except: 
            speak_answer("먼저 얼굴 등록을 해주세요.")
            is_active_recognition = False
            continue

        ret, frame = video_capture.read()
        if not ret: continue

        # 4. 인식 시도
        try:
            small_frame = cv2.resize(frame, (0, 0), fx=0.25, fy=0.25)
            
            # [안전 변환]
            rgb_small_frame = cv2.cvtColor(small_frame, cv2.COLOR_BGR2RGB)
            rgb_small_frame = np.ascontiguousarray(rgb_small_frame, dtype=np.uint8)
            
            face_locations = face_recognition.face_locations(rgb_small_frame)
            
            if face_locations:
                face_encodings = face_recognition.face_encodings(rgb_small_frame, face_locations)
                for face_encoding in face_encodings:
                    matches = face_recognition.compare_faces([owner_encoding], face_encoding, tolerance=0.45)
                    
                    if True in matches:
                        print("[Face] 🔓 주인님 확인됨!")
                        speak_answer("주인님이시군요. 문을 열어드립니다.")
                        send_to_java("UNLOCK")
                        
                        # 성공 시점 기록 (쿨타임 시작)
                        last_unlock_time = time.time()
                        
                        # 인식 완료했으니 즉시 종료 (다음 루프에서 카메라 꺼짐)
                        is_active_recognition = False 
                        break 
        except: pass

    if video_capture is not None:
        video_capture.release()

face_thread = threading.Thread(target=face_recognition_loop, daemon=True)
face_thread.start()


# ================================
# 👂 Java GUI 버튼 감시자
# ================================
def listen_java_commands():
    global is_active_recognition
    print("[Thread] 👁️ GUI 버튼 감시 시작...")
    
    while True:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((JAVA_IP, CMD_PORT))
            while True:
                data = sock.recv(1024).decode().strip()
                if not data: break
                if my_command_lock: continue

                print(f"[GUI 수신] {data}")
                
                # 1. 얼굴 인식 요청
                if data == "REQ_FACE_UNLOCK":
                    print("📸 얼굴 인식 요청됨! 10초간 시도")
                    speak_answer("정면을 봐주세요.")
                    is_active_recognition = True
                    
                    # 10초 뒤 타임아웃 처리
                    def timeout_timer():
                        time.sleep(10)
                        global is_active_recognition
                        if is_active_recognition:
                            print("⏰ 타임아웃: 얼굴 인식 실패")
                            is_active_recognition = False
                            speak_answer("얼굴이 확인되지 않았습니다.")
                    threading.Thread(target=timeout_timer).start()

                # 2. 얼굴 등록 요청
                elif data == "REGISTER_FACE":
                    threading.Thread(target=start_face_registration).start()
                
                # 3. 일반 제어 (TTS 피드백 복구됨!)
                elif data == "LED_ON":       speak_answer("조명을 켰습니다.")
                elif data == "LED_OFF":      speak_answer("조명을 껐습니다.")
                elif data == "FAN_ON":       speak_answer("선풍기를 켰습니다.")
                elif data == "FAN_OFF":      speak_answer("선풍기를 껐습니다.")
                elif data == "LIGHT_SLEEP":  speak_answer("수면 모드를 실행합니다.")
                elif data == "LIGHT_WARM":   speak_answer("따뜻한 조명으로 바꿨습니다.")
                elif data == "RGB_ON":       speak_answer("무드등을 켰습니다.")
                elif data == "RGB_OFF":      speak_answer("무드등을 껐습니다.")
                elif data == "UNLOCK":       speak_answer("문을 열었습니다.")
                elif data == "PROMPT_AUTH":
                    speak_answer("얼굴을 보여주세요. 키패드로도 열 수 있습니다.")
                    # 얼굴 인식도 바로 시작
                    is_active_recognition = True
                elif data == "PASSWORD_UNLOCK":
                    speak_answer("비밀번호 확인. 문을 엽니다.")

            sock.close()
        except: time.sleep(3)

cmd_thread = threading.Thread(target=listen_java_commands, daemon=True)
cmd_thread.start()

# ================================
# 🔥 Whisper STT & Logic (기존 유지)
# ================================
print("📢 Whisper 모델 로딩 중...")
model = whisper.load_model("base", device="cpu")
print("✅ 시스템 준비 완료")

running_event = threading.Event()
SAMPLE_RATE = 16000
is_recording = False
audio_chunks = []
stream = None

def start_recording():
    global is_recording, stream, audio_chunks
    is_recording = True
    audio_chunks = []
    print("🎤 녹음 시작...")
    stream = sd.InputStream(channels=1, samplerate=SAMPLE_RATE, dtype=np.float32)
    if stream is not None: stream.start()
    
    def recording_thread():
        global stream
        while is_recording:
            try:
                if stream is not None:
                    chunk, _ = stream.read(SAMPLE_RATE // 10)
                    if chunk is not None: audio_chunks.append(chunk)
            except: break
        if stream is not None: stream.stop(); stream.close()
    threading.Thread(target=recording_thread, daemon=True).start()

def stop_recording():
    global is_recording, audio_chunks
    is_recording = False
    time.sleep(0.1)  # shorter wait for faster turnaround
    if not audio_chunks: return None
    return np.concatenate(audio_chunks, axis=0)

def audio_to_file(audio):
    temp = Path(__file__).parent / "temp_audio.wav"
    import soundfile as sf
    sf.write(str(temp), audio, SAMPLE_RATE)
    return str(temp)

def process_command(text):
    print(f"[STT] 🗣️ {text}")
    text = text.lower()
    if "불 켜" in text:
        speak_answer("네, 조명을 켜겠습니다.")
        send_to_java("LED_ON")
    elif "불 꺼" in text:
        speak_answer("조명을 끕니다.")
        send_to_java("LED_OFF")
    elif "선풍기 켜" in text:
        speak_answer("선풍기를 켭니다.")
        send_to_java("FAN_ON")
    elif "선풍기 꺼" in text:
        speak_answer("선풍기를 끕니다.")
        send_to_java("FAN_OFF")
    elif "문 열어" in text:
        speak_answer("문을 엽니다.")
        send_to_java("UNLOCK")

def stop_recording_and_process():
    audio = stop_recording()
    if audio is None: return
    f = audio_to_file(audio)
    try:
        res = model.transcribe(f, language="ko", verbose=False)
        txt = str(res["text"]).strip()
        if txt: process_command(txt)
    except: pass
    try: os.remove(f)
    except: pass

def listen_door_events():
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((JAVA_IP, DOOR_EVENT_PORT))
        while True:
            data = sock.recv(1024).decode().strip()
            if not data: break
            if data == "UNLOCKED":
                start_recording()
        sock.close()
    except: pass
threading.Thread(target=listen_door_events, daemon=True).start()

def listen_voice_server():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try: s.bind(("127.0.0.1", VOICE_SERVER_PORT)); s.listen(5)
    except: return
    while True:
        try:
            c, _ = s.accept()
            d = c.recv(1024).decode().strip()
            if d == "START_RECORDING": start_recording()
            elif d == "STOP_RECORDING": stop_recording_and_process()
            c.close()
        except: pass
threading.Thread(target=listen_voice_server, daemon=True).start()

print("\n=== [PC 비서 시스템 가동] ===")
print("1. 음성 인식 (Whisper)")
print("2. GUI 연동 (Toss Style)")
print("3. 얼굴 인식/등록 (절전 모드)")
print("============================")

try:
    while True: time.sleep(1)
except KeyboardInterrupt: print("종료")
