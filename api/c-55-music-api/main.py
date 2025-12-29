import copy
import dataclasses

from fastapi import FastAPI, Depends, HTTPException
from fastapi.responses import FileResponse, RedirectResponse

import data
import key
import reminders
from data import Music, Reminder
from sort_order import SortOrder

app = FastAPI(title="第 55 屆分區技能競賽 - Android 程式設計", redoc_url=None)


@app.get("/", include_in_schema=False)
def root():
    return RedirectResponse(url="/docs")


@app.get("/music/list", name="取得音樂列表", tags=["音樂"])
def music_list(
    sort_order: SortOrder,
    filter_term: str | None = None,
    api_key: str = Depends(key.API_KEY_HEADER),
) -> list[Music]:
    key.validate(api_key)
    result_list = data.MUSIC_LIST
    if filter_term:
        result_list = [m for m in result_list if filter_term.lower() in m.title.lower()]
    return sorted(
        result_list, key=lambda m: m.date, reverse=sort_order == SortOrder.descending
    )


@app.get(
    "/music/picture",
    name="取得音樂圖片",
    response_class=FileResponse,
    responses={200: {"content": {"image/jpeg": {}}}},
    tags=["音樂"],
)
def music_picture(
    music_id: int, api_key: str = Depends(key.API_KEY_HEADER)
) -> FileResponse:
    key.validate(api_key)
    if not any(music_id == item.music_id for item in data.MUSIC_LIST):
        raise HTTPException(404, "Invalid music_id")
    return FileResponse(f"data/picture/{music_id}.jpg", media_type="image/jpeg")


@app.get(
    "/music/audio",
    name="取得音樂",
    response_class=FileResponse,
    responses={200: {"content": {"audio/mpeg": {}}}},
    tags=["音樂"],
)
def music_audio(
    music_id: int, api_key: str = Depends(key.API_KEY_HEADER)
) -> FileResponse:
    key.validate(api_key)
    if not any(music_id == item.music_id for item in data.MUSIC_LIST):
        raise HTTPException(404, "Invalid music_id")
    return FileResponse(f"data/music/{music_id}.mp3", media_type="audio/mpeg")


@app.get("/reminders/list", name="取得提醒列表", tags=["提醒"])
def reminders_list(
    api_key: str = Depends(key.API_KEY_HEADER), music_id: int | None = None
) -> list[Reminder]:
    key.validate(api_key)
    return [
        r
        for r in reminders.reminders[api_key]
        if (music_id is None) or (r.music_id == music_id)
    ]


@app.patch("/reminders/{reminder_id}", name="更新提醒", tags=["提醒"])
def reminders_patch(
    reminder_id: int,
    reminder_patch: data.ReminderPatch,
    api_key: str = Depends(key.API_KEY_HEADER),
) -> Reminder:
    key.validate(api_key)
    reminder = reminders.get_reminder(api_key, reminder_id)
    return reminders.update_reminder(
        api_key,
        dataclasses.replace(reminder, **reminder_patch.model_dump(exclude_unset=True)),
    )


@app.delete("/reminders", name="重設所有提醒狀態", tags=["提醒"], status_code=204)
def reminders_reset(api_key: str = Depends(key.API_KEY_HEADER)):
    key.validate(api_key)
    reminders.reminders[api_key] = copy.deepcopy(data.REMINDERS_DEFAULT_LIST)


@app.delete("/reminders/reset-all", include_in_schema=False, status_code=204)
def reminders_reset_all(password: str):
    if password != key.MASTER_RESET_PASSWORD:
        raise HTTPException(401)
    reminders.reminders = {
        user: copy.deepcopy(data.REMINDERS_DEFAULT_LIST) for user in key.USER_KEYS
    }
