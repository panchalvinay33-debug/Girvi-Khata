package com.girvikhata.app.domain

import java.math.BigDecimal

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val fieldErrors: Map<String, String>) : ValidationResult
}

class CustomerValidator {
    fun validate(customer: Customer): ValidationResult {
        val errors = linkedMapOf<String, String>()
        if (customer.name.trim().length < 2) errors["name"] = "Customer name is required"
        val digits = customer.mobile.filter(Char::isDigit)
        if (digits.length !in 10..15) errors["mobile"] = "Valid mobile number is required"
        if (customer.alternateMobile.isNotBlank() && customer.alternateMobile.filter(Char::isDigit).length !in 10..15) {
            errors["alternateMobile"] = "Alternate mobile number is invalid"
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}

class GirviValidator {
    fun validate(account: GirviAccount): ValidationResult {
        val errors = linkedMapOf<String, String>()
        if (account.girviNumber.isBlank()) errors["girviNumber"] = "Girvi number is required"
        if (account.customerId.isBlank()) errors["customerId"] = "Customer is required"
        if (account.items.isEmpty()) errors["items"] = "At least one item is required"
        if (account.originalPrincipal <= BigDecimal.ZERO) errors["originalPrincipal"] = "Principal must be greater than zero"
        if (account.expectedDueDate?.isBefore(account.startDate) == true) errors["expectedDueDate"] = "Due date cannot be before start date"
        account.items.forEachIndexed { index, item ->
            if (item.quantity <= BigDecimal.ZERO) errors["items[$index].quantity"] = "Quantity must be greater than zero"
            if (item.weight != null && item.weight.deductionGrams > item.weight.grossGrams) {
                errors["items[$index].weight"] = "Deduction cannot exceed gross weight"
            }
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
