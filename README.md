# NgocHuyenOfflineTts

App Android đọc TTS offline bằng model Ngọc Huyền ONNX và Sherpa-ONNX.

## Cách chạy

1. Mở thư mục này bằng Android Studio.
2. Bấm **Sync Now**.
3. Build lần đầu sẽ tự tải:
   - assets giọng đọc từ `https://github.com/aricphan/onnxvoice`
   - native libs Sherpa-ONNX từ release chính chủ `v1.13.2`
4. Bấm Run.

Nếu Gradle báo không chạy được lệnh `tar`, hãy cài Git for Windows hoặc giải nén file `sherpa-onnx-v1.13.2-android.tar.bz2` thủ công rồi copy thư mục `jniLibs` vào `app/src/main/jniLibs`.
