package com.ds2.jepto.actors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ds2.jepto.actors.ActorMain.EptoInputException;
import com.ds2.jepto.actors.cyclon.JoinMsg;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import scala.concurrent.duration.Duration;

public class EptoMain {

	private static final Logger LOGGER = Logger.getLogger("");
	private static FileHandler loggerFileHandler;

	private static final String SYSTEM_NAME = "epto";
	private static AtomicLong  SEED = new AtomicLong(42);

	private static int  viewSize      = 100;
	private static int  shuffleLength = 30;
	// TODO: for easy wrong execution: K = 2, max_ttl = 2, n_actors = 3
	//	private static int 	numReceivers  = 17;
	private static int 	numReceivers  = 2;
	//	private static long max_ttl       = 3 * 14 + 1;
	private static long maxTtl        = 2;
	private static long roundInterval = 5000l;
	private static long shufflePeriod = 100l;

	private static long numActors   = 100l;
	private static boolean asPaper  = false;
	private static Level logLevel   = Level.INFO;
	private static Duration simTime = null;

	private static void createExecutionLogFile(Level level) {
		try {
			// create a specific log for the actor
			String path = System.getProperty("user.home") + File.separator
					+ "EpTOlogs" + File.separator + "execution.log";
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

	/**
	 * Convenience method used to generate actors quickly.
	 *
	 * @param system
	 * @param name
	 * @return
	 */
	private static ActorRef createActor(ActorSystem system, String name) {
		return system.actorOf(EptoActor.props(maxTtl,
				numReceivers,
				roundInterval,
				viewSize,
				shuffleLength,
				shufflePeriod,
				// TODO: for easy wrong execution select SEED.get()
				//SEED.get(),
				SEED.getAndIncrement(),
				asPaper),
				name);
	}

	/**
	 * Define a single star topology centered to
	 * a unique tracker and launch the execution.
	 *
	 * @param numActors
	 * @throws EptoInputException
	 */
	public static void runSingleStar(long numActors) throws EptoInputException {
		if (numActors < 2) {
			throw new EptoInputException("Too few actors defined");
		}

		ActorSystem system = ActorSystem.create(SYSTEM_NAME);
		ActorRef tracker;
		tracker = createActor(system, "tracker_0");
		List<ActorRef> peers = new ArrayList<>();
		for(int i = 1; i < numActors; i++) {
			peers.add(createActor(system, "actor_" + i));
		}

		//peers.get(23).tell(new DebugMsg(DebugType.TRACE_CACHE), null);
		// create a star topology centered at the
		// first element of the list
		try {
			for(ActorRef peer : peers) {
				peer.tell(new JoinMsg(tracker), null);
			}
			Thread.sleep(5000l);
			tracker	.tell(new EptoActor.EptoStartMsg(), null);
			for(ActorRef peer : peers) {
				peer.tell(new EptoActor.EptoStartMsg(), null);
			}
			if (simTime != null) {
				Thread.sleep(simTime.toMillis());
				system.terminate();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
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
				"jepto.config.num-actors",
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
		numActors     = Long.parseUnsignedLong(config.getString(parameters[6]));
		asPaper       = Boolean.parseBoolean(config.getString(parameters[7]));

		// handling optional parameters
		// 1) if simulation time is not defined, run the simulation indefinitely
		if (config.hasPath("jepto.config.sim-time")) {
			simTime = Duration.create(
					Long.parseUnsignedLong(config.getString("jepto.config.sim-time")),
					TimeUnit.SECONDS);
		}
		// 2) define the minimum log level from which starting to print logs
		if (config.hasPath("jepto.config.log-level"))
			logLevel = DebugLevel.parse(config.getString("jepto.config.log-level"));
	}

	public static void printRunParameters() {
		StringBuilder str = new StringBuilder();
		str.append("Num actors:\t\t" + numActors + "\n");
		str.append("Max ttl:\t\t" + maxTtl + "\n");
		str.append("Num receivers (K):\t" + numReceivers + "\n");
		str.append("Round interval:\t\t" + roundInterval + "\n");
		str.append("As paper:\t\t" + asPaper + "\n");
		str.append("Cyclon view size:\t" + viewSize + "\n");
		str.append("Cyclon shuffle length:\t"+ shuffleLength + "\n");
		str.append("Cyclon shuffle period:\t" + shufflePeriod);
		LOGGER.log(Level.INFO, "Run parameters\n" + str.toString());
	}

	public static void main(String[] args) throws EptoInputException {
		/*********************************************************************/
		/*                       Input handling                              */
		/*********************************************************************/
		String runConfigPath = System.getProperty("run.config");
		Config runConfig;
		if (runConfigPath != null) {
			// try to parse the config file as an external
			// file. If it fails, default to a file within the classpath
			File configFile = new File(runConfigPath);
			if (configFile.exists()) {
				runConfig = ConfigFactory.parseFile(configFile);
			} else {
				runConfig = ConfigFactory.load(runConfigPath);
			}
			setRunParameters(runConfig);
		} else {
			// try to obtain num actors and as_paper parameters form cli
			String numActorStr = System.getProperty("actors.num");
			String defaultNumActorStr = System.getProperty("actors.num.default");
			if (numActorStr == null && defaultNumActorStr == null) {
				throw new ActorMain.EptoInputException("No actors number defined");
			}
			String asPaperStr = System.getProperty("as.paper");
			String asPaperDefault = System.getProperty("as.paper.default");
			if (asPaperStr != null) {
				asPaper = Boolean.parseBoolean(asPaperStr);
			}
			if (numActorStr != null) {
				numActors = Long.parseUnsignedLong(numActorStr);
			} else {
				numActors = Long.parseUnsignedLong(defaultNumActorStr);
			}
		}
		createExecutionLogFile(logLevel);
		LOGGER.log(logLevel, "Set base log level to " + logLevel.toString());
		printRunParameters();
		/*********************************************************************/
		if (simTime != null) {
			LOGGER.log(Level.INFO, "Starting run for {0} s",
					new Object[]{simTime.toSeconds()});
			runSingleStar(numActors);
			LOGGER.log(Level.INFO, "Terminating run");
		} else {
			LOGGER.log(Level.INFO, "Starting run");
			runSingleStar(numActors);
		}
	}
}
