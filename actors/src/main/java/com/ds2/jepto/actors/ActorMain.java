package com.ds2.jepto.actors;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ds2.jepto.actors.cyclon.JoinMsg;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

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

	public static class EptoInputException extends Exception {
		public EptoInputException() {
			super();
		}
		public EptoInputException(String message, Throwable cause, boolean enableSuppression,
				boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
		public EptoInputException(String message, Throwable cause) {
			super(message, cause);
		}
		public EptoInputException(String message) {
			super(message);
		}
		public EptoInputException(Throwable cause) {
			super(cause);
		}
	}

	public static void setRunParameters(Config config) throws EptoInputException {
		String parameters[] = new String[] {
				"jepto.config.cyclon.view-size",
				"jepto.config.cyclon.shuffle-length",
				"jepto.config.cyclon.shuffle-period-millis",
				"jepto.config.num-receivers",
				"jepto.config.max-ttl",
				"jepto.config.round-interval",
				"jepto.config.as-paper"};
		for (String param : parameters) {
			if (config.hasPath(param) == false) {
				throw new EptoInputException("No parameter " +
						param +
						" found in the run configuration file.");
			}
		}
		viewSize      = Integer.parseUnsignedInt(config.getString(parameters[0]));
		shuffleLength = Integer.parseUnsignedInt(config.getString(parameters[1]));
		shufflePeriod = Long.parseUnsignedLong(config.getString(parameters[2]));
		numReceivers  = Integer.parseUnsignedInt(config.getString(parameters[3]));
		maxTtl        = Long.parseUnsignedLong(config.getString(parameters[4]));
		roundInterval = Long.parseUnsignedLong(config.getString(parameters[5]));
		asPaper       = Boolean.parseBoolean(config.getString(parameters[7]));
		if (config.hasPath("jepto.config.log-level"))
			logLevel = DebugLevel.parse(config.getString("jepto.config.log-level"));
	}

	public static void printRunParameters() {
		StringBuilder str = new StringBuilder();
		str.append("Seed:\t\t\t" + SEED + "\n");
		str.append("Max ttl:\t\t" + maxTtl + "\n");
		str.append("Max ttl:\t\t" + maxTtl + "\n");
		str.append("Num receivers (K):\t" + numReceivers + "\n");
		str.append("Round interval:\t\t" + roundInterval + "\n");
		str.append("As paper:\t\t" + asPaper + "\n");
		str.append("Cyclon view size:\t" + viewSize + "\n");
		str.append("Cyclon shuffle length:\t"+ shuffleLength + "\n");
		str.append("Cyclon shuffle period:\t" + shufflePeriod + "\n");
		LOGGER.log(Level.INFO, "Run parameters\n" + str.toString());
	}

	// the logger to be recorder to file is the one related to
	// EptoActor, not to ActorMain
	private static final Logger LOGGER = Logger.getLogger(EptoActor.class.getName());
	private static FileHandler  loggerFileHandler;

	private static final String SYSTEM_NAME = "epto";
	private static long  SEED = 42;

	private static int  viewSize      = 10;
	private static int  shuffleLength = 3;
	private static int 	numReceivers  = 3;
	private static long maxTtl       = 5;
	private static long roundInterval = 5000l;
	private static long shufflePeriod = 3000l;

	private static Level logLevel = Level.INFO;
	private static boolean asPaper    = false;

	private static void createActorLogFile(String actorName, Level level) {
		try {
			// create a specific log for the actor
			String path = System.getProperty("user.home") + File.separator
					+ "EpTOlogs" + File.separator + actorName + ".log";
			File targetFile = new File(path);
			File parent = targetFile.getParentFile();
			if (!parent.mkdirs() && !parent.exists()) {
			    throw new IllegalStateException("Couldn't create dir: " + parent);
			}
			loggerFileHandler = new FileHandler(path);
			LOGGER.addHandler(loggerFileHandler);
			SimpleFormatter sf = new SimpleFormatter();
			loggerFileHandler.setFormatter(sf);
			// change level for the handlers and the logger
			LOGGER.setLevel(level);
			for (Handler handler: LOGGER.getHandlers()) {
				handler.setLevel(level);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
/*---------------------------------------------------------------------------*/
/*                              MAIN                                         */
/*---------------------------------------------------------------------------*/
	public static void main(String[] args) throws EptoInputException {
		// config.resource is a default property defined by the typecase
		// library
		String actorConfigFile        = System.getProperty("config.resource");
		String defaultActorConfigFile = System.getProperty("config.resource.default");
		String inputActorId           = System.getProperty("peer");
		String inputPortNumber        = System.getProperty("port");
		String seed                   = System.getProperty("seed");

		String asPaperStr = System.getProperty("as.paper");
		String asPaperDefault = System.getProperty("as.paper.default");
		if (asPaperStr != null) {
			asPaper = Boolean.parseBoolean(asPaperStr);
			if (asPaper)
				LOGGER.log(Level.INFO, "Starting simulation as described in the paper.");
		}
		if (actorConfigFile == null && defaultActorConfigFile == null) {
			throw new EptoInputException("No akka config resource defined");
		}
		Config actorConfig;
		if (actorConfigFile != null) {
			actorConfig = ConfigFactory.load(actorConfigFile);
		} else {
			// load default config file and set peer id and port number
			// given by input
			actorConfig = ConfigFactory.load(defaultActorConfigFile);
			if (inputActorId == null || inputPortNumber == null) {
				throw new EptoInputException("No actor name or port number"+
						" defined for custom actor");
			}
			actorConfig = actorConfig.withValue("akka.remote.netty.tcp.port",
					ConfigValueFactory.fromAnyRef(
							Integer.parseUnsignedInt(inputPortNumber)));
			actorConfig = actorConfig.withValue("participant.id",
					ConfigValueFactory.fromAnyRef(inputActorId));
			if (seed != null) {
				SEED = Integer.parseInt(seed);
			}
		}
		EptoMain.setRunParameters(actorConfig);

		ActorSystem system;
        ActorRef    actor;
        ActorRef    tracker = null;
		if (isTracker(actorConfig)) {
			// Actor init as tracker
			String  participantId = actorConfig.getString("participant.id");
			system = ActorSystem.create(SYSTEM_NAME, actorConfig);
			actor = system.actorOf(EptoActor.props(maxTtl,
	        		numReceivers,
	        		roundInterval,
	        		viewSize,
	        		shuffleLength,
	        		shufflePeriod,
	        		SEED,
	        		asPaper,
	        		null),
	        		participantId);
			createActorLogFile(participantId, logLevel);
			LOGGER.log(Level.INFO, "Tracker {0} started.",
					actor.path().name());
		} else if (isPeer(actorConfig)) {

			// Actor init as normal peer
			String trackerAddress = actorConfig
					.getString("participant.tracker_address");
			String trackerId      = actorConfig
					.getString("participant.tracker_id");
	        String participantId  = actorConfig.getString("participant.id");

	        String trackerPath    = "akka.tcp://" + SYSTEM_NAME + "@" +
	        		trackerAddress + "/user/" + trackerId;

	        system = ActorSystem.create(SYSTEM_NAME, actorConfig);
	        actor  = system.actorOf(EptoActor.props(maxTtl,
	        		numReceivers,
	        		roundInterval,
	        		viewSize,
	        		shuffleLength,
	        		shufflePeriod,
	        		SEED,
	        		asPaper,
	        		null),
	        		participantId);

			// Let the node send a join request
			// to the tracker it knows.
	        // First, retrieve the tracker actor reference
	        ActorSelection trackerSelection = system.actorSelection(trackerPath);
	        try {
				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				Future<Object> future = Patterns.ask(trackerSelection,
						new Identify(""), timeout);
				ActorIdentity reply =
						(ActorIdentity) Await.result(future, timeout.duration());
				tracker = reply.ref().get();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "The requested tracker didn't reply." +
						" TERMINATING...");
				System.exit(-1);
			}
	        createActorLogFile(participantId, logLevel);
	        LOGGER.log(Level.INFO, "Peer {0} started.",
					actor.path().name());
	        actor.tell(new JoinMsg(tracker), null);
		} else {
			// Invalid settings, terminate
			throw new EptoInputException("Invalid actor config properties given");
		}
		printRunParameters();
		try {
			LOGGER.info("Starting EpTO...");
			Thread.sleep(5000);
			actor.tell(new EptoActor.EptoStartMsg(), null);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
