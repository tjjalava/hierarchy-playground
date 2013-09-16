package models

import scala.collection.mutable.ListBuffer

/**
 * @author tjjalava
 * @since 3.9.2013 
 */
case class Entity(_id:Option[Long] = None,
                  name:String,
                  description:String = "",
                  parentId:Long = -1,
                  hasChildren:Boolean = false) {

  val id:Long = _id.getOrElse(-1)
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

object Entity {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  val entityReads = (
    (__ \ "id").readNullable[Long] and
      (__ \ "name").read[String] and
      (__ \ "description").read[String] and
      JsPath.read[Long](-1) and
      JsPath.read[Boolean](false)
    )(Entity.apply _)

  val entityWrites = (
    (__ \ "id").write[Option[Long]] and
      (__ \ "name").write[String] and
      (__ \ "description").write[String] and
      (__ \ "parentId").write[Long] and
      (__ \ "hasChildren").write[Boolean]
    )(unlift(Entity.unapply))

  implicit val entityFormat = Format(entityReads, entityWrites)
}




