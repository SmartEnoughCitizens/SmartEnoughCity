import logging
import os
from logging.config import dictConfig

import logging_loki


def configure_logging() -> None:
    """
    Configure the root logger with a console handler and a Grafana Loki handler.
    
    If the root logger already has handlers, this function does nothing. Otherwise it sets up a console handler and adds a Loki handler that sends logs to the endpoint specified by the LOKI_URL environment variable (default "http://localhost:3100/loki/api/v1/push") and tags logs with app="data-handler" and pod from POD_NAME (default "local"). Both handlers are configured at the INFO level.
    """
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
