package com.example.vision.core

import com.example.nutrition.engine.CalorieCalculationEngine
import com.example.nutrition.engine.CookingMethod
import com.example.nutrition.engine.NutrientBreakdown
import com.example.nutrition.engine.StandardCalorieCalculationEngine
import com.example.vision.classification.FoodClassificationEngine
import com.example.vision.classification.HybridFoodClassifier
import com.example.vision.classification.TopKPrediction
import com.example.vision.detection.BoundingBox
import com.example.vision.detection.ChromaticSaliencyFoodDetector
import com.example.vision.detection.DetectedFoodObject
import com.example.vision.detection.FoodDetectionEngine
import com.example.vision.detection.FoodMask
import com.example.vision.detection.FoodRegion
import com.example.vision.detection.FoodSegmentationEngine
import com.example.vision.detection.SalienceAndContourFoodSegmenter
import com.example.vision.portion.PortionEstimate
import com.example.vision.portion.PortionEstimationEngine
import com.example.vision.portion.StandardPortionEstimator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class FoodVisionOutput(
    val detectedObjects: List<DetectedFoodObject>,
    val foodMask: FoodMask,
    val items: List<FoodVisionItem>,
    val totalNutrients: NutrientBreakdown,
    val frameHash: String
)

data class FoodVisionItem(
    val id: String,
    val label: String,
    val nepaliLabel: String,
    val boundingBox: BoundingBox,
    val region: FoodRegion?,
    val topPrediction: TopKPrediction?,
    val portionEstimate: PortionEstimate,
    val nutrients: NutrientBreakdown
)

interface FoodImageProcessor {
    suspend fun processFrame(
        frame: FoodFrame,
        onStageChanged: ((VisionProcessingStage) -> Unit)? = null
    ): VisionProcessingResult<FoodVisionOutput>

    fun cancelProcessing()
    fun release()
}

/**
 * Standard implementation of the end-to-end food computer vision pipeline.
 * Runs on background coroutines (Dispatchers.Default) and handles cancellation gracefully.
 */
