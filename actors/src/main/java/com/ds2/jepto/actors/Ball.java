package com.ds2.jepto.actors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Ball {
	
	List<Event> events;
	
	public Ball() {
		this.events = new ArrayList<>();
	}
	
	public void insert(Event event) {
		this.events.add(event);
	}
	
	public Event get(Event fevent) {
		for (Event event : events) {
			if (fevent.getId() == event.getId() &&
				fevent.getSource().equals(event.getSource()))
				return event;
		}
		return null;
	}
	
	public void update(Event fevent) {
		Event tmp;
		Iterator<Event> eventIterator = this.events.iterator();
		while (eventIterator.hasNext()) {
			tmp = eventIterator.next();
			if (fevent.getId() == tmp.getId() &&
				fevent.getSource().equals(tmp.getSource())) {
				eventIterator.remove();
				break;
			}
		}
		this.insert(fevent);
	}
	
	public void incrementTtl() {
		synchronized (events) {
			for (Event event : events) {
				event.setTtl(event.getTtl() + 1);
			}
		}
	}
	
	public List<Event> toList() {
		return this.events;
	}
	
	public void fromList(List<Event> eventList) {
		this.events = new ArrayList<>(eventList);
	}
	
	
	public boolean isEmpty() {
		return this.events.isEmpty();
	}
}
