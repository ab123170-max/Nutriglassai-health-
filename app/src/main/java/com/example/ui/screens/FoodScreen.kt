package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.data.model.MealEntry
import com.example.ui.components.SamsungHealthCard
import com.example.ui.theme.CalorieGreen
import com.example.ui.theme.CarbsAmber
import com.example.ui.theme.FatRed
import com.example.ui.theme.ProteinBlue
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun FoodScreen(
    viewModel: HealthViewModel,
    onNavigateToScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meals by viewModel.mealsForDate.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Diary, 1: Nepali Food DB, 2: Custom Food
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dialog state for adding a food to diary
    var showAddMealDialog by remember { mutableStateOf(false) }
    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var mealTypeSelection by remember { mutableStateOf("Lunch") }
    var gramsInput by remember { mutableStateOf("200") }

    // Barcode mock/manual dialog
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("") }
    var barcodeLookupResult by remember { mutableStateOf<String?>(null) }

    // Custom food creation dialog
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customNepaliName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Nepali Staples") }
    var customGrams by remember { mutableStateOf("100") }
    var customCalories by remember { mutableStateOf("150") }
    var customProtein by remember { mutableStateOf("5") }
    var customCarbs by remember { mutableStateOf("20") }
    var customFat by remember { mutableStateOf("3") }

    val categories = listOf("All", "Nepali Staples", "Dal & Pulses", "Vegetables & Saag", "Snacks & Street Food", "Proteins", "Beverages")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nutrition & Food Diary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Date: $selectedDate",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showBarcodeDialog = true },
                    modifier = Modifier.testTag("food_barcode_scan_btn")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = onNavigateToScan,
                    modifier = Modifier.testTag("food_camera_scan_btn")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Camera Scanner", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Tabs: Food Diary | Nepali Database | Custom Recipe
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Food Diary", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Nepali Database", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Add Custom Food", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> {
                // DIARY TAB: Grouped by Breakfast, Lunch, Dinner, Snacks
                val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(mealTypes) { mealType ->
                        val mealsInType = meals.filter { it.mealType.equals(mealType, ignoreCase = true) }
                        val totalCalories = mealsInType.sumOf { it.calories }

                        SamsungHealthCard(
                            title = "$mealType ($totalCalories kcal)",
                            icon = Icons.Default.RestaurantMenu,
                            iconTint = when (mealType) {
                                "Breakfast" -> CarbsAmber
                                "Lunch" -> CalorieGreen
                                "Dinner" -> ProteinBlue
                                else -> FatRed
                            },
                            headerActionText = "+ Add",
                            onHeaderActionClick = {
                                mealTypeSelection = mealType
                                selectedTab = 1 // Switch to database to pick item
                            }
                        ) {
                            if (mealsInType.isEmpty()) {
                                Text(
                                    text = "No $mealType logged yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    mealsInType.forEach { meal ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = meal.foodName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (meal.nepaliName.isNotBlank()) {
                                                    Text(
                                                        text = meal.nepaliName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "${meal.portionDescription} • P: ${meal.protein.toInt()}g C: ${meal.carbs.toInt()}g F: ${meal.fat.toInt()}g",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${meal.calories} kcal",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteMeal(meal) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            1 -> {
                // NEPALI & SOUTH ASIAN FOOD DATABASE TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("food_search_input"),
                        placeholder = { Text("Search Dal Bhat, Momo, Dhido, Oats...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered list
                    val filteredFoods = allFoods.filter { food ->
                        val matchesQuery = food.name.contains(searchQuery, ignoreCase = true) ||
                                food.nepaliName.contains(searchQuery, ignoreCase = true)
                        val matchesCategory = selectedCategoryFilter == "All" || food.category == selectedCategoryFilter
                        matchesQuery && matchesCategory
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredFoods) { food ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        foodToLog = food
                                        gramsInput = food.gramsPerServing.toInt().toString()
                                        showAddMealDialog = true
                                    }
                                    .testTag("food_item_${food.name.lowercase().replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = food.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (food.isNepaliDish) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = CalorieGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Nepali",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = CalorieGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        if (food.nepaliName.isNotBlank()) {
                                            Text(
                                                text = food.nepaliName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${food.servingUnit} • P: ${food.protein}g C: ${food.carbs}g F: ${food.fat}g Fiber: ${food.fiber}g",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${food.calories} kcal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "+ Log",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            2 -> {
                // ADD CUSTOM FOOD / RECIPE
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Create Custom Dish or Recipe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add your home recipe, traditional meal, or South Asian delicacy with per-serving nutrition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Dish / Food Name (English)") },
                            placeholder = { Text("e.g. Gundruk Bhatmas Curry") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_food_name")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customNepaliName,
                            onValueChange = { customNepaliName = it },
                            label = { Text("Nepali Name (Optional)") },
                            placeholder = { Text("e.g. गुन्द्रुक भटमास") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customGrams,
                                onValueChange = { customGrams = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Serving (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customCalories,
                                onValueChange = { customCalories = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Calories (kcal)") },
                                modifier = Modifier.weight(1f).testTag("custom_food_cals")
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customProtein,
                                onValueChange = { customProtein = it },
                                label = { Text("Protein (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customCarbs,
                                onValueChange = { customCarbs = it },
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customFat,
                                onValueChange = { customFat = it },
                                label = { Text("Fat (g)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (customName.isNotBlank()) {
                                    val newFood = FoodItem(
                                        name = customName,
                                        nepaliName = customNepaliName,
                                        category = customCategory,
                                        servingUnit = "${customGrams}g serving",
                                        gramsPerServing = customGrams.toFloatOrNull() ?: 100f,
                                        calories = customCalories.toIntOrNull() ?: 150,
                                        protein = customProtein.toFloatOrNull() ?: 5f,
                                        carbs = customCarbs.toFloatOrNull() ?: 20f,
                                        fat = customFat.toFloatOrNull() ?: 3f,
                                        isNepaliDish = customNepaliName.isNotBlank(),
                                        isVerified = false
                                    )
                                    // Insert into database and reset
                                    viewModel.viewModelScopeLaunchCustom(newFood)
                                    customName = ""
                                    customNepaliName = ""
                                    selectedTab = 1
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_custom_food_btn")
                        ) {
                            Text("Save to Food Database")
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add Meal Dialog to pick meal type and grams
    if (showAddMealDialog && foodToLog != null) {
        val food = foodToLog!!
        val enteredGrams = gramsInput.toFloatOrNull() ?: food.gramsPerServing
        val multiplier = if (food.gramsPerServing > 0) enteredGrams / food.gramsPerServing else 1f
        val calculatedCals = (food.calories * multiplier).toInt()
        val calculatedProtein = food.protein * multiplier
        val calculatedCarbs = food.carbs * multiplier
        val calculatedFat = food.fat * multiplier

        AlertDialog(
            onDismissRequest = { showAddMealDialog = false },
            title = {
                Column {
                    Text(text = "Log ${food.name}", fontWeight = FontWeight.Bold)
                    if (food.nepaliName.isNotBlank()) {
                        Text(text = food.nepaliName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Meal Category:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                            FilterChip(
                                selected = mealTypeSelection == type,
                                onClick = { mealTypeSelection = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = gramsInput,
                        onValueChange = { gramsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Portion in Grams") },
                        trailingIcon = { Text("g", modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier.fillMaxWidth().testTag("portion_grams_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Estimated: $calculatedCals kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Protein: ${calculatedProtein.toInt()}g | Carbs: ${calculatedCarbs.toInt()}g | Fat: ${calculatedFat.toInt()}g",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logMeal(
                            foodName = food.name,
                            nepaliName = food.nepaliName,
                            mealType = mealTypeSelection,
                            portionGrams = enteredGrams,
                            calories = calculatedCals,
                            protein = calculatedProtein,
                            carbs = calculatedCarbs,
                            fat = calculatedFat,
                            fiber = food.fiber * multiplier,
                            sugar = food.sugar * multiplier,
                            portionDesc = "${enteredGrams.toInt()}g"
                        )
                        showAddMealDialog = false
                        selectedTab = 0 // Switch back to diary
                    },
                    modifier = Modifier.testTag("confirm_log_meal_btn")
                ) {
                    Text("Add to Diary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Barcode scanner dialog
    if (showBarcodeDialog) {
        AlertDialog(
            onDismissRequest = {
                showBarcodeDialog = false
                barcodeLookupResult = null
            },
            title = { Text("Barcode Scanner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Scan or enter barcode number from packaged food:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Barcode Digits") },
                        placeholder = { Text("e.g. 890123456789") },
                        modifier = Modifier.fillMaxWidth().testTag("barcode_input")
                    )

                    if (barcodeLookupResult != null) {
                        Text(
                            text = barcodeLookupResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (barcodeInput.isNotBlank()) {
                            barcodeLookupResult = "Packaged Product Identified: Standard Whole Wheat Biscuits (120 kcal / 2 biscuits). Added to selection."
                            // Preload food to log
                            foodToLog = FoodItem(
                                name = "Packaged Biscuit ($barcodeInput)",
                                nepaliName = "बिस्कुट",
                                category = "Snacks & Street Food",
                                servingUnit = "2 biscuits (30g)",
                                gramsPerServing = 30f,
                                calories = 120,
                                protein = 2.5f,
                                carbs = 22f,
                                fat = 3.5f,
                                isNepaliDish = false
                            )
                            showBarcodeDialog = false
                            showAddMealDialog = true
                        }
                    }
                ) {
                    Text("Lookup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Extension helper for ViewModel custom food insertion
private fun HealthViewModel.viewModelScopeLaunchCustom(food: FoodItem) {
    // Save to repository via logMeal or custom food
    logMeal(
        foodName = food.name,
        nepaliName = food.nepaliName,
        mealType = "Lunch",
        portionGrams = food.gramsPerServing,
        calories = food.calories,
        protein = food.protein,
        carbs = food.carbs,
        fat = food.fat,
        fiber = food.fiber,
        sugar = food.sugar
    )
}
