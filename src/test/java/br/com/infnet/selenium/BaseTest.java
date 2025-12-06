// ==================== BaseTest.java ====================
package br.com.infnet.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Classe base para testes Selenium com melhorias de performance e estabilidade.
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected final String baseUrl = "http://localhost:8080";

    @BeforeAll
    public static void setupClass() {
        // Setup do WebDriver uma única vez para toda a classe
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");

        // --- BLOCO CORRIGIDO PARA O GITHUB ACTIONS ---
        options.addArguments("--headless");              // Roda sem tela (obrigatório no GitHub)
        options.addArguments("--no-sandbox");            // Evita erro de permissão no Linux
        options.addArguments("--disable-dev-shm-usage"); // Evita estouro de memória
        options.addArguments("--remote-allow-origins=*"); // [IMPORTANTE] Evita erro 403 de conexão WebSocket
        // ---------------------------------------------

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
