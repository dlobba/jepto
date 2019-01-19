package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;

import akka.actor.ActorRef;

/**
 * The message is sent by a superior
 * entity, which informs a new node of
 * a possible tracker node, already inside
 * the network.
 *
 */
public class JoinMsg implements Serializable {
	private ActorRef tracker;

	public JoinMsg(ActorRef tracker) {
		this.tracker = tracker;
	}

	public ActorRef getTracker() {
		return tracker;
	}
}
