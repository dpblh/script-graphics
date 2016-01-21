package script.graphics

import org.scalatest.{Matchers, FreeSpec}

/**
 * Created by tim on 25.12.15.
 */
class ScriptGraphicsTest extends FreeSpec with Matchers with ScriptGraphics {

  "test base stricture" - {


    parse(variable, "parser")
    parse(variable, "string2Number")
    parse(variable, "matching")
    parse(variable, "matching_2")
    intercept[ParseException] {
      parse(variable, "def")
    }
    intercept[ParseException] {
      parse(variable, "match")
    }

    parse(number, "123123")
    parse(number, "0")
    intercept[ParseException] {
      parse(number, "123f")
    }

    parse(string, "'asdadad asd ad'")
    parse(string, "' прива\n\t   '")
    intercept[ParseException] {
      parse(string, "'asda ada da' sd'")
    }
    parse(string, "'asda ada da\\' sd'")

    parse(regexString, """/asd \n sef \s/ig""")
    intercept[ParseException] {
      parse(regexString, """/asd \n /sef \s/ig""")
    }
    intercept[ParseException] {
      parse(regexString, """/asd \n sef \s/igasd""")
    }

    parse(stringInterpolate, """ "${id + 1} asda dad asd ${id} a" """)
    parse(stringInterpolate, """ "${id + 1} asda dad' asd ${id} a" """)
    parse(stringInterpolate, """ "asda dad asd a" """)
    intercept[ParseException] {
      parse(stringInterpolate, """ "${id + 1} asda "dad asd ${id} a" """)
    }
    parse(stringInterpolate, """ "${id + 1} asda dad\" asd ${id} a" """)
    intercept[ParseException] {
      parse(stringInterpolate, """ "${id + 1 asda dad asd ${id} a" """)
    }

    parse(comment, "/*asd asd adadaфвфы фыв фвы ф*/")
    parse(comment,
      """/*asd asd
        |adadaфвфы
        |фыв фвы ф*/""".stripMargin)
    intercept[ParseException] {
      parse(comment, "/*asd asd adadaфвфы *фыв фвы ф*/")
    }
    intercept[ParseException] {
      parse(comment, "/*asd asd adadaфвфы /фыв фвы ф*/")
    }

//    parse(`this`, "#apply()")
//    parse(`this`, "#property")
//    intercept[ParseException] {
//      parse(`this`, "#()")
//    }
//    intercept[ParseException] {
//      parse(`this`, "#[]")
//    }

    parse(newInstance, "new Date()")
    parse(newInstance, "new variable")
    intercept[ParseException] {
      parse(newInstance, "new")
    }

    parse(array, "[]")
    parse(array, "[1]")
    parse(array, "[1, 'as']")
    intercept[ParseException] {
      parse(array, "[]]")
    }

    parse(loop, "for (property value) in hashMap console.log(\"${property} = ${value}\")")
    intercept[ParseException] {
      parse(loop, "for (value) in hashMap console.log(\"${value}\")")
    }

    parse(parameters, "1")
    parse(parameters, "1,2")
    parse(parameters, "1, in + 4, in * out")
    intercept[ParseException] {
      parse(parameters, "=")
    }
    parse(parameters, "height:100, width:300")
    intercept[ParseException] {
      parse(parameters, "height:100, width:300, 4")
    }

    parse(arguments, "in, out")
    parse(arguments, "in = 0, out = in + 10")
    intercept[ParseException] {
      parse(arguments, "in out")
    }

    parse(variableDefinition, "id = 1")
    parse(variableDefinition, "id = 1 + 1")
    parse(variableDefinition, "id = id or 1")
    parse(variableDefinition, "id = id > 1")
    parse(variableDefinition, "id = id or /^some pattern/ig")
    parse(variableDefinition, "id = id2 = id_2 = 1")
    intercept[ParseException] {
      parse(variableDefinition, "id() = id2")
    }
    intercept[ParseException] {
      parse(variableDefinition, "object.id = id2")
    }
    println(parse(variableDefinition,
      """
        |id = d match {
        | case x > 1 => x + 3
        |}
      """.stripMargin))

    parse(updateVariable, "object.id = 1")
    parse(updateVariable, "object.id().d = 1")
    parse(updateVariable, "object.id()[0].d = 1")
    intercept[ParseException] {
      parse(updateVariable, "object.id() = 1")
    }
    intercept[ParseException] {
      parse(updateVariable, "object.id()[] = 1")
    }

    parse(updateArray, "apply()[1] = 1")
    parse(updateArray, "[1,2,3,'1212'][1] = 1 + 3")
    parse(updateArray,
      """
        |[1,2,3,'1212'][1] = s match {
        | case x => 1
        |}
      """.stripMargin)

    parse(lambda, "=> 1")
    parse(lambda, "(in) => 1")
    parse(lambda, "(in) => 1 + 2")
    parse(lambda, "(in = 0) => 1 + 2")
    parse(lambda, "(a) => s + 1".stripMargin)
    parse(lambda, "(a, b, c) => s + 1".stripMargin)
    parse(lambda,
      """
        |(a, b, c) => {
        | a match {
        |   case x => z + x
        | }
        |}
      """.stripMargin)
    parse(lambda,
      """
        |(a, b, c) =>
        | a match {
        |   case x => z + x
        | }
      """.stripMargin)
    parse(lambda,
      """
        |(a, b, c) =>
        | a match(y) {
        |   case y => z + x
        | }
      """.stripMargin)
    parse(lambda,
      """
        |(a, b, c) =>
        | a match(y) {
        |   case y => y[0].apply()[1].apply()
        | }
      """.stripMargin)



    parse(array, "[1, 'as', id]")
    println(parse(array, "[1, 'as', id, ((in) => in + 3), (out) => 1]"))
    println(parse(array, "[1, 'as', id, ((in) => in + 3), (out) => 1]"))
    println(parse(array, "[1, 'as', id, ((in) => in + 3), (out) => 1, 1 + 3]"))
    parse(pipeline, "parser.apply")
    parse(pipeline, "parser.apply(){ |str| console.log(str) }")
    parse(pipeline, "#property")
    parse(pipeline, "#property('body')")
    parse(pipeline, "#property('body'){}")
    println(parse(pipeline, "id(1, 'asd')()().apply(1)(1, '1')[0](1).apply()[1][1][0]((in) => in).name"))
    println(parse(pipeline, "id[1]"))
    println(parse(pipeline, "apply()[1]"))
    println(parse(pipeline, "[1, 'as', id, ((in) => in + 3), (out) => 1, 1 + 3][1]"))
    println(parse(pipeline, "[1, 'as', id, ((in) => in + 3), (out) => 1, 1 + 3][id]"))
    println(parse(pipeline, "[1, 'as', id, ((in) => in + 3), (out) => 1, 1 + 3][id + 1]"))

    parse(pipeline, "a {}")

    parse(pipeline, "parser.apply()")
    parse(pipeline, "Builder(root).build {}")
    parse(pipeline, "parser.apply(in)")
    parse(pipeline, "parser.apply(in, reader)")
    parse(pipeline, "parser.apply(in + 1)")
    parse(pipeline, "parser.apply((in) => in + 1)")
    parse(pipeline, "parser.apply((in, out) => in + 1)")
    parse(pipeline, "parser.apply {(in, out) => in + 1}")
    parse(pipeline, "parser.apply(in, reader).apply(in)")
    parse(pipeline, "parser.apply(in, reader).apply(in).sd()")
    parse(pipeline, "[][1].apply()")
    parse(pipeline, "parser.apply(in, reader)()")
    parse(pipeline, "parser.apply(in, reader)()()")
    parse(pipeline, "parser.apply(parser.apply(in, reader).apply(in).sd())")
    parse(pipeline, "parser.apply([[1,2,3]][0][1])")
    parse(pipeline, "parser.apply(Parser.new('1').and('+').apply('1+'))")
    parse(pipeline, "x[0].hasNext()")
    parse(pipeline, "x[0].hasNext().next()")
    parse(pipeline, "x[0].hasNext().next()[0][1].apply()")

    parse(array, "[1, 'asdas', asd, parser.apply(in)]")

    parse(expression, "[y[1]]")

    println(parse(array, "[y[1]]"))
    println(parse(array, "[y[1], [x, y[2]]]"))

    parse(pipeline, "x[1]")
    parse(pipeline, "x[s]")
    parse(pipeline, "[1,2,3][0]")
    parse(pipeline, "[[1,2,3]][0][1]")

    parse(pipeline,
      """
        |new {
        |   Parser(f)
        |}
      """.stripMargin)
  }

