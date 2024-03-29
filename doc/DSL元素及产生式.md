## DSL 元素

| 序号 | 元素         | 元素类型  | 值       | 约束                                              | 备注                      |
|----|------------|-------|---------|-------------------------------------------------|-------------------------|
| 1  | 属性名        | 标示符   | -       | 第一个字符必须是字母 [a-z\|A-Z]+[a-z\|A-Z\|0-9\|_]*       |                         |
| 2  | 等值匹配符      | 关键字   | :(英文)   |                                                 |                         |
| 3  | 双引号        | 关键字   | "       |                                                 | 必须成对出现,做等值匹配时，用来匹配字段时应使用短语匹配 |
| 4  | 小于         | 关系运算符 | <       |                                                 |                         |
| 5  | 大于         | 关系运算符 | >       |                                                 |                         |
| 6  | 小于等于       | 关系运算符 | <=      |                                                 |                         |
| 7  | 大于等于       | 关系运算符 | >=      |                                                 |                         |
| 8  | 逻辑与        | 逻辑运算符 | and     |                                                 | 默认运算优先级 not>and>or      |
| 9  | 逻辑或        | 逻辑运算符 | or      |                                                 | 默认运算优先级 not>and>or      |
| 10 | 逻辑非        | 逻辑运算符 | not     |                                                 | 默认运算优先级 not>and>or      |
| 11 | 括号         | 关键字   | ()      | 必须成对出现,用来主动指定多个逻辑运算符之间的结合性                      |                         |
| 12 | 通配符        | 关键字   | *       |                                                 | 可出现在标示符中也可出现在字面量中       |
| 13 | 花括号        | 关键字   | {}      | 必须成对出现,用来标记文档的嵌套字段查询                            |                         |
| 14 | 字面量(字符串常量) | 字面量   | -       |                                                 | 所有非关键字之间的所有字符组成的串均为字面量  |
| 15 | 聚合关键字      | 关键字   | group   | 后面必须跟 by关键字,前面必须是一个()子树                         |
| 16 | 聚合关键字      | 关键字   | by      | 后面必须是(${fieldName})子树,前面必须是group 关键字            |
| 23 | 限制查询数目关键字  | 关键字   | limit   | 前面必须是合法的查询子树 关键字后下一个有效token必须为int数字             |
| 24 | 排序字段关键字    | 关键字   | order   | 前面必须是合法的查询子树 关键字后必须跟by(${sort_Field} desc\|asc) |
| 25 | 指定排序倒叙关键字  | 关键字   | desc    | 必须存在by()中                                       |                         |
| 26 | 指定排序顺序关键字  | 关键字   | asc     | 必须存在by()中                                       |                         |





## DSL 语法 产生式

### 大写字母开头的为非终结符
### 小写字母开头的为终结符



Query:Statement+;

Statement:
BracketStatement   
|MatchAllStatement  --->>> MatchAllQuery   
|TextMatchStatement  --->>> FullTextMatch   
|MatchPhraseStatement  --->>> PhraseMatch   
|LogicCalcStatement  --->>> BooleanQuery   
|RelationCalcStatement   --->>> FieldRelationSearch | DateFieldRelationSearch   
|PhraseLiteralValue  --->>> MultiMatchQuery   
|MultiFiledMatchStatement  --->>> MultiMatchQuery   
|NestedMatchStatement  --> 内部属性查询  
|AggregationStatement;    --->>> 无对应的QueryUnit，在翻译阶段直接翻译聚合的filter树后再二次处理agg   
|LimitResultStatement     --->>> 无对应的QueryUnit，在翻译阶段直接翻译聚合的filter树后再二次处理sort  


终结符列表  
[filedName,literalValue,∅,or|and|not,<|>|>=|<=,(,),:|group|by|limit|desc|asc]

//字段名表达式  
FiledName:filedName  
//字面量表达式  
LiteralValue:literalValue  
//短语查询字面量表达式    
PhraseLiteralValue:"LiteralValue"  
//空查询表达式    
MatchAllStatement: ∅  
//逻辑运算关键字表达式  
LogicCalcId: or|and|not
//冒号  
Colon: :  
//关系运算符表达式    
RelationCalcId: <|>|>=|<=  
//多字段查询语句  
MultiFiledMatchStatement:(LiteralValue)+  
//全文查询语句  
TextMatchStatement: FiledName Colon (LiteralValue)| BracketStatement  
//带括号的属性查询语句  
MatchWithBracketStatement:FiledName: BracketStatement  
//短语查询语句  
MatchPhraseStatement : FiledName Colon (PhraseLiteralValue|PhraseLiteralValue)  
//关系查询语句  
RelationCalcStatement: FiledName RelationCalcId literalValue  
//逻辑查询语句  
LogicCalcStatement1: (LogicCalcStatement1|LogicCalcStatement|TextMatchStatement|MatchPhraseStatement|MultiFiledMatchStatement|RelationCalcStatement|BracketStatement|MatchWithBracketStatement|NestedMatchStatement) (LogicCalcId (TextMatchStatement|MatchPhraseStatement|MultiFiledMatchStatement|RelationCalcStatement|BracketStatement|MatchWithBracketStatement|NestedMatchStatement))*  
//逻辑查询语句  
LogicCalcStatement: [not] (LogicCalcStatement1)+  
LBracket: (  
RBracket: )  
//语句优先级改变语句  
BracketStatement: ( Statement )  
// 聚合函数语句  
AggregationFunction:  LiteralValue LBracket  RBracket  
// 聚合字段选择语句  
AggregationFiled: (FiledName)  
// 分桶关键字  
GroupKeyWord: group  
// By关键字  
ByKeyWord: by  
// 聚合查询表达式  
AggregationStatement:BracketStatement GroupKeyWord ByKeyWord AggregationFiled AggregationFunction  
// limit 关键字  
LimitKeyWord: limit  
// int类型的数据字面量  
IntegerLiteralValue: [0-9]+  
// 限制查询数据条数表达式  
LimitResultStatement: Statement LimitKeyWord IntegerLiteralValue  
// 排序结果关键字  
OrderKeyWord: order  
// 排序方法关键字  
OrderMethod: desc|asc  
// 排序字段选择语句  
OrderFiled: ( FiledName )  
// 排序结果表达式  
OrderResultStatement: Statement OrderKeyWord ByKeyWord OrderFiled  
LBrace: {  
RBrace: }  
NestedAcceptStatement: NestedAcceptUnitStatement [and|or NestedAcceptUnitStatement]+  
NestedAcceptUnitStatement: MatchPhraseStatement|RelationCalcStatement|TextMatchStatement|NestedMatchStatement  
// 内部属性查询  
NestedMatchStatement: FiledName Colon LBrace NestedAcceptStatement RBrace  