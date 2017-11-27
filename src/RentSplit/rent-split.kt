/*
 *  Rent-Split.kt
 *  Made for Rent Split 2 by Ben Leggiero, starting 2017-11-23
 *
 *  Written in Kotlin/JS 1.2
 *
 *  Copyright Blue Husky Studios 2017 BH-1-PS
 */

@file:Suppress("MemberVisibilityCanPrivate", "LocalVariableName")

package RentSplit

import jQueryInterface.*
import org.w3c.dom.events.Event


///// APP-GLOBAL CONSTANTS /////

/// Selectors ///

var addARoommateRowId = "Add-Roommate-Row"
var addARoommateRowSelector = "#" + addARoommateRowId
var addARoommateButtonId = "Add-Roommate-Button"
var addARoommateButtonSelector = "#" + addARoommateButtonId
var removeARoommateButtonClassName = "remove-roommate-button"
var removeARoommateButtonSelector = "." + removeARoommateButtonClassName
var addAnExpenseRowId = "Add-Expense-Row"
var addAnExpenseRowSelector = "#" + addAnExpenseRowId
var addAnExpenseButtonId = "Add-Expense-Button"
var addAnExpenseButtonSelector = "#" + addAnExpenseButtonId
var removeAnExpenseButtonClassName = "remove-expense-button"
var removeAnExpenseButtonSelector = "." + removeAnExpenseButtonClassName

var roommateRowDataName = "roommate-row"
var roommateRowSelector = "[data-" + roommateRowDataName + "]"
var expenseRowDataName = "expense-row"
var expenseRowSelector = "[data-" + expenseRowDataName + "]"

var roommateNameInputClassName = "roommate-name"
var roommateNameInputSelector = "." + roommateNameInputClassName
var roommateIncomeInputClassName = "roommate-income"
var roommateIncomeInputSelector = "." + roommateIncomeInputClassName
var roommateProportionClassName = "roommate-proportion"
var roommateProportionSelector = "." + roommateProportionClassName
var roommateAnyInputFieldSelector = roommateNameInputSelector + "," + roommateIncomeInputSelector

var expenseNameInputClassName = "expense-name"
var expenseNameInputSelector = "." + expenseNameInputClassName
var expenseCostInputClassName = "expense-cost"
var expenseCostInputSelector = "." + expenseCostInputClassName
var expenseAnyInputFieldSelector = expenseNameInputSelector + "," + expenseCostInputSelector
var expenseTableSelector = "#Expenses"
var expenseTableBodySelector = expenseTableSelector + ">tbody"

var anyInputFieldSelector = roommateAnyInputFieldSelector + "," + expenseAnyInputFieldSelector
var anyInputButtonSelector = addARoommateButtonSelector + "," + addAnExpenseButtonSelector + "," + removeAnExpenseButtonSelector
var anyInputSelector = anyInputFieldSelector + "," + anyInputButtonSelector

var moneyAmountInputSelector = roommateIncomeInputSelector + "," + expenseCostInputSelector

var resultsTableSelector = "#Results"
var resultsTableBodySelector = resultsTableSelector + ">tbody"
var resultsTableHeadRowSelector = resultsTableSelector + ">thead>tr"


/// Label text ///

var rentExpenseTitle = "Rent"
var utilitiesExpenseTitle = "Utilities"

var roommateNamePlaceholderText = "Name"
var roommateIncomePlaceholderText = "Income"

var expenseTypePlaceholderText = "Type"
var expenseCostPlaceholderText = "Monthly Cost"

var roommateNameColumnTitle = "Name"
var totalColumnTitle = "Total Cost"


/// Defaults ///

var defaultRoommateIncome: Double = 1000.0

var defaultExpenseCost: Double = 100.0
var defaultRentExpenseCost: Double = 800.0
var defaultUtilitiesExpenseCost: Double = 50.0


/**
 * @author Ben Leggiero
 * @since 2017-11-23
 */
