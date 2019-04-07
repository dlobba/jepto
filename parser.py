#!/usr/bin/python3
import re
import sys
import operator

from functools import reduce

import total_order2 as to
import data_analysis as da

from msc_helper import MSCHelper

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


def print_agreement(msg_delivery_rate):
    for msg, drate in msg_delivery_rate.items():
        print("{} delivery perc: {}%".format(msg, drate))


def summary(num_actors, epoch, num_messages, avg_msg, min_drate, max_drate):
    return "Total actors: {}\n".format(num_actors) +\
    "Epoch: {}-{}\n".format(epoch[0], epoch[1]) +\
    "Total messages: {}\n".format(num_messages) +\
    "Avg messages per actor: {}\n".format(avg_msg) +\
    "Lowest delivery rate: {}\n".format(min_drate) +\
    "Highest delivery rate: {}\n".format(max_drate)

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

def wrong_exec_parser(log_file, msg_filter_func=lambda x: x is not None):
    """
    Given a log file perform the parsing collecting information to
    reproduce a wrong execution.

    Return
    ------
    An ordered list of items `<system_time, source, destination, label>`
    to produce a (hopefully) valid mscgen chart.
    The list is sorted by system_time.
    """
    actor_regexp = re.compile("INFO:\s+EpTO:\s+(\w+)\s+at_(\d+)_(\d+)\s+(\w+)\s+(.*)")
    # collect information
    data  = []
    with open(log_file, "r") as fh:
        for line in fh:
            match = actor_regexp.match(line)
            if not match:
                continue
            actor, gclock, lclock, action, argument = match.groups()
            time = gclock
            if "received_ball" in action:
                rec_ball_reg = re.compile(r"\s*(\w+)\s+{\s+([^}]*)\s+}")
                receiver, events = rec_ball_reg.match(argument).groups()
                events_label = parse_ball(argument)
                data.append((gclock, actor, receiver, "received_ball\\n\\n " + events_label))
            elif "received_set" in action:
                events_label = parse_ball(argument)
                data.append((gclock, actor, actor, "received_set\\n\\n " + events_label))
            if "deliverable_set" in action:
                events_label = parse_ball(argument)
                data.append((gclock, actor, actor, "deliverable_set\\n\\n " + events_label))
            if "delivered" in action:
                ts, source, msg_id = parse_event(argument)
                message = "{}:{}".format(source,msg_id)
                data.append((gclock, actor, actor, "delivered\\n\\n " + message))
                
    return sorted(data, key=operator.itemgetter(0))

def make_msc(csv_data):
    h = MSCHelper()
    for _, destination, source, label in csv_data:
        new_lines = label.split(",")
        label = str.join("\\n"*2, new_lines)
        h.add_entity(source)
        h.add_entity(destination)
        if "delivered" in label:
            color = "#ff0000"
        else:
            color = "#000000"
        h.add_message(source, destination, label=label, linecolor=color)
        h.new_line()
        for x in range(0, 2):
            h.add_gap()
            h.new_line()
        h.end_line()
    return h

if __name__ == "__main__":
    """
    Compute the following

    1. the total number of actors
    2. the total number of messages generated
    3. the average number of messages generated by a single actor
    4. the minimum and maximum delivery rate found, considering
       all the messages.
    """    
    #filter_func = lambda x: da.filter_msg(x, 10, 100)
    filter_func = lambda x: False
    broadcast, msg_delivery, delivery_order, delta_msg = parse_logs(sys.argv[1], filter_func)
    #p2 = wrong_exec_parser(sys.argv[1], filter_func)
    #k = make_msc(p2)
    #with open("msc.txt", "w") as fh:
    #    fh.write(k.make_msc())


    actor_msg     = da.compute_actor_msg(msg_delivery)
    num_actors   = len(actor_msg.keys()) 
    num_messages = len(reduce(lambda x,y: list(x) + list(y), actor_msg.values()))
    avg_msg      = int(num_messages / num_actors)

    epoch = da.compute_epoch(actor_msg)

    delivery_rate =  da.compute_delivery_rate(msg_delivery, num_actors)
    min_drate = min(delivery_rate.values())
    max_drate = max(delivery_rate.values())

    print(summary(num_actors, epoch, num_messages, avg_msg,\
                  min_drate, max_drate))

    bins = da.plot_drate_pdf(delivery_rate, 5)
    """
    delays1 = da.compute_delivery_delay(delta_msg)
    delays2 = list(da.delay_count(delays1, num_actors))
    delays3 = da.sum_delivery_fraction(delays2)
    da.plot_count(delays3)
    """
