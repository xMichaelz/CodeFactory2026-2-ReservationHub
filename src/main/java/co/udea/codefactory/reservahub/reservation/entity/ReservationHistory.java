package co.udea.codefactory.reservahub.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_history")
public class ReservationHistory {

	@Id
	private UUID id;

	@Column(name = "reservation_id", nullable = false)
	private UUID reservationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReservationAction action;

	@Column(name = "actor_user_id", nullable = false)
	private UUID actorUserId;

	@Column(length = 500)
	private String reason;

	@Column(name = "old_date")
	private LocalDate oldDate;

	@Column(name = "old_start")
	private LocalTime oldStart;

	@Column(name = "old_end")
	private LocalTime oldEnd;

	@Column(name = "new_date")
	private LocalDate newDate;

	@Column(name = "new_start")
	private LocalTime newStart;

	@Column(name = "new_end")
	private LocalTime newEnd;

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

	public UUID getReservationId() {
		return reservationId;
	}

	public void setReservationId(UUID reservationId) {
		this.reservationId = reservationId;
	}

	public ReservationAction getAction() {
		return action;
	}

	public void setAction(ReservationAction action) {
		this.action = action;
	}

	public UUID getActorUserId() {
		return actorUserId;
	}

	public void setActorUserId(UUID actorUserId) {
		this.actorUserId = actorUserId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDate getOldDate() {
		return oldDate;
	}

	public void setOldDate(LocalDate oldDate) {
		this.oldDate = oldDate;
	}

	public LocalTime getOldStart() {
		return oldStart;
	}

	public void setOldStart(LocalTime oldStart) {
		this.oldStart = oldStart;
	}

	public LocalTime getOldEnd() {
		return oldEnd;
	}

	public void setOldEnd(LocalTime oldEnd) {
		this.oldEnd = oldEnd;
	}

	public LocalDate getNewDate() {
		return newDate;
	}

	public void setNewDate(LocalDate newDate) {
		this.newDate = newDate;
	}

	public LocalTime getNewStart() {
		return newStart;
	}

	public void setNewStart(LocalTime newStart) {
		this.newStart = newStart;
	}

	public LocalTime getNewEnd() {
		return newEnd;
	}

	public void setNewEnd(LocalTime newEnd) {
		this.newEnd = newEnd;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
