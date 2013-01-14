
/**
  * Lavi is a library for Longitudinal Analysis of VIrtual communities
  * 
  * Longitudinal analysis of social network dynamics is a field in its infancy.
  * This library was born out of a personal need for doing research on software developer communities.
  * While its design tries to be as general as possible, it is inevitably geared towards
  * representing and analyzing e-mail group communities. 
  * 
  * Before proceeding with this reference documentation, 
  * you are recommended to check out other documentation distibuted with Lavi, 
  * which describes dynamic social network analysis approach that shapes the Lavi software design.   
  * 
  * Virtual communities are represented as directed, binary multi-graphs in Lavi. 
  * An instance of [[Community]]  class both represents such a community, and provides
  * utility methods for importing datasets. A community is internally represented by usings sets
  * of [[Act]]s and [[Actor]]s. Instances of Community class can then be fed into
  * one of the specializatios of [[Analyst]] class which suits your needs. An analyst instance
  * will take care of (1) dividing the longitudinal sequence of communication acts into a
  * sequence of frames, and (2) reporting desired statistics about the frames, actors, and acts within
  * each frame, preferably dumping them into a CSV file for further analysis to be done using
  * a statistical tool of your choosing. Examples in relevant documentation is likely to be given
  * using R statistics package.
  *
  * An example session using scalac is as follows::
  * {{{
  *  $ ~/scala-XXX/bin/scalac -classpath lavi/build
  *  scala> import lavicore.-;
  *  scala> val comm=Community.importLax("./testdata/test1.lax")
  *  scala> System.out.println(comm)
  *  scala> val a=new AnalystSequenceFrame(comm)
  *  scala> a.setup()
  *  scala> a.run(fname="/tmp/out.csv")
  * }}}
  * Lavi can optionally use graphstream library to visualize the communities::
  * {{{
  *  scala> VisualizerGS.drawCommunity(comm)
  * }}}
  * 
  * 
  * @author Mehmet Gencer, mehmetgencer@yahoo.com
  */
package object lavicore {}