// 장소 후보를 화면에서 다루는 공통 형태로 바꾸는 도우미.
// 화면용 후보: { key, placeId, name, category, address, latitude, longitude, distanceMeters, distanceLabel, reason }
// key는 장소 ID가 있으면 ID, 이름만 있는 예전 후보는 이름이다(선택 비교에 쓴다).

export const formatDistance = (candidate) => {
  if (candidate.distanceLabel) return candidate.distanceLabel;
  const meters = candidate.distanceMeters;
  if (meters === null || meters === undefined) return "";
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)}km` : `${meters}m`;
};

// 장소 검색·추천 응답의 장소 -> 화면용 후보
export const fromSearchResult = (place) => ({
  key: place.id,
  placeId: place.id,
  name: place.name,
  category: place.category || "",
  address: place.address || "",
  latitude: place.latitude,
  longitude: place.longitude,
  distanceMeters: place.distanceMeters ?? null,
  distanceLabel: "",
  reason: place.reason || "",
});

// 서버에 저장된 추천 후보(정책의 options) -> 화면용 후보. 이름만 있는 예전 후보는 장소 ID와 좌표가 없다.
export const fromOption = (option) => ({
  key: option.placeId || option.name,
  placeId: option.placeId || "",
  name: option.name,
  category: option.category || "",
  address: option.address || "",
  latitude: option.latitude ?? null,
  longitude: option.longitude ?? null,
  distanceMeters: null,
  distanceLabel: option.distance || "",
  reason: "",
});

// 확인 화면용 장소 상세(draft.place) -> 화면용 후보(다시 들어왔을 때 이미 고른 장소를 보여 주려고 쓴다)
export const fromPlaceDetail = (place) => ({
  key: place.id || place.name,
  placeId: place.id || "",
  name: place.name,
  category: place.category || "",
  address: place.address || "",
  latitude: place.latitude ?? null,
  longitude: place.longitude ?? null,
  distanceMeters: null,
  distanceLabel: "",
  reason: "",
});

// 화면용 후보 -> 모임 생성·수정 요청의 추천 후보(placePolicy.options[])
export const toOption = (candidate) => ({
  name: candidate.name,
  distance: formatDistance(candidate).slice(0, 20),
  placeId: candidate.placeId || undefined,
});

// 확인 화면에서 보여 줄 선택한 장소의 상세
export const toPlaceDetail = (candidate) => ({
  id: candidate.placeId || null,
  name: candidate.name,
  address: candidate.address || "",
  category: candidate.category || "",
  latitude: candidate.latitude,
  longitude: candidate.longitude,
});

export const hasCoordinates = (candidate) =>
  typeof candidate.latitude === "number" && typeof candidate.longitude === "number";

export const lastCategory = (category) => (category || "").split(" > ").pop();

// 좌표가 있는 후보들의 중심(없으면 null)
export const centerOf = (candidates) => {
  const located = candidates.filter(hasCoordinates);
  if (located.length === 0) return null;
  return {
    lat: located.reduce((sum, item) => sum + item.latitude, 0) / located.length,
    lng: located.reduce((sum, item) => sum + item.longitude, 0) / located.length,
  };
};
