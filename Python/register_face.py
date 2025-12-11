import cv2
import face_recognition
import numpy as np

# 웹캠 켜기
video_capture = cv2.VideoCapture(0)

print("📸 [얼굴 등록 모드]")
print("카메라를 바라보고 키보드의 's' 키를 누르면 저장됩니다.")
print("('q'를 누르면 취소)")

while True:
    ret, frame = video_capture.read()
    if not ret: break

    cv2.imshow('Register Face', frame)

    key = cv2.waitKey(1) & 0xFF
    
    # 's' 누르면 저장
    if key == ord('s'):
        rgb_frame = frame[:, :, ::-1] # 색상 변환
        
        # 얼굴 찾기
        boxes = face_recognition.face_locations(rgb_frame)
        
        if len(boxes) == 0:
            print("❌ 얼굴을 못 찾겠어요. 정면을 봐주세요!")
        else:
            # 얼굴 특징 추출
            encodings = face_recognition.face_encodings(rgb_frame, boxes)
            owner_encoding = encodings[0]
            
            # 파일로 저장
            np.save("owner_face.npy", owner_encoding)
            print("✅ 얼굴 저장 완료! (owner_face.npy 생성됨)")
            break

    elif key == ord('q'):
        print("취소됨")
        break

video_capture.release()
cv2.destroyAllWindows()