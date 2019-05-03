import matplotlib.pyplot as plt
import matplotlib

from scipy.stats import norm
from collections import Counter

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
    for delay value found, from the lowest
    to the largest delay value.
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


def filter_msg(msg, min_msg=None, max_msg=None):
    """
    Return False if, given a message id `msg_i`, it is true:

    min_msg <= msg_i <= max_msg

    Note
    ----
    The message `msg` is given in the form `actor:msg_id`.
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
    return {msg : len(actors)/num_actors * 100\
            for msg, actors in delivered_msg_map.items()}

def compute_epoch(actor_msg):
    """
    Compute the lowest and highest message ids.
    """
    messages = reduce(lambda x,y: set(x).union(set(y)), actor_msg.values())
    return min(messages), max(messages)

def plot_count(delay_count, linestyle="-"):
    x = list(delay_count.keys())
    x.sort()
    y = [delay_count[k] for k in x]
    y = [sum(y[0:i+1]) for i in range(0, len(y))]
    plt.step(x, y, linestyle=linestyle)

def plot_drate_pdf(drate_dict):
    """
    Produce an histogram plotting the fraction
    of messages having a given delivery rate.

    During the processing, delivery rate values
    below 0.01 are discarded, in order to exclude
    bars whose value would be unnoticeable.

    Additionally, zero-valued bins are grouped
    within a single bar (whose value is 0).
    """
    # convert probabilities from float to int
    x = [int(v) for v in drate_dict.values()]
    # count how many equal values are there for each value
    count = Counter(x)
    # normalise counter with the total number of messages
    num_mex = reduce(lambda x,y: x + y, count.values())
    count = {k : round(v/num_mex,2) for k,v in count.items()}
    # remove 0s
    count = {k : v for k,v in count.items() if v > 0.01}
    bins = {}
    # store previous min max bin extremes
    pmin = 0
    pmax = 0
    for r in range(0, 101):
        if r in count:
            # aggregate ranges containing value 0 within a single bin
            bins[(pmin, pmax)] = 0
            bins[(r,r)] = count[r]
            pmin = r + 1
            pmax = r + 1
        else:
            pmax = r

    # replace tuple keys with strings
    plot_bins = {}
    for bin_, v in bins.items():
        i1, i2 = bin_
        if i1 == i2:
            plot_bins[str(i1)] = v
        else:
            plot_bins["{}-{}".format(i1, i2)] = v
    plt.bar(plot_bins.keys(), plot_bins.values())
    return plot_bins

def plot_drate_ranges(drate_dict, range_of_interest):
    """
    Produce an histogram plotting the fraction
    of messages having a given delivery rate.

    `range_of_interest` is a two-valued
    tuple defining the extremes to be considered in the
    counting.
    """
    left, right = range_of_interest
    left  = int(left)
    right = int(right)
    if left >= right:
        raise ValueError("same extreme values given")
    if left < 0:
        raise ValueError()
    if right > 100:
        raise ValueError()
    # convert probabilities from float to int
    x = [int(v) for v in drate_dict.values()]
    # filter values not in the range of interest
    x = list(filter(lambda x: x >= left and x <= right, x))
    # count how many equal values are there for each value
    count = Counter(x)
    # normalise counter with the total number of messages
    num_mex = reduce(lambda x,y: x + y, count.values())
    bins = count
    plt.bar(bins.keys(), bins.values())
    plt.xlabel("Delivery rate")
    plt.ylabel("pdf")
    plt.show()
    return bins


if __name__ == "__main__":
    pass
