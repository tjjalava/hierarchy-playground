package dal

import scala.slick.driver.ExtendedProfile
import models.Entity
import scala.slick.lifted.ForeignKeyAction._
import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import Q.interpolation
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

/**
 * @author tjjalava
 * @since 6.9.2013 
 */
class EntityDal(val driver: ExtendedProfile) {
  import driver.simple._

  object Entities extends Table[Entity]("ENTITY") {
    def _id = column[Long]("id", O.PrimaryKey, O.AutoInc, O.Nullable)
    def name = column[String]("NAME")
    def description = column[String]("DESCRIPTION")
    def * = _id.? ~ name ~ description <> (Entity, Entity.unapply _)
    def autoInc = name ~ description <> ({ (n,d) => Entity(None, n, d) }, { e:Entity => Some((e.name, e.description))}) returning _id
  }

  object EntityHierarchy extends Table[(Long, Long, Int)]("ENTITY_HIERARCHY") {
    def ancestor = column[Long]("ANCESTOR")
    def descendant = column[Long]("DESCENDANT")
    def depth = column[Int]("DEPTH")
    def pk = primaryKey("pk", (ancestor, descendant))
    def fkAncestor = foreignKey("fk_an", ancestor, Entities)(_._id, Restrict, Restrict)
    def fkDecendant = foreignKey("fk_de", descendant, Entities)(_._id, Restrict, Restrict)
    def * = ancestor ~ descendant ~ depth
  }

  private val ddl = Entities.ddl ++ EntityHierarchy.ddl

  def create(implicit session: Session) = ddl.create

  def drop(implicit session: Session) = ddl.drop

  def addEntity(entity: Entity, parentId:Long = -1) = {
    val newId = Entities.autoInc insert entity
    EntityHierarchy.insert((newId, newId, 0))

    if (parentId > 0) {
      (Q.u +
        "insert into ENTITY_HIERARCHY " +
          "select p.ANCESTOR, c.DESCENDANT, p.DEPTH+c.DEPTH+1 " +
          "from ENTITY_HIERARCHY p, ENTITY_HIERARCHY c " +
          "where p.DESCENDANT = " +? parentId + " and c.ANCESTOR = " +? newId
        ).execute()
    }

    Entity(Some(newId), entity.name, entity.description)
  }

  def getEntity(id:Long) = {
    Query(Entities).filter(_._id === id).firstOption() match {
      case Some(e) => e
      case None => throw new Exception("Not found")
    }
  }

  def getEntityHierarchy(root:Long, maxDepth:Int = Int.MaxValue) = {

    implicit val getHierarchyResult = GetResult(r => {
      val e = Entity(Some(r.<<), r.<<, r.<<)
      e.parentId = r.<<
      e.depth = r.<<
      e
    })



    sql"""
      select e."id", e.name, e.description, h.ancestor, max(g.depth) as level
      from entity e
      join entity_hierarchy h on e."id" = h.descendant
      join entity_hierarchy g on e."id" = g.descendant
      where h.depth = 1 and e."id" in (
        select descendant from entity_hierarchy where ancestor = $root and depth <= $maxDepth
      )
      group by e."id", h.ancestor, h.depth order by level, h.ancestor, e."id"
    """.as[Entity].list()

  }
}

object EntityDal {
  class EntityListHierarchy(list:List[Entity]) {
    def asHierarchy = {
      val tmpMap = collection.mutable.Map[Long, Entity]()
      val rootList = ListBuffer[Entity]()
      list.foreach(e => {
        tmpMap get e.parentId match {
          case Some(x) => x addChild e
          case None => rootList += e
        }
        tmpMap put (e.id, e)
      })
      rootList.toList
    }
  }

  implicit def asHierarchy(list:List[Entity]) = new EntityListHierarchy(list)

}
