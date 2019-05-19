package net.serenitybdd.screenplay.ensure

import net.serenitybdd.screenplay.Actor

typealias BooleanPredicate<A> = (actor: Actor?, actual: A) -> Boolean
typealias SingleValuePredicate<A, E> = (actor: Actor?, actual: A, expected: E) -> Boolean
typealias DoubleValuePredicate<A, E> = (actor: Actor?, actual: A, startRange: E, endRange: E) -> Boolean

fun <A, E> expectThatActualIs(message: String, predicate: SingleValuePredicate<A, E>) = Expectation("is $message", "is not $message", predicate)
fun <A, E> expectThatActualIs(message: String, negatedMessage: String, predicate: SingleValuePredicate<A, E>) = Expectation(message, negatedMessage, predicate)
fun <A, E> expectThatActualIs(message: String, predicate: DoubleValuePredicate<A, E>) = DoubleValueExpectation(message, predicate)
fun <A> expectThatActualIs(message: String, predicate: BooleanPredicate<A>) = PredicateExpectation("is $message", "is not $message", predicate)
fun <A> expectThatActualIs(message: String, negatedMessage: String, predicate: BooleanPredicate<A>) = PredicateExpectation(message, negatedMessage, predicate)
fun <A, E> expectThatActualIs(message: String, predicate: SingleValuePredicate<A, E>, predicateDescription: String) = Expectation("is $message", "is not $message", predicate, predicateDescription)
fun <A, E> expectThatActualIs(message: String, negatedMessage: String, predicate: SingleValuePredicate<A, E>, predicateDescription: String) = Expectation(message, negatedMessage, predicate, predicateDescription)
fun <A> expectThatActualContainsElementsThat(message: String,
                                             predicate: BooleanPredicate<A>,
                                             predicateDescription: String,
                                             qualifier: ElementQualifier,
                                             predicateNumber: GrammaticalNumber) =
        CollectionPredicateExpectation(message,
                predicateDescription,
                qualifier,
                predicate,
                predicateNumber)

/**
 * Models a single value predicate with a description
 */
class Expectation<A, E>(private val expectation: String,
                        private val negatedExpectation: String,
                        val predicate: SingleValuePredicate<A, E>,
                        private val readableExpectedValue: String? = null,
                        private val readableActualDescription : String? = null) {
    fun describe(actual: A?, expected: E, isNegated: Boolean = false, expectedDescription: String?) =
            comparisonDescription(expectation,
                    negatedExpectation,
                    actual as Any?,
                    expected as Any?,
                    isNegated,
                    if (expectedDescription != null) expectedDescription else readableExpectedValue,
                    readableActualDescription)
    fun apply(actual: A, expected: E, actor: Actor?) = predicate(actor, actual, expected)
}

/**
 * Models a value predicate with a description that takes no parameters
 */
open class PredicateExpectation<A>(val expectation: String,
                                   private val negatedExpression : String,
                                   val predicate: BooleanPredicate<A>,
                                   private val isNegated: Boolean = false) {
    open fun describe(actual: A?, isNegated: Boolean = false, expectedDescription: String? = "a value") = predicateDescription(expectation, negatedExpression, actual as Any?, isNegated, expectedDescription)
    fun apply(actual: A, actor: Actor?) = if (isNegated) !predicate(actor, actual) else predicate(actor, actual)
}

class CollectionPredicateExpectation<A>(expectation: String,
                                        private val predicateDescription: String,
                                        private val qualifier: ElementQualifier,
                                        predicate: BooleanPredicate<A>,
                                        private val number: GrammaticalNumber) : PredicateExpectation<A>(expectation, "not $expectation",predicate) {
    override fun describe(actual: A?, isNegated: Boolean, expectedDescription: String?) = collectionPredicateDescription(
            expectation,
            qualifier,
            predicateDescription,
            number,
            actual as Any?,
            isNegated)
}

/**
 * Models a value predicate with a description that takes two parameters
 */
