/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zio.macros.accessible

import com.github.ghik.silencer.silent
import zio.macros.core.ModulePattern

import scala.reflect.macros.whitebox.Context

private[macros] class Macro(val c: Context) extends ModulePattern {
  import c.universe._

  case class Config(name: Option[String])

  def apply(annottees: c.Tree*): c.Tree = {

    @silent("pattern var [^\\s]+ in method unapply is never used")
    val config: Config = c.prefix.tree match {
      case Apply(_, args) =>
        val name = args.collectFirst { case q"$cfg" => c.eval(c.Expr[String](cfg)) }.map { ident =>
          util.Try(c.typecheck(c.parse(s"object $ident {}"))) match {
            case util.Failure(_) =>
              c.abort(c.enclosingPosition, s"""Invalid identifier "$ident". Cannot generate accessors object.""")
            case util.Success(_) => ident
          }
        }

        Config(name)
      case other => c.abort(c.enclosingPosition, s"Invalid accessible macro call ${showRaw(other)}")
    }

    val trees            = extractTrees(annottees)
    val module           = extractModule(trees.module)
    val companion        = extractCompanion(trees.companion)
    val service          =
      companion.flatMap(c => extractService(c.body))
    val capabilites      = extractCapabilities(service.getOrElse(ServiceSummary(module.previousSiblings, module.nextSiblings, module.mods, module.typeParams, module.earlyDefinitions, module.parents, module.self, module.body)))
    val accessors        = generateCapabilityAccessors(module.name, module.serviceName.getOrElse(TermName(module.name.toString.head.toLower + module.name.toString.tail)), capabilites, service.isEmpty)
    val updatedCompanion = generateUpdatedCompanion(config, module, companion, accessors, service.isEmpty)

    q"""
       ${trees.module}
       $updatedCompanion
     """
  }

  private def generateCapabilityAccessors(
    moduleType: TypeName,
    serviceName: TermName,
    capabilities: List[Capability],
    structuralTyping: Boolean
  ): List[Tree] =
    capabilities.map { capability =>
      val (name, e, a) = (capability.name, capability.error, capability.value)
      val mods =
        if (capability.impl == EmptyTree) Modifiers()
        else Modifiers(Flag.OVERRIDE)

      val returnType = if (structuralTyping) (if (capability.env.toString != "Any") tq"_root_.zio.ZIO[${capability.env} {val $serviceName: $moduleType}, $e, $a]" else tq"_root_.zio.ZIO[{val $serviceName: $moduleType}, $e, $a]") else tq"_root_.zio.ZIO[$moduleType, $e, $a]"
      val returnValue =
        capability.argLists match {
          case Some(argLists) if argLists.flatten.nonEmpty =>
            val argNames = argLists.map(_.map(_.name))
            q"_root_.zio.ZIO.accessM(_.$serviceName.$name(...$argNames))"
          case _ =>
            q"_root_.zio.ZIO.accessM(_.$serviceName.$name)"
        }

      capability.argLists match {
        case None            => q"$mods val $name: $returnType = $returnValue"
        case Some(Nil)       => q"$mods def $name: $returnType = $returnValue"
        case Some(List(Nil)) => q"$mods def $name(): $returnType = $returnValue"
        case Some(argLists)  => q"$mods def $name(...$argLists): $returnType = $returnValue"
      }
    }

  private def generateUpdatedCompanion(
    config: Config,
    module: ModuleSummary,
    companion: Option[CompanionSummary],
    capabilityAccessors: List[Tree],
    structuralTyping: Boolean
  ): Tree = {

    val accessor: Tree = config.name match {
      case Some(name) => c.parse(s"object $name extends Accessors")
      case None => EmptyTree
    }

    companion.fold {
      q"""
      object ${module.name.toTermName} {

       trait Accessors {
         ..$capabilityAccessors
       }

       $accessor
     }
    """
    } { companion =>
      if (!structuralTyping)
        q"""
      object ${module.name.toTermName} {

        ..${companion.body}

       trait Accessors extends Service[${module.name}]  {
         ..$capabilityAccessors
       }

       $accessor
     }
    """
      else
        q"""
      object ${module.name.toTermName} {

        ..${companion.body}

       trait Accessors {
         ..$capabilityAccessors
       }

       $accessor
     }
    """
    }
  }
}
