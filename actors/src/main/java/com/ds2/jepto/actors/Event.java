package com.ds2.jepto.actors;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Event implements Serializable {

	enum Action {
		DO,
		DONT
	}

	private int id;
	private ActorRef source;
	private long timestamp;
	private int  ttl;
	private final Action action;

	private Event(int id,
			Action action,
			ActorRef source,
			long timestamp,
			int ttl) {
		super();
		this.id = id;
		this.action = action;
		this.source = source;
		this.timestamp = timestamp;
		this.ttl = ttl;
	}

	/**
	 * Initialise a new event with ttl set to 0.
	 *
	 * @param source
	 * @param timestamp
	 */
	public Event(int id, Action action) {
		this(id, action, null, 0l, 0);
	}

	public Event(Event event) {
		this(event.id,
			event.action,
			event.source,
			event.timestamp,
			event.ttl);
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
				", action=" + action +
				", ttl=" + ttl + "]";
	}
}
