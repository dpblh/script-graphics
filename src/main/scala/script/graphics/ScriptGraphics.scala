package script.graphics
import parser.RegexpParsers

/**
 * Created by tim on 25.12.15.
 */


trait ScriptGraphics extends RegexpParsers {

  /**
   * Паттерн для определения имен переменных
   * @return
   */
  def variable = """(?!(^match$|^case$|^strict$|^def$|^for$|^new$))([a-zA-Z]|\d|_)+""".r                                   fn NameLiteral

  /**
   * Паттерн для определения числовых значений
   * @return
   */
  def number = """(0|[1-9])+""".r                                    fn { n => NumberLiteral(n.toInt)}

  /**
   * Паттерн для определения строковых литерал
   * @return
   */
  def string = "'" ~> saveSpace("""^(?:[^\\']|\\[\s\S])*""".r) <~ "'"                 fn { case string => StringLiteral(string) }

  /**
   * Интерполяция строк
   * Принимает вид "(${expression} | string).*"
   * @see expression
   * @return
   */
  def stringInterpolate = {

    def exp = "${" ~> expression <~ "}"

    def string = saveSpace("""^(?:[^\\"$]|\\[\s\S])+""".r) fn StringLiteral

    "\"" ~> (string | exp).* <~ "\"" fn StringInterpolateLiteral

  }

  /**
   * Литерал регулярных выражений
   * повторяет JavaScript
   * @return
   */
  def regexString = {

    def _regex = "([^[/\\n\\/]|\\/[^\\n]|\\/[(\\/[^\\n]|[^\\\\]\\n\\/])*\\/])*".r fn StringLiteral
    def flags = """[imgy]*""".r fn StringLiteral

    ("/" ~> _regex <~ "/") ~ flags fn { case regex ~ flags => RegexLiteral(regex, flags) }

  }

  /**
   * Комментарий
   * символы * и / не доступны внутри коментария
   * @return
   */
  def comment = {
    def string = saveSpace("^(?:[^\\/\\*]|\\\\[\\s\\S])*".r) fn StringLiteral
    "/*" ~> string <~ "*/" fn Comment
  }

  /**
   * Сокращенное определение переменной this.
   * Недоступно в формате # из за неоднозначности
   * @example #apply() #property
   * @return
   */
  def `this` = "#" fn { case str => `This`(None) }

  /**
   * Инстанцирование javaScript объектов
   * @return
   */
  def newInstance = "new" ~> pipeline fn NewInstance

  /**
   * Объявление литерала массива
   * @see parameters
   * @return
   */
  def array = {

    def parameters = {
      def zero = success(List())
      def one = expression
      def more = one ~ ("," ~> one).* fn { case one ~ more => one :: more }

      more | zero
    }

    "[" ~> parameters <~ "]" fn ArrayLiteral

  }

  /**
   * Цикл по свойствам объекта
   * @example for (property value) in hashMap console.log("${property} = ${value}")
   * @return
   */
  def loop = ("for" ~> ("(" ~> variable ~ variable <~ ")") <~ "in") ~ pipeline ~ statement fn { case property ~ value ~ from ~ body => Loop(property, value, from, body) }

  /**
   * Параметры функции. любые выражения разделенные запятой или именованные параметры
   * @see expression
   * @return
   */
  def parameters = {

    def zero = success(SimpleParameters(List()))
    def simpleOne = expression fn SimpleParameter
    def namedOne = variable ~ (":" ~> expression)  fn { case name ~ expression => NamedParameter(name, expression) }

    def moreSimple = simpleOne ~ ("," ~> simpleOne).* fn { case one ~ more => SimpleParameters(one :: more) }
    def moreNamed = namedOne ~ ("," ~> namedOne).* fn { case one ~ more => NamedParameters(one :: more) }

    (moreNamed | moreSimple) | zero

  }

