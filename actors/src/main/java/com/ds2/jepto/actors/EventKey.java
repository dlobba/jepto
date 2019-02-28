package com.ds2.jepto.actors;

import akka.actor.ActorRef;

public class EventKey {
	private final ActorRef peer;
	private final int eventId;
	
	public EventKey(ActorRef peer, int eventId) {
		super();
		this.peer = peer;
		this.eventId = eventId;
	}
	
	public EventKey(Event event) {
		this(event.getSource(), event.getId());
	}
	
	public ActorRef getPeer() {
		return peer;
	}
	public int getEventId() {
		return eventId;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + eventId;
		result = prime * result + ((peer == null) ? 0 : peer.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventKey other = (EventKey) obj;
		if (eventId != other.eventId)
			return false;
		if (peer == null) {
			if (other.peer != null)
				return false;
		} else if (!peer.equals(other.peer))
			return false;
		return true;
	}
}
