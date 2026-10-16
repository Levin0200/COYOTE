import { useEffect, useRef, useState } from "react";
import { DEFAULT_CENTER, KAKAO_ERRORS, QUICK_LOCATIONS, loadKakaoMaps } from "../utils/kakaoMap";

const ERROR_MESSAGES = {
  [KAKAO_ERRORS.NO_KEY]: "카카오 지도 키(VITE_KAKAO_MAP_KEY)가 설정되지 않아 지도를 표시할 수 없습니다.",
  [KAKAO_ERRORS.LOAD_FAILED]: "카카오 지도를 불러오지 못했습니다. 키와 등록한 사이트 주소, 카카오맵 사용 설정을 확인해 주세요.",
};

const markerStyle = (selected) =>
  [
    "display:flex",
    "align-items:center",
    "justify-content:center",
    "width:34px",
    "height:34px",
    "border-radius:9999px",
    "border:3px solid #fff",
    "box-shadow:0 4px 10px rgba(15,23,42,.3)",
    "font-weight:800",
    "font-size:13px",
    "cursor:pointer",
    `background:${selected ? "#2563EB" : "#1E293B"}`,
    "color:#fff",
  ].join(";");

/**
 * 카카오맵 지도.
 * - center: 현재 기준 위치 {lat, lng}. pickable이면 지도를 누르거나 핀을 끌어 위치를 바꾸고 onPick({lat, lng})이 호출된다.
 *   검색으로 옮기면 onPick({lat, lng, label})로 검색어(label)도 함께 알려 준다.
 * - radiusMeters: 기준 위치 둘레에 반경 원을 그린다.
 * - markers: 후보 장소 [{id, name, lat, lng}]. 번호가 붙은 마커로 표시하고 누르면 onSelectMarker(id)가 호출된다.
 * - showSearch: 지역 이름으로 검색해 지도를 옮기는 입력창(카카오 검색은 화면 이동에만 쓰고 결과를 저장하지 않는다).
 * 지도를 쓸 수 없을 때(키 없음 등)는 안내와 함께 대표 위치 버튼을 보여 준다.
 */
