package models

import scala.collection.mutable.ListBuffer

/**
 * @author tjjalava
 * @since 3.9.2013 
 */
case class Entity(_id:Option[Long] = None, name:String, description:String = "") {
  val id:Long = _id.getOrElse(-1)

  var parentId:Long = _
  var depth:Int = _

  private val children:ListBuffer[Entity] = ListBuffer()

  def addChild(entity:Entity) {
    children += entity
  }

  def getChildren:List[Entity] = children.toList

  override def toString = {
    "Entity(\n" +
      "\tid: " + _id.getOrElse("None") + "\n" +
      "\tname: " + name + "\n" +
      "\tdescription: " + description + "\n" +
      "\tparentId: " + parentId + "\n" +
      "\tdepth: " + depth + "\n" +
    ")"
  }

}



