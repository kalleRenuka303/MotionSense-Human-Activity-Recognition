# MotionSense Deployment Guide

## 1. Frontend Deployment (Vercel)
The React + Vite + Tailwind CSS frontend is configured for deployment on Vercel.

### Configuration (`vercel.json`)
```json
{
  "buildCommand": "cd frontend && npm run build",
  "outputDirectory": "frontend/dist",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### Environment Variables
Configure in Vercel Project Settings:
- `VITE_API_URL`: URL of the deployed FastAPI backend (e.g., `https://api.motionsense.yourdomain.com`).

---

## 2. Backend Deployment (Render / Railway / GCP Cloud Run)
The Python FastAPI backend serves inference using lightweight saved checkpoints (< 11 MB total).

### Start Command
```bash
uvicorn backend.app.main:app --host 0.0.0.0 --port $PORT --workers 2
```

### Environment Variables (`backend/.env`)
```bash
PROJECT_NAME="MotionSense"
API_V1_STR="/api"
BACKEND_CORS_ORIGINS=["https://motionsense.vercel.app","http://localhost:5173"]
DATABASE_URL="sqlite:///./motionsense.db"
```

### Dockerfile (Optional for Containerized Python Hosting)
```dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY backend/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
CMD ["uvicorn", "backend.app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```
