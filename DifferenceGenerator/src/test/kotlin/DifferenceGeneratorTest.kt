import algorithms.AlignmentAlgorithm
import algorithms.AlignmentElement
import algorithms.DivideAndConquerAligner
import algorithms.Gotoh
import hashing.VideoFrameHasher
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File

internal class DifferenceGeneratorTest {
    private val resourcesPathPrefix = "src/test/resources/"

    private val compressedVideo = resourcesPathPrefix + "compressedVideo.mov"

    // Video with 9 frames
    private val video9Frames = resourcesPathPrefix + "9Screenshots.mov"

    // Modified video with 9 frames
    private val modifiedVideo9Frames = resourcesPathPrefix + "9ScreenshotsModified.mov"

    // Video with 10 frames, the second frame is added compared to the vide9Frames
    private val video10Frames = resourcesPathPrefix + "10Screenshots.mov"

    // Modified video with 10 frames
    private val modifiedVideo10Frames = resourcesPathPrefix + "10ScreenshotsModified.mov"

    // Video with 11 frames, the second frame is like in video9Frames and two frames are added
    // at the end
    private val video11Frames = resourcesPathPrefix + "11Screenshots.mov"

    // Modified video with 11 frames
    private val modifiedVideo11Frames = resourcesPathPrefix + "11ScreenshotsModified.mov"

    private val mask = resourcesPathPrefix + "mask.png"

    private val metric = PixelCountMetric(normalize = true)

    private var algorithm: AlignmentAlgorithm<BufferedImage> =
        Gotoh<BufferedImage>(metric, gapOpenPenalty = -0.5, gapExtensionPenalty = -0.0)

    init {
        // comment out if simple Gotoh preferred
        algorithm = DivideAndConquerAligner(algorithm, VideoFrameHasher())
    }

    @Test
    fun `Test a generated case using TestCaseGenerator`() {
        var gapOpen = -0.5
        var gapExtension = -0.0
        val iterations = 20
        System.getProperty("gapOpenPenalty")?.let {
            gapOpen = it.toDouble()
        }
        System.getProperty("gapExtensionPenalty")?.let {
            gapExtension = it.toDouble()
        }
        print("Gap Open Penalty: $gapOpen, Gap Extension Penalty: $gapExtension, Iterations: $iterations")
        val pathVideo1 = resourcesPathPrefix + "generatedVideo1.mkv"
        val pathVideo2 = resourcesPathPrefix + "generatedVideo2.mkv"
        val outputPath = resourcesPathPrefix + "generatedOutput.mkv"
        var algorithm: AlignmentAlgorithm<BufferedImage> = Gotoh<BufferedImage>(metric, gapOpenPenalty = -0.5, gapExtensionPenalty = -0.0)

        // Comment out if simple Gotoh wanted
        algorithm = DivideAndConquerAligner(algorithm, VideoFrameHasher())

        var allDistances = Array(iterations) { 0 }
        for (i in 0 until iterations) {
            val testCaseGenerator = TestCaseGenerator(pathVideo1, pathVideo2, 12)
            val expectedAlignment = testCaseGenerator.generateRandomTestCase()
            val differenceGenerator = DifferenceGenerator(pathVideo1, pathVideo2, outputPath, algorithm)
            val actualAlignment = differenceGenerator.alignment
            println("Calculated Alignment: " + actualAlignment.joinToString())
            println("Expected Alignment: " + expectedAlignment.joinToString())

            val levenshteinDistance = LevenshteinDistance(expectedAlignment, actualAlignment)
//            println("Levenshtein Distance: " + levenshteinDistance.distance)
            allDistances[i] = levenshteinDistance.distance
            println(levenshteinDistance.distance)
        }
        println(allDistances.joinToString())
        println("Average Levenshtein Distance: " + allDistances.average())
    }

    @Test
    fun `test if DifferenceGenerator finds deletion`() {
        val outputPath = resourcesPathPrefix + "outputDeletion.mov"

        // Delete output file if it exists
        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val g =
            DifferenceGenerator(
                modifiedVideo10Frames,
                video9Frames,
                outputPath,
                algorithm,
            )
        val expectedAlignment =
            arrayOf(
                AlignmentElement.MATCH,
                AlignmentElement.DELETION,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
            )
        assertArrayEquals(
            expectedAlignment,
            g.alignment,
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.isFile)
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun `test if DifferenceGenerator finds insertion`() {
        val outputPath = resourcesPathPrefix + "outputInsertion.mov"

        // Delete output file if it exists
        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val g =
            DifferenceGenerator(
                video9Frames,
                modifiedVideo10Frames,
                outputPath,
                algorithm,
            )
        println(g.alignment.joinToString())
        val expectedAlignment =
            arrayOf(
                AlignmentElement.MATCH,
                AlignmentElement.INSERTION,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
            )
        assertArrayEquals(
            expectedAlignment,
            g.alignment,
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.isFile)
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun `test if DifferenceGenerator finds insertion and deletion`() {
        val outputPath = resourcesPathPrefix + "outputInsertionDeletion.mov"

        // Delete output file if it exists
        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val g = DifferenceGenerator(video11Frames, video10Frames, outputPath, algorithm)
        println(g.alignment.joinToString())
        val expectedAlignment =
            arrayOf(
                AlignmentElement.MATCH,
                AlignmentElement.INSERTION,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.DELETION,
                AlignmentElement.DELETION,
            )
        assertArrayEquals(
            expectedAlignment,
            g.alignment,
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.isFile)
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun `test constructor with non-lossless codec`() {
        val outputPath = resourcesPathPrefix + "output.mov"
        val algorithm =
            Gotoh<BufferedImage>(
                metric,
                gapOpenPenalty = -0.5,
                gapExtensionPenalty = -0.0,
            )
        assertThrows(Exception::class.java) {
            DifferenceGenerator(video9Frames, compressedVideo, outputPath, algorithm)
        }
    }

    @Test
    fun `test masking`() {
        val outputPath = resourcesPathPrefix + "maskingChanges.mov"

        // Delete output file if it exists
        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val g =
            DifferenceGenerator(
                modifiedVideo10Frames,
                video9Frames,
                outputPath,
                algorithm,
                mask,
            )
        val expectedAlignment =
            arrayOf(
                AlignmentElement.MATCH,
                AlignmentElement.DELETION,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
                AlignmentElement.MATCH,
            )
        assertArrayEquals(
            expectedAlignment,
            g.alignment,
        )

        assertTrue(outputFile.exists())
        assertTrue(outputFile.isFile)
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun `test differences`() {
        val outputPath = resourcesPathPrefix + "differences.mov"

        // Delete output file if it exists
        val outputFile = File(outputPath)
        if (outputFile.exists()) {
            outputFile.delete()
        }

        DifferenceGenerator(
            video9Frames,
            modifiedVideo9Frames,
            outputPath,
            algorithm,
        )
    }
}
