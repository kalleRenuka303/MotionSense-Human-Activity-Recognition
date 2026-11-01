from typing import List, Optional
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from sqlalchemy import desc

from backend.app.database.session import get_db
from backend.app.database.models import PredictionRecord

router = APIRouter()

@router.get("")
def list_prediction_history(
    limit: int = 50, 
    offset: int = 0,
    db: Session = Depends(get_db)
):
    query = db.query(PredictionRecord).order_by(desc(PredictionRecord.created_at))
    total_count = query.count()
    records = query.offset(offset).limit(limit).all()
    
    return {
        "total": total_count,
        "items": [r.to_dict() for r in records]
    }

@router.get("/{record_id}")
def get_prediction_record(record_id: int, db: Session = Depends(get_db)):
    record = db.query(PredictionRecord).filter(PredictionRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail=f"Record {record_id} not found.")
    return record.to_dict()

@router.delete("/{record_id}")
def delete_prediction_record(record_id: int, db: Session = Depends(get_db)):
    record = db.query(PredictionRecord).filter(PredictionRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail=f"Record {record_id} not found.")
    db.delete(record)
    db.commit()
    return {"status": "deleted", "id": record_id}
