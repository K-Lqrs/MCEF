package net.rk4z.mcef

import net.rk4z.mcef.progress.MCEFProgressTracker
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

/**
 * A downloader and extraction tool for java-cef builds.
 *
 * Downloads for [CinemaMod java-cef](https://github.com/CinemaMod/java-cef) are provided by the CinemaMod Group unless changed
 * in the MCEFSettings properties file; see [MCEFSettings].
 * Email ds58@mailbox.org for any questions or concerns regarding the file hosting.
 */
class MCEFResourceManager private constructor(
    private val host: String,
    private val javaCefCommitHash: String,
    private val platform: MCEFPlatform,
    directory: File
) {

    val progressTracker = MCEFProgressTracker()
    private val commitDirectory = File(directory, javaCefCommitHash)
    val platformDirectory = File(commitDirectory, platform.normalizedName)

    @Throws(IOException::class)
    fun requiresDownload(): Boolean {
        if (!commitDirectory.exists() && !commitDirectory.mkdirs()) {
            throw IOException("Failed to create directory")
        }

        val checksumFile = File(commitDirectory, "${platform.normalizedName}.tar.gz.sha256")

        // We always download the checksum for the java-cef build
        // We will compare this with <platform>.tar.gz.sha256
        // If the contents of the files differ (or it doesn't exist locally), we know we need to redownload JCEF
        val checksumMatches = try {
            compareChecksum(checksumFile)
        } catch (e: IOException) {
            MCEF.logger.error("Failed to compare checksum", e)
            // Assume checksum matches if we can't compare
            true
        }

        val platformDirectoryExists = platformDirectory.exists()

        MCEF.logger.info("Checksum matches: $checksumMatches")
        MCEF.logger.info("Platform directory exists: $platformDirectoryExists")

        return !checksumMatches || !platformDirectoryExists
    }

    @Throws(IOException::class)
    fun downloadJcef() {
        var retry = 0

        while (true) {
            try {
                val tarGzArchive = File(commitDirectory, "${platform.normalizedName}.tar.gz")
                if (tarGzArchive.exists() && !tarGzArchive.delete()) {
                    throw IOException("Failed to delete existing tar.gz archive")
                }

                // Download JCEF from file hosting
                MCEF.logger.info("Downloading JCEF...")
                progressTracker.setTask("Downloading JCEF")
                downloadFile(javaCefDownloadUrl, tarGzArchive, progressTracker)

                if (platformDirectory.exists() && !platformDirectory.delete()) {
                    throw IOException("Failed to delete existing platform directory")
                }

                // Compare checksum of .tar.gz file with remote checksum file
                progressTracker.setTask("Comparing Checksum")

                val checksumFile = File(commitDirectory, "${platform.normalizedName}.tar.gz.sha256")
                if (!compareChecksum(checksumFile, tarGzArchive)) {
                    throw IOException("Checksum mismatch")
                }

                progressTracker.setProgress(1.0f)
                progressTracker.done()

                // Extract JCEF from tar.gz
                MCEF.logger.info("Extracting JCEF...")
                extractTarGz(tarGzArchive, commitDirectory, progressTracker)
                if (tarGzArchive.exists() && !tarGzArchive.delete()) {
                    // Retry deletion on exit
                    tarGzArchive.deleteOnExit()
                }
                break
            } catch (e: Exception) {
                MCEF.logger.error("Failed to download and extract JCEF", e)
                retry++

                // Retry up to 3 times
                if (retry >= 3) {
                    throw e
                }
            }
        }

        progressTracker.done()
    }

    val javaCefDownloadUrl: String
        get() = formatURL(JAVA_CEF_DOWNLOAD_URL)

    val javaCefChecksumDownloadUrl: String
        get() = formatURL(JAVA_CEF_CHECKSUM_DOWNLOAD_URL)

    private fun formatURL(url: String): String {
        return url.replace("\${host}", host)
            .replace("\${java-cef-commit}", javaCefCommitHash)
            .replace("\${platform}", platform.normalizedName)
    }

    /**
     * @return true if the jcef build checksum file matches the remote checksum file (for the [MCEFResourceManager.javaCefCommitHash]),
     * false if the jcef build checksum file did not exist or did not match; this means we should redownload JCEF
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun compareChecksum(checksumFile: File): Boolean {
        // Create temporary checksum file with the same name as the real checksum file and .temp appended
        val tempChecksumFile = File(checksumFile.canonicalPath + ".temp")

        progressTracker.setTask("Downloading Checksum")
        downloadFile(javaCefChecksumDownloadUrl, tempChecksumFile, progressTracker)

        if (checksumFile.exists()) {
            val sameContent = FileUtils.contentEquals(checksumFile, tempChecksumFile)

            if (sameContent) {
                tempChecksumFile.delete()
                return true
            }
        }

        tempChecksumFile.renameTo(checksumFile)
        return false
    }

    private fun compareChecksum(checksumFile: File, archiveFile: File): Boolean {
        progressTracker.setTask("Comparing Checksum")

        if (!checksumFile.exists()) {
            throw RuntimeException("Checksum file does not exist")
        }

        return try {
            val checksum = FileUtils.readFileToString(checksumFile, "UTF-8").trim()
            val actualChecksum = DigestUtils.sha256Hex(FileInputStream(archiveFile)).trim()

            checksum == actualChecksum
        } catch (e: IOException) {
            throw RuntimeException("Error reading checksum file", e)
        }
    }

    private fun downloadFile(urlString: String, outputFile: File, percentCompleteConsumer: MCEFProgressTracker) {
        try {
            MCEF.logger.debug("Downloading '{}' to '{}'", urlString, outputFile.canonicalPath)
        } catch (e: IOException) {
            throw RuntimeException("Error getting canonical path for file", e)
        }

        try {
            val url = URL(urlString)
            val httpConn = url.openConnection() as HttpURLConnection

            val responseCode = httpConn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("HTTP error code: $responseCode")
            }

            val fileSize = httpConn.contentLength
            if (fileSize <= 0) {
                throw RuntimeException("Cannot read file size or file size is 0")
            }

            try {
                BufferedInputStream(httpConn.inputStream).use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        val buffer = ByteArray(2048)
                        var bytesRead: Int
                        var readBytes = 0
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            readBytes += bytesRead
                            val percentComplete = readBytes.toFloat() / fileSize
                            percentCompleteConsumer.setProgress(percentComplete)
                        }
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Error writing to file from input stream", e)
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException("Invalid URL format for $urlString", e)
        } catch (e: IOException) {
            throw RuntimeException("Error connecting to $urlString", e)
        }
    }

    @Throws(IOException::class)
    private fun extractTarGz(
        tarGzFile: File,
        outputDirectory: File,
        percentCompleteConsumer: MCEFProgressTracker
    ) {
        percentCompleteConsumer.setTask("Extracting")
        outputDirectory.mkdirs()

        try {
            TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(tarGzFile))).use { tarInput ->
                var totalBytesRead: Long = 0
                val fileSizeEstimate = tarGzFile.length() * 2.6158204f // Initial estimate for progress

                var entry: TarArchiveEntry?
                while (tarInput.nextTarEntry.also { entry = it } != null) {
                    if (!entry!!.isDirectory) {
                        val outputFile = File(outputDirectory, entry!!.name)
                        outputFile.parentFile.mkdirs()

                        BufferedOutputStream(FileOutputStream(outputFile)).use { outputStream ->
                            val buffer = ByteArray(8192) // Adjust buffer size for optimal I/O
                            var bytesRead: Int
                            while (tarInput.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead.toLong()
                                val percentComplete = totalBytesRead.toFloat() / fileSizeEstimate
                                percentCompleteConsumer.setProgress(percentComplete)
                            }
                        }
                    }
                }
            }
        } finally {
            percentCompleteConsumer.setProgress(1.0f) // Ensure completion regardless of exceptions
            percentCompleteConsumer.done()
        }
    }


    companion object {
        private const val JAVA_CEF_DOWNLOAD_URL = "\${host}/java-cef-builds/\${java-cef-commit}/\${platform}.tar.gz"
        private const val JAVA_CEF_CHECKSUM_DOWNLOAD_URL = "\${host}/java-cef-builds/\${java-cef-commit}/\${platform}.tar.gz.sha256"

        @Throws(IOException::class)
        fun newResourceManager(): MCEFResourceManager {
            val javaCefCommit = MCEF.getJavaCefCommit()
            MCEF.logger.info("JCEF Commit: $javaCefCommit")
            val settings = MCEF.settings

            return MCEFResourceManager(
                settings!!.downloadMirror, javaCefCommit!!,
                MCEFPlatform.getPlatform(), settings.librariesDirectory!!
            )
        }
    }
}
