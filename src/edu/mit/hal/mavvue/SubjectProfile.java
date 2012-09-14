package edu.mit.hal.mavvue;

public class SubjectProfile {
	
	final String subjectID;
	final String trialID;
	
	boolean isCompleted;
	boolean roomEntered;
	// in milliseconds
	long trialStartTime = Long.MAX_VALUE;
	long trialEndTime;
	
	int commandCount;
	double commandMagnitude;
	
	int collisionCount;
	
	long firstAttemptStart = -1;
	long finalAttemptStart;
	long timeEnteredRoom = -1;
	
	double pathLength;

	public SubjectProfile(final String subjectID, final String trialID) {
		this.subjectID = subjectID;
		this.trialID = trialID;
	}
	
	public long getTimeToEnterRoom() {
		if (!roomEntered) return -1;
		return timeEnteredRoom - firstAttemptStart;
	}
	
	public long getFinalAttemptTime() {
		if (!roomEntered) return -1;
		return timeEnteredRoom - finalAttemptStart;
	}
	
	public long getCompletionTime() {
		if (!isCompleted) return -1;
		return trialEndTime - trialStartTime;
	}
	
	public String getTrialType() {
		int id = Integer.parseInt(subjectID.substring(1));
		int trial = Integer.parseInt(trialID.substring(1));
		if (id % 2 == 0 && trial == 2 || id % 2 == 1 && trial == 4) {
			return "WITH";
		} else {
			return "WITHOUT";
		}
	}
	
	public String toString() {
		return String.format("%s,%s,%s,%b,%d,%d,%d,%d,%d,%f,%f", subjectID, trialID, getTrialType(), isCompleted, getCompletionTime(), collisionCount, commandCount, getTimeToEnterRoom(), getFinalAttemptTime(), commandMagnitude, pathLength);
	}
	
	public static String getHeader() {
		return "SubjectID,TrialID,TrialType,Completed,CompletionTime,CrashCount,CommandCount,TimeToEnterRoom,FinalAttemptTime,CommandMagnitude,PathLength";
	}
}
