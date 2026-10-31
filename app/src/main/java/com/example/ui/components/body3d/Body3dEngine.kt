package com.example.ui.components.body3d

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class BodyZone(val title: String, val description: String, val healthInsight: String) {
    ALL(
        "Full Body",
        "Overall anatomical human physique with balanced musculoskeletal alignment.",
        "Holistic body composition reflects total metabolic rate, cardiorespiratory fitness, and physical vigor."
    ),
    HEAD(
        "Head & Neck",
        "Cranial and cervical musculature and neck circumference marker.",
        "Neck circumference is an essential biometric in the U.S. Navy body fat formula and obstructive sleep health assessments."
    ),
    CHEST(
        "Chest & Upper Torso",
        "Pectoral muscles, ribcage perimeter, and thoracic subcutaneous tissue.",
        "Upper torso fat distribution correlates with cardiovascular endurance. Strength training enhances pectoral and thoracic posture."
    ),
    ABDOMEN(
        "Abdomen & Core",
        "Abdominal wall, waistline, subcutaneous fat, and intra-abdominal visceral region.",
        "Abdominal perimeter is a primary biomarker for visceral fat. Reductions in waist circumference directly decrease risks of type 2 diabetes and hypertension."
    ),
    ARMS(
        "Arms & Shoulders",
        "Deltoids, biceps, triceps, and forearm musculature.",
        "Upper arm lean mass reflects functional upper body strength and daily physical activity."
    ),
    LEGS(
        "Thighs & Lower Limbs",
        "Quadriceps, hamstrings, gluteals, and calf muscle groups.",
        "Lower body muscle mass comprises the body's largest glucose disposal reservoir, critical for metabolic health and insulin sensitivity."
    )
}

enum class CameraAnglePreset(val displayName: String, val yawRad: Float, val pitchRad: Float) {
    FRONT("Front", 0f, 0f),
    SIDE("Side", (Math.PI / 2.0).toFloat(), 0f),
    REAR("Rear", Math.PI.toFloat(), 0f),
    PERSPECTIVE("3D Orbit", 0.45f, 0.20f)
}

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun rotateY(angleRad: Float): Point3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val nx = (x * cosA) + (z * sinA)
        val nz = (-x * sinA) + (z * cosA)
        return Point3D(nx, y, nz)
    }

    fun rotateX(angleRad: Float): Point3D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val ny = (y * cosA) - (z * sinA)
        val nz = (y * sinA) + (z * cosA)
        return Point3D(x, ny, nz)
    }

    fun scale(factor: Float): Point3D = Point3D(x * factor, y * factor, z * factor)

    fun translate(dx: Float, dy: Float, dz: Float): Point3D = Point3D(x + dx, y + dy, z + dz)

    companion object {
        fun normal(p1: Point3D, p2: Point3D, p3: Point3D): Point3D {
            val v1x = p2.x - p1.x
            val v1y = p2.y - p1.y
            val v1z = p2.z - p1.z

            val v2x = p3.x - p1.x
            val v2y = p3.y - p1.y
            val v2z = p3.z - p1.z

            val crossX = (v1y * v2z) - (v1z * v2y)
            val crossY = (v1z * v2x) - (v1x * v2z)
            val crossZ = (v1x * v2y) - (v1y * v2x)

            val length = sqrt((crossX * crossX) + (crossY * crossY) + (crossZ * crossZ)).coerceAtLeast(0.0001f)
            return Point3D(crossX / length, crossY / length, crossZ / length)
        }
    }
}

data class PolygonFace(
    val vertexIndices: List<Int>,
    val zone: BodyZone
)

data class BodyMesh(
    val vertices: List<Point3D>,
    val faces: List<PolygonFace>
)

object BodyMeshGenerator {

