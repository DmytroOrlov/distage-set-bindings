package com.example.data_reader.json

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, OffsetDateTime}

import cats.syntax.either._
import io.circe.{Decoder, Encoder}

trait CirceMarshallers {

  import CirceMarshallers._

  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] =
    Encoder.encodeString.contramap[OffsetDateTime](_.format(dateTimeFormatter))

  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(OffsetDateTime.parse(str, dateTimeFormatter))
        .leftMap(_ => """Required ISO date time format such as "2011-12-03T10:15:30+01:00"""")
    }

  protected val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  implicit val dateEncoder: Encoder[LocalDate] =
    Encoder.encodeString.contramap[LocalDate](_.format(dateFormatter))

  implicit val dateDecoder: Decoder[LocalDate] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(LocalDate.parse(str, dateFormatter))
        .leftMap(_ => "Required date format DD/MM/YYYY")
    }
}

object CirceMarshallers extends CirceMarshallers {

  val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
}
