import requests
from config import *

session = requests.Session()
session.headers.update(HEADERS)


def get(url, params=None):

    r = session.get(url, params=params)

    if r.status_code != 200:
        return None

    return r.json()