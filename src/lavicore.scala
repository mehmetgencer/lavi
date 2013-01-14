package lavicore
import scala.collection.mutable.{ListBuffer,HashMap};
import scala.xml._;
import java.util.Date;
import org.graphstream.graph.{Graph => GSGraph, Node};
import org.graphstream.graph.implementations.{MultiGraph,SingleGraph};
import java.io.FileWriter;

/**
 * A Debugger object to inject debug messages.
 * 
 * The two methods provided can be used to print single line messages, as well
 * as providing optional arguments to avoid newlines, or to produce
 * progress messages (using \r instead of \n)
 */
object Debugger { 
	/** The current debug level*/
	var level:Int=1
	/**
	 * Prints a debug message to System.err stream.
	 * 
	 * @param msg The message to be written (prepended by "DEBUG: " for visual distinction)
	 * @param level target debug level. Message will be printed only if less than or equal to the current level
	 * @param progress If true a carriage return will be printed instead of a newline, allowing consequent messages to appear on the same line
	 * @param nl if true (and if progress is false) a newline will be printed at the end  
	 */
	def debug(msg:String="", level:Int=1, progress:Boolean=false,nl:Boolean=true) {
		if (progress)
			System.err.print("\r")
		if (nl)
			System.err.print("DEBUG: ")
		System.err.print(msg)
		if (!progress)
			if (nl) 
				System.err.println()
	}
	/**
	 * Shortcut method for debug messages without a newline
	 */
	def debugn(msg:String, level:Int=1, progress:Boolean=false)=debug(msg,level,progress,false)
}

/**
 * Represents a communication act within a community
 * 
 * Each act must be given a unique id, a timestamp (a double value), and a source actor.
 * Not all acts have targets. 
 */
abstract class Act {
    /** The community this act belongs to */
	val comm:Community;
    /** The unique id of act */
	val id: Int
	/** The unique id of actor */
	val src: Int
	/** Time of act. If timestamps of acts represent Datetime (i.e. seconds from epoch)
	 *  this can be indicated in the instance of Community and they will be displayed
	 * appropriately.  
	 */
	val time:Double
	/** Method to get the target actor id, if possible. This will throw an exception
	 * when used for acts of type 'Call'.
	 */
	val directed:Option[Boolean]
	val weight:Option[Float]
	def getTargetActorId:Int
	/** A custom toStr method which allows to represent timestamps as Datetime instances*/
	def toStringCustom(useDate:Boolean):String  
}

/**
 * Represents an act of calling out to the community. This type of communication act does not have
 * a target actor. An example is an email message in a mailgroup which
 * is the root of a message thread.
 */
case class Call(icomm:Community, iid:Int, isrc:Int, itime:Double) extends Act {
    val comm = icomm
	val id: Int = iid
	val src: Int = isrc
	val time:Double = itime
	val directed:Option[Boolean]=None
	val weight:Option[Float]=None
	def getTargetActorId = {throw new Exception("Invalid request from a Call type act")}
	override def toString:String = s"act-call: id:${id}, src=${src}, time=${time}"
	def toStringCustom(useDate:Boolean):String = if (useDate) s"act-call: id:${id}, src=${src}, time=${(new Date(time.toLong*1000)).toString()}" else toString 
}

/**
 * Represents an act of replying to a previous communication act (a Call or Reply).
 * This type of act have a target actor.
 */
case class Reply(icomm:Community, iid:Int, isrc:Int, ireference:Int, itime:Double,idirected:Option[Boolean]=None,iweight:Option[Float]=None) extends Act {
    val comm = icomm
	val id: Int = iid
	val src: Int = isrc
	val reference = ireference
	val time:Double = itime
	val directed=idirected
	val weight=iweight
	def getTargetActorId:Int = {comm.getAct(reference).src}
	override def toString:String = s"act-reply: id:${id}, src=${src}, reference=${reference}, time=${time}" 
	def toStringCustom(useDate:Boolean):String = if (useDate) s"act-reply: id:${id}, src=${src}, reference=${reference}, time=${(new Date(time.toLong*1000)).toString()}" else toString 
}

