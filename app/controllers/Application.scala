package controllers

import play.api.mvc._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import scala.slick.session.Database
import dal.{MyPostgresDriver, EntityDal}
import scala.language.implicitConversions
import models.Entity
import play.api.libs.concurrent.Akka
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global


object Application extends Controller {

  lazy val database = Database.forDataSource(DB.getDataSource("hierarchy"))
  val entityDal = new EntityDal(MyPostgresDriver)

  def withSession[T](f: => T) = {
    database.withSession {
      dal.session = Database.threadLocalSession
      f
    }
  }

  def queryForHierarchy(rootId:Long, depth:Option[Int] = None) = {
    withSession {
      val now = System.currentTimeMillis()
      val entityHierarchy = depth match {
        case Some(x) => entityDal.getEntityHierarchy(rootId, x)
        case None => entityDal.getEntityHierarchy(rootId)
      }
      (System.currentTimeMillis() - now, entityHierarchy)
    }
  }

  def hierarchyToString(eh:List[Entity]) = {
    val buf = new StringBuilder
    var count = 0

    def p(e:Entity) {
      count += 1
      val indent = (for (i <- 0 until e.depth) yield "  ").foldLeft("")(_ + _)
      buf ++= indent + e.id + ": " + e.name + "  " + e.parentId + "  " + e.depth + "\n"
      e.getChildren.foreach(p)
    }

    eh.foreach(p)
    (buf.toString(), count)
  }
  
  def index = Action {
    Ok(views.html.index("Hello world"))
  }

  def hierarchy(rootId:Long, depth:Option[Int]) = Action {
    val promise = Akka.future {
      val (elapsed, entityHierarchy) = queryForHierarchy(rootId, depth)
      val (hs, count) = hierarchyToString(entityHierarchy)
      (elapsed, hs, count)
    }
    Async {
      promise.map(f => Ok("Fetched " + f._3 + " entities in " + f._1 + " ms\n" + f._2))
    }
  }

  def hierarchyJSON(rootId:Long, depth:Option[Int]) = Action {
    val promise = Akka.future {
      val (_, entityHierarchy) = queryForHierarchy(rootId, depth)
      entityHierarchy
    }
    Async {
      promise.map(f => Ok(Json.toJson(f)))
    }
  }

  def createNode = Action(parse.json) { request =>
    request.body.validate[Entity].map {
      case entity => {
        withSession {
          val e = entityDal.addEntity(entity, -1)
          Created(Json.toJson(entityDal.getEntity(e.id)))
        }
      }
    }.recoverTotal {
      e => BadRequest("Detected error:" + JsError.toFlatJson(e))
    }
  }

  def renameNode(nodeId:Long, name:String) = TODO

  def deleteNode(nodeId:Long) = TODO

  def copyNode(nodeId:Long, targetId:Long) = TODO

  def moveNode(nodeId: Long, targetId: Long) = Action {
    if (nodeId == targetId)
      Conflict("Source and target id's can't be the same")
    else Async {
      Akka.future {
        withSession {
          entityDal.moveNode(nodeId, targetId)
        }
      } map(f => NoContent)
    }
  }
}