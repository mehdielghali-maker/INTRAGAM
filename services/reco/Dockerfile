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
# Réseau PyPI parfois instable (coupures DNS / SSL EOF observées) : les --retries internes de pip
# rejouent trop vite pour des coupures TLS, donc on ENVELOPPE pip dans une boucle avec pauses.
# Torch CPU-only d'abord : ultralytics tire torch, dont la build CUDA par défaut télécharge plusieurs
# Go de wheels nvidia_* INUTILES (YOLO-nano tourne sur CPU) → image plus légère + build plus robuste.
RUN for i in 1 2 3 4 5 6; do \
      pip install --no-cache-dir --retries 8 --timeout 180 \
        torch torchvision --index-url https://download.pytorch.org/whl/cpu && break; \
      echo "torch: tentative $i échouée, pause 15s"; sleep 15; \
    done
RUN for i in 1 2 3 4 5 6; do \
      pip install --no-cache-dir --retries 8 --timeout 180 -r requirements.txt && break; \
      echo "requirements: tentative $i échouée, pause 15s"; sleep 15; \
    done; \
    python -c "import fastapi, numpy, cv2, onnxruntime, ultralytics, fast_alpr" \
      || { echo 'ECHEC: dépendances incomplètes après retries'; exit 1; }

COPY app.py attestation_regles.py ./

# Pre-telecharger les poids pour pouvoir demarrer SANS reseau ensuite.
# (Si le build n'a pas internet, commenter ces lignes : les modeles seront
#  telecharges au premier demarrage a la place.)
RUN python -c "from ultralytics import YOLO; YOLO('yolov8n.pt')" || true
RUN python -c "from fast_alpr import ALPR; ALPR(detector_model='yolo-v9-t-384-license-plate-end2end', ocr_model='cct-xs-v2-global-model')" || true
# OCR-DOC : modeles PaddleOCR fr + arabic baked dans l'image (~/.paddleocr) → AUCUN appel
# sortant a l'execution (contrainte : donnees personnelles, OCR strictement local).
RUN python -c "from paddleocr import PaddleOCR; PaddleOCR(use_angle_cls=True, lang='fr', show_log=False); PaddleOCR(use_angle_cls=True, lang='arabic', show_log=False)" || true

EXPOSE 8088
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8088", "--workers", "1"]
