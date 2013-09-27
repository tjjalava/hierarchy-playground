package dal

import models.Entity
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import scala.sys.ShutdownHookThread

/**
 * @author tjjalava
 * @since 27.9.2013 
 */
trait GraphDbEntityDalComponent extends EntityDalComponent {

  val dbDir:String

  def entityDal: EntityDal = new GraphDbEntityDal

  class GraphDbEntityDal extends EntityDal {

    val graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(dbDir)
    ShutdownHookThread {
      graphDb.shutdown()
    }

    def addEntity(entity: Entity, parentId: Long): Entity = ???

    def getEntity(id: Long): Entity = ???

    def getEntityHierarchy(root: Long, maxDepth: Int): List[Entity] = ???

    def updateEntity(nodeId: Long, entity: Entity) {}

    def deleteEntity(nodeId: Long) {}

    def moveNode(nodeId: Long, targetId: Long) {}
  }

}
