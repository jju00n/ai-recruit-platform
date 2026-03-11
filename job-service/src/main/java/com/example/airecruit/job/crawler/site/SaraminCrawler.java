package com.example.airecruit.job.crawler.site;

import com.example.airecruit.job.crawler.CrawledJobData;
import com.example.airecruit.job.domain.enums.JobSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 사람인 채용공고 크롤러 (Apache HttpClient + Jsoup HTML 파싱)
 *
 * NOTE: 사람인 HTML 구조는 변경될 수 있습니다.
 * 셀렉터가 동작하지 않을 경우 브라우저 DevTools로 확인 후 수정이 필요합니다.
 * 크롤링 시 사람인 이용약관을 준수하세요 (포트폴리오/학습 목적).
 *
 * Jsoup 내장 HTTP는 사람인 TLS 핑거프린트 감지에 막히므로
 * Apache HttpClient로 HTML을 가져온 뒤 Jsoup으로 파싱한다.
 */
@Component
@Slf4j
public class SaraminCrawler {

    // IT 개발·데이터 카테고리 (cat_kewd=84)
    private static final String LIST_URL =
            "https://www.saramin.co.kr/zf_user/jobs/list/job-category" +
            "?cat_kewd=84&panel_type=category&search_optional_item=n" +
            "&search_done=y&panel_count=y&preview=y&page=%d";
    private static final String BASE_URL = "https://www.saramin.co.kr";
    private static final int MAX_PAGES = 3;     // 최대 3페이지 (약 45건)
    private static final int REQUEST_DELAY_MS = 1500;

    public List<CrawledJobData> crawl() {
        List<CrawledJobData> result = new ArrayList<>();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setSocketTimeout(10_000)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            for (int page = 1; page <= MAX_PAGES; page++) {
                try {
                    HttpGet request = new HttpGet(String.format(LIST_URL, page));
                    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    request.setHeader("Accept-Language", "ko-KR,ko;q=0.9");

                    String html;
                    try (CloseableHttpResponse response = httpClient.execute(request)) {
                        html = EntityUtils.toString(response.getEntity(), "UTF-8");
                    }

                    Document doc = Jsoup.parse(html);
                    Elements jobItems = doc.select(".box_item");

                    if (jobItems.isEmpty()) {
                        log.info("[SaraminCrawler] {}페이지 공고 없음, 종료", page);
                        break;
                    }

                    for (Element item : jobItems) {
                        try {
                            CrawledJobData data = parseItem(item);
                            if (data != null) result.add(data);
                        } catch (Exception e) {
                            log.warn("[SaraminCrawler] 공고 파싱 실패 - {}", e.getMessage());
                        }
                    }

                    log.info("[SaraminCrawler] {}페이지 수집 - 누적 {}건", page, result.size());
                    Thread.sleep(REQUEST_DELAY_MS);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[SaraminCrawler] {}페이지 크롤링 실패 - {}", page, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[SaraminCrawler] HttpClient 생성 실패 - {}", e.getMessage());
        }

        log.info("[SaraminCrawler] 수집 완료 - 총 {}건", result.size());
        return result;
    }

    private CrawledJobData parseItem(Element item) {
        // 공고 링크 & sourceUrl
        Element linkEl = item.selectFirst(".job_tit a");
        if (linkEl == null) return null;

        String href = linkEl.attr("href");
        String sourceUrl = href.startsWith("http") ? href : BASE_URL + href;
        String title = linkEl.text().trim();

        // 회사명 (셀렉터 변경: .corp_name a → .company_nm a.str_tit)
        Element companyEl = item.selectFirst(".company_nm a.str_tit");
        String companyName = companyEl != null ? companyEl.text().trim() : "";
        if (companyName.isEmpty()) return null;

        // 근무 지역 (셀렉터 변경: .job_condition span → .recruit_info p.work_place)
        Element workPlaceEl = item.selectFirst(".recruit_info p.work_place");
        String location = workPlaceEl != null ? workPlaceEl.text().trim() : "";

        // 경력 / 고용형태 (셀렉터 변경: 하나의 p.career에 "신입 · 경력 · 정규직 외" 형태로 합쳐짐)
        Element careerEl = item.selectFirst(".recruit_info p.career");
        String experienceLevel = "";
        String employmentType = "";
        if (careerEl != null) {
            for (String part : careerEl.text().split("·")) {
                String p = part.trim().replace(" 외", "").trim();
                if (p.contains("정규") || p.contains("계약") || p.contains("인턴") || p.contains("파트")) {
                    employmentType = p;
                } else if (p.contains("신입") || p.contains("경력")) {
                    experienceLevel = p;
                }
            }
        }

        // 기술스택 (셀렉터 변경: .job_sector a → .job_sector span)
        StringBuilder skillBuilder = new StringBuilder();
        for (Element tag : item.select(".job_sector span")) {
            String text = tag.text().trim();
            if (!text.isEmpty() && !text.equals("외")) {
                if (skillBuilder.length() > 0) skillBuilder.append(",");
                skillBuilder.append(text);
            }
        }

        // 마감일 (셀렉터 변경: .job_date .date → .support_detail .date)
        Element deadlineEl = item.selectFirst(".support_detail .date");
        String deadline = null;
        if (deadlineEl != null) {
            deadline = parseDeadline(deadlineEl.text().trim());
        }

        return CrawledJobData.builder()
                .companyName(companyName)
                .companyIndustry("IT")
                .companyLocation(location)
                .title(title)
                .description(title)  // 목록에서는 상세 JD 없음 → 제목으로 대체
                .location(location)
                .employmentType(employmentType)
                .experienceLevel(experienceLevel)
                .jobCategory("개발")
                .skills(skillBuilder.toString())
                .deadline(deadline)
                .sourceUrl(sourceUrl)
                .source(JobSource.SARAMIN)
                .build();
    }

    /**
     * "~04/30(수)" → "2026-04-30"
     * "상시채용" → null
     */
    private String parseDeadline(String text) {
        if (text == null || text.contains("상시") || text.contains("채용시")) return null;
        try {
            // "~MM/dd(요일)" 패턴에서 MM/dd 추출
            String cleaned = text.replaceAll("[~(].*", "").trim(); // "~04/30" → "04/30"
            cleaned = cleaned.replace("~", "").trim();
            if (cleaned.contains("/")) {
                String[] parts = cleaned.split("/");
                int month = Integer.parseInt(parts[0].trim());
                int day = Integer.parseInt(parts[1].trim().replaceAll("\\D.*", ""));
                int year = java.time.LocalDate.now().getYear();
                // 이미 지난 월이면 내년으로
                if (month < java.time.LocalDate.now().getMonthValue()) year++;
                return String.format("%d-%02d-%02d", year, month, day);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
