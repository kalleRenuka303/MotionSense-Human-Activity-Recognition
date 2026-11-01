import json
from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, DateTime, Text
from backend.app.database.session import Base

class PredictionRecord(Base):
    __tablename__ = "prediction_history"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    filename = Column(String(255), nullable=False)
    input_format = Column(String(50), nullable=False)  # 'features' or 'raw_inertial'
    selected_model = Column(String(50), nullable=False)
    num_windows = Column(Integer, nullable=False)
    avg_confidence = Column(Float, nullable=False)
    activity_distribution_json = Column(Text, nullable=False)
    timeline_summary_json = Column(Text, nullable=False)
    ground_truth_accuracy = Column(Float, nullable=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))

    def to_dict(self):
        return {
            "id": self.id,
            "filename": self.filename,
            "input_format": self.input_format,
            "selected_model": self.selected_model,
            "num_windows": self.num_windows,
            "avg_confidence": round(self.avg_confidence, 4),
            "activity_distribution": json.loads(self.activity_distribution_json),
            "timeline_summary": json.loads(self.timeline_summary_json),
            "ground_truth_accuracy": round(self.ground_truth_accuracy, 4) if self.ground_truth_accuracy is not None else None,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }
