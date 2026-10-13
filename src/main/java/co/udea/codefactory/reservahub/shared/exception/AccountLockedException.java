package co.udea.codefactory.reservahub.shared.exception;

import java.time.Instant;

public class AccountLockedException extends BusinessException {

	private final Instant lockedUntil;

	public AccountLockedException(Instant lockedUntil) {
		super(ErrorCodes.ACCOUNT_LOCKED, "Account temporarily locked due to too many failed login attempts", 423);
		this.lockedUntil = lockedUntil;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}
}