/** 
 * Represents and actor. It must be given a unique id, and optionally a name
 * (which can be used in visualizations, cross -matching of external actor info, etc.)
 */
class Actor(iid: Int, iname: String="") {
	val id = iid
	val name = if (iname.size>0) iname else iid.toString
	override def toString:String = s"actor: id:${id}, name=${name}" 
}
/**
 * Represents a community.
 * For optimization purposes, it is sealed an will not accept new actors or acts once
 * you use certain lazy fields/methods like actorCount, toString, etc
 * 
 * Whether a community graph is directed or weighted is determined by looking at 
 * its first act. Thus it is the user's responsibility to be consistent.
 * No analysis is provided for mixed networks.
 * 
 * A community is a multigraph by definition (i.e. multiple acts
 * between actors).
 */
class Community (iname:String="",iuseDates:Boolean=false){
	var name=iname
	/** Once project stats are produced, it is sealed to prevent further addition of acts and actors.*/
	var seal:Boolean = false
	/** If true timestamps of acts are interpreted as 'seconds since epoch' representation
	 * of Datetime's
	 */
	var useDates=iuseDates
	/** A map for access to actors by their ids*/
	var actors = new HashMap[Int,Actor]
	/** A map for access to acts by their ids */
	var actsMap = new HashMap[Int,Act]
	/** A time ordered list of acts. Please note that it is the user's responsibility
	 * to make sure the acts are ordered, to ensure correct results from analysis */
	var acts=new ListBuffer[Act]
	def getAct(actId:Int) = {actsMap(actId)}
	lazy val isDirected = acts(0).directed match {
	  case None => true
	  case x:Some[Boolean] => x
	  }
	lazy val isWeighted = acts(0).weight match {
	  case None => false
	  case x:Some[Float] => true
	  }
	lazy val actorCount= {seal=true;actors.size}
	lazy val actCount={seal=true;acts.size}
	/** The time of earliest act*/
	lazy val minTime = {acts.minBy((act:Act) => {act.time})}
	/** The time of latest act */
	lazy val maxTime = {acts.maxBy((act:Act) => {act.time})}
	def addAct(act:Act) = {
		if (!actors.contains(act.src))
			actors.update(act.src,new Actor(act.src))
		actsMap.update(act.id,act)
		acts+=act
	}
	def addActor(actor:Actor) = {
		if (!actors.contains(actor.id))
			actors.update(actor.id,actor)
	}
	def summary:String = {
		return s"Community:${name}\nUse dates:${useDates}\nNum actors: ${actors.size}\nNum acts:${acts.size}\nis directed: ${isDirected}\nis weighted:${isWeighted}"
	}
	override def toString:String = {
		var r=summary+"\nActors:"
		r+=(actors.values.foldLeft("")((a,b) => s"${a.toString}\n  ${b.toString}"))
		r+="\nActs:"
		r+=(acts.foldLeft("")((a,b) => s"${a.toString}\n  ${b.toStringCustom(useDates)}"))
		r
	}
	def printSummary = { System.out.println(summary)}
	def print = {
		System.out.println(summary)
		System.out.println("Actors:")
		actors.values.foreach( (a:Actor) => {System.out.println("  "+a.toString)})
		System.out.println("Acts:")
		acts.foreach( (a:Act) => {System.out.println("  "+a.toStringCustom(useDates))})
	}
}