class DoubleValueExpectation<A, E>(val expectation: String, val predicate: DoubleValuePredicate<A, E>) {
    fun describe(actual: A,
                 startRange: E,
                 endRange: E,
                 isNegated: Boolean,
                 expectedDescription: String?) = rangeDescription(expectation,
            actual,
            startRange as Any,
            endRange as Any,
            isNegated,
            expectedDescription)

    fun apply(actual: A, startRange: E, endRange: E, actor: Actor?) = predicate(actor, actual, startRange, endRange)
}


private fun comparisonDescription(expectation: String,
                                  negatedExpectation: String,
                                  actual: Any?,
                                  expected: Any?,
                                  isNegated: Boolean,
                                  readableExpectedValue: String?,
                                  readableActualDescription: String?): String {
    val expectedAsText = specifiedDesciptionOrDescriptionFromType(expected, readableExpectedValue)
    val actualAsText = if (BlackBox.hasLastEntry()) BlackBox.lastEntry().actual else actual.toString()// else specifiedDesciptionOrDescriptionFromType(actualValue(actual), readableActualDescription)
    val expressionAsText = if (isNegated) negatedExpectation else expectation
    val expectedDescription = "Expecting $expectedAsText that $expressionAsText"
    val butGot = "But got".padEnd(expectedDescription.length, '.')
    val expectedValue = if (BlackBox.hasLastEntry()) BlackBox.lastEntry().expected else expectedValue(expected)

    return """
$expectedDescription: <$expectedValue>
$butGot: <$actualAsText>"""
}

private fun specifiedDesciptionOrDescriptionFromType(actual: Any?, readableActualDescription: String?) : String {
    return if (readableActualDescription != null) return readableActualDescription else return expectedType(actual)
}

private fun rangeDescription(expectation: String,
                             actual: Any?,
                             startRange: Any,
                             endRange: Any,
                             isNegated: Boolean,
                             expectedDescription: String?): String {
    val expecedType = if (expectedDescription == null) expectedType(actual) else expectedDescription
    val expectedDescription = "Expecting $expecedType ${thatIsOrIsnt(isNegated)}$expectation ${expectedValue(startRange)} and ${expectedValue(endRange)}"

    return """
$expectedDescription
But got: ${actualValue(actual)}"""
}


private fun predicateDescription(expectation: String, negatedExpectation: String, actual: Any?, isNegated: Boolean, expectedDescription: String? = "value"): String {
    val expectedExpression = if (!isNegated) expectation else negatedExpectation
    val expectedType = if (expectedDescription == null) expectedType(actual) else expectedDescription
    val expectedAsString = "Expecting $expectedType that $expectedExpression"

    return """
$expectedAsString
But got: ${actualValue(actual)}"""
}

private fun collectionPredicateDescription(expectation: String,
                                           qualifier: ElementQualifier,
                                           predicateDescription: String,
                                           number: GrammaticalNumber,
                                           actual: Any?,
                                           isNegated: Boolean): String {
    // Expecting a collection that [matches|does not match]: [all elements|no element] [have size|has size] [greater than 4]
    val qualifier = qualifier.resolve(number)
    val matchesOrDoesNotMatch = matchesOrDoesNotMatch(isNegated)
    val expected = "Expecting a collection that $matchesOrDoesNotMatch: $expectation $qualifier $predicateDescription"

    return """
$expected
But got: ${actualValue(actual)}"""
}

fun expectedType(expected: Any?) = if (expected is Collection<*>) "a collection" else "a value"
fun thatIsOrIsnt(isNegated: Boolean) = if (isNegated) "that is not " else "that is "
fun matchesOrDoesNotMatch(isNegated: Boolean) = if (isNegated) "does not match" else "matches"
fun expectedValue(expected: Any?) = if (expected is String) "\"$expected\"" else expected
fun actualValue(actual: Any?) = if (actual is String) "\"$actual\"" else actual