  /**
   * Аргументы функции. идентификатор с возможностью установления дефолтного значения.
   * @example def name(name = 'undefined')
   * @example def register(email, name = email)
   * @see variable
   * @see expression
   * @return
   */
  def arguments = {

    def zero = success(List())
    def one = variable ~ ("=" ~> expression).? fn { case id ~ predef => Argument(id, predef) }
    def more = one ~ ("," ~> one).* fn { case one ~ more => one :: more }

    more | zero

  }

  /**
   * Доступные состояния в системе
   * Каждое состояние в конце возвращает состояние. Без ключевого слова return
   * @return
   */
  def statement: Parser[Statement] = block | loop | strict | methodCurrying | method | matching | variableDefinition | updateVariable | updateArray | comment | expression

  /**
   * Обьединение нескольких стейтментов. Мас являеться стейтментом
   * @see statement
   * @return
   */
  def block = "{" ~> statement.* <~ "}" fn {
    case body => Block(body)
  }

  /**
   * Определение переменной
   * @see variable
   * @see statement
   * @return
   */
  def variableDefinition:Parser[VariableDefinition] = {
    def statement = variableDefinition | matching | expression
    (variable <~ "=") ~ statement fn { case name ~ value => VariableDefinition(name, value) }
  }

  /**
   * Обновление свойства у объекта
   * @return
   */
  def updateVariable:Parser[UpdateProperty] = {

    def statement = matching | expression

    def pipelineProperty = pipeline ~ ("." ~> variable) fn { case pipeline ~ property => pipeline.copy(pipe = pipeline.pipe:::Property(property)::Nil) }
    def thisProperty = `this` ~ variable fn { case t ~ property => Pipeline(`This`(Some(property))::Nil) }


    (pipelineProperty | thisProperty) ~ ("=" ~> statement) fn { case property ~ statement => UpdateProperty(property, statement) }

  }

  /**
   * Обновление элемента массива
   * @see pipeline
   * @see expression
   * @return
   */
  def updateArray = {
    def statement = variableDefinition | matching | expression
    pipeline ~ ("[" ~> expression <~ "]" <~ "=") ~ statement fn { case pipe ~ index ~ value => UpdateArray(pipe, index, value) }
  }

  /**
   * Замыкание. Принимает формат (arguments) => statement
   * @example (in, out) => in + out
   * @see arguments
   * @see statement
   */
  def lambda = {

    def zero = success(List())
    def more = "(" ~> arguments <~ ")"

    (more | zero) ~ ("=>" ~> statement) fn { case args ~ body => Lambda(args, body) }

  }

  /**
   * Паттерн матчинг. альтернатива if, switch
   * Принимает формат expression 'match'(variableName)? { 'case' expression '=>' statement }
   * variableName по умолчанию имеет значение 'x'
   * если матчинг не находит совпадения, он выдает ошибку
   * @example
   *          name match {
   *            case x eq 'tim' => 'hi tim'
   *            case x eq 'mari' => 'hi mari'
   *          }
   * @example
   *          lastName match(name) {
   *            case name eq 'tim' => 'hi tim'
   *            case name eq 'mari' => 'hi mari'
   *          }
   *
   * @see expression
   * @see variable
   * @see statement
   * @return
   */
  def matching:Parser[Match] = {

    def statements = (strict | methodCurrying | method | matching | variableDefinition | updateArray | expression).*

    def matchCase = ("case" ~> expression <~ "=>") ~ statements fn {
      case exp ~ body => MatchCase(exp, body)
    }

    def matchBlock = "{" ~> matchCase.* <~ "}"

    (expression <~ "match") ~ ("(" ~> variable <~ ")").? ~ matchBlock fn {
      case exp ~ name ~ matches => Match(exp, name, matches)
    }

  }

  /**
   * Объявление метода. Имеет формат 'def' variable '(' arguments ')' statement
   * @see variable
   * @see arguments
   * @return
   */
  def method = {

    "def" ~> variable ~ ("(" ~> arguments <~ ")") ~ statement fn {
      case name ~ args ~ body => InstanceMethod(name, args, body)
    }

  }

