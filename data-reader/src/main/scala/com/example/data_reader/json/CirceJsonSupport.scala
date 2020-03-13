package com.example.data_reader.json

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.{Decoder, Encoder}

trait CirceJsonSupport extends FailFastCirceSupport with CirceMarshallers with AutoDerivation {

  import shapeless._
  implicit def encoderValueClass[T <: AnyVal, V](
      implicit
      g: Lazy[Generic.Aux[T, V :: HNil]],
      e: Encoder[V]
  ): Encoder[T] = Encoder.instance { value =>
    e(g.value.to(value).head)
  }
  implicit def decoderValueClass[T <: AnyVal, V](
      implicit
      g: Lazy[Generic.Aux[T, V :: HNil]],
      d: Decoder[V]
  ): Decoder[T] = Decoder.instance { cursor =>
    d(cursor).map { value =>
      g.value.from(value :: HNil)
    }
  }

  protected val camelCaseConfig: Configuration = Configuration.default.withDefaults
  protected val snakeCaseConfig: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  protected implicit val circeConfiguration: Configuration = snakeCaseConfig
}
