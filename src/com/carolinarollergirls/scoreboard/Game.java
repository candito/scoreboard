package com.carolinarollergirls.scoreboard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import com.carolinarollergirls.scoreboard.game.*;

public class Game {
	public Game() {
		this.logging = false;
		this.sb = null;
	}

	public void setScoreBoard(ScoreBoard sb) {
		this.sb = sb;
	}
	
	public void start(String i) {
		identifier = i;
		teams = new TeamInfo[2];
		teams[0] = new TeamInfo(this, Team.ID_1);
		teams[1] = new TeamInfo(this, Team.ID_2);
		periods = new ArrayList<PeriodStats>();
		logging = true;
	}

	public void stop() {
		this.identifier = "";
		this.teams = null;
		this.periods = null;
		this.logging = false;
	}

	private JamStats findJamStats(long period, long jam) {
		PeriodStats ps = null;
		for (PeriodStats ps1 : periods) {
			if (ps1.getPeriod() == period) {
				ps = ps1;
				break;
			}
		}
		if (ps == null) {
			ps = new PeriodStats(this, period);
			periods.add(ps);
		}

		return ps.getJamStats(jam);
	}

	public void snapshot() {
		if (!logging || sb == null)
			return;

		try {
			teams[0].snapshot();
			teams[1].snapshot();

			long period = sb.getClock(Clock.ID_PERIOD).getNumber();
			long jam = sb.getClock(Clock.ID_JAM).getNumber();

			ScoreBoardManager.printMessage("Looking for period: " + period + "  jam: " + jam);
			JamStats js = findJamStats(period, jam);
			ScoreBoardManager.printMessage("  found js: " + js);

			js.snapshot();

			saveFile();
		} catch (Exception e) {
			ScoreBoardManager.printMessage("Error catching snapshot: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("identifier", identifier);

		JSONArray t = new JSONArray();
		t.put(teams[0].toJSON());
		t.put(teams[1].toJSON());
		json.put("teams", t);

		JSONArray p = new JSONArray();
		for (PeriodStats ps : periods) {
			p.put(ps.toJSON());
		}
		json.put("periods", p);

		return json;
	}

	public Team getTeam(String id) {
		if (sb == null)
			return null;
		return sb.getTeam(id);
	}

	public Clock getClock(String id) {
		if (sb == null)
			return null;
		return sb.getClock(id);
	}

	private void saveFile() {
		File file = new File(new File(ScoreBoardManager.getDefaultPath(), "GameData"), identifier + ".json");
		file.getParentFile().mkdirs();
		FileWriter out = null;
		try {
			out = new FileWriter(file);
			out.write(this.toJSON().toString(2));
		} catch (Exception e) {
		} finally {
			if (out != null) {
				try { out.close(); } catch (Exception e) { }
			}
		}
	}

	protected ScoreBoard sb = null;
	private TeamInfo[] teams = null;
	private ArrayList<PeriodStats> periods = null;
	private boolean logging = false;
	private String identifier = "";
}