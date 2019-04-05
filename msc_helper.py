import re

class EntityNotPresentError(Exception): pass
class InvalidArcTypeError(Exception): pass
class InvalidDividerError(Exception): pass
class InvalidColorError(Exception): pass

# entities and arc properties
LABEL            = "label"
URL              = "url"
ID               = "id"
ARCSKIP          = "arcskip"
LINECOLOR        = "linecolor"
TEXTCOLOR        = "textcolor"
TEXTBGCOLOR      = "textbgcolor"
ARCLINECOLOR     = "arclinecolor"
ARCTEXTBGCOLOR   = "arctextbgcolor"

# arc primitives
MESSAGE   = "message"
FUNCTION  = "function"
RETURN    = "return"
CALLBACK  = "callback"
EMPH      = "emph"
LOST      = "lost"
BROADCAST = "bcast"
BOX       = "box"
ROUND_BOX = "rbox"
ANG_BOX   = "abox"
NOTE      = "note"

# arcs enum
ARCS = {
    MESSAGE  : "->",
    FUNCTION : "=>",
    RETURN   : ">>",
    CALLBACK : "=>>",
    EMPH     : ":>",
    LOST     : "-x",
    BROADCAST: "->*",
    BOX      : "box",
    ROUND_BOX: "rbox",
    ANG_BOX  : "abox",
    NOTE     : "note"
}

# dividers
COMMENT = "comment"
GAP     = "gap"

# dividers enum
DIVS = {
    COMMENT : "---",
    GAP     : "|||"
}


# helper specific properties
FROM = "from"
TO   = "to"
TYPE = "type"
ARC_TYPE = "arc_type"
DIV_TYPE = "div_type"

# define the token allowed within a line
DIVIDER = "divider"
ARC = "arc"

def is_color(color_str):
    """
    Check whether the string represents a valid
    hexadecimal color specification.

    Return
    ------
    True if the string is a valid color representation.
    """
    return re.match("#[0-9a-fA-F]{6}", color_str) is not None


