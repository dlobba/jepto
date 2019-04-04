#!/usr/bin/python3
import re
import sys

import total_order2 as to
import data_analysis as da

class ParseEventException(Exception):
    def __init__(self, message=None):
        if not message:
            super().__init__("Event string representation expected.")
        else:
            super().__init__(message)


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

def parse_logs(log_file, msg_filter_func=lambda x: x is not None):
    """
    Given a log file perform the parsing collecting data for further
    processing. If a filter function is given it is applied to
    message ids in order to filter them.
    """
    actor_regexp = re.compile("INFO:\s+EpTO:\s+(\w+)\s+at_(\d+)_(\d+)\s+(\w+)\s+(.*)")
    # the set of actor+message_id
    # for messages broadcast
    broadcast = set()
    # for each message (actor+messageid)
    # corresponds a list of actors who delivered
    # the message
    delivered = dict()
    # for each actor, list messages ordered by delivery
    delivery_order = dict()

    # dictionary containing
    delta_msg = {}
    with open(log_file, "r") as fh:
        for line in fh:
            match = actor_regexp.match(line)
            if not match:
                continue
            actor, ts, lclock, action, argument = match.groups()
            time = lclock
            if actor not in delivery_order:
                delivery_order[actor] = []

            if "broadcast" in action or "delivered" in action:
                ts, source, msg_id = parse_event(argument)
                message = "{}:{}".format(source,msg_id)

                # perform the message filtering
                if msg_filter_func(message):
                    continue

                if "broadcast" in action:
                    broadcast.add(message)
                    delivered[message] = []
                    delta_msg[message] = {"at" : time, "actors" : []}
                elif "delivered" in action:
                    deliveryat = time
                    if message not in delivered:
                        delivered[message] = [actor]
                    else:
                        delivered[message].append(actor)
                        # add message to the delivery list for
                        # the current actor
                        delivery_order[actor].append(message)
                        delta_msg[message]["actors"].append(deliveryat)
    return broadcast, delivered, delivery_order, delta_msg
                
def check_actors_total_order(delivery_order_map):
    for actor1, order1 in delivery_order_map.items():
        for actor2, order2 in delivery_order.items():
            torder_satisfied, relation =  to.total_order(order1, order2)
            if not torder_satisfied:
                str_index, el1, el2 = relation
                if str_index is 0:
                    actor = actor1
                else:
                    actor = actor2
                raise to.OOOException("actor {} and {}"\
                                      " have different delivery order: "\
                                      "{} has {}->{}"
                                      .format(actor1, actor2, actor, el1, el2))

def print_agreement(delivered_msg_map, num_actors):
    for msg, actors in delivered_msg_map.items():
        print(msg + " delivery perc: {}%".format(round(len(actors)/num_actors * 100, 2)))


if __name__ == "__main__":
    filter_func = lambda x: da.filter_msg(x, 10, 100)
    b, msg_delivery, delivery_order, delta_msg = parse_logs(sys.argv[1], filter_func)
    num_actors = 100
    print_agreement(msg_delivery, num_actors)
    #delta_msg = da.filter_msg(delta_msg, 10, 100)
    delays1 = da.compute_delivery_delay(delta_msg)
    delays2 = list(da.delay_count(delays1, num_actors))
    delays3 = da.sum_delivery_fraction(delays2)
    da.plot_count(delays3)