  "text expression" - {
    parse(expression, "y[0].apply()[1].apply()")
    parse(expression, "1 * (x + 1)")
    println("!!!!!!")
    println(parse(expression, "block.toString() == 1"))
    parse(expression, "!1")
    parse(expression, "!(x or y)")
    parse(expression, "x and !(x or y)")
    parse(expression, "1 == 1")
    parse(expression, "1 == 1 and e != 3")
    parse(expression, "1 == 1 and e != 3 or 'qweqe' == f")
  }

  "test statement" - {
    "test match" - {
      parse(matching,
        """
          |1 match {
          | case x => x + 1
          |}
        """.stripMargin)
      parse(matching,
        """
          |1 match(y) {
          | case y => x + 1
          |}
        """.stripMargin)
      parse(matching,
        """
          |1 match {
          | case x => x + 1
          | case x + y => x + 1
          |}
        """.stripMargin)
      parse(matching,
        """
          |parser.apply(in) match {
          | case x => x + 1
          | case x == 2 => x + 1
          |}
        """.stripMargin)
      parse(matching,
        """
          |parser.apply(in) match {
          | case x => x + 1
          | case x == 3 and x > 5 =>
          |   [x][0] = 2
          |   parser.apply(in) match {
          |     case t + 1 == 4 => f.apply(d)
          |   }
          |}
        """.stripMargin)
    }

    "test define method" - {
      parse(method,
        """
          |def Parser() {
          |
          |}
        """.stripMargin)
      parse(methodCurrying,
        """
          |def add(one)(two) {
          | one + two
          |}
        """.stripMargin)
      parse(method,
        """
          |def Parser(offset, str) {
          |
          |}
        """.stripMargin)
      parse(method,
        """
          |def Parser(offset = 1, str = offset + 3, asd) {
          |
          |}
        """.stripMargin)
      parse(method,
        """
          |def Parser(offset = 1, str = offset + 3, asd) str[offset]
        """.stripMargin)
      parse(method,
        """
          |def Parser(offset, strs, sd, sdf, Ddd, dDD = (in) => 1) {
          | self.apply(in) match {
          |   case x => x + 1
          |   case x > 10 => [x]
          | }
          |}
        """.stripMargin)
    }

    "test class" - {
      parse(strict,
        """
          |strict Parser()
        """.stripMargin)
      parse(strict,
        """
          |strict Parser(offset = 1, str)
        """.stripMargin)
      parse(strict,
        """
          |strict Parser(offset = 'qwead', str) {
          |
          |}
        """.stripMargin)
      parse(strict,
        """
          |strict Parser(offset, str) {
          | def and(parser) {
          |
          | }
          | def or(parser) {
          |
          | }
          |}
        """.stripMargin)
      parse(strict,
        """
          |strict Parser(offset, str) {
          | def self.new() {
          |   new Parser(offset)
          | }
          | def and(parser) {
          |
          | }
          | def or(parser) {
          |
          | }
          |}
        """.stripMargin)
      parse(strict,
        """
          |strict Reader(str, offset = 0) {
          | def hasNext() {
          |   str[offset]
          | }
          | def next() {
          |   new Reader(str, offset)
          | }
          | def peek() {
          |   str[offset]
          | }
          |}
        """.stripMargin)
      println(parse(strict,
        """
          |strict Parser(apply) {
          | def new(f) {
          |   new Parser(f)
          | }
          | def char(x) {
          |   new { (in) =>
          |     sd match {
          |       case x => [in.next(), x]
          |     }
          |   }
          | }
          | def and(parser) {
          |   new { (in) =>
          |     self.apply(in) match {
          |       case x => parser.apply(x.reader) match {
          |         case y => [y[1], [x, y[2]]]
          |       }
          |     }
          |   }
          | }
          |
          |}
        """.stripMargin))

      val st = parse(script,
        """
          |strict Rect(x = 0, y = 0) {
          | def copy(x = that.x, y = that.y) {
          |   Rect(x, y)
          | }
          | def next() {
          |   Rect(x + 1, y)
          | }
          | def self.rand() {
          |   Rect(Math.rand(), Math.rand())
          | }
          |}
          |
          |var rect = Rect(3, 6)
          |console.log(rect.x)
          |
          |def print(string = 'hello', prefix = '<<', postfix = '>>') {
          | alert(" ${prefix} ${string}  ${postfix}  ")
          |}
          |
          |print()
          |
        """.stripMargin)

//      println(st)
//
//      println(CompileJs.compile(st))
      parse(pipeline,
        """body {
          | h1 {}
          | array.forEach {
          |   p {
          |     create('span'){}
          |   }
          | }
          |}""".stripMargin)

      val e = parse(script,
        """
          |
          |root = parent = document.createElement('html')
          |gros = parent
          |
          |def create(elName)(block) {
          | el = document.createElement(elName)
          |
          | parent.appendChild(el)
          |
          | gros = parent
          | parent = el
          |
          | block()
          | parent = gros
          |
          | gros
          |
          |}
          |
          |paragraphs = [1,2,3,4,5]
          |body = create('body')
          |h1 = create('h1')
          |h2 = create('h2')
          |div = create('div')
          |
          |html = body {
          | h1 {}
          | h2 {}
          | paragraphs.forEach {
          |   div {
          |     create('span') {}
          |     create('p') {
          |       create('i') {}
          |       create('b') {}
          |     }
          |   }
          | }
          |}
          |
          |console.log(root)
          |
          |
        """.stripMargin)

//      println(e)
//
//      println(CompileJs.compile(e))

      val dsl = parse(script,
        """
          |strict Builder(parent) {
          | def build() {
          |   #body = #create('body')
          |   #h1 = #create('h1')
          |   #h2 = #create('h2')
          |   #div = #create('div')
          |   #p = #create('p')
          |   #i = #create('i')
          |   #span = #create('span')
          |   block.call(this)
          | }
          | def create(tagName)() {
          |   el = document.createElement(tagName)
          |   for (property value) in hashMap el.setAttribute(property, value)
          |   parent.appendChild(el)
          |
          |   Builder(el).build(block)
          | }
          | def text(str) {
          |   parent.innerText = str
          | }
          |}
          |
          |root = document.createElement('html')
          |
          |Builder(root).build {
          |
          | paragraphs = [1,2,3,4,5]
          |
          | #body {
          |   #h1(width:'100px', height:'200px', class:'h1') {
          |     #text('hello')
          |   }
          |   #h2 {}
          |  /*присвоил ссылку this
          |   переменная this доступна в self*/
          |   self = this
          |   paragraphs.forEach {
          |     self.div {
          |       #p {
          |         #i {}
          |         #span {}
          |       }
          |     }
          |   }
          |
          | }
          |}
          |console.log(root)
        """.stripMargin)

      println(dsl)

      println(CompileJs.compile(dsl))

      val currying = parse(script,
        """
          |def add(one)(two) one + two
          |var oneAdd = add(2)
          |oneAdd(3)
          |
        """.stripMargin)

      println(CompileJs.compile(currying))

    }
  }

}
