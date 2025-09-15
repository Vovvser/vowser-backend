package com.vowser.backend.application.service.speech;

import com.vowser.backend.common.enums.SpeechMode;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 네이버 클라우드 플랫폼 STT를 사용해 음성을 텍스트로 변환
 * 업로드된 오디오 파일을 처리하고, 음성 명령용 변환 텍스트를 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechService {

    private final NaverSpeechService naverSpeechService;

    /**
     * 네이버 클라우드 STT를 사용하여 오디오 파일을 텍스트로 변환
     *
     * @param audioFile 변환할 오디오 파일
     * @return 인식된 텍스트(양끝 공백 제거)
     */
    public String transcribe(MultipartFile audioFile) {
        return naverSpeechService.transcribe(audioFile);
    }
    
    /**
     * 모드 기반 음성 인식 처리
     *
     * @param audioFile 변환할 오디오 파일
     * @param modes 활성화된 인식 모드들
     * @param customPhrases 추가 phrase hints
     * @return 모드별 후처리가 적용된 인식 텍스트
     */
    public String transcribeWithModes(MultipartFile audioFile, EnumSet<SpeechMode> modes, List<String> customPhrases) {
        return naverSpeechService.transcribeWithModes(audioFile, modes, customPhrases);
    }

}