function KakaoMap({ center = DEFAULT_CENTER, radiusMeters = null, markers = [], selectedId = null, pickable = false, showSearch = false, onPick, onSelectMarker, height = 360 }) {
  const containerRef = useRef(null);
  const kakaoRef = useRef(null);
  const mapRef = useRef(null);
  const pinRef = useRef(null);
  const circleRef = useRef(null);
  const overlaysRef = useRef([]);
  const callbacksRef = useRef({ onPick, onSelectMarker });
  const [status, setStatus] = useState("loading");
  const [errorCode, setErrorCode] = useState(null);
  const [query, setQuery] = useState("");
  const [searchMessage, setSearchMessage] = useState("");

  useEffect(() => {
    callbacksRef.current = { onPick, onSelectMarker };
  });

  // 지도 만들기(처음 한 번)
  useEffect(() => {
    let cancelled = false;
    loadKakaoMaps()
      .then((kakao) => {
        if (cancelled || !containerRef.current) return;
        kakaoRef.current = kakao;
        const position = new kakao.maps.LatLng(center.lat, center.lng);
        const map = new kakao.maps.Map(containerRef.current, { center: position, level: 5 });
        mapRef.current = map;
        if (pickable) {
          const pin = new kakao.maps.Marker({ position, draggable: true, map });
          pinRef.current = pin;
          kakao.maps.event.addListener(pin, "dragend", () => {
            const moved = pin.getPosition();
            callbacksRef.current.onPick?.({ lat: moved.getLat(), lng: moved.getLng() });
          });
          kakao.maps.event.addListener(map, "click", (event) => {
            pin.setPosition(event.latLng);
            callbacksRef.current.onPick?.({ lat: event.latLng.getLat(), lng: event.latLng.getLng() });
          });
        }
        setStatus("ready");
      })
      .catch((error) => {
        if (cancelled) return;
        setErrorCode(error.message);
        setStatus("error");
      });
    return () => {
      cancelled = true;
    };
    // 지도는 처음 한 번만 만든다. 이후 값 변화는 아래 효과들이 반영한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 기준 위치가 바뀌면 핀을 옮기고, 화면 밖이면 지도도 옮긴다.
  useEffect(() => {
    if (status !== "ready") return;
    const kakao = kakaoRef.current;
    const position = new kakao.maps.LatLng(center.lat, center.lng);
    pinRef.current?.setPosition(position);
    if (!mapRef.current.getBounds().contain(position)) mapRef.current.setCenter(position);
  }, [status, center.lat, center.lng]);

  const markersKey = JSON.stringify(markers.map((marker) => [marker.id, marker.lat, marker.lng]));

  // 반경 원
  useEffect(() => {
    if (status !== "ready") return;
    const kakao = kakaoRef.current;
    circleRef.current?.setMap(null);
    circleRef.current = null;
    if (!radiusMeters) return;
    const circle = new kakao.maps.Circle({
      map: mapRef.current,
      center: new kakao.maps.LatLng(center.lat, center.lng),
      radius: radiusMeters,
      strokeWeight: 2,
      strokeColor: "#2563EB",
      strokeOpacity: 0.7,
      fillColor: "#3B82F6",
      fillOpacity: 0.1,
    });
    circleRef.current = circle;
    if (markers.length === 0) mapRef.current.setBounds(circle.getBounds());
  }, [status, radiusMeters, center.lat, center.lng, markers.length]);

  // 후보 마커(선택 표시 포함)
  useEffect(() => {
    if (status !== "ready") return;
    const kakao = kakaoRef.current;
    overlaysRef.current.forEach((overlay) => overlay.setMap(null));
    overlaysRef.current = markers.map((marker, index) => {
      const element = document.createElement("button");
      element.type = "button";
      element.title = marker.name;
      element.textContent = String(index + 1);
      element.setAttribute("style", markerStyle(marker.id === selectedId));
      element.addEventListener("click", () => callbacksRef.current.onSelectMarker?.(marker.id));
      const overlay = new kakao.maps.CustomOverlay({
        position: new kakao.maps.LatLng(marker.lat, marker.lng),
        content: element,
        yAnchor: 1,
        zIndex: marker.id === selectedId ? 3 : 2,
      });
      overlay.setMap(mapRef.current);
      return overlay;
    });
    // markers 배열은 매 렌더링마다 새로 만들어지므로 내용이 바뀐 때만(markersKey) 다시 그린다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, markersKey, selectedId]);

  // 후보가 생기거나 바뀌면 후보와 기준 위치가 모두 보이게 지도 범위를 맞춘다.
  useEffect(() => {
    if (status !== "ready" || markers.length === 0) return;
    const kakao = kakaoRef.current;
    const bounds = new kakao.maps.LatLngBounds();
    bounds.extend(new kakao.maps.LatLng(center.lat, center.lng));
    markers.forEach((marker) => bounds.extend(new kakao.maps.LatLng(marker.lat, marker.lng)));
    mapRef.current.setBounds(bounds);
    // 범위는 후보가 바뀔 때(markersKey)만 맞춘다. 기준 위치를 옮길 때마다 지도가 움직이면 불편하다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, markersKey]);

  const search = (event) => {
    event.preventDefault();
    const text = query.trim();
    if (!text || status !== "ready") return;
    const kakao = kakaoRef.current;
    setSearchMessage("");
    const moveTo = (item) => callbacksRef.current.onPick?.({ lat: parseFloat(item.y), lng: parseFloat(item.x), label: text });
    new kakao.maps.services.Places().keywordSearch(text, (places, state) => {
      if (state === kakao.maps.services.Status.OK && places.length) return moveTo(places[0]);
      new kakao.maps.services.Geocoder().addressSearch(text, (addresses, geoState) => {
        if (geoState === kakao.maps.services.Status.OK && addresses.length) return moveTo(addresses[0]);
        setSearchMessage("검색 결과가 없습니다. 다른 지역명으로 검색하거나 지도를 눌러 위치를 고르세요.");
      });
    });
  };

  const failed = status === "error";

  return (
    <div className="relative overflow-hidden rounded-[2rem] border border-blue-100 bg-slate-100" style={{ minHeight: height }}>
      <div ref={containerRef} style={{ width: "100%", height, visibility: failed ? "hidden" : "visible" }} />
      {showSearch && status === "ready" && (
        <form onSubmit={search} className="absolute left-3 right-3 top-3 z-10 flex gap-2">
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="지역·장소 이름으로 지도 이동 (예: 송도, 강남역)" className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-white/95 px-3 py-2 text-sm shadow-sm" />
          <button type="submit" className="shrink-0 rounded-xl bg-slate-900 px-4 py-2 text-sm font-bold text-white shadow-sm">이동</button>
        </form>
      )}
      {searchMessage && <p className="absolute bottom-3 left-3 right-3 z-10 rounded-xl bg-white/95 px-3 py-2 text-xs font-bold text-rose-600 shadow-sm">{searchMessage}</p>}
      {status === "loading" && <div className="absolute inset-0 flex items-center justify-center text-sm font-bold text-slate-400">지도를 불러오는 중...</div>}
      {failed && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 p-6 text-center">
          <p className="text-sm font-bold text-slate-500">{ERROR_MESSAGES[errorCode] || ERROR_MESSAGES[KAKAO_ERRORS.LOAD_FAILED]}</p>
          {pickable && (
            <>
              <p className="text-xs text-slate-400">대신 아래 대표 위치 중에서 기준 위치를 고를 수 있습니다.</p>
              <div className="flex flex-wrap justify-center gap-2">
                {QUICK_LOCATIONS.map((place) => (
                  <button key={place.label} type="button" onClick={() => callbacksRef.current.onPick?.({ lat: place.lat, lng: place.lng })} className={`rounded-xl border px-3 py-2 text-sm font-bold ${Math.abs(center.lat - place.lat) < 1e-6 && Math.abs(center.lng - place.lng) < 1e-6 ? "border-blue-500 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-600"}`}>
                    📍 {place.label}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default KakaoMap;
