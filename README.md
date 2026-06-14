# Ngọc Huyền Offline TTS Android

App Android đọc TTS offline bằng model `ngoc-huyen.onnx` từ repo:

https://github.com/aricphan/onnxvoice/tree/main/assets

## Cách dùng trong Android Studio

1. Giải nén file ZIP này.
2. Mở thư mục `NgocHuyenOfflineTts` bằng Android Studio.
3. Bấm **Sync Now** nếu Android Studio hỏi.
4. Bấm **Run**.

Lần build đầu tiên Gradle sẽ tự tải assets từ `aricphan/onnxvoice` và copy vào:

```text
app/src/main/assets/voice/
```

Sau khi APK đã build/cài vào máy, app đọc offline, không cần mạng để tạo giọng nói.

## Nếu muốn tự copy assets thay vì tải tự động

Copy các file này vào `app/src/main/assets/voice/`:

```text
ngoc-huyen.onnx
ngoc-huyen.onnx.json
tokens.txt
espeak-ng-data/
```

Sau đó build/run bình thường.
