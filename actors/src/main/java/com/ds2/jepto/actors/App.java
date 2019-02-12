package com.ds2.jepto.actors;

import java.util.HashMap;
import java.util.Map;

import com.ds2.jepto.actors.cyclon.CyclonActor;
import com.ds2.jepto.actors.cyclon.CyclonActor.DebugMsg;
import com.ds2.jepto.actors.cyclon.JoinMsg;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	/*
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
    	ActorSystem system = ActorSystem.create("JeptoTestJoin");
    	ActorRef a, b, c;
    	a = system.actorOf(CyclonActor.props(4, 2, 10000000l, 10l, 42l),"actor_a");
    	b = system.actorOf(CyclonActor.props(4, 2, 10000000l, 10l, 42l),"actor_b");
    	c = system.actorOf(CyclonActor.props(4, 2, 10000000l, 10l, 42l),"actor_c");
    	
    	a.tell(new JoinMsg(b), a);
    	b.tell(new JoinMsg(a), b);
    	a.tell(new DebugMsg(), null);
    	b.tell(new DebugMsg(), null);
    	c.tell(new JoinMsg(a), a);
    	
    	try {
			Thread.sleep(15l);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	c.tell(new DebugMsg(), null);
    }
}
