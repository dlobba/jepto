package com.ds2.jepto.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class BallMsg implements Serializable {

	private List<Event> ball;

	public BallMsg(EventMap ball) {
		super();
		this.ball = Collections.unmodifiableList(ball.toList());
	}

	public List<Event> getBall() {
		return ball;
	}



}