class RentSplit {
    ///// SETUP /////

    /**
     * The total amount of income of all roommates
     */
    var totalIncome: Double? = undefined

    /**
     * The total amount of expenses of all roommates
     */
    var totalExpenses: Double? = undefined

    /**
     * The running number of roommates, used to generate generic table headers
     */
    var roommateCounter: Int = 0

    /**
     * The running number of expenses, used to generate generic table headers
     */
    var expenseCounter: Double = 0.0


    fun onReady() {
        this.registerListeners()
        this.addDefaults()
        this.recalculateRentSplit()
        this.presentToUser()
    }

    /**
     * De- and re-registers every listener
     */
    fun reRegisterListeners() {
        jq(anyInputSelector).off()
        this.registerListeners()
    }

    /**
     * Registers every listener
     */
    fun registerListeners() {
        jq(anyInputFieldSelector).change(::recalculateRentSplit)
        jq(addAnExpenseButtonSelector).click(::addNewExpense)
        jq(removeAnExpenseButtonSelector).click(::removeExpense)
        jq(addARoommateButtonSelector).click(::addNewRoommate)
        jq(removeARoommateButtonSelector).click(::removeRoommate)
    }

    /**
     * Adds default expenses and roommates. Does not perform any calculations.
     */
    fun addDefaults() {
        // TODO: Read GET parameters
        this.addNewRoommate(undefined, "", defaultRoommateIncome, true, true)
        this.addNewRoommate(undefined, "", defaultRoommateIncome, true, true)

        this.addNewExpense(undefined, rentExpenseTitle, defaultRentExpenseCost, true, true)
        this.addNewExpense(undefined, utilitiesExpenseTitle, defaultUtilitiesExpenseCost, true, true)
    }

    /**
     * Throws out the old calculations and recalculates every roommate's share of every expense, and displays
     * the output
     */
    fun recalculateRentSplit() {
        val roommates = this.fetchRoommates()
        val expenses = this.fetchExpenses()

        this.recalculateRoommateProportions(roommates)
        this.totalExpenses = this.recalculateTotalExpenses(expenses)

        this.fillOutResults(roommates, expenses)
    }

    fun presentToUser() {
        jq(".rent").addClass("rent-ready")
    }

    ///// FETCHING /////

    /**
     * Finds all roommates in the DOM, parses them into RentRoommate objects, and returns them.
     */
    fun fetchRoommates(): List<RentRoommate> {
        return this.roommateRowsToRoommates(jq(roommateRowSelector))
    }

    /**
     * Finds all expenses in the DOM, parses them into RentExpense objects, and returns them.
     */
    fun fetchExpenses(): List<RentExpense> {
        return this.expenseRowsToExpenses(jq(expenseRowSelector))
    }

    /**
     * Takes in a jQuery result containing roommate input rows, parses each to a RentRoommate, and returns the
     * results in an array
     */
    fun roommateRowsToRoommates(jq_roommateRows: JQuery): List<RentRoommate> {
        this.roommateCounter = 0
        return jq_roommateRows.map(this::roommateRowToRoommate).asList()
    }

    /**
     * Takes in a jQuery result containing expense input rows, parses each to a RentExpense, and returns the
     * results in an array
     */
    fun expenseRowsToExpenses(jq_expenseRows: JQuery): List<RentExpense> {
        return jq_expenseRows.map(this::expenseRowToExpense).asList()
    }

    /**
     * Takes in a jQuery result containing a single roommate input row, parses it to a RentRoommate, and returns that
     */
    @Suppress("UNUSED_PARAMETER")
    fun roommateRowToRoommate(index: Int, jq_roommateRow: JQuery): RentRoommate {
        this.roommateCounter++
        return RentRoommate(
                jq(roommateNameInputSelector, jq_roommateRow).`val`() ?: "Roommate #$roommateCounter",
                jq(roommateIncomeInputSelector, jq_roommateRow).`val`()?.toDoubleOrNull() ?: Double.NaN,
                jq_roommateRow
        )
    }

