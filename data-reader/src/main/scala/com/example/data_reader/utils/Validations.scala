package com.example.data_reader.utils

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.kernel.Semigroup
import cats.implicits._
import shapeless.{::, HList, HNil}

import scala.util.Try

trait Validator[A, B] extends Validations {

  def validate(from: A): ValidatedNel[String, B]

  def |@|[In <: String, Out](validatorTail: Validator[In, Out]) =
    combine(
      this,
      new Validator[In :: HNil, Out :: HNil] {
        override def validate(from: In :: HNil): ValidatedNel[String, Out :: HNil] = {
          val x :: HNil = from
          validatorTail.validate(x).map(_ :: HNil)
        }
      }
    )

  protected def combine[HIn, HOut, TailIn <: HList, TailOut <: HList](
      validatorH: Validator[HIn, HOut],
      validatorTail: Validator[TailIn, TailOut]
  ): Validator[HIn :: TailIn, HOut :: TailOut] =
    new Validator[HIn :: TailIn, HOut :: TailOut] {
      override def validate(from: HIn :: TailIn): ValidatedNel[String, HOut :: TailOut] = {
        val a :: a1 = from
        (validatorH.validate(a), validatorTail.validate(a1)).mapN {
          case (b, b1) => b :: b1
        }
      }
    }

}

trait Validations {

  trait ValidationTo[T] {
    type ValidationResult = ValidatedNel[String, T]
  }

  implicit class StringToValidated(property: String) extends ValidationTo[String] {
    def mustBeNonEmpty(name: String): ValidationResult =
      if (property.nonEmpty) property.validNel[String] else s"$name cannot be empty".invalidNel[String]
  }

  implicit class OptionToValidated[T](property: Option[T]) extends ValidationTo[T] {
    def mustBeNonEmpty(name: String): ValidationResult =
      Validated
        .fromOption[String, T](property, s"$name is mandatory")
        .toValidatedNel[String, T]
  }

  implicit class OptionStringToValidated(property: Option[String]) extends OptionToValidated[String](property) {
    override def mustBeNonEmpty(name: String): ValidationResult =
      super
        .mustBeNonEmpty(name)
        .andThen[NonEmptyList[String], String](_.mustBeNonEmpty(name))
  }

  implicit object StringSemigroup extends Semigroup[String] {
    override def combine(x: String, y: String): String = s"$x, $y"
  }

  implicit class StringToIntValidated(property: String) extends ValidationTo[String] {
    def mustBeValidNumber(name: String): ValidationResult =
      if (Try(property.toInt).isSuccess) property.validNel[String] else s"$name is invalid".invalidNel[String]

    def mustBeValidNaturalNumber(name: String): ValidationResult =
      mustBeValidNumber(name)
        .andThen(
          p => if (p.toInt > 0) property.validNel[String] else s"$name must be a positive number".invalidNel[String]
        )

    def mustBeValidWholeNumber(name: String): ValidationResult =
      mustBeValidNumber(name)
        .andThen(
          p =>
            if (p.toInt >= 0) property.validNel[String] else s"$name must be 0 or a positive number".invalidNel[String]
        )
  }
}

object Validations extends Validations
