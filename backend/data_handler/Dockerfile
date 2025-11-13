# -------- Builder --------
FROM python:3.12-slim AS builder
ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app
COPY pyproject.toml ./

RUN python -m pip install --upgrade pip wheel

COPY src/ ./src/

RUN python -m pip wheel --no-deps --wheel-dir /wheels .

# -------- Runtime --------
FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN useradd -u 10001 -m appuser
WORKDIR /app

COPY --from=builder /wheels /wheels
RUN python -m pip install --no-cache-dir --find-links=/wheels data-handler

USER 10001:10001

ENTRYPOINT ["data-handler"]