object Community {
	//private var currentInstance:Option[Community] = None
	//def setCurrentInstance(c:Community,force:Boolean=false) {if(currentInstance!=None && !force) throw new Exception ("Cannot use multiple community instances");else currentInstance=Some(c);}
	//def getCurrentInstance= { if (currentInstance==None) throw new Exception("No current instance"); else currentInstance.get}
	//def getAct(actId:Int) = {getCurrentInstance.actsMap(actId)}
	/** 
	 * Import an XML formatted dataset. Self explanatory examples can be found in the 'testdata' directory
	 * of software distribution.
	 */
	def importLax(fname:String,useDates:Boolean=false,forceReplaceInstance:Boolean=false): Community = {
		val lax = scala.xml.XML.loadFile(fname)
		val comm=new Community((lax \\ "meta" \ "name").text,useDates)
		val actors = (lax \\ "actors" \ "actor").map { node => 
			{
				new Actor((node \ "@id").text.toInt,(node \ "@name").text)
			}
		}
		actors.foreach( (a:Actor) => { 
				comm.addActor(a)
			} )
		val acts = (lax \\ "actions" \ "act").map { node => 
			{
				val id=(node \ "@id").text.toInt
				val acttype=(node \ "@type").text
				val src=(node \ "@src").text.toInt
				val time=(node \ "@time").text.toDouble
				//Debugger.debug(s"Act id: ${id}, type:${acttype}, src: ${src}, time: ${time}")
				if ((node \ "@type").text=="call")
					Call(comm,id,src,time)
				else {
					var directed:Option[Boolean]=None
					if (node contains "@directed")
						 directed=Some((node \ "@directed").text.toBoolean)
					var weight:Option[Float]=None
					if (node contains "@weight")
						 weight=Some((node \ "@weight").text.toFloat)
					Reply(comm,id,src,(node \ "@reference").text.toInt,time,directed,weight)
				}
			}
		}
		acts.foreach( (a:Act) => { 
				//System.out.println(a)
				comm.addAct(a)
			} )
		Debugger.debug("Done importing")
		//setCurrentInstance(comm,forceReplaceInstance)
		comm
	}
}

/** 
 * A class to ease writing out community statistics to CSV files for further analysis
 * outside this software library.
 */
class CSVWriter (fname:String) {
	//val headers=iheaders
	//val cw=new CsvMapWriter(new FileWriter(fname),CsvPreference.STANDARD_PREFERENCE)
	val fp=new FileWriter(fname)
	val headers=new ListBuffer[String]
	var headersWritten=false
	//val hmap= new HashMap[String,String]
	def writeHeaders(line:Map[String,Any])= {
		line.keys.foreach ( (h:String) => {headers+= h;fp.write(h+",")} )
		fp.write("\n")
		//cw.write(hmap)
		headersWritten=true
	}
	def writeLine(line:Map[String,Any]) = {	
		if (!headersWritten) writeHeaders(line)
		//cw.write(line,hmap)
		headers foreach ( (h:String) => {fp.write(line(h)+",")} )
		fp.write("\n")
		fp.flush()
	}
}

/**
 * Represents an ego network of an actor.
 * Analysis routines create and update instances of this class to produce
 * actor statistics during longitudinal analysis.
 * @param istep indicates the longitudinal step (see Analyst class) 
 * in which the actor appears in the community 
 */
