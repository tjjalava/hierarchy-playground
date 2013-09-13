import play.api.Logger
import scala.slick.driver.PostgresDriver
import scala.slick.session.Session
import scala.language.implicitConversions

/**
 * @author tjjalava
 * @since 10.9.2013 
 */
package object dal {
  implicit var session: Session = _

  trait MyPostgresDriver extends PostgresDriver { driver =>
    override def quoteIdentifier(id:String) = super.quoteIdentifier(id.toLowerCase)
  }

  object MyPostgresDriver extends MyPostgresDriver

  def withTiming[T](msg:String)(f: => T): T = {
    val now = System.currentTimeMillis()
    val ret = f
    Logger.debug(msg + " - elapsed " + (System.currentTimeMillis() - now) + " ms")
    ret
  }
}
