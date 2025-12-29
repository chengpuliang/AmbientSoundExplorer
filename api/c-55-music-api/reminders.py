import copy

from fastapi import HTTPException

import data
import key
from data import Reminder

reminders: dict[str, list[Reminder]] = {
    user: copy.deepcopy(data.REMINDERS_DEFAULT_LIST) for user in key.USER_KEYS
}


def get_reminder(api_key: str, reminder_id: int) -> Reminder:
    for reminder in reminders[api_key]:
        if reminder.reminder_id == reminder_id:
            return reminder
    raise HTTPException(404)


def update_reminder(api_key: str, new_reminder: Reminder) -> Reminder:
    for index, reminder in enumerate(reminders[api_key]):
        if reminder.reminder_id == new_reminder.reminder_id:
            reminders[api_key][index] = new_reminder
            return new_reminder
    raise HTTPException(404)
