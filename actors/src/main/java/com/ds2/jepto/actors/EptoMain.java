package com.ds2.jepto.actors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ds2.jepto.actors.ActorMain.EptoInputException;
import com.ds2.jepto.actors.cyclon.JoinMsg;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class EptoMain {

	private static final Logger LOGGER = Logger.getLogger("");
	private static FileHandler loggerFileHandler;

	private static final String SYSTEM_NAME = "epto";
	private static AtomicLong  SEED = new AtomicLong(42);

	private static int  viewSize      = 100;
	private static int  shuffleLength = 100;
	private static int 	numReceivers  = 20;
	private static long max_ttl       = 2 * 14 + 1;
	private static long roundInterval = 5000l;
	private static long shufflePeriod = 100l;

	private static boolean asPaper = false;

	private static void createExecutionLogFile() {
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
		return system.actorOf(EptoActor.props(max_ttl,
				numReceivers,
				roundInterval,
				viewSize,
				shuffleLength,
				shufflePeriod,
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
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws EptoInputException {
		/*********************************************************************/
		/*                       Input handling                              */
		/*********************************************************************/
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

		long numActors;
		if (numActorStr != null) {
			numActors = Long.parseUnsignedLong(numActorStr);
		} else {
			numActors = Long.parseUnsignedLong(defaultNumActorStr);
		}
		/*********************************************************************/

		createExecutionLogFile();
		runSingleStar(numActors);
	}
}
