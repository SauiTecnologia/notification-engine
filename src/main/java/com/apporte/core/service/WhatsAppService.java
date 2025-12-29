package com.apporte.core.service;

import com.apporte.api.dto.WorkflowNotificationRequest;
import com.apporte.core.dto.WhatsAppTemplateData;
import com.apporte.core.model.RecipientResolution;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@RegisterForReflection
public class WhatsAppService {
    
    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppService.class);
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Inject
    WhatsAppTemplateService templateService;
    
    @ConfigProperty(name = "whatsapp.enabled", defaultValue = "true")
    boolean enabled;
    
    @ConfigProperty(name = "whatsapp.headless", defaultValue = "false")
    boolean headless;
    
    @ConfigProperty(name = "whatsapp.timeout.seconds", defaultValue = "30")
    int timeoutSeconds;
    
    @ConfigProperty(name = "whatsapp.qr.timeout.seconds", defaultValue = "120")
    int qrTimeoutSeconds;
    
    @ConfigProperty(name = "whatsapp.max.retries", defaultValue = "3")
    int maxRetries;
    
    @ConfigProperty(name = "whatsapp.retry.delay.ms", defaultValue = "2000")
    long retryDelayMs;
    
    @ConfigProperty(name = "whatsapp.session.save", defaultValue = "true")
    boolean saveSession;
    
    @ConfigProperty(name = "whatsapp.session.path", defaultValue = "./whatsapp-session")
    String sessionPath;
    
    @ConfigProperty(name = "whatsapp.driver.path")
    Optional<String> driverPath;
    
    @ConfigProperty(name = "app.system.url", defaultValue = "https://app.apporte.com")
    String systemUrl;
    
    @ConfigProperty(name = "app.name", defaultValue = "Apporte")
    String appName;
    
    private WebDriver driver;
    private WebDriverWait wait;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isLoggedIn = new AtomicBoolean(false);
    private final Map<String, LocalDateTime> sentMessages = new ConcurrentHashMap<>();
    private final Map<String, Integer> retryCounts = new ConcurrentHashMap<>();
    private final Object driverLock = new Object();
    
    @PostConstruct
    void initialize() {
        if (!enabled) {
            LOG.warn("WhatsAppService is disabled in configuration");
            return;
        }
        
        LOG.info("Initializing WhatsAppService for application: {}", appName);
        LOG.info("Initialization started at: {}", LocalDateTime.now().format(LOG_FORMATTER));
        createSessionDirectory();
        
        // Inicialização assíncrona do driver
        new Thread(() -> {
            try {
                LOG.info("Starting asynchronous initialization of WhatsApp WebDriver at: {}", 
                         LocalDateTime.now().format(LOG_FORMATTER));
                initializeDriver();
            } catch (Exception e) {
                LOG.error("Failed to initialize WhatsApp WebDriver asynchronously at: {}", 
                          LocalDateTime.now().format(LOG_FORMATTER), e);
            }
        }, "whatsapp-driver-init").start();
    }
    
    @PreDestroy
    void shutdown() {
        LOG.info("Shutting down WhatsAppService at: {}", LocalDateTime.now().format(LOG_FORMATTER));
        closeDriver();
        LOG.info("WhatsAppService shutdown completed at: {}", LocalDateTime.now().format(LOG_FORMATTER));
    }
    
    public void sendMessage(RecipientResolution recipient, WorkflowNotificationRequest request) {
        if (!enabled) {
            LOG.warn("WhatsAppService is disabled. Message not sent to phone: {}", 
                    recipient.getPhone() != null ? maskPhone(recipient.getPhone()) : "null");
            return;
        }
        
        String phoneNumber = validateAndFormatPhone(recipient.getPhone());
        if (phoneNumber == null) {
            LOG.error("Invalid phone number for recipient email: {}", recipient.getEmail());
            throw new IllegalArgumentException("Invalid or missing phone number");
        }
        
        // Verificar se já enviou mensagem recentemente para este número
        if (hasSentRecently(phoneNumber)) {
            LOG.warn("Message already sent recently to {}. Skipping.", maskPhone(phoneNumber));
            return;
        }
        
        String message = buildMessageWithTemplate(recipient, request);
        String messageKey = phoneNumber + ":" + request.getEventType() + ":" + request.getEntityId();
        
        LOG.info("Sending WhatsApp to {} for event: {} at: {}", 
                 maskPhone(phoneNumber), request.getEventType(), LocalDateTime.now().format(LOG_FORMATTER));
        
        boolean sent = false;
        Exception lastError = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LOG.debug("Attempt {} of {} to send WhatsApp message started at: {}", 
                         attempt, maxRetries, LocalDateTime.now().format(LOG_FORMATTER));
                sendMessageInternal(phoneNumber, message, request.getEventType());
                
                sent = true;
                recordSentMessage(phoneNumber);
                LOG.info("WhatsApp message sent successfully to {} at: {}", 
                         maskPhone(phoneNumber), LocalDateTime.now().format(LOG_FORMATTER));
                break;
                
            } catch (Exception e) {
                lastError = e;
                LOG.warn("Attempt {} failed for {} at {}: {}", 
                         attempt, maskPhone(phoneNumber), LocalDateTime.now().format(LOG_FORMATTER), e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        long delay = retryDelayMs * attempt; // Backoff exponencial
                        LOG.debug("Waiting {}ms before next attempt at: {}", 
                                 delay, LocalDateTime.now().format(LOG_FORMATTER));
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        LOG.warn("Thread interrupted during retry delay at: {}", 
                                 LocalDateTime.now().format(LOG_FORMATTER));
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    // Reiniciar driver em caso de falha
                    resetDriver();
                }
            }
        }
        
        if (!sent && lastError != null) {
            LOG.error("Failed to send WhatsApp to {} after {} attempts at: {}", 
                     maskPhone(phoneNumber), maxRetries, LocalDateTime.now().format(LOG_FORMATTER));
            retryCounts.put(messageKey, retryCounts.getOrDefault(messageKey, 0) + 1);
            throw new RuntimeException("Failed to send WhatsApp message: " + lastError.getMessage(), lastError);
        }
    }
    
    private void sendMessageInternal(String phoneNumber, String message, String eventType) {
        ensureDriverInitialized();
        ensureLoggedIn();
        
        try {
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            String chatUrl = String.format("https://web.whatsapp.com/send?phone=%s&text=%s", phoneNumber, encodedMessage);
            
            LOG.debug("Accessing WhatsApp URL for phone: {} at: {}", 
                     maskPhone(phoneNumber), LocalDateTime.now().format(LOG_FORMATTER));
            driver.get(chatUrl);
            
            // Aguardar carregamento da página
            Thread.sleep(3000);
            
            // Verificar se número é inválido
            checkInvalidNumber();
            
            // Verificar se usuário está bloqueado
            checkBlockedUser();
            
            // Aguardar botão de enviar ficar disponível
            By sendButtonLocator = By.xpath("//button[@data-testid='compose-btn-send' or @aria-label='Send']");
            wait.until(ExpectedConditions.elementToBeClickable(sendButtonLocator));
            
            // Clicar no botão de enviar
            WebElement sendButton = driver.findElement(sendButtonLocator);
            sendButton.click();
            
            LOG.debug("Message sent, waiting for confirmation at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            
            // Aguardar confirmação de envio (checkmark)
            try {
                By sentIndicator = By.xpath("//span[@data-testid='msg-check' or @data-icon='msg-check']");
                wait.until(ExpectedConditions.presenceOfElementLocated(sentIndicator));
                LOG.debug("Send confirmation received at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            } catch (TimeoutException e) {
                LOG.warn("Send confirmation not detected at: {}, but message was sent", 
                         LocalDateTime.now().format(LOG_FORMATTER));
            }
            
            // Aguardar um momento antes de próxima ação
            Thread.sleep(1000);
            
        } catch (Exception e) {
            LOG.error("Error in WhatsApp internal send process at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER), e);
            throw new RuntimeException("Error sending WhatsApp message: " + e.getMessage(), e);
        }
    }
    
    private String buildMessageWithTemplate(RecipientResolution recipient, WorkflowNotificationRequest request) {
        try {
            WhatsAppTemplateData templateData = new WhatsAppTemplateData(
                recipient.getName() != null ? recipient.getName() : "Colaborador",
                request.getEventType(),
                request.getEntityType(),
                request.getEntityId(),
                request.getContext() != null ? request.getContext() : new HashMap<>()
            );
            
            String message = templateService.renderTemplate(templateData);
            
            // Adicionar cabeçalho personalizado
            String header = String.format("*%s NOTIFICACAO*\n\n", appName.toUpperCase());
            String timestamp = "Data: " + LocalDateTime.now().format(LOG_FORMATTER) + "\n\n";
            
            return header + timestamp + message;
            
        } catch (Exception e) {
            LOG.error("Error building message with template at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER), e);
            return buildFallbackMessage(recipient, request);
        }
    }
    
    private String buildFallbackMessage(RecipientResolution recipient, WorkflowNotificationRequest request) {
        return String.format(
            "*NOTIFICACAO DO %s*\n\n" +
            "Ola %s,\n\n" +
            "Voce recebeu uma notificacao:\n" +
            "* Evento: %s\n" +
            "* Tipo: %s\n" +
            "* ID: %s\n" +
            "* Data: %s\n\n" +
            "Acesse o sistema para detalhes:\n" +
            "%s\n\n" +
            "---\n" +
            "Esta e uma mensagem automatica.\n" +
            "Por favor, nao responda este WhatsApp.",
            appName.toUpperCase(),
            recipient.getName() != null ? recipient.getName() : "Colaborador",
            request.getEventType(),
            request.getEntityType(),
            request.getEntityId(),
            LocalDateTime.now().format(LOG_FORMATTER),
            systemUrl
        );
    }
    
    private void ensureDriverInitialized() {
        if (!isInitialized.get()) {
            synchronized (driverLock) {
                if (!isInitialized.get()) {
                    initializeDriver();
                    isInitialized.set(true);
                }
            }
        }
    }
    
    private void initializeDriver() {
        if (driver != null) {
            return;
        }
        
        LOG.info("Initializing WhatsApp WebDriver at: {}", LocalDateTime.now().format(LOG_FORMATTER));
        
        try {
            // Configurar ChromeDriver
            System.setProperty("webdriver.chrome.silentOutput", "true");
            
            ChromeOptions options = new ChromeOptions();
            
            // Argumentos para evitar detecção como bot
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-gpu");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-notifications");
            options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
            options.setExperimentalOption("useAutomationExtension", false);
            
            if (headless) {
                options.addArguments("--headless=new");
                LOG.info("Running Chrome in headless mode at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            }
            
            // Configurar diretório de sessão
            if (saveSession) {
                Path sessionDir = Paths.get(sessionPath);
                if (!Files.exists(sessionDir)) {
                    Files.createDirectories(sessionDir);
                }
                options.addArguments("--user-data-dir=" + sessionDir.toAbsolutePath());
                LOG.info("WhatsApp session will be saved to: {} at: {}", 
                         sessionDir.toAbsolutePath(), LocalDateTime.now().format(LOG_FORMATTER));
            }
            
            // Configurar caminho do driver se especificado
            if (driverPath.isPresent() && !driverPath.get().isEmpty()) {
                System.setProperty("webdriver.chrome.driver", driverPath.get());
                LOG.debug("Using custom ChromeDriver path: {} at: {}", 
                         driverPath.get(), LocalDateTime.now().format(LOG_FORMATTER));
            }
            
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            
            LOG.info("WhatsApp WebDriver initialized successfully at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER));
            
        } catch (Exception e) {
            LOG.error("Failed to initialize WhatsApp WebDriver at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER), e);
            throw new RuntimeException("Failed to initialize WhatsApp WebDriver", e);
        }
    }
    
    private void ensureLoggedIn() {
        if (isLoggedIn.get()) {
            return;
        }
        
        LOG.info("Checking WhatsApp login status at: {}", LocalDateTime.now().format(LOG_FORMATTER));
        driver.get("https://web.whatsapp.com");
        
        try {
            WebDriverWait qrWait = new WebDriverWait(driver, Duration.ofSeconds(qrTimeoutSeconds));
            By searchBoxSelector = By.xpath("//div[@contenteditable='true'][@data-tab='3']");
            
            // Verificar se já está logado
            try {
                qrWait.until(ExpectedConditions.presenceOfElementLocated(searchBoxSelector));
                LOG.info("Already logged in to WhatsApp Web at: {}", LocalDateTime.now().format(LOG_FORMATTER));
                isLoggedIn.set(true);
                return;
                
            } catch (TimeoutException e) {
                // Não encontrou a barra de pesquisa, mostrar QR Code
                LOG.info("Please scan the QR Code to login to WhatsApp Web at: {}", 
                         LocalDateTime.now().format(LOG_FORMATTER));
                
                // Aguardar QR Code aparecer
                By qrCodeSelector = By.xpath("//canvas[@aria-label='Scan me!']");
                qrWait.until(ExpectedConditions.presenceOfElementLocated(qrCodeSelector));
                
                // Aguardar até que o QR Code desapareça (login realizado)
                LOG.info("Waiting for QR Code scan at: {}", LocalDateTime.now().format(LOG_FORMATTER));
                qrWait.until(ExpectedConditions.invisibilityOfElementLocated(qrCodeSelector));
                
                // Aguardar barra de pesquisa após login
                wait.until(ExpectedConditions.presenceOfElementLocated(searchBoxSelector));
                
                isLoggedIn.set(true);
                LOG.info("Successfully logged in to WhatsApp Web at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            }
            
        } catch (Exception e) {
            LOG.error("Error during WhatsApp login process at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER), e);
            throw new RuntimeException("Failed to login to WhatsApp Web", e);
        }
    }
    
    private void checkInvalidNumber() {
        try {
            By invalidMsg = By.xpath("//div[contains(text(), 'Phone number shared via url is invalid')]");
            if (!driver.findElements(invalidMsg).isEmpty()) {
                throw new IllegalArgumentException("Invalid WhatsApp number format");
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            // Número válido - usando fully qualified name
            LOG.debug("No invalid number message found - number is valid at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER));
        }
    }
    
    private void checkBlockedUser() {
        try {
            By blockedMsg = By.xpath("//div[contains(text(), 'blocked') or contains(text(), 'bloqueado')]");
            if (!driver.findElements(blockedMsg).isEmpty()) {
                throw new IllegalStateException("User has blocked WhatsApp messages");
            }
        } catch (org.openqa.selenium.NoSuchElementException e) {
            // Usuário não bloqueado - usando fully qualified name
            LOG.debug("No blocked user message found - user is not blocked at: {}", 
                     LocalDateTime.now().format(LOG_FORMATTER));
        }
    }
    
    private String validateAndFormatPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            LOG.error("Phone number is null or empty at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            return null;
        }
        
        // Remover caracteres não numéricos
        String cleaned = phone.replaceAll("[^0-9]", "");
        
        if (cleaned.length() < 10 || cleaned.length() > 15) {
            LOG.error("Invalid phone number length: {} digits at: {}", 
                     cleaned.length(), LocalDateTime.now().format(LOG_FORMATTER));
            return null;
        }
        
        // Adicionar código do país se necessário
        if (!cleaned.startsWith("55") && cleaned.length() <= 11) {
            cleaned = "55" + cleaned;
        }
        
        LOG.debug("Phone number formatted: {} -> {} at: {}", 
                 phone, maskPhone(cleaned), LocalDateTime.now().format(LOG_FORMATTER));
        return cleaned;
    }
    
    private boolean hasSentRecently(String phoneNumber) {
        LocalDateTime lastSent = sentMessages.get(phoneNumber);
        if (lastSent == null) {
            return false;
        }
        
        // Verificar se passou mais de 1 hora desde o último envio
        boolean recent = lastSent.plusHours(1).isAfter(LocalDateTime.now());
        if (recent) {
            LOG.debug("Message sent recently to {} at {} at: {}", 
                     maskPhone(phoneNumber), lastSent.format(LOG_FORMATTER), LocalDateTime.now().format(LOG_FORMATTER));
        }
        
        return recent;
    }
    
    private void recordSentMessage(String phoneNumber) {
        sentMessages.put(phoneNumber, LocalDateTime.now());
        LOG.debug("Recorded message sent to {} at: {}", 
                 maskPhone(phoneNumber), LocalDateTime.now().format(LOG_FORMATTER));
    }
    
    private void resetDriver() {
        synchronized (driverLock) {
            closeDriver();
            isInitialized.set(false);
            isLoggedIn.set(false);
            LOG.info("WhatsApp WebDriver reset at: {}", LocalDateTime.now().format(LOG_FORMATTER));
        }
    }
    
    private void closeDriver() {
        if (driver != null) {
            try {
                LOG.info("Closing WhatsApp WebDriver at: {}", LocalDateTime.now().format(LOG_FORMATTER));
                driver.quit();
                driver = null;
                wait = null;
                LOG.info("WhatsApp WebDriver closed successfully at: {}", LocalDateTime.now().format(LOG_FORMATTER));
            } catch (Exception e) {
                LOG.error("Error closing WhatsApp WebDriver at: {}", 
                         LocalDateTime.now().format(LOG_FORMATTER), e);
            }
        }
    }
    
    private void createSessionDirectory() {
        if (saveSession) {
            try {
                Path dir = Paths.get(sessionPath);
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir);
                    LOG.info("Created WhatsApp session directory: {} at: {}", 
                             dir.toAbsolutePath(), LocalDateTime.now().format(LOG_FORMATTER));
                }
            } catch (IOException e) {
                LOG.error("Failed to create WhatsApp session directory at: {}", 
                         LocalDateTime.now().format(LOG_FORMATTER), e);
            }
        }
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
    
    // Método para verificação de saúde do serviço
    public HealthStatus getHealthStatus() {
        return new HealthStatus(
            enabled,
            driver != null,
            isLoggedIn.get(),
            sentMessages.size(),
            LocalDateTime.now()
        );
    }
    
    // Classe interna para status de saúde
    public static class HealthStatus {
        public final boolean enabled;
        public final boolean driverInitialized;
        public final boolean loggedIn;
        public final int messagesSentToday;
        public final LocalDateTime timestamp;
        
        public HealthStatus(boolean enabled, boolean driverInitialized, boolean loggedIn, 
                           int messagesSentToday, LocalDateTime timestamp) {
            this.enabled = enabled;
            this.driverInitialized = driverInitialized;
            this.loggedIn = loggedIn;
            this.messagesSentToday = messagesSentToday;
            this.timestamp = timestamp;
        }
        
        // Método para formatar o timestamp
        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}