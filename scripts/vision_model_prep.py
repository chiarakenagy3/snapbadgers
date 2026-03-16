import torch
import torch.nn as nn
import torchvision.models as tv_models
import onnx
from qai_hub_models.models.efficientnet_b0.model import EfficientNetB0

class ModifiedEfficientNetB0(nn.Module):
    def __init__(self, base_model, projection_dim=128):
        super().__init__()
        # Extract features and GAP from the original torchvision model
        self.features = base_model.features
        self.avgpool = base_model.avgpool
        # New projection layer: 1280 is for EfficientNet-B0
        self.projection = nn.Linear(1280, projection_dim)

    def forward(self, x):
        x = self.features(x)
        x = self.avgpool(x)
        x = torch.flatten(x, 1)
        x = self.projection(x)
        x = torch.nn.functional.normalize(x, p=2, dim=1)  # L2 Normalization
        return x

def main():
    # 1. Load the pre-optimized base model structure (using weights from QAI Hub logic)
    qai_model = EfficientNetB0.from_pretrained()
    base_net = qai_model.net
    base_net.eval()

    # 2. Modify architecture
    modified_model = ModifiedEfficientNetB0(base_net)
    modified_model.eval()

    # 3. Export to ONNX
    dummy_input = torch.randn(1, 3, 224, 224)
    onnx_file = "efficientnet_b0_128d.onnx"
    
    torch.onnx.export(
        modified_model,
        dummy_input,
        onnx_file,
        input_names=['image_tensor'],
        output_names=['scene_embedding'],
        dynamic_axes={'image_tensor': {0: 'batch_size'}, 'scene_embedding': {0: 'batch_size'}},
        opset_version=14
    )
    
    print(f"Model exported to {onnx_file}")

if __name__ == "__main__":
    main()
