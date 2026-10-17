package com.coyote.moyeon.place;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 클라이언트가 보낸 장소 ID를 공식 데이터로 다시 확인한다. 클라이언트가 보낸 이름과 좌표는 믿지 않고
 * 조회한 공식 값만 저장하므로, 게스트가 가짜 좌표를 넣어 합의 지역을 조작할 수 없다.
 * 외부 API를 호출하므로 DB 트랜잭션이나 모임 잠금 밖에서 먼저 호출해야 한다.
 */
@Service
public class PlaceLookupService {

	public static final int MAX_IDS = 11;
	private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{1,40}$");

	private final PlaceSearchProvider provider;

	public PlaceLookupService(PlaceSearchProvider provider) {
		this.provider = provider;
	}

	/** 장소 ID마다 공식 장소 정보를 돌려준다. 하나라도 없거나 형식이 틀리면 요청 전체를 거부한다. */
	public Map<String, Place> lookupAll(Collection<String> ids) {
		Map<String, Place> places = new LinkedHashMap<>();
		if (ids.isEmpty()) {
			return places;
		}
		if (ids.size() > MAX_IDS) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "한 번에 확인할 수 있는 장소 수를 넘었습니다.");
		}
		if (!provider.isAvailable()) {
			throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "PLACE_SEARCH_UNAVAILABLE",
					"장소 확인을 지금 사용할 수 없습니다.");
		}
		for (String id : ids) {
			if (!ID.matcher(id).matches()) {
				throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "올바르지 않은 장소 번호입니다.");
			}
			Optional<Place> place = provider.findById(id);
			places.put(id, place.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "PLACE_NOT_FOUND",
					"존재하지 않는 장소입니다.")));
		}
		return places;
	}
}
