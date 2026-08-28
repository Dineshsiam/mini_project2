import json
import os


def save_json(filename, data):

    os.makedirs("data/schemes", exist_ok=True)

    with open(
        f"data/schemes/{filename}.json",
        "w",
        encoding="utf-8"
    ) as f:

        json.dump(
            data,
            f,
            indent=4,
            ensure_ascii=False
        )