class EgoNet(icomm:Community, istep:Int) {
	val comm=icomm
	val stepJoined=istep
	/** contains ids of acts (eg. replies to this actor) */
	val inActs=new ListBuffer[Int] 
	/** ids of reply acts from this actor */
	val outActs=new ListBuffer[Int] 
	def inDegree = {inActs.size}
	def outDegree = {outActs.size}
	def getAct(actId:Int):Act = {comm.getAct(actId)}
	def isInactive={ inActs.size==0&&outActs.size==0}
	def joinedInStep(step:Int) = {stepJoined==step}
	var inActors=new ListBuffer[Int]
	var outActors=new ListBuffer[Int]
	var buildStep= -1
	def buildActorSets(currentStep:Int,force:Boolean=false) = {
		if (buildStep!=currentStep || force) {
			inActors=new ListBuffer[Int]
			outActors=new ListBuffer[Int]
			inActs.foreach( (x:Int) => {if (!inActors.contains(getAct(x).src)) inActors += getAct(x).src})
			outActs.foreach((x:Int) => {if (!outActors.contains(getAct(x).getTargetActorId)) outActors += getAct(x).getTargetActorId})
		}
		(inActors,outActors)
	}
	def overlaps(other:EgoNet,currentStep:Int) = {
		val (inActors,outActors)=buildActorSets(currentStep)
		val (oinActors,ooutActors)=other.buildActorSets(currentStep)
		val ii=inActors.toSet.intersect(oinActors.toSet).size
		val io=inActors.toSet.intersect(ooutActors.toSet).size
		val oo=outActors.toSet.intersect(ooutActors.toSet).size
		val oi=outActors.toSet.intersect(oinActors.toSet).size
		//scala.math.sqrt(ioverlap*ooverlap)
		(ii,io,oo,oi)
	}
}

class Analyst {}
/**
 * Analyzes a community using frames containing a constant number of actions.
 * This approach does not necessarily reflect a constant speed of passing of time
 * since moving average time between acts changes as a community grows (more frequent acts)
 * or shrinks (less frequent acts). 
 */   
class AnalystSequenceFrame (icomm:Community) extends Analyst  {
	val comm = icomm
	var frameSize:Int= -1
	var stepSize:Int= -1
	def useDates=comm.useDates
	//val actors = new HashMap[Int,Actor]
	val actorEgoNets = new HashMap[Int,EgoNet] //mapping from actor ids to their egonets (within frame)
	//val actsMap = new HashMap[Int,Act]
	val acts=new ListBuffer[Act]	
	def setup(frameSizePref:Option[Int]=None) = {
		frameSizePref match {
			case Some(i) => {if (frameSize<=0) throw new Exception("Invalid frame size");frameSize=i}
			case None => { frameSize=comm.actCount/10 }
		}
		if (comm.minTime==comm.maxTime) {
			System.out.println("No time information for the data. Analysis frame options are ignored. Analysis will be done using a single frame (which is probably not very meaningful!)")
			frameSize=comm.actCount
			stepSize=frameSize
		} else {
			stepSize=frameSize/2
		}
	}
	def addAct(a:Act,frameno:Int) = {
		acts+=a
		a match {
			case y:Call => {}
			case y:Reply => { //}(id,src,reference,time) => {
				val target=a.getTargetActorId
				if (!actorEgoNets.contains(a.src)) actorEgoNets(a.src)=new EgoNet(comm,frameno)
				if (!actorEgoNets.contains(target)) actorEgoNets(target)=new EgoNet(comm,frameno)
				actorEgoNets(a.src).outActs+=a.id
				actorEgoNets(target).inActs+=a.id
			}
		}
	}
	def dropAct = {
		val a=acts.remove(0)
		a match {
			case y:Call => {}
			case y:Reply => { //}(id,src,reference,time) => {
				val target=a.getTargetActorId
				actorEgoNets(a.src).outActs -= a.id
				actorEgoNets(target).inActs -= a.id
			}
		}
	}
	def analyseFrame(step:Int,cw:CSVWriter) = {
		Debugger.debug(s"Analysis: frame act count ${acts.size}")
		//acts foreach ( (a:Act) => {Debugger.debugn("  "+ a.id.toString)})
		Debugger.debug()
		val numJoined=actorEgoNets.values.filter( {_.joinedInStep(step)}).size
		val numLeft=actorEgoNets.values.filter( {_.isInactive}).size
		acts foreach ( (a:Act) => {
			a match {
				case y:Call => {}
				case Reply(_,id,src,reference,time,_,_) => {
					val target=comm.actsMap.get(reference).get.src
					val (ii,io,oo,oi)=actorEgoNets(a.src).overlaps(actorEgoNets(target),step)
					val (srci,srco)=actorEgoNets(a.src).buildActorSets(step)
					val srcis=srci.size
					val srcos=srco.size
					cw.writeLine(Map(
						"step" -> step,
						"numjoined" -> numJoined,
						"numleft" -> numLeft,
						"numactors" -> (actorEgoNets.values.filter {! _.isInactive}).size,
						"numacts" -> acts.size,
						"from" -> a.src, 
						"to" -> target,
						"fromInDegree" -> actorEgoNets(a.src).inDegree,
						"toInDegree" -> actorEgoNets(target).inDegree,
						"ii" -> ii,
						"io" -> io,
						"oo" -> oo,
						"oi" -> oi,
						"srcos" -> srcos,
						"srcis" -> srcis
						))
				}
			}	
		})
	}
	def run(fname:String="/tmp/lavi-output.csv") {
		if (frameSize<0)
			throw new Exception("Analyst is not initialized yet")
		var cw=new CSVWriter(fname)
		var i=0
		var j=0
		var step=0
		while (i<comm.actCount) {
			Debugger.debug(s"Analysis step: ${step}")
			val start=step*stepSize
			val end=start+frameSize
			while(j<start) {
				dropAct
				j+=1
			}
			while(i<end&&i<comm.actCount) {
				addAct(comm.acts(i),step)
				i+=1
			}
			if (i==end) {
				analyseFrame(step,cw)
			}
			step+=1
		}
	}
}

