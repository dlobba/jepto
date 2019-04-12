import total_order2 as to

# Out-Of-Order exception
class OOOException(Exception):

    def __init__(self, msg=None, upair=(None,None, None, None)):
        """
        upair(Tuple): a tuple comprising (actor1, actor2, msg1, msg2)
        """
        try:
            act1, act2, msg1, msg2 = upair
            msg = "Actors {}, {} delivery order differs by: {} -> {}"\
                  .format(act1, act2, msg1, msg2)
        except:
            raise ValueError("Invalid tuple (el1, el2, first_sequence) given")
        if msg is None:
            msg = "Delivery order differs"
        super().__init__(msg)
        self.upair = upair

# Double Delivery Exception
class DDException(Exception): pass


def element_appears(given_sequence, in_sequence, appearing_elements=[]):
    temp_seq = [i for i in in_sequence]
    for element in given_sequence:
        if element in temp_seq:
            appearing_elements.append(element)
            return True
        temp_seq.append(element)
    return False

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
                raise OOOException("actor {} and {}"\
                                   " have different delivery order: "\
                                   "{} has {}->{}"
                                   .format(actor1, actor2, actor, el1, el2),\
                                   (actor1, actor2, actor, el1, el2))
