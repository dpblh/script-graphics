package script.graphics

/**
 * Created by tim on 06.01.16.
 */
object CompileJs {

  def compile(literal: Grammar)(implicit stack: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()): String = {
    literal match {
      case `This`(call) =>
        call match {
          case Some(x) => s"this.${compile(x)}"
          case _ => "this"
        }
      case NameLiteral(name) => name
      case NumberLiteral(number) => number.toString
      case StringLiteral(string) => s"'$string'"
      case ArrayLiteral(items) => s"[${items.map(compile).mkString(",")}]"
      case StringInterpolateLiteral(sequence) => s"${sequence.map(i => s"(${compile(i)})").mkString("+")}"
      case ExpressionPartial(operator, operand) => s"${compile(operator)} ${compile(operand)}"
      case ExpressionList(head, tail) => s"${compile(head)} ${tail.map(compile).mkString("")}"
      case Not(expression) => s"!${compile(expression)}"
      case UpdateArray(pipe, index, value) => s"${compile(pipe)}[${compile(index)}] = ${compile(value)}"
      case UpdateProperty(pipe, value) => s"${compile(pipe)} = ${compile(value)}"
      case NewInstance(pipe) => s"new ${compile(pipe)}"
      case Comment(str) => ""
      case s@Script(statements) =>

        val (string, innerStack) = returnStatement(s)
        innerStack--=stack

        s"""(function(){
           |  ${defineVariables(innerStack)}
           |  $string
           |})();
         """.stripMargin
      case Block(statements) =>
        statements match {
          case Nil => ""
          case _ =>
            val head :: tail = statements.reverse
            val strictHead = tail.sortWith((a, b) => a.isInstanceOf[Strict])
            s"""${strictHead.reverse.map(compile).mkString(";\n  ")} ${if (strictHead.nonEmpty) ";" else "" }
                |return ${compile(head)};""".stripMargin
        }
      case MatchCase(exp, statements) =>
        statements match {
          case Nil => ""
          case _ =>
            val head :: tail = statements.reverse
            val strictHead = tail.sortWith((a, b) => a.isInstanceOf[Strict])

            s"""if (${compile(exp)}){
                | ${strictHead.reverse.map(compile).mkString(";\n  ")} ${if (strictHead.nonEmpty) ";" else "" }
                | result = ${compile(head)};
                |}
                |else""".stripMargin
        }

      case Match(exp, variableName, matches) =>
        val name = compile(variableName.getOrElse(NameLiteral("x")))
        val matchStack = scala.collection.mutable.Set[String](name, "result")
        val maths = matches.map( i => compile(i)(matchStack)).mkString("\n  ")
        s"""(function(){
           | ${defineVariables(matchStack)}
           | $name = ${compile(exp)};
           | $maths {
           |   throw 'Math error'
           | }
           | return result;
           |})()""".stripMargin
      case Lambda(arguments, body) =>
        val args = arguments.map(_.name)
        val (string, innerStack) = returnStatement(body)
        innerStack--=stack
        s"""function(${args.map(compile).mkString(",")}){
           |  ${defineVariables(innerStack)}
           |  ${predefArguments(arguments)}
           |  $string
           |}""".stripMargin
      case InstanceMethod(name, arguments, body) =>
        val args = arguments.map(_.name)
        val (string, innerStack) = returnStatement(body)
        innerStack--=stack

        s"""function ${compile(name)} (${args.map(compile).mkString(",")}) {
            | ${defineVariables(innerStack)}
            | ${predefArguments(arguments)}
            | $string
            |}""".stripMargin
      case Loop(property, value, from, body) =>
        val fromString = compile(from)
        val propertyString = compile(property)
        s"""for (var $propertyString in $fromString) {
           |  var ${compile(value)} = $fromString[$propertyString];
           |  ${compile(body)}
           |}""".stripMargin
      case InstanceMethodCurrying(name, argumentsCurrying, arguments, body) =>
        val argsCurrying = argumentsCurrying.map(_.name)
        val args = arguments.map(_.name)
        val (string, innerStack) = returnStatement(body)
        innerStack--=stack--=argsCurrying.map(compile).toSet

        s"""function ${compile(name)} (${argsCurrying.map(compile).mkString(",")}) {
            | ${predefArguments(argumentsCurrying)}
            | return function (${args.map(compile).mkString(",")}){
            |   ${defineVariables(innerStack)}
            |   ${predefArguments(arguments)}
            |   $string
            | }
            |}""".stripMargin
      case ClassMethod(name, arguments, body) =>
        val args = arguments.map(_.name)
        val (string, innerStack) = returnStatement(body)
        innerStack--=stack

        s"""function (${args.map(compile).mkString(",")}) {
            | ${defineVariables(innerStack)}
            | ${predefArguments(arguments)}
            | $string
            |};""".stripMargin
      case Operator(symbol) =>
        symbol match {
          case "and" => "&&"
          case "or" => "||"
          case "is" => "instanceof"
          case x => x
        }
      case CallFromObject(name, parameters) =>s".${compile(name)}(${formatParameter(parameters)})"
      case CallAnonymous(parameters) => s"(${formatParameter(parameters)})"
      case CallArray(index) => s"[${compile(index)}]"
      case Pipeline(pipe) => s"${pipe.map(compile).mkString("")}"
      case CallMethod(name, parameters) => s"${compile(name)}(${formatParameter(parameters)})"
      case Property(name) => s".${compile(name)}"
      //        TODO
      case VariableDefinition(name, value) =>
        val name2 = compile(name)
        stack+=name2
        s"$name2 = ${compile(value)}"
      case Strict(name, arguments, methods) =>
        val args = arguments.map(_.name)
        val instanceMethod = methods.filter(m => m.isInstanceOf[InstanceMethod] || m.isInstanceOf[InstanceMethodCurrying])
        val classMethod = methods.filter(_.isInstanceOf[ClassMethod])

        val name2 = compile(name)

        stack+=name2

        val scriptStack = stack++scala.collection.mutable.Set(args.map(_.a):_*)

        s"""$name2 = (function(){
           |  function temp (${args.map(compile).mkString(",")}){
           |    var that = {};
           |    ${predefArguments(arguments)}
           |    ${args.map(i => s"that.${compile(i)} = ${compile(i)}").mkString(";\n  ")};
           |    ${instanceMethod.map(i => s"that.${compile(i.name)} = ${compile(i.name)}").mkString(";\n  ")};
           |    ${instanceMethod.map(i => compile(i)(scriptStack)).mkString("\n  ")}
           |    return that;
           |  }
           |  ${classMethod.map(i => s"temp.${compile(i.name)} = ${compile(i)}").mkString("\n  ")}
           |  return temp;
           |})()""".stripMargin
    }
  }

