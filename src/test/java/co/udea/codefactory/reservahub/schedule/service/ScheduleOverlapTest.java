package co.udea.codefactory.reservahub.schedule.service;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleOverlapTest {

	@Test
	void detectsOverlap() {
		assertTrue(ScheduleService.overlaps(
				LocalTime.of(9, 0), LocalTime.of(12, 0),
				LocalTime.of(11, 0), LocalTime.of(13, 0)));
	}

	@Test
	void adjacentRangesDoNotOverlap() {
		assertFalse(ScheduleService.overlaps(
				LocalTime.of(9, 0), LocalTime.of(12, 0),
				LocalTime.of(12, 0), LocalTime.of(14, 0)));
	}

	@Test
	void containedRangeOverlaps() {
		assertTrue(ScheduleService.overlaps(
				LocalTime.of(9, 0), LocalTime.of(17, 0),
				LocalTime.of(10, 0), LocalTime.of(11, 0)));
	}
}
