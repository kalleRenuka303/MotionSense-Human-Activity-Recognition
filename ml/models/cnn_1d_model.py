"""
1D Convolutional Neural Network for Pipeline B (9 inertial channels, 128 time steps)
"""

import torch
import torch.nn as nn
from typing import Optional
from ml.configs.config import CNN1DConfig

class MotionSense1DCNN(nn.Module):
    def __init__(self, config: Optional[CNN1DConfig] = None):
        super().__init__()
        self.config = config or CNN1DConfig()

        # Block 1
        self.conv1 = nn.Conv1d(
            in_channels=self.config.in_channels,
            out_channels=self.config.conv1_filters,
            kernel_size=self.config.conv1_kernel,
            padding=self.config.conv1_kernel // 2
        )
        self.bn1 = nn.BatchNorm1d(self.config.conv1_filters)
        self.relu1 = nn.ReLU()
        self.pool1 = nn.MaxPool1d(kernel_size=2)
        self.drop1 = nn.Dropout(p=self.config.dropout * 0.7)

        # Block 2
        self.conv2 = nn.Conv1d(
            in_channels=self.config.conv1_filters,
            out_channels=self.config.conv2_filters,
            kernel_size=self.config.conv2_kernel,
            padding=self.config.conv2_kernel // 2
        )
        self.bn2 = nn.BatchNorm1d(self.config.conv2_filters)
        self.relu2 = nn.ReLU()
        self.pool2 = nn.MaxPool1d(kernel_size=2)
        self.drop2 = nn.Dropout(p=self.config.dropout * 0.85)

        # Block 3
        self.conv3 = nn.Conv1d(
            in_channels=self.config.conv2_filters,
            out_channels=self.config.conv3_filters,
            kernel_size=self.config.conv3_kernel,
            padding=self.config.conv3_kernel // 2
        )
        self.bn3 = nn.BatchNorm1d(self.config.conv3_filters)
        self.relu3 = nn.ReLU()
        self.gap = nn.AdaptiveAvgPool1d(1)
        self.drop3 = nn.Dropout(p=self.config.dropout)

        # Fully Connected Head
        self.fc1 = nn.Linear(self.config.conv3_filters, self.config.fc_dim)
        self.relu_fc = nn.ReLU()
        self.drop_fc = nn.Dropout(p=self.config.dropout)
        self.out_proj = nn.Linear(self.config.fc_dim, self.config.num_classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Input x shape: (batch_size, 128, 9) or (batch_size, 9, 128)
        Output logits shape: (batch_size, 6)
        """
        if x.dim() == 2:
            x = x.unsqueeze(0)  # Add batch dim if single sample

        # If shape is (B, 128, 9), transpose to (B, 9, 128)
        if x.shape[-1] == self.config.in_channels and x.shape[1] != self.config.in_channels:
            x = x.transpose(1, 2)

        out = self.drop1(self.pool1(self.relu1(self.bn1(self.conv1(x)))))
        out = self.drop2(self.pool2(self.relu2(self.bn2(self.conv2(out)))))
        out = self.drop3(self.gap(self.relu3(self.bn3(self.conv3(out)))))
        
        out = torch.flatten(out, 1)
        out = self.drop_fc(self.relu_fc(self.fc1(out)))
        logits = self.out_proj(out)
        return logits

    def count_parameters(self) -> int:
        return sum(p.numel() for p in self.parameters() if p.requires_grad)
