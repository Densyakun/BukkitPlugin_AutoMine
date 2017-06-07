package io.github.densyakun.bukkit.automine;

public enum Direction {
	zero, left, right, down, up, back, forward;

	public static Direction getDirection(double x, double y, double z) {
		if (x < y) {
			if (y < -x) {
				if (-x < z) {
					return forward;
				} else if (-x < -z) {
					return back;
				} else {
					return left;
				}
			} else {
				if (y < z) {
					return forward;
				} else if (y < -z) {
					return back;
				} else {
					return up;
				}
			}
		} else if (x == 0 && y == 0) {
			return zero;
		} else {
			if (-y < x) {
				if (x < z) {
					return forward;
				} else if (x < -z) {
					return back;
				} else {
					return right;
				}
			} else {
				if (-y < z) {
					return forward;
				} else if (-y < -z) {
					return back;
				} else {
					return down;
				}
			}
		}
	}
}
