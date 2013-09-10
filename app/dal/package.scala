import scala.slick.driver.PostgresDriver
import scala.slick.session.Session

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
}
