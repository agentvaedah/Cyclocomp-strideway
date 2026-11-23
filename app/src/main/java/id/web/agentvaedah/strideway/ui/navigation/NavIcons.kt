package id.web.agentvaedah.strideway.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom 24x24 navigation icons built on a consistent 1.9dp stroke grid with distinct
 * active (filled/accented) and inactive (outlined) states so bottom-bar tabs feel tailored
 * to an endurance sport app rather than generic Material placeholders.
 */
object NavIcons {
    val HomeOutlined: ImageVector by lazy {
        icon("NavHomeOutlined") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 10.75f)
                lineTo(12f, 3.75f)
                lineTo(20.5f, 10.75f)
                moveTo(5.75f, 9.25f)
                verticalLineTo(19.25f)
                curveTo(5.75f, 19.94f, 6.31f, 20.5f, 7f, 20.5f)
                horizontalLineTo(17f)
                curveTo(17.69f, 20.5f, 18.25f, 19.94f, 18.25f, 19.25f)
                verticalLineTo(9.25f)
                moveTo(9.75f, 20.5f)
                verticalLineTo(14.75f)
                curveTo(9.75f, 13.78f, 10.53f, 13f, 11.5f, 13f)
                horizontalLineTo(12.5f)
                curveTo(13.47f, 13f, 14.25f, 13.78f, 14.25f, 14.75f)
                verticalLineTo(20.5f)
            }
        }
    }

    val HomeFilled: ImageVector by lazy {
        icon("NavHomeFilled") {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11.12f, 3.22f)
                curveTo(11.63f, 2.8f, 12.37f, 2.8f, 12.88f, 3.22f)
                lineTo(20.63f, 9.65f)
                curveTo(21.17f, 10.1f, 21.15f, 10.94f, 20.59f, 11.36f)
                curveTo(20.23f, 11.63f, 19.74f, 11.61f, 19.4f, 11.33f)
                lineTo(19f, 11f)
                verticalLineTo(19f)
                curveTo(19f, 20.1f, 18.1f, 21f, 17f, 21f)
                horizontalLineTo(14.5f)
                verticalLineTo(15f)
                curveTo(14.5f, 13.9f, 13.6f, 13f, 12.5f, 13f)
                horizontalLineTo(11.5f)
                curveTo(10.4f, 13f, 9.5f, 13.9f, 9.5f, 15f)
                verticalLineTo(21f)
                horizontalLineTo(7f)
                curveTo(5.9f, 21f, 5f, 20.1f, 5f, 19f)
                verticalLineTo(11f)
                lineTo(4.6f, 11.33f)
                curveTo(4.26f, 11.61f, 3.77f, 11.63f, 3.41f, 11.36f)
                curveTo(2.85f, 10.94f, 2.83f, 10.1f, 3.37f, 9.65f)
                close()
            }
        }
    }

    /** Route & terrain compass-pin map icon for Peta. */
    val MapOutlined: ImageVector by lazy {
        icon("NavMapOutlined") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 6.2f)
                lineTo(9f, 4f)
                lineTo(15f, 6.4f)
                lineTo(20.5f, 4.2f)
                verticalLineTo(17.8f)
                lineTo(15f, 20f)
                lineTo(9f, 17.6f)
                lineTo(3.5f, 19.8f)
                close()
                moveTo(9f, 4f)
                verticalLineTo(17.6f)
                moveTo(15f, 6.4f)
                verticalLineTo(20f)
            }
        }
    }

    val MapFilled: ImageVector by lazy {
        icon("NavMapFilled") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.2f, 5.7f)
                lineTo(8.3f, 3.65f)
                verticalLineTo(17.5f)
                lineTo(3.2f, 19.55f)
                close()
                moveTo(9.7f, 3.7f)
                lineTo(14.3f, 5.55f)
                verticalLineTo(19.4f)
                lineTo(9.7f, 17.55f)
                close()
                moveTo(15.7f, 5.5f)
                lineTo(20.8f, 3.45f)
                verticalLineTo(17.3f)
                lineTo(15.7f, 19.35f)
                close()
            }
        }
    }

    /**
     * Athletic interval-bars / endurance pulse icon for Latihan (replaces the generic gym dumbbell).
     * Three ascending rounded interval columns with a crisp cadence pulse baseline.
     */
    val TrainingOutlined: ImageVector by lazy {
        icon("NavTrainingOutlined") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Bar 1 (warm-up)
                moveTo(4.25f, 19.5f)
                verticalLineTo(13.25f)
                curveTo(4.25f, 12.56f, 4.81f, 12f, 5.5f, 12f)
                horizontalLineTo(7f)
                curveTo(7.69f, 12f, 8.25f, 12.56f, 8.25f, 13.25f)
                verticalLineTo(19.5f)
                close()
                // Bar 2 (tempo)
                moveTo(10f, 19.5f)
                verticalLineTo(8.75f)
                curveTo(10f, 8.06f, 10.56f, 7.5f, 11.25f, 7.5f)
                horizontalLineTo(12.75f)
                curveTo(13.44f, 7.5f, 14f, 8.06f, 14f, 8.75f)
                verticalLineTo(19.5f)
                close()
                // Bar 3 (peak interval)
                moveTo(15.75f, 19.5f)
                verticalLineTo(5.25f)
                curveTo(15.75f, 4.56f, 16.31f, 4f, 17f, 4f)
                horizontalLineTo(18.5f)
                curveTo(19.19f, 4f, 19.75f, 4.56f, 19.75f, 5.25f)
                verticalLineTo(19.5f)
                close()
            }
        }
    }

    val TrainingFilled: ImageVector by lazy {
        icon("NavTrainingFilled") {
            path(fill = SolidColor(Color.Black)) {
                // Bar 1
                moveTo(4f, 13.2f)
                curveTo(4f, 12.26f, 4.76f, 11.5f, 5.7f, 11.5f)
                horizontalLineTo(6.8f)
                curveTo(7.74f, 11.5f, 8.5f, 12.26f, 8.5f, 13.2f)
                verticalLineTo(19.2f)
                curveTo(8.5f, 19.75f, 8.05f, 20.2f, 7.5f, 20.2f)
                horizontalLineTo(5f)
                curveTo(4.45f, 20.2f, 4f, 19.75f, 4f, 19.2f)
                close()
                // Bar 2
                moveTo(9.75f, 8.7f)
                curveTo(9.75f, 7.76f, 10.51f, 7f, 11.45f, 7f)
                horizontalLineTo(12.55f)
                curveTo(13.49f, 7f, 14.25f, 7.76f, 14.25f, 8.7f)
                verticalLineTo(19.2f)
                curveTo(14.25f, 19.75f, 13.8f, 20.2f, 13.25f, 20.2f)
                horizontalLineTo(10.75f)
                curveTo(10.2f, 20.2f, 9.75f, 19.75f, 9.75f, 19.2f)
                close()
                // Bar 3
                moveTo(15.5f, 5.2f)
                curveTo(15.5f, 4.26f, 16.26f, 3.5f, 17.2f, 3.5f)
                horizontalLineTo(18.3f)
                curveTo(19.24f, 3.5f, 20f, 4.26f, 20f, 5.2f)
                verticalLineTo(19.2f)
                curveTo(20f, 19.75f, 19.55f, 20.2f, 19f, 20.2f)
                horizontalLineTo(16.5f)
                curveTo(15.95f, 20.2f, 15.5f, 19.75f, 15.5f, 19.2f)
                close()
            }
        }
    }

    val YouOutlined: ImageVector by lazy {
        icon("NavYouOutlined") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Head circle
                moveTo(12f, 4.25f)
                curveTo(14.07f, 4.25f, 15.75f, 5.93f, 15.75f, 8f)
                curveTo(15.75f, 10.07f, 14.07f, 11.75f, 12f, 11.75f)
                curveTo(9.93f, 11.75f, 8.25f, 10.07f, 8.25f, 8f)
                curveTo(8.25f, 5.93f, 9.93f, 4.25f, 12f, 4.25f)
                close()
                // Shoulders arc
                moveTo(4.75f, 19.75f)
                curveTo(5.2f, 16.25f, 8.15f, 14.25f, 12f, 14.25f)
                curveTo(15.85f, 14.25f, 18.8f, 16.25f, 19.25f, 19.75f)
            }
        }
    }

    val YouFilled: ImageVector by lazy {
        icon("NavYouFilled") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3.75f)
                curveTo(14.35f, 3.75f, 16.25f, 5.65f, 16.25f, 8f)
                curveTo(16.25f, 10.35f, 14.35f, 12.25f, 12f, 12.25f)
                curveTo(9.65f, 12.25f, 7.75f, 10.35f, 7.75f, 8f)
                curveTo(7.75f, 5.65f, 9.65f, 3.75f, 12f, 3.75f)
                close()
                moveTo(4.25f, 19.5f)
                curveTo(4.25f, 15.8f, 7.5f, 13.75f, 12f, 13.75f)
                curveTo(16.5f, 13.75f, 19.75f, 15.8f, 19.75f, 19.5f)
                curveTo(19.75f, 20.19f, 19.19f, 20.75f, 18.5f, 20.75f)
                horizontalLineTo(5.5f)
                curveTo(4.81f, 20.75f, 4.25f, 20.19f, 4.25f, 19.5f)
                close()
            }
        }
    }

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()
}
