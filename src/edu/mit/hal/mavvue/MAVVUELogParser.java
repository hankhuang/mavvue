package edu.mit.hal.mavvue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MAVVUELogParser {

	private final static String subjectsData = "subjects.csv";
	private final static String crashesData = "crashes.csv";
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

	private final HashMap<String, SubjectProfile> profiles = new HashMap<String, SubjectProfile>();
	private final ArrayList<CrashLocation> locations = new ArrayList<CrashLocation>();

	public static void main(String[] args) {
		MAVVUELogParser parser;
		try {
			parser = new MAVVUELogParser(args);
			parser.printResults();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MAVVUELogParser(String[] files) throws IOException {
		for (String file : files) {
			if (file.contains("T2") || file.contains("T4")) {
				final String[] tokens = file.substring(file.lastIndexOf("/"), file.lastIndexOf(".")).split("-");
				final String subjectID = tokens[4];
				final String trialID = tokens[5];
				final String key = subjectID + trialID;
				if (!profiles.containsKey(key)) {
					profiles.put(key, new SubjectProfile(subjectID, trialID));
				}
				final SubjectProfile profile = profiles.get(key);
				final BufferedReader input = new BufferedReader(new FileReader(new File(file)));
				input.mark(0);
				parseStatuses(profile, input, file);
				parseCommands(profile, input, file);
				parsePathLength(profile, input, file);
				parseTeleOps(profile, input, file);
			}
		}
	}

	// is completed
	// completion time
	// # of collisions
	// location of collisions
	public void parseStatuses(final SubjectProfile profile, final BufferedReader input, final String file) throws IOException {
		if (file.contains("status")) {
			input.reset();
			String line = null;
			int count = 0;
			int crashes = 0;
			while ((line = input.readLine()) != null) {
				// Check completion and set trial end time
				try {
					if (line.contains("TIMEPOINT")) {
						count++;
						final String[] tokens = line.split(",");
						final long time = dateFormat.parse(tokens[0]).getTime();
						if (count >= 2) {
							profile.trialEndTime = time;
							profile.isCompleted = true;
						} else if (count == 1){
							profile.timeEnteredRoom = time;
							profile.roomEntered = true;
						}
					}
					// Parse Crashes
					if (line.contains("crashed")) {
						crashes++;
						final String[] tokens = line.split(",");
						final long time = dateFormat.parse(tokens[0]).getTime();
						final double x = Double.parseDouble(tokens[4]);
						final double y = Double.parseDouble(tokens[5]);
						locations.add(new CrashLocation(profile, time, x, y));
					}
					// Parse Start-point 2
					if (line.contains("start-point,2") && profile.firstAttemptStart == -1) {
						final String[] tokens = line.split(",");
						final long time = dateFormat.parse(tokens[0]).getTime();
						profile.firstAttemptStart = time;
						profile.finalAttemptStart = time;
					}
					// Parse time of final attempt to enter room
					if (line.contains("robot-initialized") && profile.timeEnteredRoom == -1 && profile.firstAttemptStart != -1) {
						final String[] tokens = line.split(",");
						final long time = dateFormat.parse(tokens[0]).getTime();
						profile.finalAttemptStart = time;
					}
					// Parse time in room
					if (line.contains("start-point,3")) {
						final String[] tokens = line.split(",");
						final long time = dateFormat.parse(tokens[0]).getTime();
						profile.roomEntered = true;
						profile.timeEnteredRoom = time;
					}
					if (line.contains("disconnected")) {
						break;
					}
				} catch (Exception e) {
					System.out.println(line);
					e.printStackTrace();
				}
			}
			profile.collisionCount = crashes;
		}
	}

	// start of trial
	// # of controls
	public void parseCommands(final SubjectProfile profile, final BufferedReader input, final String file) throws IOException {
		if (file.contains("command")) {
			input.reset();
			String line = null;
			int count = 0;
			while ((line = input.readLine()) != null) {
				if (line.contains("takeoff")) {
					final String[] tokens = line.split(",");
					try {
						long time = dateFormat.parse(tokens[0]).getTime();
						profile.trialStartTime = Math.min(profile.trialStartTime, time); // just keep the earliest takeoff time
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
				if (line.contains("uilog")) {
					count++;
				}
			}
			profile.commandCount = count;
		}
	}

	// magnitude of controls
	public void parseTeleOps(final SubjectProfile profile, final BufferedReader input, final String file) throws IOException {
		if (file.contains("teleop")) {
			input.reset();
			String line = null;
			double magnitude = 0;
			while ((line = input.readLine()) != null) {
				if (line.contains("relative_move")) {
					final String[] tokens = line.split(",");
					final double x = Double.parseDouble(tokens[4]);
					final double y = Double.parseDouble(tokens[5]);
					magnitude += Math.sqrt(x * x + y * y);
				}
			}
			profile.commandMagnitude = magnitude;
		}
	}

	// total path length of flights
	public void parsePathLength(final SubjectProfile profile, final BufferedReader input, final String file) throws IOException {
		if (file.contains("pos")) {
			input.reset();
			String line = null;

			double x = Double.NaN;
			double y = Double.NaN;
			double length = 0;
			while ((line = input.readLine()) != null) {
				final String[] tokens = line.split(",");
				if (Double.isNaN(x) && Double.isNaN(y)) {
					x = Double.parseDouble(tokens[3]);
					y = Double.parseDouble(tokens[4]);
				} else {
					final double newX = Double.parseDouble(tokens[3]);
					final double newY = Double.parseDouble(tokens[4]);
					final double dX = newX - x;
					final double dY = newY - y;
					length += Math.sqrt(dX * dX + dY * dY);
				}
			}
			profile.pathLength = length;
		}
	}

	public void printResults() throws IOException {
		BufferedWriter output = new BufferedWriter(new FileWriter(new File(subjectsData)));
		output.write(SubjectProfile.getHeader());
		output.newLine();
		for (final SubjectProfile profile : profiles.values()) {
			output.write(profile.toString());
			output.newLine();
		}
		output.flush();
		output.close();

		output = new BufferedWriter(new FileWriter(new File(crashesData)));
		output.write(CrashLocation.getHeader());
		output.newLine();
		for (final CrashLocation location : locations) {
			output.write(location.toString());
			output.newLine();
		}
		output.flush();
		output.close();
	}
}
