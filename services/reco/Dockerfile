# Service RECO — image Docker autonome
FROM python:3.11-slim

ENV PIP_NO_CACHE_DIR=1 \
    PYTHONUNBUFFERED=1 \
    # Cache des modeles (persiste si on monte un volume sur /models)
    ULTRALYTICS_DIR=/models \
    FAST_ALPR_HOME=/models/fast-alpr

# Dependances systeme minimales (OpenCV headless + libs ONNX)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 libgl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY app.py .

# Pre-telecharger les poids pour pouvoir demarrer SANS reseau ensuite.
# (Si le build n'a pas internet, commenter ces lignes : les modeles seront
#  telecharges au premier demarrage a la place.)
RUN python -c "from ultralytics import YOLO; YOLO('yolov8n.pt')" || true
RUN python -c "from fast_alpr import ALPR; ALPR(detector_model='yolo-v9-t-384-license-plate-end2end', ocr_model='cct-xs-v2-global-model')" || true

EXPOSE 8088
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8088", "--workers", "1"]
