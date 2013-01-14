LAVi: Longitudinal Analysis of Virtual Communities
==================================================

"Longitudinal Analysis of VIrtual Networks" is a software library, written in Scala (hence compatible with Java), for descriptive analysis of social networks through time.

Aim of Lavi is to contribute to analysis of the dynamics of virtual communities
which are commonplace nowadays in software developer communities to social media networks.

Available social network analysis libraries are insufficient for this purpose in many aspects.
Almost all of those libraries assume a graph 'edge' as the only relational element. Some 
(e.g. Gephi, Graphstream) propose schemes to capture the network time, but these schemes,
along with the analysis offered are limited. These shortcomings and incompatibilities stem
from differences in application focus, and not surprising in a newly developing field.

Lavi aims to be as generally applicable as possible, but possibly doomed to exhibit some
similar shortcomings. Acknowledgeing the immaturity of the field, our design aims to
capture e-mail based communities in particular, by keeping the generality as a secondary
design criteria. 

Communication Model and Input
-------------------------------
Lavi is designed to read its data as a -time ordered- sequence of communication acts.
Following communication acts, and corresponding data input fragments are recognized:

 * <call id=number src=actorId time=number-or-date/>
   Represents a call from an actor. This is the case when an actor -attempts to- initiates a
   discussion. Note that this act does not create a graph edge! Yet, these are important acts
   which initiate conversations (an important element of communication model). 
   This and all other acts has a unique id number (an integer).
 * <reply id=number reference=an-act-id src=actorId time=number-or-date/>
   This is identical to a directed, unweighted edge in similar libraries (from replier to original call's sender), but in Lavi it is tracked as part of a conversation.
 * <forward id=number reference=an-act-id src=actorId time=number-or-date/>
   Not yet imlemented. Similar to a re-tweet act.

Any act can add other attributes (e.g. indicating weight  and an XML element body, 
