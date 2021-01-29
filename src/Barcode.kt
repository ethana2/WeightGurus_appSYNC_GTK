import org.freedesktop.cairo.Context
import org.freedesktop.cairo.Format
import org.freedesktop.cairo.ImageSurface
import org.gnome.gdk.RGBA
import org.opencv.core.*
import org.opencv.core.Core.mean
import org.opencv.highgui.HighGui.imshow
import org.opencv.highgui.HighGui.waitKey
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

class Barcode() {

    private val displayStructure = DisplayStructure()
    var displayBitsList: Array<DisplayBit> = displayStructure.getDisplayBits()
    val lcd = LCD(displayBitsList)
    var codeWordPlane = Array<Boolean> (70) {false}
    var barcodeVersion = BarcodeVersion.ONE

    init {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME) // For OpenCV performance
    }

    fun setStateFromCodeWordPlane(codeWordPlane : Array<Boolean>) {
        this.codeWordPlane = codeWordPlane
        for (each in 0..67) {
            displayBitsList[each].value = codeWordPlane[each]
        }
    }

    fun setStateFromImage(filename: String) {
        val image = lcd.isolateBarcodeImage(filename)
        imshow("Isolated image", image)
        waitKey()
        for (each in displayBitsList) {
            val p1 = lcd.samplePixel(image, each.x1, each.y1)
            val p2 = lcd.samplePixel(image, each.x2, each.y2)
            each.value = p1 < p2
            this.codeWordPlane[each.index] = each.value
        }
    }

    // This will later be modified with an argument for barcode version. What's here now is v1.
    fun getImageFile() : String {
        return lcd.render()
    }

    enum class BarcodeVersion {
        ONE, TWO
    }

    class LCD(val payloadBitsList: Array<DisplayBit>) {

        val pixelWidth = 34.5
        val pixelHeight = 63.0
        val sideMargins = 100.0
        val topBottomMargins = 32.0
        val keyingCircleRadius = 32.0

        val displayWidth = 980.0
        val displayHeight = 480.0

        val background = RGBA(0.6, 0.6, 0.6, 1.0)
        val pixelOn = RGBA(0.2, 0.2, 0.2, 1.0)
        val pixelOff = RGBA(0.55, 0.55, 0.55, 1.0)

        val pMarginWidth = 4.0
        val vMarginHeight = 7.0

        fun render(): String {
            val filePath = "/home/ethan/Android/images/generated.png"
            val image = ImageSurface(Format.ARGB32, displayWidth.toInt(), displayHeight.toInt())
            val cr = Context(image)
            cr.setSource(background)
            cr.rectangle(0.0, 0.0, displayWidth, displayHeight)
            cr.fill()
            renderKeyingCircle(cr, 50.0, 380.0) // Left
            renderKeyingCircle(cr, 932.0, 110.0) // Top right
            renderKeyingCircle(cr, 932.0, 335.0) // Bottom right

            for (each in payloadBitsList) {
                renderPixel(cr, each.x1, each.y1, each.v1.invoke())
                renderPixel(cr, each.x2, each.y2, each.v2.invoke())
            }

            cr.save()
            image.writeToPNG(filePath)
            return filePath
        }

        private fun renderPixel(cr: Context, x: Int, y: Int, value: Boolean) {
            val columnsOffset = x.toInt() / 5
            val top = topBottomMargins + (y * pixelHeight)
            // left and right will need empty column widths accounted for next
            val left = sideMargins + ((x + columnsOffset) * pixelWidth)
            cr.setSource(pixelOn)
            if (value) {
                cr.setSource(pixelOn)
            } else {
                cr.setSource(pixelOff)
            }
            cr.rectangle(left, top, pixelWidth - pMarginWidth, pixelHeight - vMarginHeight)
            cr.fill()
        }

        fun samplePixel(barcodeImage: Mat, x: Int, y: Int) : Double {
            val columnsOffset = x.toInt() / 5
            val top = topBottomMargins + (y * pixelHeight)
            // left and right will need empty column widths accounted for next
            val left = sideMargins + ((x + columnsOffset) * pixelWidth)

            val topLeft = Point(left, top)
            val bottomRight = Point(left + pixelWidth - pMarginWidth, top + pixelHeight - vMarginHeight)
            val r = Rect(topLeft, bottomRight)
            val pixelImage = Mat(barcodeImage, r)
            val meanScalar = mean(pixelImage).`val` // WTF OpenCV?
            return meanScalar[0]
        }

        private fun renderKeyingCircle(cr: Context, x: Double, y: Double) {
            cr.setSource(pixelOn)
            cr.arc(x, y, keyingCircleRadius, 0.0, 2 * Math.PI)
            cr.fill()
        }

        fun isolateBarcodeImage(filename: String) : Mat {
                        val src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_COLOR)
            val image = Mat()
            Imgproc.cvtColor(src, image, Imgproc.COLOR_BGR2GRAY)
            Imgproc.medianBlur(image, image, 5)

            val circles = Mat()
            Imgproc.HoughCircles(image, circles, Imgproc.HOUGH_GRADIENT, 1.0,
                    image.rows().toDouble() / 16,
                    100.0, 30.0, 30, 50)
            for (x in 0 until circles.cols()) {
                val c = circles[0, x]
                val center = Point(c[0], c[1])
                println("Circle at " + c[0] + ", " + c[1])
                Imgproc.circle(src, center, 1,
                        Scalar(255.0, 0.0, 255.0),
                        3, 8, 0)
                val radius = c[2].roundToInt()
                Imgproc.circle(src, center, radius,
                        Scalar(255.0, 0.0, 255.0),
                        3, 8, 0)
            }

            val maxY = maxOf(circles[0,0][1], circles[0,1][1], circles[0,2][1])
            val minY = minOf(circles[0,0][1], circles[0,1][1], circles[0,2][1])
            val maxX = maxOf(circles[0,0][0], circles[0,1][0], circles[0,2][0])
            val minX = minOf(circles[0,0][0], circles[0,1][0], circles[0,2][0])
            val rangeY = maxY - minY
            val rangeX = maxX - minX
            val cropTop = minY - rangeY*0.5
            val cropBottom = maxY + rangeY*0.5
            val cropHeight = cropBottom - cropTop
            val cropLeft = minX - rangeX*0.15
            val cropRight = maxX + rangeX*0.15
            val cropWidth = cropRight - cropLeft
            val cropRect = Rect(cropLeft.toInt(), cropTop.toInt(), cropWidth.toInt(), cropHeight.toInt())
            val croppedImage = Mat(image,cropRect)

            val cannyImage = Mat()
            Imgproc.Canny(croppedImage, cannyImage, 50.0, 200.0, 3)
            val analysisImage = Mat(cannyImage.height(), cannyImage.width(), cannyImage.type(), Scalar(0.0))
            val contours = MutableList<MatOfPoint>(0) { MatOfPoint() }
            val hierarchy = Mat()
            Imgproc.findContours(cannyImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

            var allPointsList = listOf(Point())
            for (index in contours.indices) {
                val contour = contours[index]
                val points = contour.toList()
                allPointsList = allPointsList + points
            }
            allPointsList = allPointsList.filter { it != Point(0.0, 0.0) }
            println("Total points: ${allPointsList.size}")

            val mergedContour = MatOfPoint()
            mergedContour.fromList(allPointsList)
            val hullIndices = MatOfInt()
            Imgproc.convexHull(mergedContour, hullIndices)
            val hullIndicesList = hullIndices.toList()
            val hullContour = MatOfPoint() // Need to rename all these so there's no "A" since there's no longer a "B".
            var hullPointsList = listOf(Point())
            for (index in hullIndicesList) {
                hullPointsList = hullPointsList + allPointsList[index]
            }
            hullPointsList = hullPointsList.filter { it != Point(0.0, 0.0) }
            hullContour.fromList(hullPointsList)
            //drawContours(analysisImage, mutableListOf(hullContourA), -1, Scalar(255.0, 0.0, 255.0), 1)
            for (point in hullPointsList) {
                Imgproc.circle(analysisImage, point, 1,
                        Scalar(255.0, 0.0, 255.0),
                        3, 8, 0)
            }

            // The "RIGHT" way to do this would be to write an algorithm to find the smallest bounding trapezoid
            // for the convex hull. I already tried to walk approxPolyDP backwards and average, and this failed
            // because approxPolyDP picks the same points regardless of whether the input contour is reversed.
            // I cannot justify the time to devise and implement a bounding trapezoid algorithm for this.
            // An erode operation to sharpen the corners first would be unjustifiably expensive.
            // An additional rotate operation would complicate code and *still* not be perfect.
            // So, the image to be analyzed is rotated counterclockwise by approxPolyDP by, like, one degree.
            // Wherever I go. Whatever I do.
            // I'll always know in the back of my mind that everywhere this code is used,
            // the image will be rotated counterclockwise by one degree.
            // This is why I don't fear death. ~ethana2

            val perspectiveQuad2f = MatOfPoint2f()
            val hullContour2f = MatOfPoint2f()
            hullContour.convertTo(hullContour2f, CvType.CV_32FC2)
            val epsilon = 0.1* Imgproc.arcLength(hullContour2f, true)
            Imgproc.approxPolyDP(hullContour2f, perspectiveQuad2f, epsilon, true)
            val perspectiveQuad = MatOfPoint()
            perspectiveQuad2f.convertTo(perspectiveQuad, CvType.CV_32S)
            //drawContours(analysisImage, mutableListOf(perspectiveQuad), -1, Scalar(255.0, 0.0, 255.0), 1)
            //imshow("analysisImage", analysisImage)

            // Take the 4 points of this quad, identify them, and pass them into a perspective operation somehow.
            println("pQuad size: " + perspectiveQuad.size())
            for (point in perspectiveQuad.toList()) {
                println(point)
            }

            // Find the center of the quad so I can iterate through and identify the corners
            val m = Imgproc.moments(perspectiveQuad)
            val cX = (m._m10 / m._m00)
            val cY = (m._m01 / m._m00)
            Imgproc.circle(analysisImage, Point(cX, cY), 1,
                    Scalar(255.0, 0.0, 255.0),
                    5, 8, 0)
            var TL1 = Point()
            var TR1 = Point()
            var BR1 = Point()
            var BL1 = Point()
            for (point in perspectiveQuad.toList()) {
                if ((point.x < cX) and (point.y < cY)) {
                    TL1 = point
                } else if ((point.x > cX) and (point.y < cY)) {
                    TR1 = point
                } else if ((point.x > cX) and (point.y > cY)) {
                    BR1 = point
                } else if ((point.x < cX) and (point.y > cY)) {
                    BL1 = point
                } else {
                    throw Exception("Error determining screen corner identities by coordinates.")
                }
            }

            val TL2 = Point(0.0,0.0)
            val TR2 = Point(displayWidth,0.0)
            val BR2 = Point(displayWidth,displayHeight)
            val BL2 = Point(0.0,displayHeight)

            val p1Mat = MatOfPoint(TL1, TR1, BR1, BL1)
            p1Mat.convertTo(p1Mat, CvType.CV_32F)
            val p2Mat = MatOfPoint(TL2, TR2, BR2, BL2)
            p2Mat.convertTo(p2Mat, CvType.CV_32F)

            val perspectiveTMat = Imgproc.getPerspectiveTransform(p1Mat, p2Mat)
            val perspectiveCorrectedImage = Mat()
            Imgproc.warpPerspective(croppedImage, perspectiveCorrectedImage, perspectiveTMat, Size(displayWidth, displayHeight))
            return perspectiveCorrectedImage
        }
    }

    // Coordinates are in a screen-like space; y is down from top left and x is x
    // Screen space does NOT include empty columns, because different models of scale have different column width
    class DisplayBit(val x1: Int, val y1: Int, val index: Int, var value: Boolean = false,
                     val orientation: BitOrientation = BitOrientation.VERTICAL) {
        var v1 = { value }
        var v2 = { !value }

        var x2: Int = when (orientation) {
            BitOrientation.VERTICAL -> x1
            BitOrientation.HORIZONTAL -> x1 + 1
        }

        var y2: Int = when (orientation) {
            BitOrientation.VERTICAL -> y1 + 1
            BitOrientation.HORIZONTAL -> y1
        }
    }

    enum class BitOrientation {
        HORIZONTAL, VERTICAL
    }

    abstract class Row() {
        abstract val displayBitArray: Array<DisplayBit>
        val h = BitOrientation.HORIZONTAL
        val v = BitOrientation.VERTICAL
    }

    class TopRow(val y: Int, val index: Int) : Row() {
        override val displayBitArray = arrayOf(
                DisplayBit( 0, y, index +  0, false, h),
                DisplayBit( 2, y, index +  1, false, h),
                DisplayBit( 4, y, index +  2, false, v),
                DisplayBit( 5, y, index +  3, false, h),
                DisplayBit( 7, y, index +  4, false, h),
                DisplayBit( 9, y, index +  5, false, v),
                DisplayBit(10, y, index +  6, false, h),
                DisplayBit(12, y, index +  7, false, h),
                DisplayBit(14, y, index +  8, false, v),
                DisplayBit(15, y, index +  9, false, h),
                DisplayBit(17, y, index + 10, false, h),
                DisplayBit(19, y, index + 11, false, v)
        )
    }

    class BottomRow(val y: Int, val index: Int) : Row() {
        override val displayBitArray = arrayOf(
                DisplayBit( 0, y, index + 0, false, h),
                DisplayBit( 2, y, index + 1, false, h),
                DisplayBit( 5, y, index + 2, false, h),
                DisplayBit( 7, y, index + 3, false, h),
                DisplayBit(10, y, index + 4, false, h),
                DisplayBit(12, y, index + 5, false, h),
                DisplayBit(15, y, index + 6, false, h),
                DisplayBit(17, y, index + 7, false, h)
        )
    }

    // x, y, and index counting from zero inclusive
    class DisplayStructure {
        private val rhl = 8 // inline RowOfVerticals.bits
        private val rvl = 4 // inline RowOfHorizontals.bits
        private val crl = 2 * rhl + rvl // Composite row bits: Horizontal, vertical, horizontal.

        val rowArray = arrayOf(
                TopRow(0, 0),
                BottomRow(1, rhl + rvl),

                TopRow(2, crl),
                BottomRow(3, crl + rhl + rvl),

                TopRow(4, 2 * crl),
                BottomRow(5, 2 * crl + rhl + rvl),

                BottomRow(6, 3 * crl)
        )

        fun getDisplayBits() : Array<DisplayBit> {
            val displayBits = Array<DisplayBit>(68)
            { DisplayBit(0, 0, 0, false, BitOrientation.HORIZONTAL) }
            for (i in rowArray.indices) {
                for (j in rowArray[i].displayBitArray) displayBits[j.index] = j
            }
            return displayBits
        }
    }
}