  private def formatParameter(parameters: ParametersWithBlock): String = parameters match {
    case ParametersWithBlock(params, block) =>
      val b = block match {
        case Some(x) =>
          params match {
            case SimpleParameters(p) if p.nonEmpty => s",${compile(x)}"
            case SimpleParameters(p) if p.isEmpty => s"${compile(x)}"
            case _ => s",${compile(x)}"
          }
        case _ => ""
      }

      s"${formatP(params)} $b"
  }

  private def formatP(parameters: Parameters):String = parameters match {
    case NamedParameters(params) =>
      s"""{${params.map(i => s"${compile(i.name)}:${compile(i.expression)}").mkString(",")}}""".stripMargin
    case SimpleParameters(params) =>
      s"""${params.map(i => compile(i.expression)).mkString(",")}""".stripMargin
  }

  private def predefArguments(arguments: List[Argument]): String = {
    val args = arguments.map(_.name)
    val defaults = arguments.filter(_.default.isDefined)
    val named = args.nonEmpty match {
      case true =>
        s"""var args = Array.prototype.slice.call(arguments);
           |var hashMap, block;
           |block = args[args.length-1];
           |hashMap = args[0];
           |if (args.length > ${args.length}) {
           |  ${args.map( i => s"${compile(i)} = args[0].${compile(i)}").mkString(";\n  ")};
           |}""".stripMargin
      case false =>
        """var block = arguments[arguments.length-1]
          |var hashMap = arguments[0];
        """.stripMargin
    }
    s"""$named
       |${defaults.map(i => s"  if (${compile(i.name)} === undefined)  ${compile(i.name)} = ${compile(i.default.get)}").mkString("\n  ")}""".stripMargin
  }

  private def returnStatement(body: Statement):(String, scala.collection.mutable.Set[String]) = {
    implicit val stack = scala.collection.mutable.Set[String]()
    val st = body match {
      case Block(x) => compile(body)
      case Script(x) =>
        x match {
          case Nil => ""
          case _ =>
            val head :: tail = x.reverse
            val scriptHead = tail.sortWith((a, b) => b.isInstanceOf[Strict])
            s"""${scriptHead.reverse.map(compile).mkString(";\n  ")} ${if (scriptHead.nonEmpty) ";" else "" }
                |return ${compile(head)};""".stripMargin
        }
      case BlockMethod(x) =>
        x match {
          case Nil => ""
          case _ =>
            val head :: tail = x.reverse
            val scriptHead = tail.sortWith((a, b) => b.isInstanceOf[Strict])
            s"""${scriptHead.reverse.map(compile).mkString(";\n  ")} ${if (scriptHead.nonEmpty) ";" else "" }
                |return ${compile(head)};""".stripMargin
        }
      case _ => s"return ${compile(body)}"
    }
    (st, stack)
  }

  private def defineVariables(innerStack: scala.collection.mutable.Set[String]):String = {
    innerStack.nonEmpty match {
      case true => s"var ${innerStack.mkString(",")};"
      case false => ""
    }
  }

}
