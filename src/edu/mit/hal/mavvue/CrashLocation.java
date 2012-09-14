package edu.mit.hal.mavvue;

public class CrashLocation {
	
	final SubjectProfile profile;
	final long time;
	final double x;
	final double y;

	public CrashLocation(final SubjectProfile profile, final long time, final double x, final double y) {
		this.profile = profile;
		this.time = time;
		this.x = x;
		this.y = y;
	}

	public static String getHeader() {
		return "SubjectID,TrialID,Time,X,Y";
	}
	
	public String toString() {
		return String.format("%s,%s,%d,%f,%f", profile.subjectID, profile.trialID, time - profile.trialStartTime, x, y);
	}
}
