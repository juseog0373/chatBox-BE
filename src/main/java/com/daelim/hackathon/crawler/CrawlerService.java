package com.daelim.hackathon.crawler;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CrawlerService {

    private WebDriver driver;
    @Value("${crawler.url}")
    private String url;

    @PostConstruct
    public void setupDriver() throws IOException {
        // resources/static 폴더에 있는 ChromeDriver 경로 설정
        File chromeDriver = new ClassPathResource("static/chromedriver.exe").getFile();
        System.setProperty("webdriver.chrome.driver", chromeDriver.getAbsolutePath());

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 브라우저 창을 띄우지 않음
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
    }

    public Map<Integer, String> getLunchMenu() {
        try {
            driver.get(url);

            // 테이블 요소 찾기
            WebElement tableElement = driver.findElement(By.cssSelector(".lineTop_tbArea table"));

            List<WebElement> rows = tableElement.findElements(By.tagName("tr"));
            Map<Integer, String> menuByDay = new LinkedHashMap<>();

            for (WebElement row : rows) {
                List<WebElement> headers = row.findElements(By.tagName("th"));
                List<WebElement> cells = row.findElements(By.tagName("td"));

                if (!headers.isEmpty() && headers.get(0).getText().contains("중식")) {
                    for (int j = 1; j <= cells.size(); j++) {
                        if (j - 1 < cells.size()) {
                            String menu = cells.get(j - 1).getText();
                            menuByDay.put(j - 1, menu);
                        }
                    }
                }
            }

            // 공백 제거 및 데이터 정리
            for (int i = 0; i < 5; i++) {
                menuByDay.put(i, menuByDay.getOrDefault(i, "").trim());
            }

            return menuByDay;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public LunchMenuResponse getTodayLunchMenu() {
        Map<Integer, String> menuByDay = getLunchMenu();
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        int dayIndex = dayOfWeek.getValue() - 1; // 월요일이 0번 인덱스가 되도록 조정
        String dayName = dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.KOREAN);

        return new LunchMenuResponse(dayName, menuByDay.getOrDefault(dayIndex, "오늘은 중식 정보가 없습니다."));
    }
}
