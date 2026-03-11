package com.example.airecruit.application.service;

import com.example.airecruit.application.domain.Resume;
import com.example.airecruit.application.domain.enums.ResumeType;
import com.example.airecruit.application.dto.ResumeDto;
import com.example.airecruit.application.repository.ResumeRepository;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public ResumeDto.Response createTextResume(Long userId, ResumeDto.TextCreateReq req) {
        Resume resume = Resume.builder()
                .userId(userId)
                .title(req.getTitle())
                .resumeType(ResumeType.TEXT)
                .content(req.getContent())
                .isDefault(false)
                .build();
        return ResumeDto.Response.from(resumeRepository.save(resume));
    }

    @Transactional
    public ResumeDto.Response uploadPdfResume(Long userId, MultipartFile file, String title) {
        if (file.isEmpty() || !isPdfFile(file)) {
            throw new BizException(Status.INVALID_FILE_TYPE);
        }

        String extractedContent = extractTextFromPdf(file);
        String savedFilePath = saveFile(file);

        Resume resume = Resume.builder()
                .userId(userId)
                .title(title)
                .resumeType(ResumeType.PDF)
                .content(extractedContent)
                .originalFilename(file.getOriginalFilename())
                .filePath(savedFilePath)
                .isDefault(false)
                .build();

        return ResumeDto.Response.from(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeDto.Response> getMyResumes(Long userId) {
        return resumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ResumeDto.Response::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDto.Response getResume(Long userId, Long id) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizException(Status.RESUME_NOT_FOUND));
        return ResumeDto.Response.from(resume);
    }

    @Transactional
    public ResumeDto.Response updateResume(Long userId, Long id, ResumeDto.TextUpdateReq req) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizException(Status.RESUME_NOT_FOUND));
        if (resume.getResumeType() != ResumeType.TEXT) {
            throw new BizException(Status.FAIL, "텍스트 이력서만 수정할 수 있습니다.");
        }
        resume.updateText(req.getTitle(), req.getContent());
        return ResumeDto.Response.from(resume);
    }

    @Transactional
    public void deleteResume(Long userId, Long id) {
        Resume resume = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizException(Status.RESUME_NOT_FOUND));
        resumeRepository.delete(resume);
    }

    private boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        return "application/pdf".equals(contentType)
                || (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));
    }

    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("[PDF] 텍스트 추출 실패: {}", e.getMessage());
            throw new BizException(Status.ERROR_OCCURRED, "PDF 텍스트 추출에 실패했습니다.");
        }
    }

    private String saveFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());
            return filePath.toString();
        } catch (IOException e) {
            log.error("[File] 파일 저장 실패: {}", e.getMessage());
            throw new BizException(Status.ERROR_OCCURRED, "파일 저장에 실패했습니다.");
        }
    }
}
