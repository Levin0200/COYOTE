import { formatDistance, lastCategory } from "../../utils/placeCandidates";

/** 장소 후보 목록. 번호는 지도 마커 번호와 같고, 누르면 선택한다. */
function PlaceCandidateList({ candidates, selectedKey, onSelect, disabled = false }) {
  if (candidates.length === 0) return null;
  return (
    <div className="space-y-3">
      {candidates.map((place, index) => {
        const selected = place.key === selectedKey;
        const distance = formatDistance(place);
        return (
          <button
            key={place.key}
            type="button"
            disabled={disabled}
            onClick={() => onSelect(place.key)}
            className={`flex w-full items-start gap-3 rounded-2xl border p-4 text-left transition ${selected ? "border-blue-500 bg-blue-50" : "border-slate-200 bg-slate-50 hover:border-blue-200"} disabled:cursor-not-allowed disabled:opacity-60`}
          >
            <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-black ${selected ? "bg-blue-600 text-white" : "bg-white text-slate-600"}`}>{index + 1}</span>
            <span className="min-w-0">
              <span className="block font-black text-slate-900">{place.name}</span>
              <span className="mt-1 block text-sm text-slate-500">
                {[lastCategory(place.category), distance].filter(Boolean).join(" · ")}
              </span>
              {place.address && <span className="mt-0.5 block truncate text-xs text-slate-400">{place.address}</span>}
              {place.reason && <span className="mt-1 block text-xs font-bold text-purple-600">✨ {place.reason}</span>}
            </span>
          </button>
        );
      })}
    </div>
  );
}

export default PlaceCandidateList;
