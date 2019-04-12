import re

DEBUG_INFO_REGEXP = "(DEBUG|INFO):\s+EpTO:\s+(\w+)\s+at_(\d+)_(\d+)\s+(\w+)\s+(.*)"
INFO_REGEXP       = "INFO:\s+EpTO:\s+(\w+)\s+at_(\d+)_(\d+)\s+(\w+)\s+(.*)"

def parse_event(event_repr):
    """
    Return timestamp, source actor and id
    related to the event string representation
    passed as input.
    """
    event_regexp = re.compile(r"\s*Event\s*"\
                              "\[timestamp=(\d+),\s*"\
                              "source=(\w+),\s*"\
                              "id=(\d+),\s*"
                              "action=\w+,\s*"
                              "ttl=\d+\s*\]\s*")
    match = event_regexp.match(event_repr)
    if not match:
        raise ParseEventException()
    return match.groups()

def parse_ball(ball):
    event_regexp = re.compile(r"\s*Event\s*"\
                              "\[timestamp=(\d+),\s*"\
                              "source=(\w+),\s*"\
                              "id=(\d+),\s*"
                              "action=(\w+),\s*"
                              "ttl=(\d+)\s*\]\s*")
    events = []
    for event in event_regexp.finditer(ball):
        ts, source, id, _, ttl = event.groups()
        label = "{}:{}@{}_ttl={}".format(source, id, ts, ttl)
        events.append(label)
    return str.join(", ", events)
