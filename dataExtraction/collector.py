from api import *
from utils import save_json

import time


def get_slugs():

    slugs = []

    start = 0
    size = 10

    while True:

        params = {
            "lang": "en",
            "q": '[{"identifier":"schemeCategory","value":"Agriculture,Rural & Environment"}]',
            "keyword": "",
            "sort": "",
            "from": start,
            "size": size
        }

        data = get(SEARCH_API, params)

        if data is None:
            break

        if data.get("statusCode") != 200:
            break

        hits = data.get("data", {}).get("hits", {})
        items = hits.get("items", [])

        print("Schemes Found:", len(items))

        if not items:
            break

        for item in items:
            slug = item.get("fields", {}).get("slug")
            if slug:
                print(slug)
                slugs.append(slug)

        page = hits.get("page", {})
        total = page.get("total", 0)

        start += size

        if start >= total:
            break

    return slugs

def get_scheme(slug):

    params={

        "slug":slug,

        "lang":"en"

    }

    detail=get(DETAIL_API,params)

    if detail is None:
        return None

    if detail["statusCode"]!=200:
        return None

    return detail["data"]


def get_documents(id):

    url=f"{DETAIL_API}/{id}/documents"

    return get(url,{"lang":"en"})


def get_faq(id):

    url=f"{DETAIL_API}/{id}/faqs"

    return get(url,{"lang":"en"})

def get_application(id):

    url=f"{DETAIL_API}/{id}/applicationchannel"

    return get(url)

def get_news(id):

    url=f"{DETAIL_API}/{id}/news"

    return get(url,{"lang":"en"})

def collect_scheme(slug):

    detail=get_scheme(slug)

    if detail is None:

        return False

    sid=detail["_id"]

    document=get_documents(sid)

    faq=get_faq(sid)

    application=get_application(sid)

    news=get_news(sid)

    result={

        "slug":slug,

        "scheme":detail,

        "documents":document,

        "faq":faq,

        "application":application,

        "news":news

    }

    save_json(slug,result)

    print("Saved :",slug)

    return True


def collect_all():

    slugs=get_slugs()

    print()

    print("Total Schemes :",len(slugs))

    print()

    success=[]

    failed=[]

    for slug in slugs:

        try:

            ok=collect_scheme(slug)

            if ok:
                success.append(slug)
            else:
                failed.append(slug)

        except Exception:

            failed.append(slug)

        time.sleep(0.5)

    save_json("index",success)

    save_json("failed",failed)

    print()

    print("Completed")