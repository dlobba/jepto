package com.ds2.jepto.actors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventMap {

	private Map<EventKey, Event> events;

	public EventMap() {
		this.events = new HashMap<>();
	}

	public EventMap(Map<EventKey, Event> events) {
		this.events = new HashMap<>();
		// do not use putAll due to reference copy!
		for (Event event : events.values()) {
			this.insert(event);
		}
	}

	public void insert(Event event) {
		this.events.put(
				new EventKey(event.getSource(), event.getId()),
				new Event(event));
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
				new Event(event));
	}

	public void incrementTtl() {
		for (Event event : events.values()) {
			event.setTtl(event.getTtl() + 1);
		}
	}

	public List<Event> toList() {
		EventMap eventMap = this.clone();
		Event events[] = new Event[eventMap.events.size()];
		return Arrays.asList(eventMap.events.values().toArray(events));
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
					new Event(event));
		}
	}

	public boolean isEmpty() {
		return this.events.isEmpty();
	}

	public void clear() {
		this.events.clear();
	}

	public EventMap clone() {
		EventMap temp = new EventMap(this.events);
		return temp;
	}

	public List<Event> toSortedList() {
		EventMap eventMap = this.clone();
		Event events[] = new Event[eventMap.events.size()];
		List<Event> tmp = Arrays.asList(eventMap.events.values().toArray(events));
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

	public String toString() {
		String eventsString =  String.join(", ",
				this.events.values().stream()
				.map(event -> event.toString())
				.collect(Collectors.toList()));
		return "{ " + eventsString + " }";
	}
}
