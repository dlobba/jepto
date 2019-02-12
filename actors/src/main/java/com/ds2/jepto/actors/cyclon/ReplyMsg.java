package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;
import java.util.Map;

import akka.actor.ActorRef;

public class ReplyMsg extends CyclonShufflingMsg implements Serializable {

	public ReplyMsg(int msgId, Map<ActorRef, Long> updatingCache, ActorRef sender, ActorRef receiver) {
		super(msgId, updatingCache, sender, receiver);
	}
}
