package com.ds2.jepto.actors.cyclon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

public class CyclonActor extends AbstractActor {
	
	public static class AgingMsg implements Serializable {};
	public static class DebugMsg implements Serializable {};
	private static class ShufflingMsg implements Serializable {};

	private long seed;
	private long maxAge;
	private long shufflePeriod;
	private int  cacheSize;
	private int msgId;
	
	private CyclonShufflingMsg buffer;
	
	
	// describes the number of nodes selected,
	// from the cache, in the shuffling process
	private int shuffleLength;
	
	/*
	 * BEWARE:
	 * Check, in case of problems, the hashing
	 * function on ActorRef
	 */
	private Map<ActorRef, Long> cache;
	
	public CyclonActor(int cacheSize, int shuffleLength, long maxAge, long shufflePeriod, long seed) {
		super();
		this.cacheSize = cacheSize;
		this.shuffleLength = shuffleLength;
		this.maxAge = maxAge;
		this.shufflePeriod = shufflePeriod;
		this.seed = seed;
		this.cache = new HashMap<ActorRef, Long>();
		this.buffer = null;
		this.msgId = 0;
	}

	public static Props props(int cacheSize, int shuffleLength, long maxAge, long shufflePeriod, long seed) {
		return Props.create(CyclonActor.class,
				() -> new CyclonActor(cacheSize, shuffleLength, maxAge, shufflePeriod, seed));
	}

	private void onJoinMsg(JoinMsg msg) {
		if (cache.size() < this.cacheSize)
			cache.put(msg.getTracker(), 0l);
	}
	/**
	 * 
	 * Given entries coming from Q, remove reference
	 * to P and try to accomodate every received entry.
	 * If space doesn't allow so, replace new entry with
	 * the one previously sent.
	 * 
	 * If those entries are no more in the cache, block
	 * the merge process and return.
	 * 
	 * @param other
	 * @param shuffledElements
	 */
	private void merge(Map<ActorRef, Long> other, Set<ActorRef> shuffledElements) {
		synchronized (this.cache) {
			Map<ActorRef, Long> other2 = new HashMap<ActorRef, Long>(other);
			// remove reference to P
			if (other.containsKey(this.getSelf()))
				other2.remove(this.getSelf());
			for (ActorRef actor : other.keySet()) {
				if (cache.containsKey(actor)) {
					other2.remove(actor);
				}
			}
			Iterator<ActorRef> shuffledIter = shuffledElements.iterator();
			ActorRef tmp = shuffledIter.next();
			for (ActorRef newActor : other2.keySet()) {
				if (cache.size() <= this.cacheSize ) {
					cache.put(newActor, other2.get(newActor));
				} else {
					// replace previously sent entry with new one
					if (cache.containsKey(tmp)) {
						cache.remove(tmp);
						cache.put(newActor, other2.get(newActor));
						
						if (shuffledIter.hasNext())
							tmp = shuffledIter.next();
						else
							break;
					}
				}
			}
		}
	}
	
	private ActorRef selectNeighbour() {
		long max = -1l;
		ActorRef old = null;
		for (ActorRef tmp : cache.keySet()) {
			if (cache.get(tmp) > max) {
				max = cache.get(tmp);
				old = tmp;
			}
		}
		return old;
	}
	
	/*
	 * copyCache is changed!
	 */
	private Map<ActorRef, Long> selectOthersFrom(int numOthers, Map<ActorRef, Long> copyCache) {
		Random rnd = new Random(this.seed);
		
		List<ActorRef> listActors = new ArrayList<ActorRef>(copyCache.keySet());
		Collections.shuffle(listActors, rnd);
		int index = 0;
		while (copyCache.size() > numOthers)
			copyCache.remove(listActors.get(index++));
		return copyCache;
	}

	private void increaseAge() {
		synchronized (cache) {
			for (ActorRef actor : cache.keySet()) {
				cache.put(actor, cache.get(actor)+1);
			}
		}
	}
	
	// periodic operations ----------------------
	

	private void onShufflingMsg(ShufflingMsg msg) {
		if (this.buffer != null) {
			// timeout occurs, remove previous Q
			cache.remove(buffer.getReceiver());
		}
		synchronized (cache) {
			increaseAge();
			ActorRef q = selectNeighbour();
			Map<ActorRef, Long> copyCache = new HashMap<>(this.cache);
			copyCache.remove(q);
			copyCache = selectOthersFrom(this.shuffleLength - 1, copyCache);
			copyCache.put(q, cache.get(q)); // owltrick
			this.buffer = new CyclonShufflingMsg(msgId, copyCache, this.getSelf(), q);
			copyCache.remove(q);
			copyCache.put(this.getSelf(), 0l);
			RequestMsg r = new RequestMsg(msgId++, copyCache, this.getSelf(), q);
			q.tell(r, this.getSelf());
		}
		sendShufflingMsg();
	}
	
	private void onRequestMsg(RequestMsg msg) {
		Map<ActorRef, Long> copyCache = new HashMap<>(this.cache);
		copyCache = selectOthersFrom(this.shuffleLength, copyCache);
		ReplyMsg reply = new ReplyMsg(msg.getId(), copyCache, this.getSelf(), this.getSender());
		this.getSender().tell(reply, this.getSelf());
		merge(msg.getUpdatingCache(), reply.getUpdatingCache().keySet());
	}
	
	private void onReplyMsg(ReplyMsg msg) {
		synchronized (cache) {
			if (msg.getId() == this.msgId - 1) {
				merge(msg.getUpdatingCache(), buffer.getUpdatingCache().keySet());
				buffer = null;
			} else {
				merge(msg.getUpdatingCache(), new HashSet<ActorRef>());
			}
		}
	}
	
	private void sendShufflingMsg() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(this.shufflePeriod,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new ShufflingMsg(),
				getContext().system().dispatcher(),
				this.getSelf());
	}
	
	// debug ------------------------------------
	
	private String printCache() {
		StringBuilder str = new StringBuilder();
		str.append("id,age\n");
		for (ActorRef actor : cache.keySet()) {
			str.append(actor.path().name() + "," + cache.get(actor) + "\n");
		}
		return str.toString();
	}
	
	private void onDebugMsg(DebugMsg msg) {
		String dbg = this.self().path().name() + "\n" + printCache();
		System.out.println(dbg);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(JoinMsg.class, this::onJoinMsg)
				.match(DebugMsg.class, this::onDebugMsg)
				.match(ShufflingMsg.class, this::onShufflingMsg)
				.match(RequestMsg.class, this::onRequestMsg)
				.match(ReplyMsg.class, this::onReplyMsg)
				.build();
	}
}
