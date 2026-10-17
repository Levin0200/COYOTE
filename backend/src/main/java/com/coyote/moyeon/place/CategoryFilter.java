package com.coyote.moyeon.place;

/** 업종 코드로 장소를 거르는 조건. 레벨은 상권업종 대·중·소분류 중 하나다. */
public record CategoryFilter(Level level, String code, String label) {

	public enum Level {
		LARGE, MEDIUM, SMALL
	}

	public static CategoryFilter large(String code, String label) {
		return new CategoryFilter(Level.LARGE, code, label);
	}

	public static CategoryFilter medium(String code, String label) {
		return new CategoryFilter(Level.MEDIUM, code, label);
	}

	public static CategoryFilter small(String code, String label) {
		return new CategoryFilter(Level.SMALL, code, label);
	}

	public boolean matches(Place place) {
		return switch (level) {
			case LARGE -> code.equals(place.largeCategoryCode());
			case MEDIUM -> code.equals(place.mediumCategoryCode());
			case SMALL -> code.equals(place.smallCategoryCode());
		};
	}
}