class StandardFoodVisionPipeline(
    private val preprocessor: ImagePreprocessor = StandardImagePreprocessor(),
    private val detectionEngine: FoodDetectionEngine = ChromaticSaliencyFoodDetector(),
    private val segmentationEngine: FoodSegmentationEngine = SalienceAndContourFoodSegmenter(),
    private val classificationEngine: FoodClassificationEngine = HybridFoodClassifier(),
    private val portionEngine: PortionEstimationEngine = StandardPortionEstimator(),
    private val calorieEngine: CalorieCalculationEngine = StandardCalorieCalculationEngine()
) : FoodImageProcessor {

    @Volatile
    private var isCancelled = false

    override suspend fun processFrame(
        frame: FoodFrame,
        onStageChanged: ((VisionProcessingStage) -> Unit)?
    ): VisionProcessingResult<FoodVisionOutput> = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val stageTimings = mutableMapOf<String, Long>()
        isCancelled = false

        try {
            // STAGE 1: PREPROCESSING
            coroutineContext.ensureActive()
            if (isCancelled) return@withContext VisionProcessingResult.Cancelled
            onStageChanged?.invoke(VisionProcessingStage.PREPROCESSING)
            val prepStart = System.currentTimeMillis()
            val preprocessed = preprocessor.preprocess(frame, PreprocessingConfig(targetWidth = 224, targetHeight = 224))
            stageTimings["preprocessing"] = System.currentTimeMillis() - prepStart

            // STAGE 2: DETECTION
            coroutineContext.ensureActive()
            if (isCancelled) return@withContext VisionProcessingResult.Cancelled
            onStageChanged?.invoke(VisionProcessingStage.DETECTION)
            val detStart = System.currentTimeMillis()
            val detectedObjects = detectionEngine.detectFoodObjects(frame)
            stageTimings["detection"] = System.currentTimeMillis() - detStart

            val foodBoxes = detectedObjects.map { it.box }

            // STAGE 3: SEGMENTATION
            coroutineContext.ensureActive()
            if (isCancelled) return@withContext VisionProcessingResult.Cancelled
            onStageChanged?.invoke(VisionProcessingStage.SEGMENTATION)
            val segStart = System.currentTimeMillis()
            val foodMask = segmentationEngine.segmentRegions(frame, foodBoxes)
            stageTimings["segmentation"] = System.currentTimeMillis() - segStart

            // STAGE 4: CLASSIFICATION & PORTION ESTIMATION
            coroutineContext.ensureActive()
            if (isCancelled) return@withContext VisionProcessingResult.Cancelled
            onStageChanged?.invoke(VisionProcessingStage.CLASSIFICATION)
            val classStart = System.currentTimeMillis()

            val visionItems = mutableListOf<FoodVisionItem>()
            val nutrientList = mutableListOf<NutrientBreakdown>()

            // Empty detection means the photo could not be reliably localized.
            // Do not invent a full-frame "Main Dish" region.
            val regions = foodMask.regions

            for (region in regions) {
                coroutineContext.ensureActive()
                if (isCancelled) return@withContext VisionProcessingResult.Cancelled

                // Classify the actual detected crop, not the same resized full
                // photo for every region.
                val source = frame.bitmap
                val cropRect = region.boundingBox.toImageCoordinates(source.width, source.height)
                val left = cropRect.left.toInt().coerceIn(0, source.width - 1)
                val top = cropRect.top.toInt().coerceIn(0, source.height - 1)
                val right = cropRect.right.toInt().coerceIn(left + 1, source.width)
                val bottom = cropRect.bottom.toInt().coerceIn(top + 1, source.height)
                val regionCrop = android.graphics.Bitmap.createBitmap(
                    source, left, top, right - left, bottom - top
                )
                val classificationOutput = try {
                    classificationEngine.classifyFood(regionBitmap = regionCrop, topK = 5)
                } finally {
                    if (regionCrop !== source && !regionCrop.isRecycled) regionCrop.recycle()
                }
                val topPred = classificationOutput.topPrediction

                // No prediction is not a food identity. Skip it instead of
                // manufacturing a generic label and default nutrient profile.
                if (topPred == null) continue
                val classId = topPred.classId
                val englishName = topPred.displayName
                val nepaliName = topPred.nepaliName

                // STAGE 5: PORTION ESTIMATION
                val portion = portionEngine.estimatePortion(region, classId)

                // STAGE 6: NUTRITION MAPPING
                val nutrients = calorieEngine.calculateNutrients(
                    baseCaloriesPer100g = topPred.caloriesPer100g,
                    baseProteinPer100g = topPred.proteinPer100g,
                    baseCarbsPer100g = topPred.carbsPer100g,
                    baseFatPer100g = topPred.fatPer100g,
                    baseFiberPer100g = topPred.fiberPer100g,
                    baseSodiumPer100g = topPred.sodiumPer100g,
                    portionGrams = portion.estimatedGrams,
                    cookingMethod = CookingMethod.STEAMED
                )
                nutrientList.add(nutrients)

                visionItems.add(
                    FoodVisionItem(
                        id = region.id,
                        label = englishName,
                        nepaliLabel = nepaliName,
                        boundingBox = region.boundingBox,
                        region = region,
                        topPrediction = topPred,
                        portionEstimate = portion,
                        nutrients = nutrients
                    )
                )
            }
            stageTimings["classification_portion"] = System.currentTimeMillis() - classStart

            // STAGE 7: AGGREGATION & COMPLETION
            onStageChanged?.invoke(VisionProcessingStage.COMPLETED)
            val totalNutrients = calorieEngine.aggregateMeal(nutrientList)
            val duration = System.currentTimeMillis() - startTime

            VisionProcessingResult.Success(
                data = FoodVisionOutput(
                    detectedObjects = detectedObjects,
                    foodMask = foodMask,
                    items = visionItems,
                    totalNutrients = totalNutrients,
                    frameHash = frame.frameHash
                ),
                processingDurationMs = duration,
                stageBreakdown = stageTimings,
                modelUsed = "NutriGlass-OnDevice-V1.2"
            )
        } catch (e: CancellationException) {
            VisionProcessingResult.Cancelled
        } catch (e: OutOfMemoryError) {
            VisionProcessingResult.Error(
                message = "Out of memory during image processing. Please choose a lower resolution.",
                errorType = VisionErrorType.OUT_OF_MEMORY,
                failedStage = VisionProcessingStage.PREPROCESSING
            )
        } catch (e: Exception) {
            VisionProcessingResult.Error(
                message = e.message ?: "An unexpected error occurred during food vision pipeline.",
                errorType = VisionErrorType.INFERENCE_FAILED,
                throwable = e,
                failedStage = VisionProcessingStage.DETECTION
            )
        }
    }

    override fun cancelProcessing() {
        isCancelled = true
    }

    override fun release() {
        isCancelled = true
    }
}
