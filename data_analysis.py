import matplotlib.pyplot as plt
import matplotlib

from functools import reduce
from itertools import groupby

def reduce_by_key(func, iterable):
    """Reduce by key.
    Equivalent to the Spark counterpart
    Inspired by http://stackoverflow.com/q/33648581/554319
    1. Sort by key
    2. Group by key yielding (key, grouper)
    3. For each pair yield
       (key, reduce(func, last element of each grouper))
    """
    get_first = lambda p: p[0]
    get_second = lambda p: p[1]
 
    return map(
        lambda l: (l[0], reduce(func, map(get_second, l[1]))),
        groupby(sorted(iterable, key=get_first), get_first))


def compute_delivery_delay(delta_dict):
    """
    Given a dictionary of dictionaries,
    with entries for a given message `actor:msg` of the form

    `at: x, actors : <list of delivery times>`

    compute the difference of each delivery time
    with the broadcast time of the message.

    Return
    ------
    A new dicionary `message : <list of delays>`,
    where list of delays is sorted in ascending order.
    """
    delays = {}
    for msg, ddict in delta_dict.items():
        t0 = int(ddict["at"])
        delays[msg] = [t - t0 for t in map(int, ddict["actors"])]
        delays[msg].sort()
    return delays

def delay_count(delay_dict, num_actors):
    """
    Given a dictionary `msg : <list of delays>`
    count how many similar delay values are there
    for each message.

    Return
    ------
    A dictionary `message : <fraction-of-round-delivery>`
    for delay value found, from the value of 0 up
    to the largest value found.
    """
    for m, dlist in delay_dict.items():
        tdelay = map(lambda x: (x, 1), dlist)
        ddict = {v[0] : v[1]/num_actors for v in\
                 reduce_by_key(lambda x,y: x + y, tdelay)}
        yield ddict

def sum_delivery_fraction(delay_dict):
    tot_delay = {}
    count_msg = 0
    for ddict in delay_dict:
        for k,v in ddict.items():
            try:
                tot_delay[k] += v
            except KeyError:
                tot_delay[k] = v
        count_msg += 1
    return {k:v/count_msg for k,v in tot_delay.items()}

def filter_msg(data, min_msg=None, max_msg=None):
    if max_msg is None and min_msg is None:
        return data
    if min_msg is None:
        min_msg = 0

    temp = data.copy()
    for msg, v in data.items():
        msg_i = int(msg.split(":")[-1])
        if msg_i < min_msg or\
           (max_msg is not None and msg_i > max_msg):
            temp.pop(msg)
    return temp

def filter_msg(msg, min_msg=None, max_msg=None):
    """
    Return False if, given a message id `msg_i`, it is true:
    
    min_msg <= msg_i <= max_msg

    Note
    ----
    The message `msg` is given in the form `actor:msg_id`.
    @deprecated
    """
    msg_i = int(msg.split(":")[-1])
    if min_msg is not None and msg_i < min_msg:
        return True
    if max_msg is not None and msg_i > max_msg:
        return True
    return False

def compute_actor_msg(msg_list):
    """
    Given a list of messages, cluster
    each message with the corresponding sender.
    """
    actor_msg = {}
    for msg in msg_list:
        actor, mid = msg.split(":")
        mid = int(mid)
        if actor in actor_msg:
            actor_msg[actor].append(mid)
        else:
            actor_msg[actor] = [mid]
    return actor_msg

def compute_delivery_rate(delivered_msg_map, num_actors):
    return {msg : round(len(actors)/num_actors * 100, 2)\
            for msg, actors in delivered_msg_map.items()}

def compute_epoch(actor_msg):
    """
    Compute the lowest and highest message ids.
    """
    messages = reduce(lambda x,y: set(x).union(set(y)), actor_msg.values())
    return min(messages), max(messages)       

def plot_count(delay_count):
    x = list(delay_count.keys())
    x.sort()
    y = [delay_count[k] for k in x]
    y = [sum(y[0:i]) for i in range(0, len(y))]
    plt.step(x, y)
    plt.show()


if __name__ == "__main__":
    pass