  /**
   * Объявление каррированного метода. Имеет формат 'def' variable '(' arguments ')(' arguments ')' statement
   * @see variable
   * @see arguments
   * @return
   */
  def methodCurrying = {

    "def" ~> variable ~ ("(" ~> arguments <~ ")") ~ ("(" ~> arguments <~ ")") ~ statement fn {
      case name ~ argsCurrying ~ args ~ body => InstanceMethodCurrying(name, argsCurrying, args, body)
    }

  }

  /**
   * Структура. Комплексный тип. Структура должна начинаться с заглавной буквы strict Rate(from = 0, to)
   * Создается структура вызовом ее конструктора Rate(3, 9)
   * Структура содержит как собственные методы так и методы экземпляра def next(){}, def self.rand(x){}
   * Структура принимает вид
   * 'strict' strictName '(' arguments ')' '{' (strictMethod | method).* '}'
   * Если нет ни одного метода, кавычки можно опустить
   * @example
   *          strict Rate(from = 0, to, current = from) {
   *            def next() {
   *              current = current + 1
   *            }
   *            def self.rand() {
   *              Math.rand()
   *            }
   *          }
   * @see arguments
   *      для выражений в arguments доступна переменная that содержащая текущую структуру
   * @see method
   * @return
   */
  def strict = {

    def strictMethod = ("def" ~> "self." ~> variable) ~ ("(" ~> arguments <~ ")") ~ statement fn { case id ~ params ~ block => ClassMethod(id, params, block) }

    def strictName = """[A-Z][a-zA-Z]+""".r                              fn NameLiteral

    def zeroMethod = success(List())
    def moreMethods = "{" ~> (strictMethod | methodCurrying | method).* <~ "}"
    def methods = moreMethods | zeroMethod

    "strict" ~> strictName ~ ( "(" ~> arguments <~ ")" ) ~ methods fn { case name ~ args ~ methods => Strict(name, args, methods) }

  }

  /**
   * Выражение. Любое чередование операндов и операторов
   * @example 1
   * @example 'one'
   * @example 1 + 2
   * @example 1 + 2 * 4
   * @example one + 2 * 4
   * @example one + 'two' * 4
   * @example name or 'tim'
   * @example callback or () => console.log('hi')
   * @see lambda | pipeline | array | variable | number | string
   */
  def expression: Parser[ExpressionList] = {

    def bracket = "(" ~> expression <~ ")"
    def not = "!" ~> expression fn Not

    def operator = """>|<|==|!=|>=|<=|>>|<<|\+|-|\*|/|and|or|is""".r fn Operator
    def operand = not | lambda | bracket | newInstance | pipeline | array | variable | number | string | stringInterpolate | regexString
    def partial = operator ~ operand fn { case op ~ operand => ExpressionPartial(op, operand) }

    def one = operand
    def more = one ~ partial.* fn { case one ~ more => ExpressionList(one, more) }

    more
  }

  /**
   * Цепочечные вызовы. Любую последовательность предполагаемых объектов
   * @example apply()
   * @example id.apply()
   * @example id.apply()[0].apply()[1]
   * @example id().apply()()().apply()
   * @return
   */
  def pipeline:Parser[Pipeline] = {

    def thisWith = `this` ~ (callMethod | variable) fn { case t ~ call => `This`(Some(call)) }

    def startWith = thisWith | callMethod | array | string | variable | number

    def bracketParameters = "(" ~> parameters <~ ")"

    def zeroBlockArguments = success(List())
    def moreBlockArguments = "|" ~> arguments <~ "|"
    def blockArguments = moreBlockArguments | zeroBlockArguments

    def blockStatements = (strict | methodCurrying | matching | updateVariable | variableDefinition | updateArray | comment | expression).*
    def block = ("{" ~> blockArguments) ~ (blockStatements <~ "}") fn { case arguments ~ statements => Lambda(arguments, Block(statements)) }

    def parametersWithBlock = bracketParameters ~ block fn { case params ~ block => ParametersWithBlock(params, Some(block)) }
    def params = bracketParameters fn { case params => ParametersWithBlock(params, None) }
    def blockParam = block fn { case block => ParametersWithBlock(SimpleParameters(List()), Some(block)) }

    def methodSignature = parametersWithBlock | params | blockParam

    def callMethod = variable ~ methodSignature fn { case name ~ params => CallMethod(name, params) }
    def callFromObject = "." ~> variable ~ methodSignature fn { case id ~ params => CallFromObject(id, params) }
    def callAnonymous = methodSignature fn CallAnonymous
    def callArray = "[" ~> expression <~ "]" fn CallArray
    def property = "." ~> variable fn Property

    def chanel = (callFromObject | callAnonymous | callArray | property) not "=\\s\\w".r

    startWith ~ chanel.* fn { case id ~ list => Pipeline(id::list) }

  }

