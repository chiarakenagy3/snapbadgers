import torch
import torch.nn as nn
import qai_hub as hub
import numpy as np

class SongEmbeddingMLP(nn.Module):
    def __init__(self):
        super().__init__()
        self.layer1 = nn.Linear(15, 64)
        self.relu = nn.ReLU()
        self.layer2 = nn.Linear(64, 128)

    def forward(self, x):
        x = self.layer1(x)
        x = self.relu(x)
        x = self.layer2(x)
        return x

model = SongEmbeddingMLP().eval()

client = hub.ClientConfig(api_token="moctw258d0li1cl5bqxarfon3vr7cchmljdg4gzt")
device = hub.Device("Samsung Galaxy S24 (Family)")

input_shape = (1, 15)
compile_job = hub.submit_compile_job(
    model=torch.jit.script(model),
    device=device,
    input_specs=dict(input_0=input_shape),
    options="--target_runtime onnx"
)
onnx_model = compile_job.get_target_model()

num_samples = 500
sample_data = np.random.rand(num_samples, 1, 15).astype(np.float32)
calibration_dataset = {"input_0": [sample_data[i] for i in range(num_samples)]}

quantize_job = hub.submit_quantize_job(
    model=onnx_model,
    calibration_data=calibration_dataset,
    weights_dtype=hub.QuantizeDtype.INT8,
    activations_dtype=hub.QuantizeDtype.INT8,
)
quantized_model = quantize_job.get_target_model()

tflite_compile_job = hub.submit_compile_job(
    model=quantized_model,
    device=device,
    options="--target_runtime tflite --quantize_io"
)
final_model = tflite_compile_job.get_target_model()

final_model.download("mlp_quantized.tflite")
print("下载完成：mlp_quantized.tflite")

