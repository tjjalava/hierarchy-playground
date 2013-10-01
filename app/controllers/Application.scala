package controllers

import play.api.mvc._
import play.api.db._
import play.api.libs.json._
import play.api.Play.current
import scala.slick.session.Database
import dal.{GraphDbEntityDalComponent, EntityDal, ClosureTableEntityDalComponent, MyPostgresDriver}
import scala.language.implicitConversions
import models.Entity
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import service.EntityServiceComponentImpl
import scala.util.DynamicVariable
import play.Logger
import play.api.Play


object Application extends Controller {

  lazy val database = Database.forDataSource(DB.getDataSource("hierarchy"))

  def withSession[T](f: => T) = {
    database.withSession {
      dal.session = Database.threadLocalSession
      f
    }
  }

  def withBackend[T](f:EntityDal => T) = {
    f(DalChoiceAction.backend.value)
  }

  def index = Action {
    Ok(views.html.index("Hello world"))
  }

  def hierarchyJSON(rootId:Long, depth:Option[Int]) = DalChoiceAction.async {
    withBackend { entityDal =>
      Future {
        withSession {
          val entityHierarchy = depth match {
            case Some(x) => entityDal.getEntityHierarchy(rootId, x)
            case None => entityDal.getEntityHierarchy(rootId)
          }
          Ok(Json.toJson(entityHierarchy))
        }
      }
    }
  }

  def createRootNode = createNode(-1)

  def createNode(parentNode:Long) = DalChoiceAction.async(parse.json) { request =>
    withBackend { entityDal =>
      Future {
        request.body.validate[Entity].map {
          case entity => withSession {
            Created(Json.toJson(entityDal.getEntity(entityDal.addEntity(entity, parentNode).id)))
          }
        }.recoverTotal {
          e => BadRequest("Detected error:" + JsError.toFlatJson(e))
        }
      }
    }
  }

  def updateNode(nodeId:Long) = DalChoiceAction.async(parse.json) { request =>
    withBackend { entityDal =>
      Future {
        request.body.validate[Entity].map {
          case entity => withSession {
            entityDal.updateEntity(nodeId, entity)
            Accepted
          }
        }.recoverTotal {
          e => BadRequest("Detected error:" + JsError.toFlatJson(e))
        }
      }
    }
  }

  def deleteNode(nodeId:Long) = DalChoiceAction.async {
    withBackend { entityDal =>
      Future {
        withSession {
          entityDal.deleteEntity(nodeId)
          Accepted
        }
      }
    }
  }

  def copyNode(nodeId:Long, targetId:Long) = TODO

  def moveNode(nodeId: Long, targetId: Long) = DalChoiceAction.async {
    withBackend { entityDal =>
      Future {
        if (nodeId == targetId)
          Conflict("Source and target id's can't be the same")
        else withSession {
          entityDal.moveNode(nodeId, targetId)
          NoContent
        }
      }
    }
  }
}

object DalChoiceAction extends ActionBuilder[Request] {

  private lazy val closureBackend = new EntityServiceComponentImpl with ClosureTableEntityDalComponent {
    val driver = MyPostgresDriver
  }.entityDal

  private lazy val graphDbBackend = new EntityServiceComponentImpl with GraphDbEntityDalComponent {
    val dbDir: String = Play.current.configuration.getString("neo4j.embedded.dir").get
  }.entityDal

  val backend = new DynamicVariable[EntityDal](null)

  protected def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    request.headers.get("X-BACKEND") match {
      case Some("SQL") => {
        Logger.debug("Backend: Closure Tables")
        backend.value = closureBackend
      }
      case Some("GRAPH") => {
        Logger.debug("Backend: GraphDb")
        backend.value = graphDbBackend
      }
      case _ => {
        Logger.debug("Backend (default): Closure Tables")
        backend.value = closureBackend
      }
    }

    block(request)
  }
}