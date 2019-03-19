#!/usr/bin/python3

from collections import OrderedDict

class OOOException(Exception):

    def __init__(self, msg=None, upair=None):
        """
        upair(Tuple): a tuple (seqno, x, y) containing the elements x,y, first_seq
        such that

        * if seqno == 0:  x -> y in the first sequence while y -> x in the second
        * if seqn0 == 1: y -> x in the first sequence while x -> y in the second
        """
        try:
            if upair is not None:
                seq,x,y = upair
                if seq != 0 or seq != 1: raise ValueError()
        except:
            raise ValueError("Invalid tuple (el1, el2, first_sequence) given")
        if msg is None:
            msg = "Unordered elements {} -> {}".format(x, y)
            
        super().__init__(msg)
        self.upair = upair


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
