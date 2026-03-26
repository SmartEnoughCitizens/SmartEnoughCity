import logging
from logging.config import dictConfig


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
