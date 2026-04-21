import os
import tensorflow as tf
import tensorflow_hub as hub

SEQ_LEN = 384
EXPORT_DIR = "build/mobilebert_embedder_savedmodel"
TFLITE_PATH = "build/mobilebert_embedder_fp32.tflite"
TFLITE_DRQ_PATH = "build/mobilebert_embedder_drq.tflite"

MOBILEBERT_ENCODER_URL = (
    "https://tfhub.dev/tensorflow/"
    "mobilebert_en_uncased_L-24_H-128_B-512_A-4_F-4_OPT/1"
)

class MobileBertEmbedder(tf.keras.Model):
    def __init__(self, encoder_handle: str):
        super().__init__()
        self.encoder = hub.KerasLayer(
            encoder_handle,
            trainable=False,
            name="mobilebert_encoder"
        )

    @tf.function(
        input_signature=[
            {
                "ids": tf.TensorSpec([None, SEQ_LEN], tf.int32, name="ids"),
                "mask": tf.TensorSpec([None, SEQ_LEN], tf.int32, name="mask"),
                "segment_ids": tf.TensorSpec([None, SEQ_LEN], tf.int32, name="segment_ids"),
            }
        ]
    )
    def serving(self, inputs):
        encoder_inputs = {
            "input_word_ids": inputs["ids"],
            "input_mask": inputs["mask"],
            "input_type_ids": inputs["segment_ids"],
        }

        outputs = self.encoder(encoder_inputs)
        sequence_output = outputs["sequence_output"]   # [B, L, H]

        mask = tf.cast(inputs["mask"], tf.float32)     # [B, L]
        mask = tf.expand_dims(mask, axis=-1)           # [B, L, 1]

        masked_seq = sequence_output * mask
        sum_embeddings = tf.reduce_sum(masked_seq, axis=1)  # [B, H]
        token_count = tf.reduce_sum(mask, axis=1)           # [B, 1]
        token_count = tf.maximum(token_count, 1e-9)

        mean_pooled = sum_embeddings / token_count          # [B, H]
        embedding = tf.math.l2_normalize(mean_pooled, axis=-1, name="embedding")

        return {"embedding": embedding}


def export_savedmodel():
    os.makedirs(os.path.dirname(EXPORT_DIR), exist_ok=True)

    model = MobileBertEmbedder(MOBILEBERT_ENCODER_URL)

    dummy = {
        "ids": tf.constant([[101, 2023, 2003, 1037, 3231] + [0] * (SEQ_LEN - 5)], dtype=tf.int32),
        "mask": tf.constant([[1, 1, 1, 1, 1] + [0] * (SEQ_LEN - 5)], dtype=tf.int32),
        "segment_ids": tf.zeros([1, SEQ_LEN], dtype=tf.int32),
    }

    out = model.serving(dummy)["embedding"]
    print("SavedModel test shape:", out.shape)
    print("SavedModel mean:", tf.reduce_mean(out).numpy())
    print("SavedModel min :", tf.reduce_min(out).numpy())
    print("SavedModel max :", tf.reduce_max(out).numpy())
    print("SavedModel head:", out.numpy()[0][:12])

    tf.saved_model.save(
        model,
        EXPORT_DIR,
        signatures={"serving_default": model.serving},
    )
    print("SavedModel exported:", EXPORT_DIR)


def export_tflite_fp32():
    converter = tf.lite.TFLiteConverter.from_saved_model(EXPORT_DIR)
    tflite_model = converter.convert()

    with open(TFLITE_PATH, "wb") as f:
        f.write(tflite_model)

    print("FP32 TFLite written:", TFLITE_PATH)


def export_tflite_drq():
    converter = tf.lite.TFLiteConverter.from_saved_model(EXPORT_DIR)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_model = converter.convert()

    with open(TFLITE_DRQ_PATH, "wb") as f:
        f.write(tflite_model)

    print("DRQ TFLite written:", TFLITE_DRQ_PATH)


def test_tflite(model_path: str):
    import numpy as np

    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    print(f"\n=== TEST {model_path} ===")
    print("=== INPUTS ===")
    for d in interpreter.get_input_details():
        print(d["index"], d["name"], d["shape"], d["dtype"])

    print("=== OUTPUTS ===")
    for d in interpreter.get_output_details():
        print(d["index"], d["name"], d["shape"], d["dtype"])

    ids = np.zeros((1, SEQ_LEN), dtype=np.int32)
    mask = np.zeros((1, SEQ_LEN), dtype=np.int32)
    segs = np.zeros((1, SEQ_LEN), dtype=np.int32)

    sample = [101, 2023, 2003, 1037, 3231, 102]  # [CLS] this is a test [SEP]
    ids[0, :len(sample)] = sample
    mask[0, :len(sample)] = 1

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # 这里假设输入顺序与保存顺序一致；后面会打印出来核对
    interpreter.set_tensor(input_details[0]["index"], ids)
    interpreter.set_tensor(input_details[1]["index"], segs)
    interpreter.set_tensor(input_details[2]["index"], mask)

    interpreter.invoke()
    out = interpreter.get_tensor(output_details[0]["index"])[0]

    print("RAW mean:", out.mean())
    print("RAW min :", out.min())
    print("RAW max :", out.max())
    print("RAW var :", out.var())
    print("RAW norm:", float((out * out).sum() ** 0.5))
    print("RAW head:", out[:12])


if __name__ == "__main__":
    export_savedmodel()
    export_tflite_fp32()
    test_tflite(TFLITE_PATH)

    export_tflite_drq()
    test_tflite(TFLITE_DRQ_PATH)