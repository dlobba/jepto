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
                delivered_events = re.compile(r"\s*{\s+([^}]*)\s+}")
                delivered_events = delivered_events.match(argument).groups()[0]
                print(delivered_events)
                for _, e_source, e_id, _ in parse_events(delivered_events):
                    message = "{}:{}".format(e_source, e_id)
                    data.append((gclock, actor, actor,
                                 "delivered\\n\\n " + message))
                    # store delivery order
                    delivery_order[actor].append(message)

    return sorted(data, key=operator.itemgetter(0)), delivery_order

def make_msc(csv_data, ooo_tuple=(None,None,None,None)):
    act1, act2, m1, m2 = ooo_tuple
    print(m1)
    print(m2)
    k_entities = (act1, act2)
    h = MSCHelper()
    for _, destination, source, label in csv_data:
        # filter events ------------------------ #

        label = [k.strip() for k in label.split()]
        tmp_label = [label[0]]
        for tag in label[1:]:
            if m1 in tag or m2 in tag:
                tmp_label.append(tag)
        label = str.join("", tmp_label)
                        
        if destination not in k_entities or\
           source not in k_entities:
            continue

        if m1 not in label and\
           m2 not in label:
            continue
        # -------------------------------------- #
        h.add_entity(source)
        h.add_entity(destination)
        new_lines = label.split(",")
        label = str.join("\\n" * 2, new_lines)
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
    ooo_tuple = (None, None, None, None)
    try:
        check_actors_total_order(delivery_order)
    except OOOException as e:
        ooo_tuple = e.upair
    finally:
        msch = make_msc(events, ooo_tuple)
    print(ooo_tuple)
    with open(out_, "w") as fh:
        fh.write(msch.make_msc())
