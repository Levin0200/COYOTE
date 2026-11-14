// 카카오맵 JavaScript SDK 로더. 키는 .env.local 의 VITE_KAKAO_MAP_KEY 에서 읽는다.
// 카카오 SDK는 지도 표시와 위치(핀) 선택, 지역 검색으로 지도 이동에만 쓴다. 카카오 검색 결과는 서버에 보내거나 저장하지 않는다.

const KAKAO_KEY = (import.meta.env.VITE_KAKAO_MAP_KEY || "").trim();
const LOAD_TIMEOUT_MS = 10000;

let loadingPromise = null;

export const hasKakaoKey = () => Boolean(KAKAO_KEY);

export const KAKAO_ERRORS = {
  NO_KEY: "NO_KEY",
  LOAD_FAILED: "LOAD_FAILED",
};

export function loadKakaoMaps() {
  if (!hasKakaoKey()) return Promise.reject(new Error(KAKAO_ERRORS.NO_KEY));
  if (window.kakao?.maps?.services) return Promise.resolve(window.kakao);
  if (loadingPromise) return loadingPromise;

  loadingPromise = new Promise((resolve, reject) => {
    const fail = () => {
      loadingPromise = null;
      reject(new Error(KAKAO_ERRORS.LOAD_FAILED));
    };
    const timer = window.setTimeout(fail, LOAD_TIMEOUT_MS);
    const script = document.createElement("script");
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(KAKAO_KEY)}&libraries=services&autoload=false`;
    script.async = true;
    script.onload = () => {
      if (!window.kakao?.maps) return fail();
      window.kakao.maps.load(() => {
        window.clearTimeout(timer);
        resolve(window.kakao);
      });
    };
    script.onerror = () => {
      window.clearTimeout(timer);
      fail();
    };
    document.head.appendChild(script);
  });
  return loadingPromise;
}

// 프로필에 저장된 지역명/주소를 지도 좌표로 바꾼다.
// 상세 주소뿐 아니라 "영종도", "강남역" 같은 지역·장소 이름도 받을 수 있도록
// 키워드 검색을 먼저 시도하고, 실패하면 주소 검색으로 보완한다.
export async function geocodeKakaoLocation(query) {
  const text = String(query || "").trim();
  if (!text) return null;

  const kakao = await loadKakaoMaps();
  const fromResult = (item) => ({
    lat: Number(item.y),
    lng: Number(item.x),
    label: item.place_name || item.address_name || text,
  });

  return new Promise((resolve) => {
    new kakao.maps.services.Places().keywordSearch(text, (places, status) => {
      if (status === kakao.maps.services.Status.OK && places.length > 0) {
        resolve(fromResult(places[0]));
        return;
      }

      new kakao.maps.services.Geocoder().addressSearch(text, (addresses, addressStatus) => {
        if (addressStatus === kakao.maps.services.Status.OK && addresses.length > 0) {
          resolve(fromResult(addresses[0]));
          return;
        }
        resolve(null);
      });
    });
  });
}

// 지도를 쓸 수 없을 때(키 없음 등) 지도 없이 위치를 고를 수 있는 대표 위치들.
export const QUICK_LOCATIONS = [
  { label: "강남역", lat: 37.4979, lng: 127.0276 },
  { label: "홍대입구역", lat: 37.5572, lng: 126.9245 },
  { label: "판교역", lat: 37.3948, lng: 127.1112 },
  { label: "송도 센트럴파크", lat: 37.3933, lng: 126.6396 },
  { label: "부평역", lat: 37.4894, lng: 126.723 },
];

export const DEFAULT_CENTER = QUICK_LOCATIONS[0];
