package service

import dal.EntityDalComponent
import models.Entity

/**
 * @author tjjalava
 * @since 27.9.2013 
 */
trait EntityServiceComponentImpl extends EntityServiceComponent {
  this: EntityDalComponent =>

  def entityService: EntityService = new EntityServiceImpl

  class EntityServiceImpl extends EntityService{
    def addEntity(entity: Entity, parentId: Long): Entity = entityDal.addEntity(entity, parentId)

    def getEntity(id: Long): Entity = entityDal.getEntity(id)

    def getEntityHierarchy(root: Long, maxDepth: Int): List[Entity] = entityDal.getEntityHierarchy(root, maxDepth)

    def updateEntity(nodeId: Long, entity: Entity) {
      entityDal.updateEntity(nodeId, entity)
    }

    def deleteEntity(nodeId: Long) {
      entityDal.deleteEntity(nodeId)
    }

    def moveNode(nodeId: Long, targetId: Long) {
      entityDal.moveNode(nodeId, targetId)
    }
  }
}
