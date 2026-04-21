import numpy as np
import tensorflow as tf

MODEL = "mobilebert_embedder_128_drq.tflite"
SEQ_LEN = 384

interpreter = tf.lite.Interpreter(model_path=MODEL)
interpreter.allocate_tensors()

print("=== INPUTS ===")
for d in interpreter.get_input_details():
    print(d["index"], d["name"], d["shape"], d["dtype"])

print("=== OUTPUTS ===")
for d in interpreter.get_output_details():
    print(d["index"], d["name"], d["shape"], d["dtype"])

# 用你 Android 日志里同样的输入
ids = np.zeros((1, SEQ_LEN), dtype=np.int32)
mask = np.zeros((1, SEQ_LEN), dtype=np.int32)
segment_ids = np.zeros((1, SEQ_LEN), dtype=np.int32)

ids[0, 0] = 101
ids[0, 1] = 19128
ids[0, 2] = 16150

mask[0, 0] = 1
mask[0, 1] = 1
mask[0, 2] = 1

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# 先按 index 顺序喂
interpreter.set_tensor(input_details[0]["index"], ids)
interpreter.set_tensor(input_details[1]["index"], mask)
interpreter.set_tensor(input_details[2]["index"], segment_ids)

interpreter.invoke()

out = interpreter.get_tensor(output_details[0]["index"])[0]

print("RAW mean:", out.mean())
print("RAW min :", out.min())
print("RAW max :", out.max())
print("RAW var :", out.var())
print("RAW head:", out[:12])
