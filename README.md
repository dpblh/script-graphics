# Scropt'Graphics

Академическая реализация препроцессора для javaScript. Не полная совместимость с javaScript.

  - Имена переменных ограничены [a-zA-Z]|\d|_.
  - Отсутствуют js объекты "{}", они заменены на структуры

### Expression

 - Стандартные js выражения.
 - доступные операторы >|<|==|!=|>=|<=|>>|<<|\+|-|\*|/|and|or|is
 - && заменен на and. || заменен на or. instanceof на is

#### Example
```
1 + e * y

add or (a, b) => a + b

calc.add or (a, b) => a + b
```

### Комментарии
```
/*некоторый коментарий*/
```

### This
 - сокращенный синтаксис для переменной this #. Недоступна сама по себе из за неоднозначности, необходимо #apply() or #property

```
#apply()

#property
```

### Statement
 - block | loop | strict | methodCurrying | method | matching | variableDefinition | updateVariable | updateArray | comment | expression
 - block массив statement заключенный в {}

#### Example
```
for (property value) in from {}

/*block*/
{
    for (property value) in from {}
    x = 1
}
```

### Parameters
 - Параметры могут быть как простыми выражениями или именованными выражениями
 - @see Expression

#### Example
```
(x or y, b, 'какаято строка', (a, b) => a + b)

(firstName: 'Иванов', lastName: 'Иван', middleName: 'Иванович', name: "${firstName} ${lastName} ${middleName}")
```

### Arguments
 - Для аргументов доступны значения по умолчанию как выражение
 - @see Expression кроме того в выражении доступны соседние параметры

#### Example
```
(x, y)

(x = 1, y = x + 2)
```

### Определение переменных
 - Переменные определяются без ключевого слова var.
 - var проставляется автоматически.
 - Если переменная используется в родительском скоупе её объявление берется от туда.

#### Example
```
a = 1
```

### Строки
#### Example
```
'некоторая строка'
```
### Интерполяция строк.
 - Допустимо любое выражение
#### Example
```
"некоторая строка ${x + 1}"
```
### Регулярные выражения
#### Example
```
/некоторое регулярное выражение/imgy
```
### Массивы
 - @see parameters
```
[parameters]
```
#### Example
```
[1, '1', 1 + 3, [2]]
```
### Pipeline
 - Любая доступная последовательность вызовов в javaScript

#### Example
```
1

'string'

[1,2,[1]]

apply()

object.apply().property[0]
```
### Установка свойства в массив
 - @see pipeline
 - @see expression
```
pipeline[expression] = expression
```
### Установка свойства в обьект
 - @see pipeline
 - @see expression
```
pipeline.property = expression
```

### Лямбда выражения.
 - @see argument
 - @see statement
#### Example
```
=> x + 1

(a, b) => x + 1

(a, b) => {
    x = 1
    a + b + x
}
```
### Матчинг
 - замена оператора if else в более функциональном стиле
 - statements массив statement
 - в case текущий результат выражения доступно по умолчанию в перемонной "x"
 - match кидает исключение если ни один из матчей не прошол
 - @see variable
 - @see expression
 - @see statement
```
variable = expression match {
    case expression => statements
}
```
#### Example
```
first = statement match {
    case x is Array => x[0]
    case x => x
}

first = statement match(data) {
    case data is Array => data[0]
    case data => data
}
```
### Цикл(loop)
 - итератор только по свойствам объекта. так как все остальное прекрасно работает в функциональноя стиле
 - @see variable
 - @see statement
```
for (variable variable) in pipeline statement
```
#### Example
```
for (property value) in hashMap el.setAttribute(property, value)

for (property value) in hashMap {
    el.setAttribute(property, value)
    console.log("${property} = ${value}")
}
```

### Определение метода(method)
 - метод можно определять в любом месте скрипта и он будет доступен в любом месте этого скоупа или его дочерних скоупах
 - методы имеют неявный параметор типа блок (block)
 - не явный параметор в совокупе с именоваными параметрами(hashMap не явная переменная) дают возможность описывать интересные дсл
 - @see variable
 - @see arguments
 - @see statement
```
def variable (arguments){block} statement
```
#### Example
```
def name() "${lastName} ${firstName} ${middleName}"

def add(a, b) {
    a + b
}

/*блок с параметрами*/
def builder { |t|
    t.div {

    }
}

body {
 h1(width:'100px', height:'300px', class:'h1') {}
 array.forEach {
   p {
     create('span'){}
   }
 }
}

/*реальный пример. не красивый но рабочий*/
strict Builder(parent) {
 def build() {

   #body =  #create('body')
   #h1   =  #create('h1')
   #h2   =  #create('h2')
   #div  =  #create('div')
   #p    =  #create('p')
   #i    =  #create('i')
   #span =  #create('span')

   block.call(this)
 }
 def create(tagName)() {

   el = document.createElement(tagName)
   for (property value) in hashMap el.setAttribute(property, value)
   parent.appendChild(el)

   Builder(el).build(block)

 }
 def text(str) {
   parent.innerText = str
 }
}

root = document.createElement('html')

/*Внешний интерфейс билдера*/
Builder(root).build {

 paragraphs = [1,2,3,4,5]

 #body {
   #h1(width:'100px', height:'200px', class:'h1') {
     #text('hello')
   }
   #h2 {}
   self = this
   paragraphs.forEach {
     self.div {
       #p {
         #i {}
         #span {}
       }
     }
   }

 }
}
console.log(root)
```

### Каррирование
 - @see method
```
def variable (arguments){block}(arguments){block} statement
```
#### Example
```
def add(a)(b) a + b

def add(a)(b) {
    a + b
}

one = add(1)
one(2)
```

### Структура
Структура составной тип данных. Ее можно называть до определения главное чтобы была определена в этом же или родительском скоупе. Структура не захватывает контекст. Имеет копирующий аргументы конструктор. Структура может содержать как методы экземплята так и собственные методы. Создается структура вызовом фабричного метода по имени структуры. В зарезервированной перемонной "that" содержатся определения текущего инстанса.
 - strictName [A-Z][a-zA-Z]+
 - strictMethod def self.variable ( arguments ) statement
 - methods @see method | strictMethod
```
strict strictName ( arguments ) methods
```
#### Example
```
strict Rect(x = 0, y = 0) {
    def self.rand(x = Math.random() / 10 << 0, y = Math.random() / 10 << 0) Rect(x, y)
    def copy(x = that.x, y = that.y) {
        Rect(x, y)
    }
}

random_rect = Rect.rand()
copy_rect.copy(x:1, y:2)
copy_rect2.copy(1, 2)
```
 - @see more to ScriptGraphicsTest