    /**
     * Takes in a jQuery result containing a single expense input row, parses it to a RentExpense, and returns that
     */
    @Suppress("UNUSED_PARAMETER")
    fun expenseRowToExpense(index: Int, jq_expenseRow: JQuery): RentExpense {
        return RentExpense(
                jq(expenseNameInputSelector, jq_expenseRow).`val`() ?: "<EXPENSE>",
                jq(expenseCostInputSelector, jq_expenseRow).`val`()?.toDoubleOrNull() ?: Double.NaN,
                jq_expenseRow
        )
    }

    ///// CALCULATION /////

    /**
     * Trows away and recalculates the total income and each roommate's proportion of that, then displays the
     * proportions in the roommate input table
     */
    fun recalculateRoommateProportions(roommates: List<RentRoommate>) {
        this.totalIncome = this.recalculateTotalIncome(roommates)
        roommates.forEach(this::recalculateRoommateProportion)
        this.displayRoommateProportions(roommates)
    }

    /**
     * Trows away and recalculates the total income
     */
    fun recalculateTotalIncome(roommates: List<RentRoommate>): Double {
        return roommates.reduce { acc, curr ->
            return@reduce RentRoommate(monthlyIncome = acc.monthlyIncome + curr.monthlyIncome, name = "TMP", originalDOMElement = jq())
        }.monthlyIncome
    }

    /**
     * Throws away and recalculates each roommate's proportion of the total income
     */
    fun recalculateRoommateProportion(roommate: RentRoommate) {
        roommate.proportion = roommate.monthlyIncome / (this.totalIncome ?: 0.0)
    }

    /**
     * Displays each roommate's proportions of the total income in their input row
     */
    fun displayRoommateProportions(roommates: List<RentRoommate>) {
        roommates.forEach(this::displayRoommateProportion)
    }

    /**
     * Displays a single roommate's proportions of the total income in their input row
     */
    fun displayRoommateProportion(roommate: RentRoommate) {
        jq(roommateProportionSelector, roommate.originalDOMElement).html((roommate.proportion * 100).toFixed(2) + "%")
    }

    /**
     * Throws away and recalculates the total of all given expenses
     */
    fun recalculateTotalExpenses(expenses: List<RentExpense>): Double {
        return expenses.reduce({ acc, curr ->
            return@reduce RentExpense(monthlyCost = acc.monthlyCost + curr.monthlyCost, type = "<EXPENSE>", originalDOMElement = jq())
        }).monthlyCost
    }


    ///// ADDING ROWS ////

    fun addNewExpense(event: Event) {
        this.addNewExpense(event, null, null, locked = false, suppressCalculation = false)
    }


    /**
     * Adds a new expense input row, its corresponding expense output column, de- and re-registers all listeners,
     * and recalculates the roommate split. If the type and cost are given, they are filled-in. If the type is
     * given, it is made non-editable.
     */
    @Suppress("UNUSED_PARAMETER")
    fun addNewExpense(event: Event?, type: String?, cost: Double?, locked: Boolean, suppressCalculation: Boolean) {
        this.expenseCounter++
        val jq_expenseButtonRow = jq(addAnExpenseRowSelector)
        jq_expenseButtonRow.before(this.buildExpenseInputRow(type, cost, locked))
        this.reRegisterListeners()
        if (!suppressCalculation) {
            this.recalculateRentSplit()
        }
    }


