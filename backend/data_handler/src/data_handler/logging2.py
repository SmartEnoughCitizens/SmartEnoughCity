import logging
import os
from logging.config import dictConfig

import logging_loki


def configure_logging() -> None:
    root = logging.getLogger()
    if root.handlers:
        return

    dictConfig(
        {
            "version": 1,
            "disable_existing_loggers": False,
            "formatters": {
                "standard": {
                    "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
                }
            },
            "handlers": {
                "console": {
                    "class": "logging.StreamHandler",
                    "level": "INFO",
                    "formatter": "standard",
                }
            },
            "root": {"level": "INFO", "handlers": ["console"]},
        }
    )

    loki_url = os.getenv("LOKI_URL", "http://localhost:3100/loki/api/v1/push")
    loki_handler = logging_loki.LokiHandler(
        url=loki_url,
        tags={
            "app": "data-handler",
            "pod": os.getenv("POD_NAME", "local"),
        },
        version="1",
    )
    loki_handler.setLevel(logging.INFO)
    root.addHandler(loki_handler)
