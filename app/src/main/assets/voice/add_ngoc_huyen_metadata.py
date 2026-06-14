import json
import os
import sys

try:
    import onnx
except ImportError:
    print("Bạn chưa cài thư viện onnx.")
    print("Chạy lệnh: py -m pip install onnx==1.17.0")
    sys.exit(1)

MODEL = "ngoc-huyen.onnx"
CONFIG = MODEL + ".json"

if not os.path.exists(MODEL):
    print(f"Không thấy file {MODEL}. Hãy đặt script này trong thư mục assets.")
    sys.exit(1)

if not os.path.exists(CONFIG):
    print(f"Không thấy file {CONFIG}.")
    sys.exit(1)

with open(CONFIG, "r", encoding="utf-8") as f:
    config = json.load(f)

# Tạo lại tokens.txt đúng theo config
id_map = config.get("phoneme_id_map", {})
with open("tokens.txt", "w", encoding="utf-8") as f:
    for symbol, ids in id_map.items():
        if not ids:
            continue
        f.write(f"{symbol} {ids[0]}\n")

meta_data = {
    "model_type": "vits",
    "comment": "piper",
    "language": "Vietnamese",
    "voice": config.get("espeak", {}).get("voice", "vi"),
    "has_espeak": 1,
    "n_speakers": config.get("num_speakers", 1),
    "sample_rate": config.get("audio", {}).get("sample_rate", 22050),
}

print("Đang mở model:", MODEL)
model = onnx.load(MODEL)

# Xóa metadata cũ trùng key để tránh bị lặp
keep = [p for p in model.metadata_props if p.key not in meta_data]
del model.metadata_props[:]
model.metadata_props.extend(keep)

for key, value in meta_data.items():
    item = model.metadata_props.add()
    item.key = str(key)
    item.value = str(value)

onnx.save(model, MODEL)

print("Đã thêm metadata vào model:")
for k, v in meta_data.items():
    print(f"  {k}: {v}")
print("Đã tạo/cập nhật tokens.txt")
print("Xong. Bây giờ commit + push lại repo onnxvoice.")
