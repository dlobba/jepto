package com.ds2.jepto.actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.ds2.jepto.actors.cyclon.CyclonActor;

import akka.actor.ActorRef;
import scala.concurrent.duration.Duration;

public class EptoActor extends CyclonActor {

	public static class RoundMsg implements Serializable {};
	
	
	public final long MAX_TTL;
	public final int  NUM_RECEIVERS; // it's the K in the paper
	
	private final long SEED;
	// private int  id; USE THE ACTOR REF REFERENCE!
	private long roundInterval;
	private long clock; // logical clock
	private int  nextEventId;

	private Ball nextBall;
	
	public EptoActor(
			long max_ttl,
			int numReceivers,
			long roundInterval,
			int viewSize,
			int shuffleLength,
			long shufflePeriod,
			long seed) {
		super(viewSize, shuffleLength, shufflePeriod, seed);
		this.roundInterval = roundInterval;
		this.clock = 0;
		this.nextEventId = 0;
		this.nextBall = new Ball();
		this.NUM_RECEIVERS = numReceivers;
		this.MAX_TTL = max_ttl;
		this.SEED = seed;
		sendRoundMsg(); // start first round
	}


	public Map<ActorRef, Long> getView() {
		return this.getCache();
	}
	
/*---------------------------------------------------------------------------*/
/*                            LOGICAL CLOCK HANDLING                         */
/*---------------------------------------------------------------------------*/
/**
 * Return true if the event has a ttl greater
 * than MAX_TTL.
 * 
 * @param event
 * @return
 */
private boolean isDeliverable(Event event) {
	return event.getTtl() > this.MAX_TTL;
}

private long getClock() {
	return ++this.clock;
}

private void updateClock(long newClock) {
	if (newClock > this.clock)
		this.clock = newClock;
}
/*---------------------------------------------------------------------------*/
/*                         EpTO: DISSEMINATION COMPONENT                     */
/*---------------------------------------------------------------------------*/
/**
 * Insert an event into the next ball of the current process.
 * 
 * @param event
 */
private void eptoBroadcast(Event event) {
	event.setTimestamp(this.getClock());
	event.setTtl(0);
	event.setSource(this.getSelf());
	synchronized (nextBall) {
		this.nextBall.insert(event);
	}
}

private void onBallMsg(BallMsg msg) {
	List<Event> events = msg.getBall();
	Event tmp;
	for (Event event : events) {
		if (event.getTtl() < this.MAX_TTL) {
			tmp = this.nextBall.get(event); 
			if (tmp != null) {
				if (tmp.getTtl() < event.getTtl()) {
					this.nextBall.update(event);
				}
			} else {
				this.nextBall.insert(event);
			}
		}
		this.updateClock(event.getTimestamp());
	}
}

private void onRoundMsg(RoundMsg msg) {
	this.nextBall.incrementTtl();
	if (!this.nextBall.isEmpty()) {
		List<ActorRef> peers = getPeers();
		BallMsg ballMsg = new BallMsg(nextBall);
		for (ActorRef peer : peers) {
			peer.tell(ballMsg, this.getSelf());
		}
		// TODO: TODO continue
	}
	
	sendRoundMsg();
}
/*---------------------------------------------------------------------------*/

private List<ActorRef> getPeers() {
	Map<ActorRef, Long> view = this.getView();
	Random rnd = new Random(this.SEED);
	List<ActorRef> listActors = new ArrayList<ActorRef>(view.keySet());
	Collections.shuffle(listActors, rnd);
	return listActors.subList(0, NUM_RECEIVERS);
}

/*---------------------------------------------------------------------------*/




private void sendRoundMsg() {
	this.getContext()
	.getSystem()
	.scheduler()
	.scheduleOnce(Duration.create(this.roundInterval,
			TimeUnit.MILLISECONDS),
			this.getSelf(),
			new RoundMsg(),
			getContext().system().dispatcher(),
			this.getSelf());
}


@Override
public Receive createReceive() {
	return super.createBuilder()
			.match(BallMsg.class, this::onBallMsg)
			.match(RoundMsg.class, this::onRoundMsg)
			.build();
}
}
