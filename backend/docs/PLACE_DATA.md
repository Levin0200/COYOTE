# 장소 검색 데이터 운영

## 동작 구조

1. 소상공인시장진흥공단 상가(상권)정보 API가 반경 안의 후보를 찾습니다.
2. 검색 결과 중 음식점·카페 후보를 행정안전부 일반음식점·휴게음식점 API로 실시간 확인합니다.
3. 상호와 주소가 일치하는 최신 영업 기록은 유지하고, 폐업 기록만 확인되는 후보는 제외합니다.
4. 볼링장·노래방 등 비음식 업종은 행안부 음식점 API 대상이 아니므로 기존 검색 결과를 사용합니다.
5. 행안부 API 장애 또는 매칭 실패 시 검색 자체를 막지 않고 기존 후보를 유지합니다.

후보 검증은 최대 8개씩 병렬 처리합니다. 같은 검색의 반경 확대나 연속 클릭이 API를 중복 호출하지 않게
결과를 서버 메모리에 60초만 캐시하며, 파일과 DB에는 API 응답을 저장하지 않습니다.

## 사용하는 행안부 API

- `https://apis.data.go.kr/1741000/general_restaurants/info`
- `https://apis.data.go.kr/1741000/rest_cafes/info`

두 API 모두 공공데이터포털 활용신청이 승인된 일반 인증키를 사용합니다.

## 설정

```properties
app.place.mois.enabled=true
app.place.mois.service-key=${MOIS_DATA_GO_KR_SERVICE_KEY:${DATA_GO_KR_SERVICE_KEY:}}
app.place.mois.base-url=https://apis.data.go.kr/1741000
app.place.mois.connect-timeout-millis=2500
app.place.mois.read-timeout-millis=7000
app.place.mois.parallelism=8
app.place.mois.cache-ttl-millis=60000
```

인증키는 환경변수 또는 Git에서 제외된 `application-local.properties`에만 둡니다.
