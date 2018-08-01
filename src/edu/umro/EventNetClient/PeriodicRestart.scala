package edu.umro.EventNetClient

import edu.umro.util.Exec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Calendar
import java.util.logging.Logger

class PeriodicRestart(RestartTime: Long) extends Runnable {
    private val log = Logger.getLogger(this.getClass.getName())

    /**
     * Wait for the given number of milliseconds, restarting the clock if there
     * is an <code>InterruptedException</code>.
     *
     * @param ms Number of millisecond to wait.
     */
    private def sleep(ms: Long) = {

        def now = System.currentTimeMillis
        val finish = now + ms;
        def remaining = finish - System.currentTimeMillis;

        while (finish > now) {
            try {
                Thread.sleep(remaining);
            }
            catch {
                case e: InterruptedException => log.severe("Unexpected exception while sleeping in PeriodicRestart: " + e)
            }
        }
    }

    /**
     * Determine how long the service has to wait until the next restart time.
     *
     * @return Time in milliseconds until next restart time.
     */
    private def waitTime: Long = {
        val oneDay: Long = 24 * 60 * 60 * 1000 // The length of one day in milliseconds        
        val now = new GregorianCalendar

        val restart = new GregorianCalendar
        restart.setTimeInMillis(RestartTime)

        val stop = new GregorianCalendar
        stop.setTimeInMillis(now.getTimeInMillis)
        stop.set(Calendar.HOUR_OF_DAY, restart.get(Calendar.HOUR_OF_DAY))
        stop.set(Calendar.MINUTE, restart.get(Calendar.MINUTE))
        stop.set(Calendar.SECOND, 0)
        stop.set(Calendar.MILLISECOND, 0)

        val interval = (stop.getTimeInMillis - now.getTimeInMillis + oneDay) % oneDay

        val stopDesc = new Date(if (stop.getTimeInMillis <= now.getTimeInMillis) stop.getTimeInMillis + oneDay else stop.getTimeInMillis).toString
        val intervalDesc = "" + (interval / (1000 * 60 * 60)) + ((interval / (1000 * 60)) % 60).formatted(":%02d")
        log.info("Service restart time" + stopDesc + "    Wait time before restarting: " + intervalDesc)
        interval
    }

    /**
     * Wait until the restart time specified in the configuration file, and then
     * terminate this service.  The YAJSW framework will restart a service when
     * it terminates with a non-zero status.
     */
    def run: Unit = {
        sleep(waitTime)
        log.info("Intentionally terminating service to initiate service restart.")
        System.exit(1)
    }

    (new Thread(this)).start

}