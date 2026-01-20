import os
from functools import lru_cache


@lru_cache(maxsize=1)
def is_dev() -> bool:
    """
    Determines whether the current application environment is set to development.

    Returns:
        bool: `True` if the application is running in development mode, `False` otherwise.
    """
    return os.getenv("APP_ENV") == "dev"
