package co.udea.codefactory.reservahub.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_blocks")
public class ScheduleBlock {

	@Id
	private UUID id;

	@Column(name = "schedule_id", nullable = false)
	private UUID scheduleId;

	@Column(name = "block_date", nullable = false)
	private LocalDate blockDate;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(length = 500)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(UUID scheduleId) {
		this.scheduleId = scheduleId;
	}

	public LocalDate getBlockDate() {
		return blockDate;
	}

	public void setBlockDate(LocalDate blockDate) {
		this.blockDate = blockDate;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
