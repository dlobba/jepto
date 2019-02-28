package com.ds2.jepto.actors;

import akka.actor.ActorRef;

public class Event {
	
	enum Action {
		DO,
		DONT
	}
	
	private int id;
	private ActorRef source;
	private long timestamp;
	private int  ttl;
	private final Action action;
	
	/**
	 * Initialise a new event with ttl set to 0.
	 * 
	 * @param source
	 * @param timestamp
	 */
	public Event(int id, Action action) {
		super();
		this.id = id;
		this.action = action;
		this.source = null;
		this.timestamp = 0;
		this.ttl = 0;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public ActorRef getSource() {
		return source;
	}
	
	public int getId() {
		return id;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public Action getAction() {
		return action;
	}

	public void setSource(ActorRef source) {
		this.source = source;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	@Override
	public String toString() {
		return "Event [timestamp=" + timestamp +
				", source=" + source.path().name() +
				", id=" + id +
				", action=" + action + "]";
	}
}
