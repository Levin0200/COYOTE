import { useEffect, useState } from "react";
import { fetchHostResult } from "../../services/meetingService";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { geocodeKakaoLocation } from "../../utils/kakaoMap";

export default function useMyPageMap({ address, addressLabel, meetingId }) {
  const [resultState, setResultState] = useState({ meetingId: null, data: null, error: "" });
  const [locationState, setLocationState] = useState({ address: "", location: null, error: "" });

  useEffect(() => {
    let cancelled = false;
    const value = String(address || "").trim();
    if (!value) return undefined;
    geocodeKakaoLocation(value)
      .then((location) => {
        if (!cancelled) setLocationState({ address: value, location, error: location ? "" : "저장한 주소의 위치를 지도에서 찾지 못했습니다." });
      })
      .catch(() => {
        if (!cancelled) setLocationState({ address: value, location: null, error: "저장한 주소의 위치를 불러오지 못했습니다." });
      });
    return () => { cancelled = true; };
  }, [address]);

  useEffect(() => {
    let cancelled = false;
    if (!meetingId) return undefined;
    fetchHostResult(meetingId)
      .then((data) => { if (!cancelled) setResultState({ meetingId, data, error: "" }); })
      .catch((error) => { if (!cancelled) setResultState({ meetingId, data: null, error: getMeetingErrorMessage(error) }); });
    return () => { cancelled = true; };
  }, [meetingId]);

  const mapResult = resultState.meetingId === meetingId ? resultState.data : null;
  const meetingMarkers = (mapResult?.selectedPlaceDetails || [])
    .filter((place) => Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude)))
    .map((place, index) => ({ id: place.id || place.name, name: place.name, label: String(index + 1), lat: Number(place.latitude), lng: Number(place.longitude) }));
  const profileLocation = locationState.address === String(address || "").trim() ? locationState.location : null;
  const profileMarker = profileLocation ? [{ id: "my-profile-location", name: `내 위치 · ${addressLabel}`, label: "내 위치", kind: "user", lat: profileLocation.lat, lng: profileLocation.lng }] : [];
  const markers = [...profileMarker, ...meetingMarkers];
  const center = profileLocation || (meetingMarkers.length ? {
    lat: meetingMarkers.reduce((sum, marker) => sum + marker.lat, 0) / meetingMarkers.length,
    lng: meetingMarkers.reduce((sum, marker) => sum + marker.lng, 0) / meetingMarkers.length,
  } : null);

  return {
    mapResult,
    mapError: resultState.meetingId === meetingId ? resultState.error : "",
    mapLoading: Boolean(meetingId) && resultState.meetingId !== meetingId,
    meetingPlaces: (mapResult?.selectedPlaceDetails || []).filter((place) => Number.isFinite(Number(place.latitude)) && Number.isFinite(Number(place.longitude))),
    myLocation: profileLocation,
    myLocationMarker: profileMarker,
    meetingMapMarkers: meetingMarkers,
    myLocationError: locationState.address === String(address || "").trim() ? locationState.error : "",
    mapMarkers: markers,
    meetingMapCenter: center,
  };
}
