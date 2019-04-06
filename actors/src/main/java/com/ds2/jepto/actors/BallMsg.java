package com.ds2.jepto.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BallMsg implements Serializable {

	private List<Event> ball;

	public BallMsg(EventMap ball) {
		super();
		this.ball = Collections.unmodifiableList(ball.toList());
	}

	public List<Event> getBall() {
		return ball;
	}

	public String toString() {
		String eventsString =  String.join(", ",
				this.ball.stream()
				.map(event -> event.toString())
				.collect(Collectors.toList()));
		return "{ " + eventsString + " }";
	}
}
