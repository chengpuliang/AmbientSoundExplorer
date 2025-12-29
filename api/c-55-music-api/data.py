import datetime
from dataclasses import dataclass
from typing import Annotated

from pydantic import BaseModel, Field


@dataclass(frozen=True)
class Music:
    music_id: int
    title: str
    date: datetime.date
    author: str


@dataclass(frozen=True)
class Reminder:
    reminder_id: int
    hour: int
    minute: int
    music_id: int
    enabled: bool


class ReminderPatch(BaseModel):
    hour: Annotated[int, Field(ge=0, le=23)] | None = None
    minute: Annotated[int, Field(ge=0, le=59)] | None = None
    enabled: bool | None = None


MUSIC_LIST: list[Music] = [
    Music(
        music_id=0,
        title="still falling",
        date=datetime.date(2025, 11, 24),
        author="Øraeth",
    ),
    Music(
        music_id=1,
        title="Chances",
        date=datetime.date(2017, 10, 16),
        author="Silent Partner",
    ),
    Music(
        music_id=2,
        title="Take You Home Tonight",
        date=datetime.date(2017, 8, 9),
        author="Vibe Tracks",
    ),
    Music(
        music_id=3,
        title="Moonlight",
        date=datetime.date(2025, 10, 30),
        author="chillity",
    ),
]

REMINDERS_DEFAULT_LIST: list[Reminder] = [
    Reminder(reminder_id=0, hour=12, minute=30, music_id=1, enabled=True),
    Reminder(reminder_id=1, hour=1, minute=58, music_id=3, enabled=False),
    Reminder(reminder_id=2, hour=18, minute=21, music_id=2, enabled=True),
    Reminder(reminder_id=3, hour=9, minute=20, music_id=3, enabled=True),
    Reminder(reminder_id=4, hour=20, minute=5, music_id=0, enabled=False),
]
