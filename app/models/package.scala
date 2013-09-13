import play.api.libs.json._
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/**
 * @author tjjalava
 * @since 10.9.2013 
 */
package object models {

  implicit object EntityWrites extends Writes[Entity] {
    def writes(o: Entity): JsValue = {
      JsObject(Seq(
        "id" -> Json.toJson(o._id),
        "name" -> JsString(o.name),
        "description" -> JsString(o.description),
        "parentId" -> JsNumber(o.parentId),
        "hasChildren" -> JsBoolean(o.hasChildren)
        //"children" -> Json.toJson(o.getChildren)
      ))
    }
  }

}
