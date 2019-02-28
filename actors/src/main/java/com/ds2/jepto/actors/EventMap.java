package com.ds2.jepto.actors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventMap {

	Map<EventKey, Event> events;
	
	public EventMap() {
		this.events = new HashMap<>();
	}
	
	public void insert(Event event) {
		this.events.put(
				new EventKey(event.getSource(), event.getId()),
				event);
	}
	
	public void remove(Event event) {
		this.events.remove(new EventKey(event));
	}
	
	public Event get(Event event) {
		return this.events.get(
				new EventKey(event.getSource(),
				event.getId()));
	}
	
	public boolean contains(Event event) {
		return this.events.containsKey(new EventKey(event));
	}
	
	/**
	 * If event is contained within the map,
	 * update its ttl.
	 * 
	 * @param fevent
	 */
	public void update(Event event) {
		this.events.put(
				new EventKey(event.getSource(), event.getId()),
				event);
	}
	
	public void incrementTtl() {
		for (Event event : events.values()) {
			event.setTtl(event.getTtl() + 1);
			this.update(event);
		}
	}
	
	public List<Event> toList() {
		Event events[] = new Event[this.events.size()];
		return Arrays.asList(this.events.values().toArray(events));
	}
	
	/**
	 * Given a list of events, fill the map.
	 * @param eventList
	 */
	public void fromList(List<Event> eventList) {
		this.events.clear();
		for (Event event : eventList) {
			this.events.put(
					new EventKey(event.getSource(), event.getId()),
					event);
		}
	}
	
	public boolean isEmpty() {
		return this.events.isEmpty();
	}
	
	public void clear() {
		this.events.clear();
	}

	public List<Event> toSortedList() {
		Event events[] = new Event[this.events.size()];
		List<Event> tmp = Arrays.asList(this.events.values().toArray(events));
		tmp.sort(new Comparator<Event>() {
			@Override
			public int compare(Event event0, Event event1) {
				if (event0.getTimestamp() < event1.getTimestamp())
					return -1;
				if (event0.getTimestamp() > event1.getTimestamp())
					return 1;
				return event0.getSource().toString().compareTo(event1.getSource().toString());
			}
		});
		return tmp;
	}
}
