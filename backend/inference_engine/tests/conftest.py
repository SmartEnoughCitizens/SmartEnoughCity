import os

# Set APP_ENV before any test module is imported so that get_api_settings()
# (called at module level in main.py) loads from .env.development instead of
# requiring HERMES_URL to be present as a bare environment variable.
os.environ.setdefault("APP_ENV", "dev")
