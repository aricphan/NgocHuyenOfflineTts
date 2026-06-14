# NgocHuyenOfflineTts real fixed v4

Bản này sửa lỗi crash do `espeak-ng-data` không được copy ra filesystem.

Cách dùng:
1. Giải nén zip.
2. Mở thư mục bằng Android Studio.
3. Sync Gradle.
4. Run app.

Build lần đầu sẽ tự tải assets từ `aricphan/onnxvoice` và native libs sherpa-onnx.
Nếu bạn đã build bản cũ, hãy gỡ app cũ trên điện thoại rồi cài lại.
