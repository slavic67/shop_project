import com.notification.domain.command.ProcessPaymentCommand
import org.junit.jupiter.api.Test


class NamingClass {


    @Test
    fun printClassName() {
        println(ProcessPaymentCommand::class.java.name)
    }
}