package io.netflow.flows

import java.net.InetAddress

import com.datastax.driver.core.Row
import com.websudos.phantom.Implicits._
import io.wasted.util.{ InetPrefix, Tryo }
import org.joda.time.DateTime

case class FlowSenderRecord(ip: InetAddress, last: Option[DateTime], prefixes: Set[InetPrefix]) {
}

sealed class FlowSender extends CassandraTable[FlowSender, FlowSenderRecord] {

  object ip extends InetAddressColumn(this) with PartitionKey[InetAddress]
  object last extends OptionalDateTimeColumn(this)
  object prefixes extends SetColumn[FlowSender, FlowSenderRecord, String](this)

  private def string2prefix(str: String): Option[InetPrefix] = {
    val split = str.split("/")
    if (split.length != 2) None else for {
      len <- Tryo(split(1).toInt)
      base <- Tryo(InetAddress.getByName(split(0)))
    } yield InetPrefix(base, len)
  }

  private implicit val strings2prefixes = (x: Set[String]) => x.flatMap(string2prefix)

  override def fromRow(row: Row): FlowSenderRecord = {
    FlowSenderRecord(ip(row), last(row), prefixes(row))
  }
}

object FlowSender extends FlowSender

case class FlowSenderCountRecord(ip: InetAddress, flows: Long, dgrams: Long)

sealed class FlowSenderCount extends CassandraTable[FlowSenderCount, FlowSenderCountRecord] {

  object ip extends InetAddressColumn(this) with PartitionKey[InetAddress]
  object dgrams extends CounterColumn(this)
  object flows extends CounterColumn(this)

  override def fromRow(row: Row): FlowSenderCountRecord = {
    FlowSenderCountRecord(ip(row), flows(row), dgrams(row))
  }
}

object FlowSenderCount extends FlowSenderCount