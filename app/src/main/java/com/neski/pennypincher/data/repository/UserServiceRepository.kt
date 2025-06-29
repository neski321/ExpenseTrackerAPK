package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Category
import com.neski.pennypincher.data.models.PaymentMethod
import com.neski.pennypincher.data.models.IncomeSource
import com.neski.pennypincher.data.models.Currency
import kotlinx.coroutines.tasks.await

object UserServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Seeds default data for a new user account
     * This creates initial categories, payment methods, income sources, and currencies
     */
    suspend fun seedDefaultUserData(userId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Seed Categories
            seedCategories(userId, batch)

            // Seed Payment Methods
            seedPaymentMethods(userId, batch)

            // Seed Income Sources
            seedIncomeSources(userId, batch)

            // Seed Currencies
            seedCurrencies(userId, batch)

            // Commit all changes
            batch.commit().await()
            
            println("Default data seeded successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error seeding default data for user $userId: ${e.message}")
            Result.failure(e)
        }
    }

    private fun seedCategories(userId: String, batch: com.google.firebase.firestore.WriteBatch) {
        val categoriesColRef = firestore.collection("users").document(userId).collection("categories")
        
        val defaultCategories = listOf(
            Category(id = "food", name = "Food & Dining", parentId = null, icon = "restaurant", color = "#FF6B6B"),
            Category(id = "transport", name = "Transportation", parentId = null, icon = "directions_car", color = "#4ECDC4"),
            Category(id = "shopping", name = "Shopping", parentId = null, icon = "shopping_cart", color = "#45B7D1"),
            Category(id = "entertainment", name = "Entertainment", parentId = null, icon = "movie", color = "#96CEB4"),
            Category(id = "health", name = "Health & Fitness", parentId = null, icon = "favorite", color = "#FFEAA7"),
            Category(id = "education", name = "Education", parentId = null, icon = "school", color = "#DDA0DD"),
            Category(id = "utilities", name = "Utilities", parentId = null, icon = "lightbulb", color = "#98D8C8"),
            Category(id = "housing", name = "Housing", parentId = null, icon = "home", color = "#F7DC6F"),
            Category(id = "travel", name = "Travel", parentId = null, icon = "flight", color = "#BB8FCE"),
            Category(id = "personal", name = "Personal Care", parentId = null, icon = "person", color = "#85C1E9"),
            Category(id = "gifts", name = "Gifts & Donations", parentId = null, icon = "card_giftcard", color = "#F8C471"),
            Category(id = "business", name = "Business", parentId = null, icon = "business", color = "#82E0AA"),
            
            // Subcategories for Food & Dining
            Category(id = "groceries", name = "Groceries", parentId = "food", icon = "local_grocery_store", color = "#FF6B6B"),
            Category(id = "restaurants", name = "Restaurants", parentId = "food", icon = "restaurant", color = "#FF6B6B"),
            Category(id = "coffee", name = "Coffee & Drinks", parentId = "food", icon = "local_cafe", color = "#FF6B6B"),
            Category(id = "takeout", name = "Takeout & Delivery", parentId = "food", icon = "delivery_dining", color = "#FF6B6B"),
            
            // Subcategories for Transportation
            Category(id = "gas", name = "Gas & Fuel", parentId = "transport", icon = "local_gas_station", color = "#4ECDC4"),
            Category(id = "public_transport", name = "Public Transport", parentId = "transport", icon = "directions_bus", color = "#4ECDC4"),
            Category(id = "parking", name = "Parking & Tolls", parentId = "transport", icon = "local_parking", color = "#4ECDC4"),
            Category(id = "maintenance", name = "Vehicle Maintenance", parentId = "transport", icon = "build", color = "#4ECDC4"),
            
            // Subcategories for Shopping
            Category(id = "clothing", name = "Clothing & Accessories", parentId = "shopping", icon = "checkroom", color = "#45B7D1"),
            Category(id = "electronics", name = "Electronics", parentId = "shopping", icon = "devices", color = "#45B7D1"),
            Category(id = "home_goods", name = "Home & Garden", parentId = "shopping", icon = "home", color = "#45B7D1"),
            
            // Subcategories for Entertainment
            Category(id = "movies", name = "Movies & Shows", parentId = "entertainment", icon = "movie", color = "#96CEB4"),
            Category(id = "games", name = "Games & Hobbies", parentId = "entertainment", icon = "sports_esports", color = "#96CEB4"),
            Category(id = "sports", name = "Sports & Recreation", parentId = "entertainment", icon = "sports_soccer", color = "#96CEB4"),
            
            // Subcategories for Health & Fitness
            Category(id = "medical", name = "Medical & Healthcare", parentId = "health", icon = "medical_services", color = "#FFEAA7"),
            Category(id = "fitness", name = "Fitness & Gym", parentId = "health", icon = "fitness_center", color = "#FFEAA7"),
            Category(id = "pharmacy", name = "Pharmacy", parentId = "health", icon = "local_pharmacy", color = "#FFEAA7"),
            
            // Subcategories for Education
            Category(id = "tuition", name = "Tuition & Fees", parentId = "education", icon = "school", color = "#DDA0DD"),
            Category(id = "books", name = "Books & Supplies", parentId = "education", icon = "book", color = "#DDA0DD"),
            Category(id = "courses", name = "Courses & Training", parentId = "education", icon = "class", color = "#DDA0DD"),
            
            // Subcategories for Utilities
            Category(id = "electricity", name = "Electricity", parentId = "utilities", icon = "lightbulb", color = "#98D8C8"),
            Category(id = "water", name = "Water & Sewage", parentId = "utilities", icon = "water_drop", color = "#98D8C8"),
            Category(id = "internet", name = "Internet & Phone", parentId = "utilities", icon = "wifi", color = "#98D8C8"),
            Category(id = "gas_utility", name = "Gas & Heating", parentId = "utilities", icon = "local_fire_department", color = "#98D8C8"),
            
            // Subcategories for Housing
            Category(id = "rent", name = "Rent", parentId = "housing", icon = "home", color = "#F7DC6F"),
            Category(id = "mortgage", name = "Mortgage", parentId = "housing", icon = "account_balance", color = "#F7DC6F"),
            Category(id = "insurance", name = "Home Insurance", parentId = "housing", icon = "security", color = "#F7DC6F"),
            Category(id = "maintenance_home", name = "Home Maintenance", parentId = "housing", icon = "build", color = "#F7DC6F")
        )

        defaultCategories.forEach { category ->
            val docRef = categoriesColRef.document(category.id)
            batch.set(docRef, category)
        }
    }

    private fun seedPaymentMethods(userId: String, batch: com.google.firebase.firestore.WriteBatch) {
        val paymentMethodsColRef = firestore.collection("users").document(userId).collection("paymentMethods")
        
        val defaultPaymentMethods = listOf(
            PaymentMethod(id = "cash", name = "Cash"),
            PaymentMethod(id = "debit_card", name = "Debit Card"),
            PaymentMethod(id = "credit_card", name = "Credit Card"),
            PaymentMethod(id = "bank_transfer", name = "Bank Transfer"),
            PaymentMethod(id = "paypal", name = "PayPal"),
            PaymentMethod(id = "venmo", name = "Venmo"),
            PaymentMethod(id = "apple_pay", name = "Apple Pay"),
            PaymentMethod(id = "google_pay", name = "Google Pay"),
            PaymentMethod(id = "check", name = "Check"),
            PaymentMethod(id = "crypto", name = "Cryptocurrency")
        )

        defaultPaymentMethods.forEach { paymentMethod ->
            val docRef = paymentMethodsColRef.document(paymentMethod.id)
            batch.set(docRef, paymentMethod)
        }
    }

    private fun seedIncomeSources(userId: String, batch: com.google.firebase.firestore.WriteBatch) {
        val incomeSourcesColRef = firestore.collection("users").document(userId).collection("incomeSources")
        
        val defaultIncomeSources = listOf(
            IncomeSource(id = "salary", name = "Salary", type = "Employment"),
            IncomeSource(id = "freelance", name = "Freelance", type = "Self-Employment"),
            IncomeSource(id = "business", name = "Business Income", type = "Business"),
            IncomeSource(id = "investments", name = "Investment Returns", type = "Investment"),
            IncomeSource(id = "rental", name = "Rental Income", type = "Property"),
            IncomeSource(id = "gifts", name = "Gifts", type = "Other"),
            IncomeSource(id = "refunds", name = "Refunds & Rebates", type = "Other"),
            IncomeSource(id = "side_hustle", name = "Side Hustle", type = "Self-Employment"),
            IncomeSource(id = "bonus", name = "Bonus", type = "Employment"),
            IncomeSource(id = "commission", name = "Commission", type = "Employment")
        )

        defaultIncomeSources.forEach { incomeSource ->
            val docRef = incomeSourcesColRef.document(incomeSource.id)
            batch.set(docRef, incomeSource)
        }
    }

    private fun seedCurrencies(userId: String, batch: com.google.firebase.firestore.WriteBatch) {
        val currenciesColRef = firestore.collection("users").document(userId).collection("currencies")
        
        val defaultCurrencies = listOf(
            Currency(id = "USD", code = "USD", name = "US Dollar", symbol = "$"),
            Currency(id = "EUR", code = "EUR", name = "Euro", symbol = "€"),
            Currency(id = "GBP", code = "GBP", name = "British Pound", symbol = "£"),
            Currency(id = "JPY", code = "JPY", name = "Japanese Yen", symbol = "¥"),
            Currency(id = "CAD", code = "CAD", name = "Canadian Dollar", symbol = "C$"),
            Currency(id = "AUD", code = "AUD", name = "Australian Dollar", symbol = "A$"),
            Currency(id = "CHF", code = "CHF", name = "Swiss Franc", symbol = "CHF"),
            Currency(id = "CNY", code = "CNY", name = "Chinese Yuan", symbol = "¥"),
            Currency(id = "INR", code = "INR", name = "Indian Rupee", symbol = "₹"),
            Currency(id = "BRL", code = "BRL", name = "Brazilian Real", symbol = "R$"),
            Currency(id = "MXN", code = "MXN", name = "Mexican Peso", symbol = "$"),
            Currency(id = "KRW", code = "KRW", name = "South Korean Won", symbol = "₩"),
            Currency(id = "SGD", code = "SGD", name = "Singapore Dollar", symbol = "S$"),
            Currency(id = "NZD", code = "NZD", name = "New Zealand Dollar", symbol = "NZ$"),
            Currency(id = "SEK", code = "SEK", name = "Swedish Krona", symbol = "kr"),
            Currency(id = "NOK", code = "NOK", name = "Norwegian Krone", symbol = "kr"),
            Currency(id = "DKK", code = "DKK", name = "Danish Krone", symbol = "kr"),
            Currency(id = "PLN", code = "PLN", name = "Polish Złoty", symbol = "zł"),
            Currency(id = "CZK", code = "CZK", name = "Czech Koruna", symbol = "Kč"),
            Currency(id = "HUF", code = "HUF", name = "Hungarian Forint", symbol = "Ft")
        )

        defaultCurrencies.forEach { currency ->
            val docRef = currenciesColRef.document(currency.id)
            batch.set(docRef, currency)
        }
    }
} 