#!/usr/bin/env python3
"""장소 기능 점검 스크립트(백엔드 전체 흐름을 API로 한 번 돌려 본다).

프론트 화면과 연결하기 전에, 서버가 장소 검색·추천·구조화 저장을 제대로 하는지 눈으로 확인하는 용도다.
추가 설치 없이 파이썬(3.9 이상)만 있으면 된다.

사용법(백엔드 서버를 먼저 실행해 둔다):
    python check_place_flow.py                       # 기본: http://localhost:8080
    python check_place_flow.py --base http://localhost:8090
    python check_place_flow.py --keep                # 끝나도 테스트 계정·모임을 지우지 않는다

진행 순서:
    1. 서버 상태 → 2. 테스트 계정 가입(인증번호는 서버 콘솔에 출력되는 6자리를 입력)
    3. 장소 검색 → 4. 장소 추천(AI) → 5. 추천 모임 생성·게스트 선택·마감·결과(+ 동률이면 호스트가 후보 중 확정)
    6. 건너뛰기 모임에서 게스트가 검색해 제안·마감·결과(+ 제안이 흩어지면 후보 3곳 중 호스트가 확정) → 7. 테스트 계정 삭제

필요한 환경: 서버에 공공데이터 키(DATA_GO_KR_SERVICE_KEY)가 있어야 장소 검색·추천이 실제로 동작한다.
OpenRouter 키가 없으면 추천은 AI 없이 규칙(가까운 순서)으로 고르고 "AI 사용: 아니오"로 표시된다.
"""
import argparse
import datetime as dt
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

# 한국어 출력이 깨지지 않게 한다(Windows 콘솔).
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

GANGNAM = (37.4979, 127.0276)  # 강남역 근처
KST = dt.timezone(dt.timedelta(hours=9))

results = []


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None, headers=None):
        h = {"Content-Type": "application/json"}
        if token:
            h["Authorization"] = "Bearer " + token
        h.update(headers or {})
        data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
        request = urllib.request.Request(self.base + path, data=data, method=method, headers=h)
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                raw, status = response.read(), response.status
        except urllib.error.HTTPError as error:
            raw, status = error.read(), error.code
        except urllib.error.URLError as error:
            raise SystemExit(f"\n서버에 연결할 수 없습니다({self.base}). 백엔드를 먼저 실행했는지 확인하세요: {error.reason}")
        try:
            return status, (json.loads(raw) if raw else None)
        except ValueError:
            return status, None


def check(name, ok, detail=""):
    results.append(bool(ok))
    print(f"  [{'통과' if ok else '실패'}] {name}" + (f"  → {detail}" if detail else ""))
    return ok


def title(text):
    print(f"\n=== {text} ===")


def query(path, **params):
    return path + "?" + urllib.parse.urlencode(params)


def day(offset):
    return (dt.datetime.now(KST).date() + dt.timedelta(days=offset)).isoformat()


