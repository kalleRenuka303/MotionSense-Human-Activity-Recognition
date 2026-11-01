import torch
import numpy as np
import pytest
from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM
from ml.models.random_forest_model import MotionSenseRandomForest

def test_1d_cnn_shape():
    model = MotionSense1DCNN()
    # Input batch: 4 samples, 128 timesteps, 9 channels
    x = torch.randn(4, 128, 9)
    logits = model(x)
    assert logits.shape == (4, 6), f"Expected shape (4, 6), got {logits.shape}"

def test_cnn_lstm_shape():
    model = MotionSenseCNNLSTM()
    x = torch.randn(4, 128, 9)
    logits = model(x)
    assert logits.shape == (4, 6), f"Expected shape (4, 6), got {logits.shape}"

def test_rf_initialization():
    rf = MotionSenseRandomForest()
    assert not rf.is_fitted
