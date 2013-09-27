package service

import models.Entity

/**
 * @author tjjalava
 * @since 27.9.2013 
 */
trait EntityServiceComponent {
  def entityService: EntityService

  trait EntityService {
    def addEntity(entity: Entity, parentId:Long = -1):Entity
    def getEntity(id:Long):Entity
    def getEntityHierarchy(root:Long, maxDepth:Int = Int.MaxValue):List[Entity]
    def updateEntity(nodeId:Long, entity:Entity)
    def deleteEntity(nodeId:Long)
    def moveNode(nodeId:Long, targetId:Long)
  }

}
