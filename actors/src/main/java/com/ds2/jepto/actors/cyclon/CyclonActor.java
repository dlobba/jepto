package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import scala.concurrent.duration.Duration;

public class CyclonActor extends AbstractActor {
	
	public static class AgingMsg implements Serializable {};

	private long seed;
	private long maxAge;
	private long agingPeriod;
	private int cacheSize;
	
	// describes the number of nodes selected,
	// from the cache, in the shuffling process
	private int shuffleLength;
	
	/*
	 * BEWARE:
	 * Check, in case of problems, the hashing
	 * function on ActorRef
	 */
	private Map<ActorRef, Long> cache;
	
	public CyclonActor(int cacheSize, int shuffleLength, long maxAge, long agingPeriod, long seed) {
		super();
		this.cacheSize = cacheSize;
		this.shuffleLength = shuffleLength;
		this.maxAge = maxAge;
		this.agingPeriod = agingPeriod;
		this.seed = seed;
		this.cache = new HashMap<ActorRef, Long>();
	}


	private void onJoinMsg(JoinMsg msg) {
		cacheAdd(msg.getTracker(), 0);
	}
	
	private void cacheAdd(ActorRef actor, long age) {
		synchronized (this.cache) {
			if (cache.containsKey(actor)) {
				if (cache.get(actor) > age)
					cache.put(actor, age);
				return;
			}
			cache.put(actor, age);
			if (cache.size() <= this.cacheSize)
				return;
			
			long max=-1l;
			ActorRef old = null;
			for (ActorRef tmp : cache.keySet()) {
				if (cache.get(tmp) > max) {
					max = cache.get(tmp);
					old = tmp;
				}
			}
			cache.remove(old);
		}
	}
	
	private void cacheAddAll(Map<ActorRef, Long> other) {
		synchronized (this.cache) {
			
			long age;
			for (ActorRef actor : other.keySet()) {
				age = other.get(actor);
				if (cache.containsKey(actor)) {
					if (cache.get(actor) > age)
						cache.put(actor, age);
				} else {
					cache.put(actor, age);
				}
			}
			while (cache.size() > this.cacheSize) {
				long max=-1l;
				ActorRef old = null;
				for (ActorRef tmp : cache.keySet()) {
					if (cache.get(tmp) > max) {
						max = cache.get(tmp);
						old = tmp;
					}
				}
				cache.remove(old);
			}
		}
	}
	
	private void onAgingMsg(AgingMsg msg) {
		synchronized (cache) {
			for (ActorRef actor : cache.keySet()) {
				cache.put(actor, cache.get(actor)+1);
			}
		}
		sendAgingMsg();
	}

	private void sendAgingMsg() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(this.agingPeriod,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new AgingMsg(),
				getContext().system().dispatcher(),
				this.getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(JoinMsg.class, this::onJoinMsg)
				.match(AgingMsg.class, this::onAgingMsg)
				.build();
	}
}
