"""
CNN-LSTM (Convolutional Neural Network combined with Long Short-Term Memory)
Primary Proposed Research Model for Pipeline B (9 inertial channels, 128 time steps)
Combines spatial convolutional filters for local movement feature extraction
with recurrent LSTM cells for modeling temporal transition dynamics.
"""

import torch
import torch.nn as nn
from typing import Optional
from ml.configs.config import CNNLSTMConfig

class MotionSenseCNNLSTM(nn.Module):
    def __init__(self, config: Optional[CNNLSTMConfig] = None):
        super().__init__()
        self.config = config or CNNLSTMConfig()

        # 1. Convolutional Spatial Feature Extraction
        self.conv1 = nn.Conv1d(
            in_channels=self.config.in_channels,
            out_channels=self.config.conv_filters,
            kernel_size=self.config.conv_kernel,
            padding=self.config.conv_kernel // 2
        )
        self.bn1 = nn.BatchNorm1d(self.config.conv_filters)
        self.relu1 = nn.ReLU()
        self.drop1 = nn.Dropout(p=self.config.dropout * 0.6)

        self.conv2 = nn.Conv1d(
            in_channels=self.config.conv_filters,
            out_channels=self.config.conv_filters,
            kernel_size=self.config.conv_kernel,
            padding=self.config.conv_kernel // 2
        )
        self.bn2 = nn.BatchNorm1d(self.config.conv_filters)
        self.relu2 = nn.ReLU()
        # Pool by 2 reduces temporal resolution from 128 to 64 steps
        self.pool = nn.MaxPool1d(kernel_size=2)
        self.drop2 = nn.Dropout(p=self.config.dropout * 0.7)

        # 2. Recurrent Temporal Modeling (LSTM)
        # Input to LSTM: (batch_size, seq_len=64, input_size=conv_filters)
        self.lstm = nn.LSTM(
            input_size=self.config.conv_filters,
            hidden_size=self.config.lstm_hidden_dim,
            num_layers=self.config.lstm_num_layers,
            batch_first=True,
            dropout=self.config.dropout if self.config.lstm_num_layers > 1 else 0.0
        )

        # 3. Dense Classification Head
        self.fc1 = nn.Linear(self.config.lstm_hidden_dim, self.config.fc_dim)
        self.relu_fc = nn.ReLU()
        self.drop_fc = nn.Dropout(p=self.config.dropout)
        self.out_proj = nn.Linear(self.config.fc_dim, self.config.num_classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Input x: (batch_size, 128, 9) or (batch_size, 9, 128)
        Output: (batch_size, 6) logits
        """
        if x.dim() == 2:
            x = x.unsqueeze(0)

        # Permute to (B, C, T) for 1D convolution
        if x.shape[-1] == self.config.in_channels and x.shape[1] != self.config.in_channels:
            x = x.transpose(1, 2)

        # CNN Feature Extraction
        c = self.drop1(self.relu1(self.bn1(self.conv1(x))))
        c = self.drop2(self.pool(self.relu2(self.bn2(self.conv2(c)))))
        # c shape: (B, conv_filters, 64)

        # Permute to (B, seq_len=64, conv_filters) for LSTM
        lstm_in = c.transpose(1, 2)

        # LSTM Sequence Processing
        lstm_out, (hn, cn) = self.lstm(lstm_in)
        # hn[-1] is the hidden state from the last LSTM layer: shape (B, lstm_hidden_dim)
        last_hidden = hn[-1]

        # Classification Head
        out = self.drop_fc(self.relu_fc(self.fc1(last_hidden)))
        logits = self.out_proj(out)
        return logits

    def count_parameters(self) -> int:
        return sum(p.numel() for p in self.parameters() if p.requires_grad)
