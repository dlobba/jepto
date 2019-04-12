#!/usr/bin/python3
import re
import operator
import sys

from msc_helper import MSCHelper
from parsing_helper import DEBUG_INFO_REGEXP, parse_event, parse_ball
from total_order_checker import check_actors_total_order, OOOException

def wrong_exec_parser(log_file, msg_filter_func=lambda x: x is not None):
    """
    Given a log file perform the parsing collecting information to
    reproduce a wrong execution.

    Return
    ------
    An ordered list of items `<system_time, source, destination, label>`
    to produce a (hopefully) valid mscgen chart.
    The list is sorted by system_time.

    A dictionary containing the delivery order for each actor.
    """
    actor_regexp = re.compile(DEBUG_INFO_REGEXP)
    # collect information
    data  = []
    # for each actor, list messages ordered by delivery
    delivery_order = dict()
    with open(log_file, "r") as fh:
        for line in fh:
            match = actor_regexp.match(line)
            if not match:
                continue
            _, actor, gclock, lclock, action, argument = match.groups()
            # add actor to delivery dictionary if not already there
            if actor not in delivery_order:
                delivery_order[actor] = []
            time = gclock
            if "received_ball" in action:
                rec_ball_reg = re.compile(r"\s*(\w+)\s+{\s+([^}]*)\s+}")
                receiver, events = rec_ball_reg.match(argument).groups()
                events_label = parse_ball(argument)
                data.append((gclock, actor, receiver,
                             "received_ball\\n\\n " + events_label))
            elif "received_set" in action:
                events_label = parse_ball(argument)
                data.append((gclock, actor, actor,
                             "received_set\\n\\n " + events_label))
            if "deliverable_set" in action:
                events_label = parse_ball(argument)
                data.append((gclock, actor, actor,
                             "deliverable_set\\n\\n " + events_label))
            if "delivered" in action:
                ts, source, msg_id = parse_event(argument)
                message = "{}:{}".format(source,msg_id)
                data.append((gclock, actor, actor,
                             "delivered\\n\\n " + message))
                # store delivery order
                delivery_order[actor].append(message)

    return sorted(data, key=operator.itemgetter(0)), delivery_order

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

def filter_OOO(msch, ooo_tuple):
    act1, act2, m1, m2 = ooo_tuple
    # elements to keep (k prefix)
    k_entities = (act1, act2)
    
    t_msch = MSCHelper()
    for entity in msch._entities:
        if entity in k_entities:
            t_msch.add_entity(entity, **msch._entities_properties[entity])
    ignored_last = False
    #for line in msch._lines:
    #    for
    

def exit():
    help_ = "\nUsage:\n trace_parse.py <log-file> <msc-out-file>\n"
    print(help_)
    sys.exit(0)

if __name__ == "__main__":
    """
    Given a log file as input, parse the log in order
    to obtain a list of events and produce a valid MSC
    chart.
    """
    if len(sys.argv) < 3:
        exit()
    in_  = sys.argv[1]
    out_ = sys.argv[2]
    if in_ == out_:
        print("You are overriding the input log file!")
        exit()
    filter_func = lambda x: False
    events, delivery_order = wrong_exec_parser(in_, filter_func)
    msch = make_msc(events)
    """
    try:
        check_actors_total_order(delivery_order)
    except OOOException as e:
        act1, act2, m1, m2 = e.upair

    print(act1)
        
    msch = make_msc(events)
    with open(out_, "w") as fh:
        fh.write(msch.make_msc())
    """
