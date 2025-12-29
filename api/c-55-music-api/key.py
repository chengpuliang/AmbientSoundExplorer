from fastapi import HTTPException
from fastapi.security import APIKeyHeader

USER_KEYS = ["KEY1", "KEY2", "KEY3", "KEY4"]

MASTER_RESET_PASSWORD = "MASTER-KEY"

KEY_HEADER_NAME = "X-API-KEY"
API_KEY_HEADER = APIKeyHeader(name=KEY_HEADER_NAME)


def validate(key: str) -> None:
    if key not in USER_KEYS:
        raise HTTPException(401, "Invalid API key")
