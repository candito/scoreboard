package com.carolinarollergirls.scoreboard.defaults;
/**
 * Copyright (C) 2008-2012 Mr Temper <MrTemper@CarolinaRollergirls.com>
 *
 * This file is part of the Carolina Rollergirls (CRG) ScoreBoard.
 * The CRG ScoreBoard is licensed under either the GNU General Public
 * License version 3 (or later), or the Apache License 2.0, at your option.
 * See the file COPYING for details.
 */

import java.util.*;

import com.carolinarollergirls.scoreboard.*;
import com.carolinarollergirls.scoreboard.model.*;
import com.carolinarollergirls.scoreboard.event.*;
import com.carolinarollergirls.scoreboard.policy.ClockSyncPolicy;

public class DefaultClockModel extends DefaultScoreBoardEventProvider implements ClockModel
{
	public DefaultClockModel(ScoreBoardModel sbm, String i) {
		scoreBoardModel = sbm;
		id = i;

		// The default value for these cannot be known
		// as each clock has a different value,
		// so we only set a default here so something will be set.
		// These defaults are almost certainly not correct,
		// so we really are relying on later configuration (like xml loading)
		// to set the correct values for these.
		// Additionally, we can't do this in reset() because
		// then the clocks would all be reset to incorrect min,max,etc values.
		// Even better, would be to force passing these in the constructor so
		// we would have good default values, that could be reset to...
		// FIXME ^^
		setMinimumNumber(DEFAULT_MINIMUM_NUMBER);
		setMaximumNumber(DEFAULT_MAXIMUM_NUMBER);
		setCountDirectionDown(DEFAULT_DIRECTION);
		setMinimumTime(DEFAULT_MINIMUM_TIME);
		setMaximumTime(DEFAULT_MAXIMUM_TIME);

		reset();
	}

	public String getProviderName() { return "Clock"; }
	public Class getProviderClass() { return Clock.class; }
	public String getProviderId() { return getId(); }

	public ScoreBoard getScoreBoard() { return scoreBoardModel.getScoreBoard(); }
	public ScoreBoardModel getScoreBoardModel() { return scoreBoardModel; }

	public String getId() { return id; }

	public Clock getClock() { return this; }

	public void reset() {
		unstartTime = 0;
		unstopTime = 0;
		unstopLastTime = 0;

		stop();
		setName(getId());

		// We hardcode the assumption that numbers count up.
		setNumber(getMinimumNumber());

		resetTime();
	}