/**
 * Visualizer using graphstream library
 * TODO: does not use timing yet
 */
object VisualizerGS {
    var graph = new MultiGraph("Graph",true,true);
    
    def reset(name: String):Unit = {
    	graph=new MultiGraph("name",true,true);
    }
    
    def addNode(a:Actor, useLabel:Boolean=false):Unit = {
      graph.addNode(a.id.toString);
      if (useLabel) {
      	val n:Node=graph.getNode(a.id.toString)
      	n.setAttribute("ui.label",a.name);
      }
    }
    /**
     * Add an edge with label/id 'en', from actor 'f' to actor 't', which can be directed
     */
    def addEdge(en:String,f:String,t:String,d:Boolean):Unit = {
      graph.addEdge(en,f,t,d)
    }
    
    def display():Unit = {
      graph.display()
    }
    
    def drawCommunity(c: Community): Unit = {
    	reset(c.name)
    	c.actors.values.foreach( (a:Actor) => {
    		//System.out.println("Adding node:"+a.id.toString); 
    		addNode(a,false)
    	})
    	c.acts.foreach( (a:Act) => {
    		a match {
    			case y:Call => {}
    			case Reply(_,id,src,reference,time,_,_) =>
    				{	val target=c.actsMap.get(reference).get.src
    					/*if (graph.getNode(target.toString)==null) {
    						System.out.println(s"Adding missing node:${target}");
    						addNode(target.toString)
    					}*/
    					//System.out.println(s"Adding edge:${a.id}");
    					addEdge(a.id.toString, a.src.toString, target.toString,true) 
    				}
    		}
    	})
    	display()
    }
}

object LaviShell extends App {
	override def main(args: Array[String]) = {
		if (args.size>0) {
		 	if (args(0)=="test")
				test()
			else
				analyze(args(0))
		} else {
			System.out.println("Usage:")
		}
	}
	def test() = {
		var comm=Community.importLax("./testdata/test1.lax")
		System.out.println(comm)
		comm=Community.importLax("./testdata/scalauser.lax",true,true)
		System.out.println(comm)
		val a=new AnalystSequenceFrame(comm)
		a.setup()
		a.run()
	}
	def analyze(fname:String) = {
		var comm=Community.importLax(fname,true)
		//CommunityAccessor.comm=comm
		comm.printSummary
		val a=new AnalystSequenceFrame(comm)
		a.setup()
		a.run()
		//VisualizerGS.drawCommunity(comm)
	}
}
