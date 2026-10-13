package co.udea.codefactory.reservahub.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.lockout")
public class LockoutProperties {

	private int maxAttempts = 5;
	private long durationMinutes = 15;

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public long getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(long durationMinutes) {
		this.durationMinutes = durationMinutes;
	}
}
