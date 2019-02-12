package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import akka.actor.ActorRef;

public class CyclonShufflingMsg implements Serializable {

	private Map<ActorRef, Long> updatingCache;
	  private ActorRef sender;
	  private ActorRef receiver;
	  private int id;

	  public CyclonShufflingMsg(int msgId, Map<ActorRef, Long> updatingCache, ActorRef sender, ActorRef receiver) {
		  super();
		  this.id = msgId;
		  this.updatingCache = Collections.unmodifiableMap(updatingCache);
		  this.sender = sender;
		  this.receiver = receiver;
	  }

	  public Map<ActorRef, Long> getUpdatingCache() {
		  return updatingCache;
	  }

	  public ActorRef getSender() {
		  return sender;
	  }
	  
	  public ActorRef getReceiver() {
		  return receiver;
	  }
	  
	  public int getId() {
		  return id;
	  }
	
}
