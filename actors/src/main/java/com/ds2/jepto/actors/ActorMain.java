package com.ds2.jepto.actors;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ds2.jepto.actors.cyclon.JoinMsg;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class ActorMain {

	private static final Logger LOGGER = Logger.getLogger(App.class.getName());
	
	private static final String SYSTEM_NAME = "epto";
	private static long  SEED = 42;
	
	private static int  viewSize      = 10;
	private static int  shuffleLength = 3;
	private static int 	numReceivers  = 3;
	private static long max_ttl       = 5;
	private static long roundInterval = 5000l;
	private static long shufflePeriod = 3000l;
	
	private static boolean isTracker(Config config) {
		if (!config.hasPath("participant.is_tracker"))
            return false;
        if (!config.hasPath("participant.id"))
        	return false;
		return true;
	}
	
	private static boolean isPeer(Config config) {
		if (!config.hasPath("participant.tracker_address"))
            return false;
		if (!config.hasPath("participant.tracker_id"))
			return false;
        if (!config.hasPath("participant.id"))
        	return false;
		return true;
		
	}


	public static void main(String[] args) {
		// config.resource is a default property define by the typecase
		// library
		String actorConfigFile = System.getProperty("config.resource");
		if (actorConfigFile == null) {
			LOGGER.log(Level.SEVERE, "No akka config resource defined." +
					" TERMINATING...");
			System.exit(-1);
		}
		Config actorConfig = ConfigFactory.load();
		
        ActorSystem system = ActorSystem.create(SYSTEM_NAME, actorConfig);
        ActorRef    actor;
        ActorRef    tracker = null;
		if (isTracker(actorConfig)) {
			// Actor init as tracker
			String  participantId = actorConfig.getString("participant.id");
			actor = system.actorOf(EptoActor.props(max_ttl,
	        		numReceivers,
	        		roundInterval,
	        		viewSize,
	        		shuffleLength,
	        		shufflePeriod,
	        		SEED),
	        		participantId);
			LOGGER.log(Level.INFO, "Tracker {0} started.",
					actor.path().name());
		} else if (isPeer(actorConfig)) {
			// Actor init as normal peer
			String  trackerAddress = actorConfig.getString("participant.tracker_address");
			String  trackerId = actorConfig.getString("participant.tracker_id");
	        String  participantId = actorConfig.getString("participant.id");
	        String  trackerPath = "akka.tcp://" + SYSTEM_NAME + "@" +
	        		trackerAddress + "/user/" + trackerId;

	        actor = system.actorOf(EptoActor.props(max_ttl,
	        		numReceivers,
	        		roundInterval,
	        		viewSize,
	        		shuffleLength,
	        		shufflePeriod,
	        		SEED),
	        		participantId);
			// Let the node send a join request
			// to the tracker it knows
	        ActorSelection trackerSelection = system.actorSelection(trackerPath);

	        try {
				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				Future<Object> future = Patterns.ask(trackerSelection, new Identify(""), timeout);
				ActorIdentity reply = (ActorIdentity) Await.result(future, timeout.duration());
				tracker = reply.ref().get();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "The tracker requested didn't reply." +
						" TERMINATING...");
				System.exit(-1);
			}
	        
	        actor.tell(new JoinMsg(tracker), null);
		} else {
			// Invalid settings, terminate
			LOGGER.log(Level.SEVERE, "Invalid actor config properties given." +
					" TERMINATING...");
			System.exit(-1);
		}
	}
}
