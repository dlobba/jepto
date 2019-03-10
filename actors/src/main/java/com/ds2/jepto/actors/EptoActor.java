package com.ds2.jepto.actors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ds2.jepto.actors.Event.Action;
import com.ds2.jepto.actors.cyclon.CyclonActor;

import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

public class EptoActor extends CyclonActor {

	private static final Logger LOGGER = Logger.getLogger(EptoActor.class.getName());

	public static class RoundMsg implements Serializable {};
	public static class GenEventMsg implements Serializable {};

	public final long MAX_TTL;
	public final int  NUM_RECEIVERS; // it's the K in the paper

	private final long SEED;
	// private int  id; USE THE ACTOR REF REFERENCE!
	private long roundInterval;
	private long genEventInterval;

	private AtomicLong     clock; // logical clock
	private AtomicInteger  nextEventId;

	private Ball nextBall;
	private EventMap received;       // events received but not yet delivered
	private Set<EventKey> delivered; // delivered events
	private long lastDeliveredTs;    // maximum ts of delivered events

	public EptoActor(
			long max_ttl,
			int numReceivers,
			long roundInterval,
			int viewSize,
			int shuffleLength,
			long shufflePeriod,
			long seed) {
		super(viewSize, shuffleLength, shufflePeriod, seed);
		this.roundInterval    = roundInterval;
		this.genEventInterval = roundInterval;
		this.clock = new AtomicLong(0);
		this.nextEventId = new AtomicInteger(0);
		this.nextBall = new Ball();
		this.NUM_RECEIVERS = numReceivers;
		this.MAX_TTL = max_ttl;
		this.SEED = seed;
		this.received = new EventMap();
		this.delivered = new HashSet<>();
		this.lastDeliveredTs = 0l;
		sendRoundMsg(); // start first round
		sendGenEventMsg();
	}

	public static Props props(
			long max_ttl,
			int numReceivers,
			long roundInterval,
			int viewSize,
			int shuffleLength,
			long shufflePeriod,
			long seed)
	{
		return Props.create(EptoActor.class,
				() -> new EptoActor(max_ttl,
						numReceivers,
						roundInterval,
						viewSize,
						shuffleLength,
						shufflePeriod,
						seed));
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
		return this.clock.incrementAndGet();
	}

	private void updateClock(long newClock) {
		synchronized (clock) {
			if (newClock > this.clock.get())
				this.clock.set(newClock);
		}
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
		LOGGER.log(Level.INFO,
				"EpTO: {0} broadcast {1}",
				new Object[] {
						this.getSelf().path().name(),
						event.toString()});
	}

	private void onBallMsg(BallMsg msg) {
		LOGGER.log(Level.INFO,
				"EpTO: {0} received_ball_from {1}",
				new Object[] {
						this.getSelf().path().name(),
						this.getSender().path().name()});
//		System.out.printf("Actor %s received a ball from %s\n",
//				this.getSelf().path().name(),
//				this.getSender().path().name());
		synchronized (this.nextBall) {
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
	}

	private void onRoundMsg(RoundMsg msg) {
		Ball ball;
		String arrayString = "";
		synchronized (this.nextBall) {
			this.nextBall.incrementTtl();
			ball = this.nextBall.clone();
			this.nextBall.clear();
		}
		if (!ball.isEmpty()) {
			List<ActorRef> peers = getPeers();
			BallMsg ballMsg = new BallMsg(ball);
			for (ActorRef peer : peers) {
				peer.tell(ballMsg, this.getSelf());
			}
			arrayString = String.join(", ",
					peers.stream()
					.map(peer -> peer.path().name())
					.collect(Collectors.toList()));
		}
		LOGGER.log(Level.INFO,
				"EpTO: {0} sent_ball_to {1}",
				new Object[] {
						this.getSelf().path().name(),
						arrayString});
		this.orderEvents(ball);
		sendRoundMsg();
	}
/*---------------------------------------------------------------------------*/
/*                         EpTO: ORDERING COMPONENT                          */
/*---------------------------------------------------------------------------*/
	public void orderEvents(Ball ball) {
		// received and delivered are used only within this
		// method. Hence no concurrent access should be
		// of concern (hopefully)
		received.incrementTtl();
		for (Event event : ball.toList()) {
			if (!this.delivered.contains(new EventKey(event)) &&
					event.getTimestamp() >= this.lastDeliveredTs) {
				if (this.received.contains(event)) {
					if (this.received.get(event).getTtl() < event.getTtl()) {
						this.received.update(event);
					}
				} else {
					this.received.insert(event);
				}
			}
		}

		long minTs = Long.MAX_VALUE;
		EventMap deliverable = new EventMap();
		for (Event event : this.received.toList()) {
			if (isDeliverable(event)) {
				deliverable.insert(event);
			} else if (minTs > event.getTimestamp()) {
				minTs = event.getTimestamp();
			}
		}
		for (Event event : deliverable.toList()) {
			// TODO:HERE: changed from > to >= wrt the original paper
			if (event.getTimestamp() >= minTs) {
				deliverable.remove(event);
			} else {
				received.remove(event);
			}
			// END:TODO:HERE:
		}
		for (Event event : deliverable.toSortedList()) {
			this.delivered.add(new EventKey(event));
			this.lastDeliveredTs = event.getTimestamp();
			this.deliver(event);
		}

	}
/*---------------------------------------------------------------------------*/

	private List<ActorRef> getPeers() {
		Map<ActorRef, Long> view = this.getView();
		Random rnd = new Random(this.SEED);
		List<ActorRef> listActors = new ArrayList<ActorRef>(view.keySet());
		Collections.shuffle(listActors, rnd);
		int size = Integer.min(listActors.size(), NUM_RECEIVERS);
		return listActors.subList(0, size);
	}

	private void deliver(Event event) {
		LOGGER.log(Level.INFO,
				"EpTO: {0} delivered {1}",
				new Object[] {
						this.getSelf().path().name(),
						event.toString()});
	}

	private void onGenEventMsg(GenEventMsg msg) {
		Event newEvent = new Event(
				nextEventId.getAndIncrement(),
				Action.values()[new Random(this.SEED)
				                .nextInt(Action.values().length)]);
		this.eptoBroadcast(newEvent);
//		LOGGER.log(Level.INFO,
//				"Actor {0} generated {1}",
//				new Object[] {
//						this.getSelf().path().name(),
//						newEvent.toString()});
		sendGenEventMsg();
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

	private void sendGenEventMsg() {
		this.getContext()
		.getSystem()
		.scheduler()
		.scheduleOnce(Duration.create(
				(new Random(this.SEED).nextLong() % this.genEventInterval) + 10000,
				TimeUnit.MILLISECONDS),
				this.getSelf(),
				new GenEventMsg(),
				getContext().system().dispatcher(),
				this.getSelf());
	}

	@Override
	public Receive createReceive() {
		return super.createBuilder()
				.match(BallMsg.class, this::onBallMsg)
				.match(RoundMsg.class, this::onRoundMsg)
				.match(GenEventMsg.class, this::onGenEventMsg)
				.build();
	}
}
