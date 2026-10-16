package com.coyote.moyeon.place.consensus;

import static org.assertj.core.api.Assertions.assertThat;

import com.coyote.moyeon.place.Place;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegionLabelerTest {

	private static PlaceVote vote(String address, int votes) {
		Place place = new Place("id-" + address, "장소", "I2", "I201", "I20101", "음식 > 한식 > 백반", address, 37.5, 127.0, 0);
		return new PlaceVote(place, votes, 1);
	}

	@Test
	void usesTheDistrictOfTheRoadAddress() {
		assertThat(RegionLabeler.areaOf("서울특별시 강남구 테헤란로 1")).isEqualTo("강남구");
		assertThat(RegionLabeler.areaOf("인천광역시 연수구 센트럴로 194")).isEqualTo("연수구");
		assertThat(RegionLabeler.areaOf("경기도 가평군 가평읍 1")).isEqualTo("가평군");
		assertThat(RegionLabeler.areaOf("경기도 구리시 경춘로 1")).isEqualTo("구리시");
	}

	@Test
	void keepsTheCityAndDistrictTogetherForLargeCities() {
		assertThat(RegionLabeler.areaOf("경기도 성남시 분당구 대왕판교로 1")).isEqualTo("성남시 분당구");
		assertThat(RegionLabeler.areaOf("경상북도 포항시 남구 새천년대로 1")).isEqualTo("포항시 남구");
	}

	@Test
	void fallsBackToTheFirstWordWhenThereIsNoDistrict() {
		assertThat(RegionLabeler.areaOf("세종특별자치시 한누리대로 2130")).isEqualTo("세종특별자치시");
		assertThat(RegionLabeler.areaOf("서울특별시")).isEqualTo("서울특별시");
	}

	@Test
	void blankAddressesGiveNoArea() {
		assertThat(RegionLabeler.areaOf(null)).isNull();
		assertThat(RegionLabeler.areaOf("   ")).isNull();
	}

	@Test
	void picksTheDistrictWithTheMostVotes() {
		List<PlaceVote> members = List.of(vote("서울특별시 서초구 서초대로 1", 1), vote("서울특별시 강남구 테헤란로 1", 2),
				vote("서울특별시 강남구 강남대로 2", 1));

		assertThat(RegionLabeler.label(members)).isEqualTo("강남구 일대");
	}

	@Test
	void usesAGenericLabelWhenNoAddressIsKnown() {
		assertThat(RegionLabeler.label(List.of(vote("", 1)))).isEqualTo(RegionLabeler.FALLBACK);
	}
}
