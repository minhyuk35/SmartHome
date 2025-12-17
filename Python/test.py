# ... import 문들 ...
import google.generativeai as genai

GEMINI_API_KEY = "AIzaSyCkDESBX1RcTrny6YrfPXnkYtNaQdEv_ew"
genai.configure(api_key=GEMINI_API_KEY)

# 🔍 내 키로 쓸 수 있는 모델 리스트 출력
print("=== 사용 가능한 모델 목록 ===")
for m in genai.list_models():
    if 'generateContent' in m.supported_generation_methods:
        print(m.name)
print("===========================")