    /**
     * Builds a string representation of a table row representing an expense input. If the type and cost are given,
     * they are filled-in.
     *
     * @param type   The type of expense; its name
     * @param cost   The monthly cost of the expense
     * @param locked Indicates whether the type should be editable and the row should be removable
     */
    fun buildExpenseInputRow(type: String?, cost: Double?, locked: Boolean): String {
        var row = "<tr data-" + expenseRowDataName + "=\"" + this.expenseCounter + "\">"
        row +=
        "<th${(if (locked) "" else " class=\"plain\"")}>" +
            "<input" +
            " type=\"" + (if (locked) "hidden" else "text") + "\"" +
            " class=\"" + expenseNameInputClassName + "   text-right\"" +
             (if (type.isNeitherNullNorEmpty()) " value=\"" + type + "\"" else "") +
            " size=\"8\"" +
            " tabindex=0" +
            " placeholder=\"" + expenseTypePlaceholderText + "\"" +
            "/>" +
            (if (locked && type.isNeitherNullNorEmpty()) type else "") +
        "</th>"

        row +=
        "<td class=\"plain vert-bottom\">" +
            "<input" +
            " type=\"number\"" +
             (if (type.isNeitherNullNorEmpty()) " id=\"total-$type\"" else "") +
            " class=\"" + expenseCostInputClassName + "\"" +
            " required" +
            " value=\"${(cost ?: defaultExpenseCost)}\"" +
            " step=\"10\"" +
            " size=\"8\"" +
            " tabindex=0" +
            " placeholder=\"$expenseCostPlaceholderText\"" +
            "/>" +
        "</td>"

        if (!locked) {
            row +=
            "<td" +
            " class=\"$removeAnExpenseButtonClassName color-danger\"" +
            " tabindex=\"0\">" +
                "<i class=\"fa fa-minus-circle\"></i>" +
            "</td>"
        }
        return row + "</tr>"
    }


    fun addNewRoommate(event: Event) {
        return this.addNewRoommate(event, name = null, income = null, locked = false, suppressCalculation = false)
    }


    /**
     * Adds a new roommate input row, its corresponding roommate output row, de- and re-registers all listeners,
     * and recalculates the roommate split. If the name and income are given, they are filled-in.
     */
    @Suppress("UNUSED_PARAMETER")
    fun addNewRoommate(event: Event?, name: String?, income: Double?, locked: Boolean, suppressCalculation: Boolean) {
        this.roommateCounter++
        val jq_roommateButtonRow = jq(addARoommateRowSelector)
        jq_roommateButtonRow.before(this.buildRoommateInputRow(name, income, locked))
        this.reRegisterListeners()
        if (!suppressCalculation) {
            this.recalculateRentSplit()
        }
    }


    /**
     * Builds a string representation of a table row representing an roommate input. If the name and income are
     * given, they are filled-in.
     *
     * @param name   The type of expense; its name
     * @param income The monthly cost of the expense
     * @param isLocked Indicates whether the row should be removable
     */
    fun buildRoommateInputRow(name: String?, income: Double?, isLocked: Boolean): String {
        var row = "<tr data-" + roommateRowDataName + "=\"" + this.roommateCounter + "\">"
        row +=
        "<th class=\"plain\">" +
            "<input" +
            " type=\"text\"" +
            " class=\"$roommateNameInputClassName   text-right\"" +
             (if (name.isNeitherNullNorEmpty()) " value=\"" + name + "\"" else "") +
            " size=\"8\"" +
            " tabindex=0" +
            " placeholder=\"$roommateNamePlaceholderText\"" +
            "/>" +
        "</th>"

        row +=
        "<td class=\"plain vert-bottom\">" +
            "<input" +
            " type=\"number\"" +
            " class=\"$roommateIncomeInputClassName\"" +
            " required" +
            " value=\"${income ?: defaultRoommateIncome}\"" +
            " step=\"100\"" +
            " size=\"8\"" +
            " tabindex=0" +
            " placeholder=\"$roommateIncomePlaceholderText\"" +
            "/>" +
        "</td>"

        row += "<td class=\"$roommateProportionClassName\">Calculating</td>"

        if (!isLocked) {
            row +=
            "<td class=\"$removeARoommateButtonClassName color-danger\"" +
                " tabindex=\"0\">" +
                    "<i class=\"fa fa-minus-circle\"></i>" +
            "</td>"
        }
        return row + "</tr>"
    }

    ///// REMOVING ROWS /////

    /**
     * Removes the expense input row referenced in the given event
     */
    fun removeExpense(event: Event) {
        val expenseRow = event.currentTarget?.parentElement
        expenseRow?.remove()
        this.reRegisterListeners()
        this.recalculateRentSplit()
    }

    /**
     * Removes the roommate input row referenced in the given event
     */
    fun removeRoommate(event: Event) {
        val roommateRow = event.currentTarget?.parentElement
        roommateRow?.remove()
        this.reRegisterListeners()
        this.recalculateRentSplit()
    }

    ///// OUTPUT /////

    /**
     * Using the given roommates and expenses, this throws away and regenerates the Results output table
     */
    fun fillOutResults(roommates: List<RentRoommate>, expenses: List<RentExpense>) {
        this.fillOutResultsTableHead(roommates, expenses)
        this.fillOutResultsTableBody(roommates, expenses)
    }

    /**
     * Using the given roommates and expenses, generates and outputs the table column heads to the Results
     * output table
     */
    @Suppress("UNUSED_PARAMETER")
    fun fillOutResultsTableHead(roommates: List<RentRoommate>, expenses: List<RentExpense>) {
        val jq_resultsTableHeadRow= jq(resultsTableHeadRowSelector)
        jq_resultsTableHeadRow.empty()
        jq_resultsTableHeadRow.append("<th class=\"text-center\">$roommateNameColumnTitle</th>")
        expenses.forEach { this.appendExpenseColumn(jq_resultsTableHeadRow, it) }
        jq_resultsTableHeadRow.append("<th class=\"text-center\">$totalColumnTitle</th>")
    }

    /**
     * Using the given expense, generates and outputs the table column head to the Results output table
     */
    fun appendExpenseColumn(jq_resultsTableHeadRow: JQuery, expense: RentExpense) {
        jq_resultsTableHeadRow.append("<th class='hide-small'>${expense.type}</th>")
    }

    /**
     * Using the given roommates and expenses, generates and outputs the roommate table rows to the Results
     * output table
     */
    fun fillOutResultsTableBody(roommates: List<RentRoommate>, expenses: List<RentExpense>) {
        val jq_resultsTableBody = jq(resultsTableBodySelector)
        jq_resultsTableBody.empty()
        roommates.forEach { this.appendResultRow(jq_resultsTableBody, it, expenses) }
    }

    /**
     * Using the given roommate and expenses, generates and outputs the table row to the Results output table
     */
    fun appendResultRow(jq_resultsTableBody: JQuery, roommate: RentRoommate, expenses: List<RentExpense>) {
        jq_resultsTableBody.append(this.buildResultRow(roommate, expenses))
    }

    /**
     * Builds a string representation of a Results table row.
     */
    fun buildResultRow(roommate: RentRoommate, expenses: List<RentExpense>): String {
        var row = "<tr><th>${roommate.name}</th>"
        row += expenses.joinToString(separator = "", transform = { "<td class='hide-small'>${(roommate.proportion * it.monthlyCost).dollarFormat}</td>" })
        row += "<th>${(roommate.proportion * (this.totalExpenses ?: 0.0)).dollarFormat}</th>"
        return "$row</tr>"
    }
}




/**
 * The RentRoommate class represents a roommate and their monthly income.
 */
data class RentRoommate(val name: String,
                        val monthlyIncome: Double,
                        val originalDOMElement: JQuery,
                        var proportion: Double = 0.0)

/**
 * The RentExpense class represents an expense and its monthly cost.
 */
data class RentExpense(val type: String, val monthlyCost: Double, val originalDOMElement: JQuery)


fun main(args: Array<String>) {
    jq({
        RentSplit().onReady()
    })
}