    /**
     * Generates a realistic, stylized 3D human body mesh with anatomically plausible parameters:
     * - Male vs Female skeletal width ratios (shoulders, waist, pelvic girdle)
     * - Body Fat expansion across abdominal anterior wall, hips, and limbs
     * - Muscle mass scale factoring in chest/lat V-taper, shoulder deltoids, and limb fullness
     * - Explicit tape circumference adjustments for waist, hips, and neck
     */
    fun createBodyMesh(
        isMale: Boolean,
        bodyFatPercent: Float,
        muscleMassScale: Float = 1.0f,
        waistCm: Float? = null,
        hipCm: Float? = null,
        neckCm: Float? = null,
        isTargetModel: Boolean = false
    ): BodyMesh {
        val vertices = mutableListOf<Point3D>()
        val faces = mutableListOf<PolygonFace>()

        // Normalized fat expansion factor: 0.0 at athletic (10% M / 16% F) up to 1.5 at obese (45%+)
        val baselineBf = if (isMale) 12f else 18f
        val fatDelta = (bodyFatPercent - baselineBf).coerceIn(-6f, 35f)
        val fatScale = (fatDelta / 20f).coerceIn(-0.25f, 1.6f)

        // Muscle hypertrophy modifier (lat V-taper, pectoral volume, bicep fullness)
        val muscleMod = (muscleMassScale - 1.0f).coerceIn(-0.3f, 0.6f)

        // Gender proportion modifiers
        val shoulderWidth = (if (isMale) 0.44f else 0.36f) * (1.0f + (muscleMod * 0.20f))
        val waistWidthBase = if (isMale) 0.28f else 0.24f
        val hipWidthBase = if (isMale) 0.30f else 0.36f
        val chestDepthBase = (if (isMale) 0.24f else 0.26f) * (1.0f + (muscleMod * 0.25f))

        // Optional circumference overrides if provided
        val waistMultiplier = waistCm?.let { (it / 82.0f).coerceIn(0.75f, 1.60f) } ?: 1.0f
        val hipMultiplier = hipCm?.let { (it / 95.0f).coerceIn(0.75f, 1.50f) } ?: 1.0f
        val neckMultiplier = neckCm?.let { (it / 38.0f).coerceIn(0.80f, 1.40f) } ?: 1.0f

        // Fat expansion offsets by body zone
        val waistFatExpandX = waistWidthBase * (1.0f + (fatScale * 0.48f)) * waistMultiplier
        val waistFatExpandZ = 0.22f * (1.0f + (fatScale * 0.70f)) * waistMultiplier // Front abdominal protrusion
        val hipFatExpandX = hipWidthBase * (1.0f + (fatScale * 0.38f)) * hipMultiplier
        val hipFatExpandZ = 0.25f * (1.0f + (fatScale * 0.45f)) * hipMultiplier
        val chestFatExpandX = shoulderWidth * (0.85f + (fatScale * 0.18f) + (muscleMod * 0.25f))
        val chestFatExpandZ = chestDepthBase * (1.0f + (fatScale * (if (isMale) 0.28f else 0.42f)) + (muscleMod * 0.20f))
        val thighFatExpand = (1.0f + (fatScale * 0.35f) + (muscleMod * 0.15f))
        val armFatExpand = (1.0f + (fatScale * 0.25f) + (muscleMod * 0.30f))
        val neckFatExpand = (1.0f + (fatScale * 0.20f)) * neckMultiplier

        // Helper to generate an elliptical cross-section ring of vertices
        fun addRing(
            y: Float,
            rx: Float,
            rz: Float,
            centerX: Float = 0f,
            centerZ: Float = 0f,
            segmentCount: Int = 10,
            frontBulgeZ: Float = 0f
        ): List<Int> {
            val ringIndices = mutableListOf<Int>()
            for (i in 0 until segmentCount) {
                val theta = (i.toFloat() / segmentCount.toFloat()) * (Math.PI.toFloat() * 2f)
                val cosT = cos(theta)
                val sinT = sin(theta)

                // Extra forward bulge for positive Z (anterior abdomen / chest)
                val extraZ = if (sinT > 0f) frontBulgeZ * sinT else 0f
                val vx = centerX + (rx * cosT)
                val vz = centerZ + (rz * sinT) + extraZ

                val index = vertices.size
                vertices.add(Point3D(vx, y, vz))
                ringIndices.add(index)
            }
            return ringIndices
        }

        // Connect two adjacent vertex rings into quad/triangle faces
        fun connectRings(ring1: List<Int>, ring2: List<Int>, zone: BodyZone) {
            val count = ring1.size
            for (i in 0 until count) {
                val next = (i + 1) % count
                val r1Current = ring1[i]
                val r1Next = ring1[next]
                val r2Current = ring2[i]
                val r2Next = ring2[next]

                faces.add(PolygonFace(listOf(r1Current, r2Current, r2Next), zone))
                faces.add(PolygonFace(listOf(r1Current, r2Next, r1Next), zone))
            }
        }

        // Helper to cap a ring with a center apex vertex
        fun capRing(ring: List<Int>, apex: Point3D, isTop: Boolean, zone: BodyZone) {
            val apexIndex = vertices.size
            vertices.add(apex)
            val count = ring.size
            for (i in 0 until count) {
                val next = (i + 1) % count
                if (isTop) {
                    faces.add(PolygonFace(listOf(apexIndex, ring[i], ring[next]), zone))
                } else {
                    faces.add(PolygonFace(listOf(apexIndex, ring[next], ring[i]), zone))
                }
            }
        }

        val ringSegments = 10

        // ================= HEAD & NECK =================
        val headApex = Point3D(0f, 0.98f, 0.02f)
        val headTop = addRing(0.92f, 0.12f, 0.13f, 0f, 0.02f, ringSegments)
        val headMid = addRing(0.84f, 0.135f, 0.15f, 0f, 0.02f, ringSegments)
        val headJaw = addRing(0.76f, 0.105f * neckFatExpand, 0.12f * neckFatExpand, 0f, 0.04f, ringSegments)
        val neckRing = addRing(0.68f, 0.09f * neckFatExpand, 0.095f * neckFatExpand, 0f, 0.01f, ringSegments)

        capRing(headTop, headApex, isTop = true, BodyZone.HEAD)
        connectRings(headTop, headMid, BodyZone.HEAD)
        connectRings(headMid, headJaw, BodyZone.HEAD)
        connectRings(headJaw, neckRing, BodyZone.HEAD)

        // ================= CHEST & UPPER TORSO =================
        val clavicleRing = addRing(0.63f, shoulderWidth * 0.92f, 0.16f * neckFatExpand, 0f, 0.01f, ringSegments)
        val chestRing = addRing(
            0.52f,
            chestFatExpandX,
            chestFatExpandZ,
            0f,
            0.03f,
            ringSegments,
            frontBulgeZ = if (isMale) (0.03f * fatScale) + (0.04f * (1.0f + muscleMod)) else 0.06f + (fatScale * 0.03f)
        )
        val lowerRibsRing = addRing(0.40f, waistFatExpandX * 0.98f, 0.20f * (1f + (fatScale * 0.35f)), 0f, 0.02f, ringSegments)

        connectRings(neckRing, clavicleRing, BodyZone.CHEST)
        connectRings(clavicleRing, chestRing, BodyZone.CHEST)
        connectRings(chestRing, lowerRibsRing, BodyZone.CHEST)

        // ================= ABDOMEN & CORE =================
        val waistRing = addRing(
            0.26f,
            waistFatExpandX,
            waistFatExpandZ,
            0f,
            0.03f * (1f + fatScale),
            ringSegments,
            frontBulgeZ = 0.08f * (1f + (fatScale * 1.2f))
        )
        val navelRing = addRing(
            0.14f,
            waistFatExpandX * 1.05f,
            waistFatExpandZ * 1.05f,
            0f,
            0.025f * (1f + fatScale),
            ringSegments,
            frontBulgeZ = 0.09f * (1f + (fatScale * 1.4f))
        )
        val pelvisRing = addRing(0.00f, hipFatExpandX, hipFatExpandZ, 0f, 0.01f, ringSegments)

        connectRings(lowerRibsRing, waistRing, BodyZone.ABDOMEN)
        connectRings(waistRing, navelRing, BodyZone.ABDOMEN)
        connectRings(navelRing, pelvisRing, BodyZone.ABDOMEN)

        // ================= LEGS (LEFT & RIGHT) =================
        val legSegments = 8
        for (side in listOf(-1f, 1f)) {
            val legOffsetX = side * (hipFatExpandX * 0.52f)

            val hipCrotchRing = addRing(
                -0.08f,
                0.12f * thighFatExpand,
                0.13f * thighFatExpand,
                legOffsetX,
                0.01f,
                legSegments
            )
            val midThighRing = addRing(
                -0.25f,
                0.10f * thighFatExpand,
                0.11f * thighFatExpand,
                legOffsetX,
                0.01f,
                legSegments
            )
            val kneeRing = addRing(
                -0.45f,
                0.075f,
                0.082f,
                legOffsetX,
                0.015f,
                legSegments
            )
            val calfRing = addRing(
                -0.62f,
                0.085f * (1f + (fatScale * 0.20f) + (muscleMod * 0.15f)),
                0.090f * (1f + (fatScale * 0.20f) + (muscleMod * 0.15f)),
                legOffsetX,
                -0.01f,
                legSegments
            )
            val ankleRing = addRing(
                -0.80f,
                0.055f,
                0.060f,
                legOffsetX,
                0.0f,
                legSegments
            )
            val footApex = Point3D(legOffsetX, -0.92f, 0.08f)

            connectRings(hipCrotchRing, midThighRing, BodyZone.LEGS)
            connectRings(midThighRing, kneeRing, BodyZone.LEGS)
            connectRings(kneeRing, calfRing, BodyZone.LEGS)
            connectRings(calfRing, ankleRing, BodyZone.LEGS)
            capRing(ankleRing, footApex, isTop = false, BodyZone.LEGS)
        }

        // ================= ARMS (LEFT & RIGHT) =================
        val armSegments = 8
        for (side in listOf(-1f, 1f)) {
            val shoulderAnchorX = side * (shoulderWidth + 0.05f)

            val shoulderRing = addRing(
                0.60f,
                0.075f * armFatExpand,
                0.078f * armFatExpand,
                shoulderAnchorX,
                0.02f,
                armSegments
            )
            val midBicepRing = addRing(
                0.46f,
                0.068f * armFatExpand,
                0.070f * armFatExpand,
                shoulderAnchorX * 1.05f,
                0.01f,
                armSegments
            )
            val elbowRing = addRing(
                0.32f,
                0.055f,
                0.058f,
                shoulderAnchorX * 1.08f,
                0.0f,
                armSegments
            )
            val forearmRing = addRing(
                0.16f,
                0.052f,
                0.055f,
                shoulderAnchorX * 1.10f,
                0.0f,
                armSegments
            )
            val wristRing = addRing(
                0.02f,
                0.042f,
                0.045f,
                shoulderAnchorX * 1.12f,
                0.0f,
                armSegments
            )
            val handApex = Point3D(shoulderAnchorX * 1.14f, -0.10f, 0.0f)

            connectRings(shoulderRing, midBicepRing, BodyZone.ARMS)
            connectRings(midBicepRing, elbowRing, BodyZone.ARMS)
            connectRings(elbowRing, forearmRing, BodyZone.ARMS)
            connectRings(forearmRing, wristRing, BodyZone.ARMS)
            capRing(wristRing, handApex, isTop = false, BodyZone.ARMS)
        }

        return BodyMesh(vertices, faces)
    }

    /**
     * Morphs between current mesh and target mesh based on progress [0.0..1.0].
     */
    fun interpolateMesh(currentMesh: BodyMesh, targetMesh: BodyMesh, progress: Float): BodyMesh {
        val t = progress.coerceIn(0f, 1f)
        val interpolatedVertices = currentMesh.vertices.mapIndexed { index, currentP ->
            val targetP = targetMesh.vertices.getOrElse(index) { currentP }
            Point3D(
                x = currentP.x + ((targetP.x - currentP.x) * t),
                y = currentP.y + ((targetP.y - currentP.y) * t),
                z = currentP.z + ((targetP.z - currentP.z) * t)
            )
        }
        return BodyMesh(interpolatedVertices, currentMesh.faces)
    }
}