  def script = statement.* fn { Script }

}

trait Grammar
trait Literal extends Grammar
case class `This`(call: Option[Grammar]) extends Literal
case class NameLiteral(a: String) extends Literal
case class NumberLiteral(a: Int) extends Literal
case class StringLiteral(a: String) extends Literal
case class ArrayLiteral(items: List[Grammar]) extends Literal
case class StringInterpolateLiteral(sequence: List[Grammar]) extends Literal
case class RegexLiteral(regex: StringLiteral, flags: StringLiteral) extends Literal
case class NewInstance(pipe: Pipeline) extends Literal
case class Not(expression: Expression) extends Literal

trait Parameter extends Grammar
case class SimpleParameter(expression: Expression) extends Parameter
case class NamedParameter(name: NameLiteral, expression: Expression) extends Parameter

trait Parameters extends Grammar
case class SimpleParameters(parameters: List[SimpleParameter]) extends Parameters
case class NamedParameters(parameters: List[NamedParameter]) extends Parameters
case class ParametersWithBlock(parameter: Parameters, block: Option[Lambda]) extends Grammar

trait Statement extends Grammar
trait Expression extends Statement
case class ExpressionPartial(op: Operator, operand: Grammar) extends Grammar
case class ExpressionList(head: Grammar, tail: List[ExpressionPartial]) extends Expression

case class Comment(string: StringLiteral) extends Statement

case class Script(statements: List[Statement]) extends Statement
case class Block(statements: List[Statement]) extends Statement
case class Loop(property: NameLiteral, value: NameLiteral, variableFrom: Pipeline, body: Statement) extends Statement
case class BlockMethod(statements: List[Statement]) extends Statement
case class MatchCase(exp: ExpressionList, statements: List[Statement]) extends Literal
case class Match(exp: ExpressionList, variableName: Option[NameLiteral], matches: List[MatchCase]) extends Statement
case class Lambda(arguments: List[Argument], body: Statement) extends Literal
trait Method extends Statement {
  val name: NameLiteral
  val arguments: List[Argument]
  val body: Statement
}
case class InstanceMethod(name: NameLiteral, arguments: List[Argument], body: Statement) extends Method
case class InstanceMethodCurrying(name: NameLiteral, argumentsCyrrying: List[Argument], arguments: List[Argument], body: Statement) extends Method
case class ClassMethod(name: NameLiteral, arguments: List[Argument], body: Statement) extends Method

case class VariableDefinition(name: NameLiteral, value: Statement) extends Statement
case class Strict(name: NameLiteral, arguments: List[Argument], methods: List[Method]) extends Statement
case class UpdateArray(pipe: Pipeline, index: Expression, value: Statement) extends Statement
case class UpdateProperty(pipe: Pipeline, value: Statement) extends Statement

case class Argument(name: NameLiteral, default: Option[Expression]) extends Literal

case class Operator(symbol: String) extends Grammar
case class CallFromObject(name: NameLiteral, parameters: ParametersWithBlock) extends Grammar
case class CallAnonymous(parameters: ParametersWithBlock) extends Grammar
case class CallArray(index: ExpressionList) extends Grammar
case class CallMethod(name: NameLiteral, parameters: ParametersWithBlock) extends Grammar
case class Property(name: NameLiteral) extends Grammar
case class Pipeline(pipe: List[Grammar]) extends Grammar