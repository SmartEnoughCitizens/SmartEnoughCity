FROM python:3.12-slim

WORKDIR /app

COPY pyproject.toml ./
COPY src/ ./src/

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir fastapi[standard] httpx pydantic uvicorn && \
    pip install apscheduler

EXPOSE 8000

CMD ["fastapi", "run", "src/inference_engine/main.py", "--port", "8000"]
