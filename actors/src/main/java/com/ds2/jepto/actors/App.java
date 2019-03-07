package com.ds2.jepto.actors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ds2.jepto.actors.cyclon.CyclonActor;
import com.ds2.jepto.actors.cyclon.DebugMsg;
import com.ds2.jepto.actors.cyclon.DebugMsg.DebugType;
import com.ds2.jepto.actors.cyclon.JoinMsg;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;


public class App {
	
	private static final Logger LOGGER = Logger.getLogger("");
	private static FileHandler loggerFileHandler;
	
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
	 * Create two Cyclon actors A and B.
	 * Let each actor send a join message to the other.
	 * When message exchange finishes, check
	 * their cache contains both their
	 * id (aging could be increasing due to timeouts).
	 * 
	 * Create a new actor C and let it send a join
	 * message to A.
	 * After some time (depending on seed and update period)
	 * check that C contains both A and B.
	 */
	public static void testJoin() {
		ActorSystem system = ActorSystem.create("JeptoTestJoin");
    	ActorRef a, b, c;
    	a = system.actorOf(CyclonActor.props(1, 1, 500l, 42l),"actor_a");
    	b = system.actorOf(CyclonActor.props(1, 1, 500l, 42l),"actor_b");
    	c = system.actorOf(CyclonActor.props(1, 1, 500l, 42l),"actor_c");
    	
    	a.tell(new JoinMsg(b), a);
    	b.tell(new JoinMsg(a), b);
    	a.tell(new DebugMsg(), null);
    	b.tell(new DebugMsg(), null);
    	c.tell(new JoinMsg(a), a);
    	
    	try {
			Thread.sleep(4000l);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	c.tell(new DebugMsg(), null);
    	a.tell(new DebugMsg(), null);
    	b.tell(new DebugMsg(), null);
	}
	
	public static void test(int numActors) {
		ActorSystem system = ActorSystem.create("JeptoTestJoin");
		
		List<ActorRef> actors = new ArrayList<>();
		
		for(int i = 0; i < numActors; i++) {
			actors.add(system.actorOf(CyclonActor.props(2, 2, 500l, 42l),"actor_" + i));
		}
		
		// create a star topology centered at the
		// first element of the list
		try {
			ActorRef center = actors.get(0);
			for(int i = 1; i < numActors; i++) {
				actors.get(i).tell(new JoinMsg(center), null);
				Thread.sleep(100l);
			}
			Thread.sleep(5600l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for(int i = 0; i < numActors; i++) {
			actors.get(i).tell(new DebugMsg(), null);
		}
	}
	
	public static void test2(int numActors, int cacheSize, int shuffleLength) {
		ActorSystem system = ActorSystem.create("JeptoTestJoin");
		
		List<ActorRef> actors = new ArrayList<>();
		
		for(int i = 0; i < numActors; i++) {
			actors.add(system.actorOf(CyclonActor.props(cacheSize, shuffleLength, 500l, 42l),"actor_" + i));
		}
		
		// create a star topology centered at the
		// first element of the list
		try {
			ActorRef center1 = actors.get(0);
			ActorRef center2 = actors.get(numActors - 1);
			for(int i = 1; i < numActors / 2; i++) {
				actors.get(i).tell(new JoinMsg(center1), null);
				Thread.sleep(100l);
			}
			for(int i = numActors / 2 + 1 ; i < numActors - 1; i++) {
				actors.get(i).tell(new JoinMsg(center2), null);
				//Thread.sleep(100l);
			}
			actors.get(numActors / 2).tell(new JoinMsg(center2), null);
			actors.get(numActors / 2).tell(new JoinMsg(center1), null);

			ActorRef traced = actors.get((new Random()).nextInt(numActors-2)+1);
			//traced.tell(new DebugMsg(DebugType.TRACE_CACHE), null);
			Thread.sleep(5000l);
			
			center1.tell(new DebugMsg(DebugType.DIE), null);
			//center2.tell(new DebugMsg(DebugType.DIE), null);
			
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for(int i = 0; i < numActors; i++) {
			actors.get(i).tell(new DebugMsg(), null);
		}
	}
	
	public static void testEpto1(
			long max_ttl,
			long roundInterval,
			int viewSize,
			int numActors,
			int shuffleLength) {
		ActorSystem system = ActorSystem.create("EptoMain");
		int numReceivers = viewSize / 3;
		
		List<ActorRef> actors = new ArrayList<>();
		for(int i = 0; i < numActors; i++) {
			actors.add(system.actorOf(EptoActor.props(
					max_ttl,
					numReceivers,
					roundInterval,
					viewSize,
					shuffleLength,
					500l,
					42l),"actor_" + i));
		}
		
		// create a star topology centered at the
		// first element of the list
		try {
			ActorRef center1 = actors.get(0);
			ActorRef center2 = actors.get(numActors - 1);
			for(int i = 1; i < numActors / 2; i++) {
				actors.get(i).tell(new JoinMsg(center1), null);
				Thread.sleep(100l);
			}
			for(int i = numActors / 2 + 1 ; i < numActors - 1; i++) {
				actors.get(i).tell(new JoinMsg(center2), null);
				//Thread.sleep(100l);
			}
			actors.get(numActors / 2).tell(new JoinMsg(center2), null);
			actors.get(numActors / 2).tell(new JoinMsg(center1), null);

			ActorRef traced = actors.get((new Random()).nextInt(numActors-2)+1);
			//traced.tell(new DebugMsg(DebugType.TRACE_CACHE), null);
			Thread.sleep(5000l);
			
			center1.tell(new DebugMsg(DebugType.DIE), null);
			//center2.tell(new DebugMsg(DebugType.DIE), null);
			
			Thread.sleep(5000l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

//		for(int i = 0; i < numActors; i++) {
//			actors.get(i).tell(new DebugMsg(), null);
//		}
	}
	
    public static void main( String[] args ) {
    	createExecutionLogFile();
    	//testJoin();
    	//test2(1000, 50, 5);
    	testEpto1(3l, 5000l, 10, 20, 5);
    }
}