def response_deadline():
    t = dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=2)
    t = t.replace(second=0, microsecond=0, minute=(t.minute // 10) * 10) + dt.timedelta(minutes=10)
    return t.strftime("%Y-%m-%dT%H:%M:%SZ")


def read_code_from_log(path, email, timeout=10):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            text = open(path, encoding="utf-8", errors="replace").read()
            found = re.findall(re.escape(email) + r": (\d{6})", text)
            if found:
                return found[-1]
        except OSError:
            pass
        time.sleep(0.5)
    return None


def signup_and_login(api, log_path):
    email = "check-%d@example.com" % int(time.time())
    password = "Check-" + uuid.uuid4().hex[:12]
    status, _ = api.call("POST", "/api/auth/email-verification", {"email": email})
    if not check("인증번호 발송", status == 204, f"HTTP {status}"):
        return None
    code = read_code_from_log(log_path, email) if log_path else None
    if code is None:
        print(f"  서버 콘솔에서 '{email}' 줄을 찾아 인증번호 6자리를 입력하세요.")
        code = input("  인증번호: ").strip()
    status, _ = api.call("POST", "/api/auth/email-verification/confirm", {"email": email, "code": code})
    if not check("인증번호 확인", status == 204, f"HTTP {status}"):
        return None
    status, _ = api.call("POST", "/api/auth/signup", {"email": email, "name": "점검계정", "password": password,
                                                       "serviceAgree": True, "privacyAgree": True})
    check("회원가입", status == 201, f"HTTP {status}")
    status, tokens = api.call("POST", "/api/auth/login", {"email": email, "password": password})
    if not check("로그인", status == 200 and tokens, f"HTTP {status}"):
        return None
    return {"email": email, "password": password, "token": tokens["accessToken"]}


def create_meeting(api, token, title_text, policy, place_mode, place_id=None):
    body = {"title": title_text, "description": "장소 기능 점검용 모임입니다.", "responseDeadline": response_deadline(),
            "linkExpirationDate": None, "candidateStartDate": day(1), "candidateEndDate": day(10),
            "placePolicy": policy, "selectedDates": [day(3)], "placeMode": place_mode}
    if place_id:
        body["placeId"] = place_id
    return api.call("POST", "/api/meetings", body, token)


def wait_result(api, meeting_id, token, seconds=40):
    """마감 직후 결과를 읽는다. 장소 후보를 만드는 중(PENDING)이면 끝날 때까지 기다린다."""
    deadline = time.time() + seconds
    while True:
        status, result = api.call("GET", f"/api/meetings/{meeting_id}/result", token=token)
        if status != 200 or not result or result.get("placeStatus") != "PENDING" or time.time() > deadline:
            return status, result
        time.sleep(1.5)


def show_places(places, limit=6):
    for p in places[:limit]:
        reason = f"  ({p['reason']})" if p.get("reason") else ""
        print(f"        {p['distanceMeters']:>5}m  {p['name'][:24]:<24} {p['category'].split(' > ')[-1][:14]:<14}{reason}")


def main():
    parser = argparse.ArgumentParser(description="장소 기능 점검 스크립트")
    parser.add_argument("--base", default="http://localhost:8080", help="백엔드 주소(기본 http://localhost:8080)")
    parser.add_argument("--keep", action="store_true", help="끝나도 테스트 계정과 모임을 지우지 않는다")
    parser.add_argument("--log", help="서버 로그 파일 경로(있으면 인증번호를 자동으로 읽는다)")
    args = parser.parse_args()
    api = Api(args.base)

    title("1. 서버 상태")
    status, health = api.call("GET", "/api/health")
    check("서버 응답", status == 200 and health and health.get("status") == "UP", f"HTTP {status}")

    title("2. 테스트 계정")
    account = signup_and_login(api, args.log)
    if not account:
        return finish()
    token = account["token"]

    title("3. 장소 검색 (강남역 근처 카페, 반경 1km)")
    status, found = api.call("GET", query("/api/places/search", latitude=GANGNAM[0], longitude=GANGNAM[1],
                                          radiusMeters=1000, keyword="조용한 카페", limit=5), token=token)
    if status == 503:
        check("장소 검색", False, "503: 서버에 공공데이터 키(DATA_GO_KR_SERVICE_KEY)가 없습니다")
        return cleanup(api, account, args.keep)
    ok = check("장소 검색", status == 200 and found and len(found["places"]) > 0, f"HTTP {status}")
    if ok:
        print(f"        알아들은 업종: {found['appliedCategories']} / 무시한 단어: {found['ignoredKeywords']} / 조회 반경: {found['searchedRadiusMeters']}m")
        show_places(found["places"], 5)
        check("거리순 정렬", [p["distanceMeters"] for p in found["places"]] == sorted(p["distanceMeters"] for p in found["places"]))

    title("4. 장소 추천 (후보 20곳 중 6곳 선정)")
    status, recommended = api.call("POST", "/api/places/recommend", {
        "latitude": GANGNAM[0], "longitude": GANGNAM[1], "radiusMeters": 1000, "keyword": "고기",
        "title": "팀 회식", "description": "다음 주 금요일 저녁에 팀원들과 회식합니다. 고기 먹고 싶어요.", "participantCount": 6},
        token)
    ok = check("장소 추천", status == 200 and recommended and len(recommended["places"]) > 0, f"HTTP {status}")
    if not ok:
        return cleanup(api, account, args.keep)
    print(f"        AI 사용: {'예' if recommended['aiUsed'] else '아니오(규칙 기반)'} / 첫 화면 {recommended['pageSize']}곳 + 새로고침 {max(0, len(recommended['places']) - recommended['pageSize'])}곳")
    show_places(recommended["places"], 6)
    ids = [p["id"] for p in recommended["places"]]
    check("추천 장소가 중복 없이 나옴", len(ids) == len(set(ids)))

    title("5. 추천 모임: 장소 ID로 후보 확정 → 게스트 선택 → 마감 → 결과")
    options = [{"name": p["name"], "distance": f"{p['distanceMeters']}m", "placeId": p["id"]} for p in recommended["places"][:3]]
    policy = {"mode": "RECOMMEND", "region": "강남역", "radius": 1, "keyword": "고기", "options": options}
    status, meeting = create_meeting(api, token, "점검: 추천 모임", policy, "RECOMMEND", ids[0])
    if not check("모임 생성", status == 201 and meeting, f"HTTP {status} {meeting if status != 201 else ''}"):
        return cleanup(api, account, args.keep)
    first = meeting["placePolicy"]["options"][0]
    check("후보에 공식 주소·좌표·업종 저장", bool(first.get("placeId") and first.get("address") and first.get("latitude") and first.get("category")),
          f"{first['name']} / {first['address']}")
    check("호스트 응답에 선택한 장소의 좌표", bool(meeting["myResponse"].get("place") and meeting["myResponse"]["place"].get("latitude")))
    token_invite, meeting_id = meeting["inviteToken"], meeting["meetingId"]
    for index, pick in ((0, 1), (1, 0)):
        guest = str(uuid.uuid4())
        api.call("POST", f"/api/invites/{token_invite}/join", {"nickname": f"게스트{index + 1}"}, headers={"X-Guest-Id": guest})
        status, res = api.call("PUT", f"/api/invites/{token_invite}/response", {
            "nickname": f"게스트{index + 1}", "selectedDates": [day(3)], "placeMode": "RECOMMEND", "placeId": ids[pick]},
            headers={"X-Guest-Id": guest})
        check(f"게스트{index + 1}이(가) 장소 ID로 선택", status in (200, 201) and res and res.get("place"), f"HTTP {status}")
    status, fake = api.call("PUT", f"/api/invites/{token_invite}/response", {
        "nickname": "가짜", "selectedDates": [day(3)], "placeMode": "RECOMMEND", "placeId": "NOT_A_REAL_ID"},
        headers={"X-Guest-Id": str(uuid.uuid4())})
    check("존재하지 않는 장소 ID는 거부", status == 400 and fake and fake.get("code") == "PLACE_NOT_FOUND", f"HTTP {status} {fake and fake.get('code')}")
    api.call("POST", f"/api/meetings/{meeting_id}/close", token=token)
    status, result = wait_result(api, meeting_id, token)
    if check("마감 후 결과 조회", status == 200 and result, f"HTTP {status}"):
        details = result.get("selectedPlaceDetails") or []
        print(f"        장소 상태: {result['placeStatus']} / 결정 방식: {result['decisionType']} / 선정 장소: {result['selectedPlace']}")
        for d in details:
            print(f"        - {d['name']} | {d.get('address')} | ({d.get('latitude')}, {d.get('longitude')})")
        check("단독 1위 장소가 바로 확정(DECIDED)", result["placeStatus"] == "DECIDED")
        check("결과에 장소 상세(좌표) 포함", bool(details) and all(d.get("latitude") for d in details))

    title("5-2. 추천 모임 동률: 호스트가 후보 중에서 장소 확정")
    options_tie = options[:2]
    policy_tie = {"mode": "RECOMMEND", "region": "강남역", "radius": 1, "keyword": "고기", "options": options_tie}
    status, tie = create_meeting(api, token, "점검: 동률 모임", policy_tie, "RECOMMEND", ids[0])
    if check("모임 생성", status == 201 and tie, f"HTTP {status}"):
        guest = str(uuid.uuid4())
        api.call("PUT", f"/api/invites/{tie['inviteToken']}/response", {
            "nickname": "동률게스트", "selectedDates": [day(3)], "placeMode": "RECOMMEND", "placeId": ids[1]},
            headers={"X-Guest-Id": guest})
        api.call("POST", f"/api/meetings/{tie['meetingId']}/close", token=token)
        status, result = wait_result(api, tie["meetingId"], token)
        check("공동 1위라 호스트 선택 대기(AWAITING_HOST)", status == 200 and result and result["placeStatus"] == "AWAITING_HOST"
              and len(result["placeCandidates"]) == 2 and result.get("hostDecisionDeadline"), f"HTTP {status}")
        status, guest_view = api.call("GET", f"/api/invites/{tie['inviteToken']}/result")
        check("게스트에게는 후보 목록을 숨김", status == 200 and guest_view and guest_view["placeCandidates"] == [])
        status, chosen = api.call("POST", f"/api/meetings/{tie['meetingId']}/result/place", {"placeId": ids[1]}, token)
        check("호스트가 후보를 선택해 확정", status == 200 and chosen and chosen["placeStatus"] == "DECIDED"
              and chosen["decisionType"] == "HOST_CHOICE" and chosen["selectedPlaceDetails"][0]["id"] == ids[1], f"HTTP {status}")
        status, again = api.call("POST", f"/api/meetings/{tie['meetingId']}/result/place", {"placeId": ids[0]}, token)
        check("이미 확정된 뒤에는 다시 고를 수 없음", status == 409 and again and again.get("code") == "PLACE_NOT_AWAITING", f"HTTP {status}")

    title("6. 건너뛰기 모임: 게스트가 검색해서 장소 제안 → 마감 → 결과")
    status, open_meeting = create_meeting(api, token, "점검: 건너뛰기 모임", {"mode": "OPEN"}, "OPEN")
    if check("모임 생성", status == 201 and open_meeting, f"HTTP {status}"):
        invite, open_id = open_meeting["inviteToken"], open_meeting["meetingId"]
        proposed = []
        for index in range(3):
            guest = str(uuid.uuid4())
            api.call("POST", f"/api/invites/{invite}/join", {"nickname": f"제안자{index + 1}"}, headers={"X-Guest-Id": guest})
            status, search = api.call("GET", query(f"/api/invites/{invite}/places/search", latitude=GANGNAM[0], longitude=GANGNAM[1],
                                                   radiusMeters=800, keyword="카페", limit=3), headers={"X-Guest-Id": guest})
            if not check(f"제안자{index + 1}이(가) 장소 검색", status == 200 and search and search["places"], f"HTTP {status}"):
                continue
            place = search["places"][min(index, len(search["places"]) - 1)]
            status, res = api.call("PUT", f"/api/invites/{invite}/response", {
                "nickname": f"제안자{index + 1}", "selectedDates": [day(3)], "placeMode": "PROPOSE", "placeId": place["id"]},
                headers={"X-Guest-Id": guest})
            check(f"제안자{index + 1}이(가) 장소 ID로 제안", status in (200, 201) and res and res.get("place"), place["name"])
            proposed.append(place["id"])
        api.call("POST", f"/api/meetings/{open_id}/close", token=token)
        status, result = wait_result(api, open_id, token)
        if check("마감 후 결과 조회", status == 200 and result, f"HTTP {status}"):
            print(f"        장소 상태: {result['placeStatus']} / 결정 방식: {result['decisionType']} / 선정 장소: {result['selectedPlace']}")
            if result["placeStatus"] == "AWAITING_HOST":
                cands = result["placeCandidates"]
                check("제안이 흩어져 합의 지역의 후보 3곳이 만들어짐", 1 <= len(cands) <= 3 and all(c.get("latitude") for c in cands), f"{len(cands)}곳")
                for c in cands:
                    reason = f"  ({c['reason']})" if c.get("reason") else ""
                    print(f"        - {c['name'][:24]} | {c.get('address')}{reason}")
                status, chosen = api.call("POST", f"/api/meetings/{open_id}/result/place", {"placeId": cands[0]["id"]}, token)
                check("호스트가 후보를 선택해 확정", status == 200 and chosen and chosen["placeStatus"] == "DECIDED", f"HTTP {status}")
            else:
                check("단독 1위 제안이 바로 확정(DECIDED)", result["placeStatus"] == "DECIDED")
                check("제안된 장소의 상세가 결과에 포함", all(d.get("latitude") for d in result.get("selectedPlaceDetails", [])))

    return cleanup(api, account, args.keep)


def cleanup(api, account, keep):
    title("7. 정리")
    if keep:
        print(f"  --keep: 테스트 계정 {account['email']} 과 모임을 그대로 둡니다.")
    else:
        status, _ = api.call("DELETE", "/api/users/me", {"password": account["password"]}, account["token"])
        check("테스트 계정과 모임 삭제", status == 204, f"HTTP {status}")
    return finish()


def finish():
    passed, total = sum(results), len(results)
    print(f"\n결과: {passed}/{total} 통과" + ("" if passed == total else "  ← 실패한 항목을 위에서 확인하세요"))
    return 0 if passed == total else 1


if __name__ == "__main__":
    sys.exit(main())
