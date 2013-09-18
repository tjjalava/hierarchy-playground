package dal

import scala.slick.driver.ExtendedProfile
import models.Entity
import scala.slick.lifted.ForeignKeyAction._
import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import Q.interpolation
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import play.api.Logger._

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
    def * = _id.? ~ name ~ description <> ({ (i,n,d) => Entity(i, n, d) }, { e:Entity => Some((e._id, e.name, e.description))})
    def autoInc = name ~ description <> ({ (n,d) => Entity(None, n, d) }, { e:Entity => Some((e.name, e.description))}) returning _id
    def update = name ~ description <> ({ (n,d) => Entity(None, n, d) }, { e:Entity => Some((e.name, e.description))})
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
    withTiming("Adding entity") {
      session.withTransaction {
        val newId = Entities.autoInc insert entity
        EntityHierarchy.insert((newId, newId, 0))

        sqlu"""
        insert into ENTITY_HIERARCHY
        select p.ANCESTOR, c.DESCENDANT, p.DEPTH+c.DEPTH+1
        from ENTITY_HIERARCHY p, ENTITY_HIERARCHY c
        where p.DESCENDANT = $parentId and c.ANCESTOR = $newId
       """.execute()

        Entity(Some(newId), entity.name, entity.description)
      }
    }
  }

  def getEntity(id:Long) = {
    getEntityHierarchy(id, 1).headOption match {
      case Some(e) => e
      case None => throw new Exception("Not found")
    }
  }

  def getEntityHierarchy(root:Long, maxDepth:Int = Int.MaxValue) = {

    implicit val getHierarchyResult = GetResult(r => {
      Entity(Some(r.<<), r.<<, r.<<, r.<<, r.<<[Int] > 0)
    })

    withTiming("EntityHierarchy queried") {
      sql"""
      select e."id", e.name, e.description, h.ancestor,
        (select count(f.*) from entity_hierarchy f where f.ancestor = e."id" and f.depth = 1) as child_count
      from entity e
      join entity_hierarchy h on e."id" = h.descendant
      where h.depth = 1 and e."id" in (
        select descendant from entity_hierarchy
        where (ancestor = $root and depth <= $maxDepth)
        or (descendant = $root and depth = 0)
      )
      order by h.ancestor, e."id"
      """.as[Entity].list()
    }
  }

  def updateEntity(nodeId:Long, entity:Entity) {
    withTiming("Update entity") {
      session.withTransaction {
        val f = for { e <- Entities if e._id === nodeId } yield e.update
        f.update(entity)
      }
    }
  }

  def deleteEntity(nodeId:Long) {
    withTiming("Delete entity") {
      session.withTransaction {
        val toDelete = (for { e <- EntityHierarchy if e.ancestor === nodeId} yield e.descendant).list()
        (EntityHierarchy where (_.descendant inSet toDelete)).delete
        (Entities where (_._id inSet toDelete)).delete
      }
    }

  }

  def moveNode(nodeId:Long, targetId:Long) {
    logger.debug("Moving node " + nodeId + " under " + targetId)

    withTiming("Moving node from " + nodeId + " to " + targetId) {
      session.withTransaction {
      val deleted = sqlu"""
        delete from entity_hierarchy
        where descendant in (select descendant from entity_hierarchy where ancestor = $nodeId)
        and ancestor not in (select descendant from entity_hierarchy where ancestor = $nodeId)
      """.first()

        logger.debug("Deleted " + deleted + " rows")

        if (targetId >= 0) {

          val inserted = sqlu"""
          insert into entity_hierarchy (ancestor, descendant, depth)
            select supertree.ancestor, subtree.descendant, supertree.depth + subtree.depth + 1
            from entity_hierarchy supertree
            cross join entity_hierarchy subtree
            where subtree.ancestor = $nodeId and supertree.descendant = $targetId
          """.first()

          logger.debug("Inserted " + inserted + " rows")
        }
      }
    }
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
