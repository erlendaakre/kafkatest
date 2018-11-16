package kafkatest

import scalaz.zio._
import scalaz.zio.console._
import java.io.IOException
import java.time.Duration
import java.util.Properties

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/*
 * Simple test of using the Java Kafka API from Scala. Produces 50 messages, then reads them
 *
 * Create topic:
 * bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic kafkatest
 */
trait Kafka {
  final val Topic: String = "kafkatest"

  val props: Properties = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("group.id", "kafkatest")
}

trait Producer extends Kafka {

  import scala.concurrent.duration._

  val pSchedule: Schedule[Any, (Int, Int)] = Schedule.recurs(50) && Schedule.spaced(25.millis)
  val producer = new KafkaProducer[String, String](props)
}

trait Consumer extends Kafka {
  val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(java.util.Collections.singletonList(Topic))
  val pollDuration: Duration = java.time.Duration.ofSeconds(5)
}

object Main extends App with Producer with Consumer {
  override def run(args: List[String]): IO[Nothing, Main.ExitStatus] =
    main.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  def main: IO[IOException, Unit] = for {
    _ <- putStrLn("Producing random junk to Kafka")
    p <- produce.repeat(pSchedule).fork
    _ <- p.join *> IO.sync(producer.close()) *> putStrLn("Closed producer\n")
    _ <- putStrLn(s"Polling $Topic for ${pollDuration.toMillis} ms...")
    c <- consume <* IO.sync(consumer.close())
    _ <- putStrLn(s"Consumed ${c.length} messages") <* putStrLn("Done")
  } yield ()

  def produce: IO[Nothing, Unit] = system.nanoTime.flatMap(t =>
    IO.sync(producer.send(new ProducerRecord(Topic, s"hi $t"))) *> putStrLn(s"\tproduced $t"))

  def consume: IO[IOException, List[String]] = {
    import scala.collection.JavaConverters._
    IO.sync(consumer.poll(pollDuration)).map(results => results.asScala.toList.map(record => record.value()))
  }
}