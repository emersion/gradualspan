package fr.emersion.gradualspan;

public enum GradualOrder {
	LOWER, EQUAL, GREATER;

	public String toString() {
		switch (this) {
		case LOWER:
			return "<";
		case EQUAL:
			return "=";
		case GREATER:
			return ">";
		}
		return super.toString();
	}
}
