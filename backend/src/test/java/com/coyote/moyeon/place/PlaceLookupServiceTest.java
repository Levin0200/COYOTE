package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlaceLookupServiceTest {

	private final List<String> lookedUp = new ArrayList<>();
	private boolean available = true;

	private final PlaceSearchProvider provider = new PlaceSearchProvider() {
		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public Optional<Place> findById(String id) {
			lookedUp.add(id);
			return id.startsWith("known")
					? Optional.of(new Place(id, "공식 이름 " + id, "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 1", 37.5, 127.0, 0))
					: Optional.empty();
		}

		@Override
		public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
			return List.of();
		}

		@Override
		public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
			return List.of();
		}
	};

	private final PlaceLookupService service = new PlaceLookupService(provider);

	private static void assertError(Runnable call, HttpStatus status, String code) {
		assertThatThrownBy(call::run).isInstanceOfSatisfying(BusinessException.class, e -> {
			assertThat(e.getStatus()).isEqualTo(status);
			assertThat(e.getCode()).isEqualTo(code);
		});
	}

	@Test
	void noIdsMeansNoCalls() {
		assertThat(service.lookupAll(List.of())).isEmpty();
		assertThat(lookedUp).isEmpty();
	}

	@Test
	void returnsTheOfficialPlaceForEveryId() {
		Map<String, Place> places = service.lookupAll(List.of("known-1", "known-2"));

		assertThat(places).containsOnlyKeys("known-1", "known-2");
		assertThat(places.get("known-2").name()).isEqualTo("공식 이름 known-2");
		assertThat(lookedUp).containsExactly("known-1", "known-2");
	}

	@Test
	void anUnknownPlaceRejectsTheWholeRequest() {
		assertError(() -> service.lookupAll(List.of("known-1", "missing")), HttpStatus.BAD_REQUEST, "PLACE_NOT_FOUND");
	}

	@Test
	void malformedIdsAreRejectedWithoutCallingTheProvider() {
		for (String bad : new String[] { "bad id", "x&serviceKey=1", "../etc", "a".repeat(41), "한글" }) {
			assertError(() -> service.lookupAll(List.of(bad)), HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
		}
		assertThat(lookedUp).isEmpty();
	}

	@Test
	void tooManyIdsAreRejected() {
		List<String> ids = new ArrayList<>();
		for (int i = 0; i <= PlaceLookupService.MAX_IDS; i++) {
			ids.add("known-" + i);
		}

		assertError(() -> service.lookupAll(ids), HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
		assertThat(lookedUp).isEmpty();
	}

	@Test
	void anUnavailableProviderIsReported() {
		available = false;

		assertError(() -> service.lookupAll(List.of("known-1")), HttpStatus.SERVICE_UNAVAILABLE, "PLACE_SEARCH_UNAVAILABLE");
		// 장소 ID가 없는 요청은 제공자와 상관없이 통과한다.
		assertThat(service.lookupAll(List.of())).isEmpty();
	}
}