	public String getName() { return name; }
	public void setName(String n) {
		synchronized (nameLock) {
			String last = name;
			name = n;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_NAME, name, last));
		}
	}

	public int getNumber() { return number; }
	public void setNumber(int n) {
		synchronized (numberLock) {
			Integer last = new Integer(number);
			number = checkNewNumber(n);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_NUMBER, new Integer(number), last));
		}
	}
	public void changeNumber(int change) {
		synchronized (numberLock) {
			Integer last = new Integer(number);
			number = checkNewNumber(number + change);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_NUMBER, new Integer(number), last));
		}
	}
	protected int checkNewNumber(int n) {
		if (n < minimumNumber)
			return minimumNumber;
		else if (n > maximumNumber)
			return maximumNumber;
		else
			return n;
	}

	public int getMinimumNumber() { return minimumNumber; }
	public void setMinimumNumber(int n) {
		synchronized (numberLock) {
			Integer last = new Integer(minimumNumber);
			minimumNumber = n;
			if (maximumNumber < minimumNumber)
				setMaximumNumber(minimumNumber);
			if (getNumber() != checkNewNumber(getNumber()))
				setNumber(getNumber());
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_MINIMUM_NUMBER, new Integer(minimumNumber), last));
		}
	}
	public void changeMinimumNumber(int change) {
		synchronized (numberLock) {
			setMinimumNumber(minimumNumber + change);
		}
	}

	public int getMaximumNumber() { return maximumNumber; }
	public void setMaximumNumber(int n) {
		synchronized (numberLock) {
			Integer last = new Integer(maximumNumber);
			if (n < minimumNumber)
				n = minimumNumber;
			maximumNumber = n;
			if (getNumber() != checkNewNumber(getNumber()))
				setNumber(getNumber());
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_MAXIMUM_NUMBER, new Integer(maximumNumber), last));
		}
	}
	public void changeMaximumNumber(int change) {
		synchronized (numberLock) {
			setMaximumNumber(maximumNumber + change);
		}
	}

	public long getTime() { return time; }
	public void setTime(long ms) {
		boolean doStop;
		synchronized (timeLock) {
			Long last = new Long(time);
			if (isRunning() && isSyncTime())
				ms = ((ms / 1000) * 1000) + (time % 1000);
			time = checkNewTime(ms);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIME, new Long(time), last));
			doStop = checkStop();
		}
		if (doStop)
			stop();
	}
	public void changeTime(long change) { _changeTime(change, true); }
	protected void _changeTime(long change, boolean sync) {
		boolean doStop;
		synchronized (timeLock) {
			Long last = new Long(time);
			if (sync && isRunning() && isSyncTime())
				change = ((change / 1000) * 1000);
			time = checkNewTime(time + change);
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_TIME, new Long(time), last));
			doStop = checkStop();
		}
		if (doStop)
			stop();
	}
	public void resetTime() {
		if (isCountDirectionDown())
			setTime(getMaximumTime());
		else
			setTime(getMinimumTime());
	}
	protected long checkNewTime(long ms) {
		if (ms < minimumTime && minimumTime - ms > 500)
			return minimumTime;
		else if (ms > maximumTime && ms - maximumTime > 500)
			return maximumTime;
		else
			return ms;
	}
	protected boolean checkStop() {
		return (getTime() == (isCountDirectionDown() ? getMinimumTime() : getMaximumTime()));
	}

	public long getMinimumTime() { return minimumTime; }
	public void setMinimumTime(long ms) {
		synchronized (timeLock) {
			Long last = new Long(minimumTime);
			minimumTime = ms;
			if (maximumTime < minimumTime)
				setMaximumTime(minimumTime);
			if (getTime() != checkNewTime(getTime()))
				setTime(getTime());
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_MINIMUM_TIME, new Long(minimumTime), last));
		}
	}
	public void changeMinimumTime(long change) {
		synchronized (timeLock) {
			setMinimumTime(minimumTime + change);
		}
	}
	public long getMaximumTime() { return maximumTime; }
	public void setMaximumTime(long ms) {
		synchronized (timeLock) {
			Long last = new Long(maximumTime);
			if (ms < minimumTime)
				ms = minimumTime;
			maximumTime = ms;
			if (getTime() != checkNewTime(getTime()))
				setTime(getTime());
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_MAXIMUM_TIME, new Long(maximumTime), last));
		}
	}
	public void changeMaximumTime(long change) {
		synchronized (timeLock) {
			setMaximumTime(maximumTime + change);
		}
	}
	public boolean isTimeAtStart(long t) {
		if (isCountDirectionDown())
			return t == getMaximumTime();
		else
			return t == getMinimumTime();
	}
	public boolean isTimeAtStart() { return isTimeAtStart(getTime()); }
	public boolean isTimeAtEnd(long t) {
		if (isCountDirectionDown())
			return t == getMinimumTime();
		else
			return t == getMaximumTime();
	}
	public boolean isTimeAtEnd() { return isTimeAtEnd(getTime()); }

	public boolean isCountDirectionDown() { return countDown; }
	public void setCountDirectionDown(boolean down) {
		synchronized (timeLock) {
			Boolean last = new Boolean(countDown);
			countDown = down;
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_DIRECTION, new Boolean(countDown), last));
		}
	}

	public boolean isRunning() { return isRunning; }

	public void start() {
		start(false);
	}
	public void start(boolean quickAdd) {
		synchronized (timeLock) {
			if (isRunning())
				return;

			isRunning = true;
			unstartTime = getTime();

			scoreBoardChange(new ScoreBoardEvent(this, EVENT_RUNNING, Boolean.TRUE, Boolean.FALSE));
			updateClockTimerTask.addClock(this, quickAdd);
		}
	}
	public void stop() {
		synchronized (timeLock) {
			if (!isRunning())
				return;

			isRunning = false;
			updateClockTimerTask.removeClock(this);
			unstopLastTime = lastTime;
			unstopTime = getTime();
			scoreBoardChange(new ScoreBoardEvent(this, EVENT_RUNNING, Boolean.FALSE, Boolean.TRUE));
		}
	}
	public void unstart() {
		synchronized (timeLock) {
			if (!isRunning())
				return;

			stop();
			setTime(unstartTime);
		}
	}
	public void unstop() {
		synchronized (timeLock) {
			if (isRunning())
				return;

			setTime(unstopTime);
			long change = updateClockTimerTask.getCurrentTime() - unstopLastTime;
			changeTime(countDown?-change:change);
			start(true);
		}
	}

	protected void timerTick(long delta) {
		if (!isRunning())
			return;

		lastTime += delta;
		_changeTime(countDown?-delta:delta, false);
	}

	protected boolean isSyncTime() {
		Policy syncPolicy = getScoreBoard().getPolicy(ClockSyncPolicy.ID);
		return (syncPolicy == null ? true : syncPolicy.isEnabled());
	}

	protected ScoreBoardModel scoreBoardModel;

	protected String id;
	protected String name;
	protected int number;
	protected int minimumNumber;
	protected int maximumNumber;
	protected long time;
	protected long minimumTime;
	protected long maximumTime;
	protected boolean countDown;

	protected long lastTime;
	protected boolean isRunning = false;

	protected long unstartTime = 0;
	protected long unstopTime = 0;
	protected long unstopLastTime = 0;

	protected Object nameLock = new Object();
	protected Object numberLock = new Object();
	protected Object timeLock = new Object();

	protected static UpdateClockTimerTask updateClockTimerTask = new UpdateClockTimerTask();

	protected static final long CLOCK_UPDATE_INTERVAL = 200; /* in ms */

	public static final int DEFAULT_MINIMUM_NUMBER = 1;
	public static final int DEFAULT_MAXIMUM_NUMBER = 999;
	public static final long DEFAULT_MINIMUM_TIME = 0;
	public static final long DEFAULT_MAXIMUM_TIME = 3600000;
	public static final boolean DEFAULT_DIRECTION = false;

	protected static class UpdateClockTimerTask extends TimerTask {
		public UpdateClockTimerTask() {
			startSystemTime = System.currentTimeMillis();
			timer.scheduleAtFixedRate(this, DefaultClockModel.CLOCK_UPDATE_INTERVAL / 4, DefaultClockModel.CLOCK_UPDATE_INTERVAL / 4);
		}

		// public void schedule(TimerTask task, long delay) {
		// 	timer.schedule(task, delay);
		// }

		public void addClock(DefaultClockModel c, boolean quickAdd) {
			synchronized (clockLock) {
				long delayStartTime = 0;
				if (c.isSyncTime() && !quickAdd) {
					// This syncs all the clocks to change second at the same time
					long timeMs = c.unstartTime % 1000;
					long nowMs = currentTime % 1000;
					if (c.countDown)
						timeMs = (1000 - timeMs) % 1000;
					long delay = timeMs - nowMs;
					if (Math.abs(delay) >= 500)
						delay = (long)(Math.signum((float)-delay) * (1000 - Math.abs(delay)));
					c.lastTime = currentTime;
					if (c.countDown)
						delay = -delay;
					c.time = c.unstartTime - delay;
				} else {
					c.lastTime = currentTime;
				}
				clocks.add(c);
			}
		}

		public void removeClock(DefaultClockModel c) {
			synchronized (clockLock) {
				clocks.remove(c);
			}
		}

		private void tick() {
			Iterator<DefaultClockModel> i;
			ArrayList<DefaultClockModel> clocks;
			synchronized (clockLock) {
				currentTime += DefaultClockModel.CLOCK_UPDATE_INTERVAL;
				clocks = new ArrayList<DefaultClockModel>(this.clocks);
			}
			DefaultClockModel clock;
			i = clocks.iterator();
			while (i.hasNext()) {
				clock = i.next();
				clock.requestBatchStart();
			}
			i = clocks.iterator();
			while (i.hasNext()) {
				clock = i.next();
				clock.timerTick(DefaultClockModel.CLOCK_UPDATE_INTERVAL);
			}
			i = clocks.iterator();
			while (i.hasNext()) {
				clock = i.next();
				clock.requestBatchEnd();
			}
		}

		public void run() {
			long curSystemTime = System.currentTimeMillis();
			long curTicks = (curSystemTime - startSystemTime) / DefaultClockModel.CLOCK_UPDATE_INTERVAL;
			while (curTicks != ticks) {
				ScoreBoardManager.printMessage("tick " + curTicks + " " + ticks);
				ticks++;
				tick();
			}
		}

		public long getCurrentTime() {
			return currentTime;
		}

		private long currentTime = 0;
		private long startSystemTime = 0;
		private long ticks = 0;
		protected static Timer timer = new Timer();
		protected Object clockLock = new Object();
		ArrayList<DefaultClockModel> clocks = new ArrayList<DefaultClockModel>();
	}
}
