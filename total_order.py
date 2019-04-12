from difflib import SequenceMatcher

def total_order(seq1, seq2):
    matcher = SequenceMatcher(None, seq1, seq2, autojunk=False)
    previous1 = []
    previous2 = []
    index1 = 0
    index2 = 0
    ref1 = "begin"
    ref2 = "begin"
    previous_ref1 = ref1
    previous_ref2 = ref2
    for match in matcher.get_matching_blocks():
        # exploit the fact there is always a last match
        # of size 0
        seq1 = matcher.a[ index1 : match.a ]
        seq2 = matcher.b[ index2 : match.b ]
        match1 = matcher.a[match.a : match.a + match.size]
        match2 = matcher.b[match.b : match.b + match.size]
        index1 = match.a + match.size
        index2 = match.b + match.size
        if len(match1) is 0:
            ref1 = "end"
        else:
            previous_ref1 = ref1
            ref1 = match1[0]
        if len(match2) is 0:
            ref2 = "end"
        else:
            previous_ref2 = ref2
            ref2 = match2[0]

        
        appearing_elements = []
        if element_appears(seq1, previous1, appearing_elements):
            return False, (0, appearing_elements[0], appearing_elements[0])
        if element_appears(seq2, previous2, appearing_elements):
            return False, (1, appearing_elements[0], appearing_elements[0])
        
        if element_appears(seq1, previous2, appearing_elements):
            return False, (0, appearing_elements[0], previous_ref1)
        if element_appears(seq2, previous1, appearing_elements):
            return False, (1, appearing_elements[0], previous_ref2)

        previous1 += [x for x in seq1] + [x for x in match1]
        previous2 += [x for x in seq2] + [x for x in match2]
    return True, ()

if __name__ == "__main__":
    pass
