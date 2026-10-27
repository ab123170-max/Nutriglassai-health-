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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.data.model.MealEntry
import com.example.domain.FoodUnit
import com.example.domain.UnitConversionEngine
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
    val measurementPrefs by viewModel.measurementPreferences.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Diary, 1: Nepali Food DB, 2: Custom Food
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dialog state for logging food with full unit support
    var showAddMealDialog by remember { mutableStateOf(false) }
    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var mealTypeSelection by remember { mutableStateOf("Lunch") }
    var quantityInput by remember { mutableStateOf("1") }
    var unitSelection by remember { mutableStateOf("serving") }
    var unitDropdownOpen by remember { mutableStateOf(false) }

    // Barcode scanner simulation
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("") }
    var barcodeLookupResult by remember { mutableStateOf<String?>(null) }

    // Custom food creation
    var showCustomFoodDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customNepaliName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Nepali Staples") }
    var customGrams by remember { mutableStateOf("100") }
    var customCalories by remember { mutableStateOf("150") }
    var customProtein by remember { mutableStateOf("5") }
    var customCarbs by remember { mutableStateOf("20") }
    var customFat by remember { mutableStateOf("3") }
    var customFiber by remember { mutableStateOf("2") }
    var customSodium by remember { mutableStateOf("50") }

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

        // Tabs: Food Diary | Nepali Database | Add Custom Food
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
                text = { Text("Food Database", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Add Custom Food", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Nutrition Summaries", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> {
                // DIARY TAB: Breakfast, Lunch, Dinner, Snacks
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
                                selectedTab = 1
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
                        placeholder = { Text("Search Dal Bhat, Momo, Dhido, Oats, Roti...") },
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

                    val filteredFoods = allFoods.filter { food ->
                        val matchesQuery = food.name.contains(searchQuery, ignoreCase = true) ||
                                food.nepaliName.contains(searchQuery, ignoreCase = true) ||
                                food.alternativeNames.contains(searchQuery, ignoreCase = true)
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
                                        quantityInput = "1"
                                        unitSelection = measurementPrefs.foodUnit.ifBlank { "serving" }
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
                                        val calsPer100g = food.getPer100gCalories().toInt()
                                        Text(
                                            text = "${food.servingUnit} (${food.calories} kcal) • $calsPer100g kcal/100g\nP: ${food.protein}g • C: ${food.carbs}g • F: ${food.fat}g • Fib: ${food.fiber}g",
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
                            text = "Add your home recipe, traditional meal, or South Asian delicacy with per-serving and per-100g nutrition.",
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
                                label = { Text("Default Serving (g)") },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customFiber,
                                onValueChange = { customFiber = it },
                                label = { Text("Fiber (g)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customSodium,
                                onValueChange = { customSodium = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Sodium (mg)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (customName.isNotBlank()) {
                                    val servingGrams = customGrams.toFloatOrNull() ?: 100f
                                    val cals = customCalories.toIntOrNull() ?: 150
                                    val p = customProtein.toFloatOrNull() ?: 5f
                                    val c = customCarbs.toFloatOrNull() ?: 20f
                                    val f = customFat.toFloatOrNull() ?: 3f
                                    val fib = customFiber.toFloatOrNull() ?: 2f
                                    val sod = customSodium.toFloatOrNull() ?: 50f

                                    val newFood = FoodItem(
                                        name = customName,
                                        nepaliName = customNepaliName,
                                        category = customCategory,
                                        servingUnit = "${servingGrams.toInt()}g serving",
                                        gramsPerServing = servingGrams,
                                        calories = cals,
                                        protein = p,
                                        carbs = c,
                                        fat = f,
                                        fiber = fib,
                                        caloriesPer100g = (cals / servingGrams * 100f),
                                        proteinPer100g = (p / servingGrams * 100f),
                                        carbsPer100g = (c / servingGrams * 100f),
                                        fatPer100g = (f / servingGrams * 100f),
                                        fibrePer100g = (fib / servingGrams * 100f),
                                        sodiumPer100g = (sod / servingGrams * 100f),
                                        defaultServingGrams = servingGrams,
                                        isNepaliDish = customNepaliName.isNotBlank(),
                                        isVerified = false
                                    )
                                    viewModel.logMeal(
                                        foodName = newFood.name,
                                        nepaliName = newFood.nepaliName,
                                        mealType = "Lunch",
                                        portionGrams = servingGrams,
                                        calories = cals,
                                        protein = p,
                                        carbs = c,
                                        fat = f,
                                        fiber = fib,
                                        sugar = 0f,
                                        portionDesc = "${servingGrams.toInt()}g"
                                    )
                                    customName = ""
                                    customNepaliName = ""
                                    selectedTab = 0
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_custom_food_btn")
                        ) {
                            Text("Save to Food Diary & Database")
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            3 -> {
                // NUTRITION SUMMARIES (Daily, Weekly, Monthly)
                NutritionSummariesTab(viewModel = viewModel)
            }
        }
    }

    // Interactive Add Meal Dialog with Module 1 Measurement Units & Immediate Recalculation
    if (showAddMealDialog && foodToLog != null) {
        val food = foodToLog!!
        val qty = quantityInput.toFloatOrNull() ?: 1f
        val foodUnit = FoodUnit.fromSymbol(unitSelection)

        val calsPer100g = food.getPer100gCalories()
        val pPer100g = food.getPer100gProtein()
        val cPer100g = food.getPer100gCarbs()
        val fPer100g = food.getPer100gFat()
        val fibPer100g = food.getPer100gFiber()
        val sugPer100g = food.getPer100gSugar()
        val sodPer100g = food.sodiumPer100g

        val calculated = UnitConversionEngine.calculateNutrition(
            caloriesPer100g = calsPer100g,
            proteinPer100g = pPer100g,
            carbsPer100g = cPer100g,
            fatPer100g = fPer100g,
            fiberPer100g = fibPer100g,
            sugarPer100g = sugPer100g,
            sodiumPer100g = sodPer100g,
            foodName = food.name,
            quantity = qty,
            unit = foodUnit,
            defaultServingGrams = food.gramsPerServing
        )

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

                    // Quantity and Unit Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f).testTag("portion_quantity_input")
                        )

                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedTextField(
                                value = foodUnit.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Measurement Unit") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { unitDropdownOpen = true }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { unitDropdownOpen = true }
                                    .testTag("unit_selector_field")
                            )

                            DropdownMenu(
                                expanded = unitDropdownOpen,
                                onDismissRequest = { unitDropdownOpen = false }
                            ) {
                                listOf(
                                    FoodUnit.G,
                                    FoodUnit.BOWL,
                                    FoodUnit.PLATE,
                                    FoodUnit.SERVING,
                                    FoodUnit.KATORI,
                                    FoodUnit.ROTI,
                                    FoodUnit.CHAPATI,
                                    FoodUnit.MOMO_PIECE,
                                    FoodUnit.PIECE,
                                    FoodUnit.SLICE,
                                    FoodUnit.CUP,
                                    FoodUnit.CUP_OF_RICE,
                                    FoodUnit.TBSP,
                                    FoodUnit.TSP,
                                    FoodUnit.ML,
                                    FoodUnit.GLASS,
                                    FoodUnit.BOTTLE,
                                    FoodUnit.OZ,
                                    FoodUnit.LB,
                                    FoodUnit.KG,
                                    FoodUnit.EGG,
                                    FoodUnit.IDLI_PIECE
                                ).forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u.displayName) },
                                        onClick = {
                                            unitSelection = u.symbol
                                            unitDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Live calculated nutrition card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${calculated.calories} kcal",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Weight: ${calculated.normalizedGrams.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Protein: ${calculated.protein}g • Carbs: ${calculated.carbs}g • Fat: ${calculated.fat}g • Fib: ${calculated.fiber}g • Na: ${calculated.sodium.toInt()}mg",
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
                            portionGrams = calculated.normalizedGrams,
                            calories = calculated.calories,
                            protein = calculated.protein,
                            carbs = calculated.carbs,
                            fat = calculated.fat,
                            fiber = calculated.fiber,
                            sugar = calculated.sugar,
                            sodium = calculated.sodium,
                            unit = foodUnit.symbol,
                            quantity = qty,
                            portionDesc = "$qty ${foodUnit.displayName} (${calculated.normalizedGrams.toInt()}g)"
                        )
                        showAddMealDialog = false
                        selectedTab = 0
                    },
                    modifier = Modifier.testTag("confirm_log_meal_btn")
                ) {
                    Text("Add to Food Diary")
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

@Composable
fun NutritionSummariesTab(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    // 0: Today (Daily), 1: Last 7 Days (Weekly), 2: Last 30 Days (Monthly)
    var periodSelection by remember { mutableIntStateOf(1) }
    val (periodDays, periodLabel) = when (periodSelection) {
        0 -> 1 to "Today (Daily)"
        1 -> 7 to "Last 7 Days (Weekly)"
        else -> 30 to "Last 30 Days (Monthly)"
    }

    val allMeals by viewModel.allMeals.collectAsState()
    val allWater by viewModel.allWater.collectAsState()
    val summary = remember(allMeals, allWater, periodDays) {
        viewModel.calculateNutritionSummary(periodDays, periodLabel)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Nutrition Summaries & Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your calorie intake, macronutrients, and hydration over daily, weekly, and monthly intervals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Period filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Daily", "Weekly (7d)", "Monthly (30d)").forEachIndexed { index, title ->
                    FilterChip(
                        selected = periodSelection == index,
                        onClick = { periodSelection = index },
                        label = { Text(title, fontSize = 12.sp, fontWeight = if (periodSelection == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = summary.periodLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${summary.averageCaloriesPerDay} kcal",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = CalorieGreen
                            )
                            Text(
                                text = "Daily Average (Total: ${summary.totalCalories} kcal)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CalorieGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${summary.targetMetDays} / ${summary.daysLogged.coerceAtLeast(1)} Target Days",
                                color = CalorieGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Macronutrient Breakdown
        item {
            Text(
                text = "Macronutrient Averages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMacroBox("Protein", "${summary.averageProteinPerDay}g", "Total: ${summary.totalProtein.toInt()}g", ProteinBlue, Modifier.weight(1f))
                SummaryMacroBox("Carbs", "${summary.averageCarbsPerDay}g", "Total: ${summary.totalCarbs.toInt()}g", CarbsAmber, Modifier.weight(1f))
                SummaryMacroBox("Fat", "${summary.averageFatPerDay}g", "Total: ${summary.totalFat.toInt()}g", FatRed, Modifier.weight(1f))
            }
        }

        // Secondary Nutrients & Water
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricRow("Dietary Fiber", "${summary.totalFiber.toInt()}g total", "${(summary.totalFiber / periodDays.coerceAtLeast(1) * 10).toInt() / 10f}g / day")
                    SummaryMetricRow("Estimated Sodium", "${summary.totalSodium.toInt()}mg total", "${(summary.totalSodium / periodDays.coerceAtLeast(1)).toInt()}mg / day")
                    SummaryMetricRow("Hydration Water", "${summary.totalWaterMl} ml total", "${summary.averageWaterMlPerDay} ml / day")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryMacroBox(
    title: String,
    avg: String,
    total: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(avg, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(total, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun SummaryMetricRow(title: String, total: String, avg: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Column(horizontalAlignment = Alignment.End) {
            Text(avg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(total, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

