import os
from functools import lru_cache
from typing import Literal

type AppMode = Literal["prod", "dev", "test"]


@lru_cache(maxsize=1)
def get_app_mode() -> AppMode:
    """
    Determines the current application mode.

    Returns:
        AppMode: The current application mode.
    """
    app_mode = os.getenv("APP_ENV")
    if app_mode is None:
        return "prod"
    if app_mode not in ["prod", "dev", "test"]:
        msg = f"Invalid app mode: {app_mode}"
        raise ValueError(msg)
    return app_mode
