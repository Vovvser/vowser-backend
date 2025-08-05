package com.vowser.backend.application.service

import com.google.cloud.speech.v2.*
import com.google.protobuf.ByteString
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class SpeechService(
    private val speechClient: SpeechClient,
    @Value("\${gcp.project-id}") private val projectId: String,
    @Value("\${gcp.location}") private val location: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun transcribe(audioFile: MultipartFile): String {
        logger.info("음성 파일 수신: {}, 크기: {} bytes", audioFile.originalFilename, audioFile.size)
        require(!audioFile.isEmpty) { "업로드된 파일이 비어있습니다." }

        val audioData = ByteString.copyFrom(audioFile.bytes)
        val config = RecognitionConfig.newBuilder()
            .addLanguageCodes("ko-KR")
            .setModel("long")
            .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
            .build()

        val recognizerPath = "projects/$projectId/locations/$location/recognizers/_"

        val request = RecognizeRequest.newBuilder()
            .setRecognizer(recognizerPath)
            .setConfig(config)
            .setContent(audioData)
            .build()

        logger.info("Google Speech-to-Text API 호출 중...")
        val response = speechClient.recognize(request)

        check(response.resultsList.isNotEmpty()) { "음성을 인식할 수 없습니다." }

        val transcript = response.getResults(0).getAlternatives(0).transcript
        logger.info("변환된 텍스트: {}", transcript)
        return transcript
    }
}