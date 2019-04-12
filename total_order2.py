#!/usr/bin/python3
from collections import OrderedDict

def store_indexes(seq1, seq2):
    """
    Store the index associated to each element.

    Note
    ----
    It is assumed each element appears within the sequence just once.
    """
    index_map = OrderedDict()
    for i in range(0, len(seq1)):
        e = seq1[i]
        index_map[e] = i
    for i in range(0, len(seq2)):
        e = seq2[i]
        if e not in index_map:
            index_map[e] = (-1, i)
        else:
            index_map[e] = (index_map[e], i)
    for k, v in index_map.items():
        try:
            if len(v): pass
        except TypeError:
            index_map[k] = (v, -1)
    return index_map

def total_order(seq1, seq2):
    index_map = store_indexes(seq1, seq2)
    # check total order seq1 against seq2
    for i in range(0, len(seq1) - 1):
        p1,p2 = index_map[seq1[i]]
        n1,n2 = index_map[seq1[i+1]]

        # if there is some "gap", ignore the comparison
        if -1 in (p1, p2, n1, n2):
            continue
        if p1 < n1 and p2 > n2:
            return False, (0, seq2[p2], seq2[n2])
        if p1 > n1 and p2 < n2:
            return False, (0, seq2[p2], seq2[n2])
    # check total order seq2 against seq1
    for i in range(0, len(seq2) - 1):
        p1,p2 = index_map[seq2[i]]
        n1,n2 = index_map[seq2[i+1]]

        if -1 in (p1, p2, n1, n2):
            continue
        if p2 < n2 and p1 > n1:
            return False, (1, seq2[p2], seq2[n2])
        if p2 > n2 and p1 < n1:
            return False, (1, seq2[p2], seq2[n2])
    return True, ()


if __name__ == "__main__":
    pass