class MSCHelper:
    """
    The helper is used to describe the entities and the the
    arcs of a given msc diagram, as defined in the mscgen program
    specification.

    An instance of helper collects the entities (actors) that will be
    depicted within the diagram and the properties related.

    Similarly to a stack, the helper defines a list of lines, each one
    containing several arcs or dividers.

    Arcs and dividers are automatically added to the last
    line defined. It is not possible to (directly and gratiously) add an
    element to a previous line.

    The helper will therefore help the user adding **sequential** arcs and
    dividers to the msc file.

    Note
    ----
    The helper strives only to generate a syntactically
    correct mscgen file. But the final result is given
    only by the execution output.
    """
    def __init__(self):
        self._entities = []
        self._entities_properties = {}
        self._lines  = []

    # LINE MANAGEMENT ##########################################################

    def new_line(self):
        self._lines.append([])

    def end_line(self):
        self.new_line()

    def _add_to_line(self, arc):
        """
        Add an arc to the current line.
        """
        if len(self._lines) == 0:
            self._lines.append([])
        self._lines[-1].append(arc)

    # ENTITY MANAGEMENT ########################################################

    def add_entity(self, id,
                   label="",
                   linecolor="#000000",
                   textcolor="#000000",
                   textbgcolor=r"#ffffff",
                   arclinecolor="#000000",
                   arctextbgcolor="#ffffff"):
        if id in self._entities:
            return
        color_properties = [linecolor, textcolor, textbgcolor,
                            arclinecolor, arctextbgcolor]
        for p in color_properties:
            if not is_color(p):
                raise InvalidColorError()
        if label is None:
            label = ""
        self._entities.append(id)
        self._entities_properties[id] =\
            {LABEL : label,
             LINECOLOR : linecolor,
             TEXTCOLOR : textcolor,
             TEXTBGCOLOR : textbgcolor,
             ARCLINECOLOR : arclinecolor,
             ARCTEXTBGCOLOR   : arctextbgcolor}

    def _entity_repr(self, eid):
        if eid not in self._entities:
            raise ValueError("Non existing entity given")
        ep = self._entities_properties[eid]
        return '"{}" [label="{}", '.format(eid, ep[LABEL]) +\
            'linecolor="{}", '.format(ep[LINECOLOR]) +\
            'textcolor="{}",  '.format(ep[TEXTCOLOR]) +\
            'textbgcolor="{}", '.format(ep[TEXTBGCOLOR]) +\
            'arclinecolor="{}", '.format(ep[ARCLINECOLOR]) +\
            'arctextbgcolor="{}"]'.format(ep[ARCTEXTBGCOLOR])

    # RAW ARC MANAGEMENT #######################################################

    def _arc(self, from_e, to_e, arc_type,
             label=None,
             url=None,
             id_=None,
             arcskip=0,
             linecolor="#000000",
             textcolor="#000000",
             textbgcolor=r"#ffffff"):
        if from_e not in self._entities:
            raise EntityNotPresentError()
        if to_e not in self._entities and to_e is not None:
            raise EntityNotPresentError()

        if arc_type not in ARCS:
            raise InvalidArcTypeError()
        color_properties = [linecolor, textcolor, textbgcolor]
        for p in color_properties:
            if not is_color(p):
                raise InvalidColorError()
        try:
            int(arcskip)
        except ValueError:
            arcskip = 0
        if id_ is None:
            id_ = ""
        if url is None:
            url = ""
        arc = {
            TYPE     : ARC,
            FROM     : from_e,
            TO       : to_e,
            ARC_TYPE : arc_type,
            LABEL    : label,
            URL      : url,
            ID       : id_,
            ARCSKIP  : arcskip,
            LINECOLOR   : linecolor,
            TEXTCOLOR   : textcolor,
            TEXTBGCOLOR : textbgcolor}
        self._add_to_line(arc)
        return arc

    def _arc_repr(self, arc):
        """
        Given a dictionary describing an arc and its properties,
        return its string representation as defined by msc.

        Return
        ------
        A string describing the arc.

        Note
        ----
        The arc is assumed to be wellformed, since it is added
        by a private method.
        """
        arc_ = ARCS[arc[ARC_TYPE]]
        from_, to_ = arc[FROM], arc[TO]
        # replace None entities with empty strings
        line = '"{}" {} "{}"'.format(from_,
                                     arc_,
                                     to_)
        if to_ is None:
            line = '"{}" {} '.format(from_,
                                     arc_)
        line += '[label="{}", '.format(arc[LABEL]) +\
             'url="{}", '.format(arc[URL]) +\
             'id="{}", '.format(arc[ID]) +\
             'arcskip="{}", '.format(arc[ARCSKIP]) +\
             'linecolor="{}", '.format(arc[LINECOLOR]) +\
             'textcolor="{}",  '.format(arc[TEXTCOLOR]) +\
             'textbgcolor="{}"]'.format(arc[TEXTBGCOLOR])
        return line

    # ARC MANAGEMENT ###########################################################

    def add_message(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, MESSAGE, **kwargs)

    def add_broadcast(self, source, **kwargs):
        return self._arc(source, None, BROADCAST, **kwargs)

    def add_function(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, FUNCTION, **kwargs)

    def add_return(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, RETURN, **kwargs)

    def add_callback(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, CALLBACK, **kwargs)

    def add_emphasis(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, EMPH, **kwargs)

    def add_lost(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, LOST, **kwargs)

    def add_box(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, BOX, **kwargs)

    def add_rounded_box(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, RBOX, **kwargs)

    def add_angular_box(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, ABOX, **kwargs)

    def add_note(self, from_e, to_e, **kwargs):
        return self._arc(from_e, to_e, NOTE, **kwargs)

    # DIVIDERS MANAGEMENT ######################################################

    def _divider(self,
                 div_type,
                 label=None,
                 url=None,
                 id_=None,
                 arcskip=0,
                 linecolor="#000000",
                 textcolor="#000000",
                 textbgcolor="#ffffff"):
        if div_type not in DIVS:
            raise InvalidDividerError()
        color_properties = [linecolor, textcolor, textbgcolor]
        for p in color_properties:
            if not is_color(p):
                raise InvalidColorError()
        if id_ is None:
            id_ = ""
        if url is None:
            url = ""
        div = {
            TYPE     : DIVIDER,
            DIV_TYPE : div_type,
            LABEL    : label,
            URL      : url,
            ID       : id_,
            LINECOLOR   : linecolor,
            TEXTCOLOR   : textcolor,
            TEXTBGCOLOR : textbgcolor}
        self._add_to_line(div)
        return div

    def _divider_repr(self, div):
        """
        Given a dictionary describing a div and its properties,
        return its string representation as defined by msc.

        Return
        ------
        A string describing the divider.

        Note
        ----
        The divider is assumed to be wellformed, since it is added
        by a private method.
        """
        div_type = DIVS[div[DIV_TYPE]]
        div_ = '{} [label="{}", '.format(div_type, div[LABEL]) +\
             'url="{}", '.format(div[URL]) +\
             'id="{}", '.format(div[ID]) +\
             'linecolor="{}", '.format(div[LINECOLOR]) +\
             'textcolor="{}",  '.format(div[TEXTCOLOR]) +\
             'textbgcolor="{}"]'.format(div[TEXTBGCOLOR])
        return div_

    def add_gap(self, **kwargs):
        self._divider(GAP, **kwargs)

    def add_comment(self, **kwargs):
        self._divider(COMMENT, **kwargs)

    # MSC GENERATOR  ###########################################################

    def _element_repr(self, element):
        """
        Return an arc or a divider representation according to the
        type of element given.
        """
        if element[TYPE] == DIVIDER:
            return self._divider_repr(element)
        return self._arc_repr(element)

    def make_msc(self):
        str_ = "msc {\n"
        str_ += "# define entities\n"
        str_ += str.join(",\n", list(map(self._entity_repr, self._entities))) + ";\n\n"

        for line in self._lines:
            if len(line) > 0:
                str_ += str.join(",\n", list(map(self._element_repr, line))) + ";\n\n"
        str_ += "}\n"
        return str_


if __name__ == "__main__":

    a = MSCHelper()
    a.add_entity("a", "A birbant")
    a.add_entity("b", "A birbbnt")
    a.add_entity("c", "A birbcnt")
    a.add_entity("d", "D birbcnt")
    a.new_line()
    a.add_message("a", "b", label="a sends to b")
    a.add_message("c", "d", label="c sends to d")
    a.end_line()
    a.add_broadcast("b", label="this is a bcast motherfucker")
    a.add_comment(label = "lol")
