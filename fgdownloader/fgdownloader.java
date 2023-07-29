///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.2
//DEPS org.seleniumhq.selenium:selenium-api:4.10.0
//DEPS org.seleniumhq.selenium:selenium-chrome-driver:4.10.0
//DEPS org.seleniumhq.selenium:selenium-java:4.10.0

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.InputStream;
import java.nio.file.Files;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "fangraphsdownloader", mixinStandardHelpOptions = true, version = "fangraphsdownloader 0.1",
        description = "fangraphsdownloader made with jbang")
class fangraphsdownloader implements Callable<Integer> {

    private WebDriver webdriver;
    private Properties props;

    private Properties reports;
    private Properties projections;
    private Properties minors;

    public fangraphsdownloader() {
        super();
    }

    public void clearLeaderboards() throws IOException {
        File downloadDir = new File("data");
        if (downloadDir.exists() && (downloadDir.isDirectory())) {
            for(File file: downloadDir.listFiles()) 
                if (!file.isDirectory()) 
                    file.delete();
        } else {
            if (!downloadDir.exists()) {
                Files.createDirectories(downloadDir.toPath());
            }
        }

        if (!downloadDir.isDirectory()) {
            throw new IOException("./data must be a directory, please remove the file named data");
        }
    }

    public void readPasswordFile() throws IOException {
        try (InputStream input = new FileInputStream("authentication.properties")) {

            props = new Properties();
            // load a properties file
            props.load(input);

        } catch (IOException ex) {
            throw ex;
        }
    }

    public void readReports() throws IOException {
        try (InputStream input = new FileInputStream("reports.properties")) {

            reports = new Properties();
            // load a properties file
            reports.load(input);

        } catch (IOException ex) {
            throw ex;
        }           

        try (InputStream input = new FileInputStream("projections.properties")) {

            projections = new Properties();
            // load a properties file
            projections.load(input);

        } catch (IOException ex) {
            throw ex;
        }

        try (InputStream input = new FileInputStream("minors.properties")) {

            minors = new Properties();
            // load a properties file
            minors.load(input);

        } catch (IOException ex) {
            throw ex;
        }           
    }


    public void login() throws IOException {

        readPasswordFile();
        readReports();

        // Instruct the browser to go to the correct page
        webdriver.get("https://blogs.fangraphs.com/wp-login.php?redirect_to=https://www.fangraphs.com/");
    
        // Initialize and wait till element(link) became clickable - timeout in 10 seconds
        WebElement firstResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("user_login")));
        WebElement secondResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("user_pass")));
        WebElement thirdResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("wp-submit")));
    
        WebElement userlogin = webdriver.findElement(By.id("user_login"));
        userlogin.sendKeys((String) props.get("username"));
        WebElement password = webdriver.findElement(By.id("user_pass"));
        password.sendKeys((String) props.get("password"));
        WebElement button = webdriver.findElement(By.id("wp-submit"));
        button.submit();    
    
        WebElement finalResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("wrapper")));
    
    }

    public void downloadLeaderboards() throws IOException, InterruptedException {
        clearLeaderboards();
        ChromeOptions co = new ChromeOptions();

        // Set our download directory to "data"
        File downloadFilepath = new File("data");
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", downloadFilepath.getAbsolutePath());
        co.setExperimentalOption("prefs", chromePrefs);
        webdriver = new ChromeDriver(co);

        login();

        // Download reports ...
        for (String name : reports.stringPropertyNames()) {
            String reportUrl = (String) reports.get(name);
            System.out.println("report name : " + name + " url=" + reportUrl);

            webdriver.get(reportUrl);

            // Wait until the element to download is available and then stop loading
            // Fangraphs has ads that cause the page to appear to continue loading
            WebElement secondResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.id("footer")));
        
            // Click the Export Data button
            // Scrolling down the page helps get to the file more reliably
            JavascriptExecutor js = (JavascriptExecutor) webdriver;
            js.executeScript("window.scrollTo(0, 200)");

            WebElement exportResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.id("LeaderBoard1_cmdCSV")));

            WebElement button = webdriver.findElement(By.id("LeaderBoard1_cmdCSV"));
            button.click(); 
            
            File downloadedLeaderBoard = new File("data/FanGraphs Leaderboard.csv");
            FluentWait<WebDriver> wait = new FluentWait<WebDriver>(webdriver).withTimeout(Duration.ofSeconds(25)).pollingEvery(Duration.ofMillis(100));
            wait.until(x -> downloadedLeaderBoard.exists());
            File newReport = new File("data", name);
            downloadedLeaderBoard.renameTo(newReport);
        }

        // Download projections ...
        for (String name : projections.stringPropertyNames()) {
            String reportUrl = (String) projections.get(name);
            System.out.println("projections name : " + name + " url=" + reportUrl);

            webdriver.get(reportUrl);

            // Wait until the element to download is available and then stop loading
            // Fangraphs has ads that cause the page to appear to continue loading
            WebElement secondResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.id("footer")));
        
            // Click the Export Data button
            // Scrolling down the page helps get to the file more reliably
            JavascriptExecutor js = (JavascriptExecutor) webdriver;
            js.executeScript("window.scrollTo(0, 200)");

            WebElement exportResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.className("data-export")));

            Thread.sleep(3000);

            WebElement button = webdriver.findElement(By.className("data-export"));
            button.click(); 
            
            File downloadedLeaderBoard = new File("data/fangraphs-leaderboard-projections.csv");
            FluentWait<WebDriver> wait = new FluentWait<WebDriver>(webdriver).withTimeout(Duration.ofSeconds(25)).pollingEvery(Duration.ofMillis(100));
            wait.until(x -> downloadedLeaderBoard.exists());
            File newReport = new File("data", name);
            downloadedLeaderBoard.renameTo(newReport);
        }

        // Download minors ...
        for (String name : minors.stringPropertyNames()) {
            String reportUrl = (String) minors.get(name);
            System.out.println("minors name : " + name + " url=" + reportUrl);

            webdriver.get(reportUrl);

            // Wait until the element to download is available and then stop loading
            // Fangraphs has ads that cause the page to appear to continue loading
            WebElement secondResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.id("footer")));
        
            // Click the Export Data button
            // Scrolling down the page helps get to the file more reliably
            JavascriptExecutor js = (JavascriptExecutor) webdriver;
            js.executeScript("window.scrollTo(0, 200)");

            WebElement exportResult = new WebDriverWait(webdriver, Duration.ofSeconds(10))
              .until(ExpectedConditions.elementToBeClickable(By.className("data-export")));

            Thread.sleep(3000);

            WebElement button = webdriver.findElement(By.className("data-export"));
            button.click(); 
            
            File downloadedLeaderBoard = new File("data/fangraphs-minor-league-leaders.csv");
            FluentWait<WebDriver> wait = new FluentWait<WebDriver>(webdriver).withTimeout(Duration.ofSeconds(25)).pollingEvery(Duration.ofMillis(100));
            wait.until(x -> downloadedLeaderBoard.exists());
            File newReport = new File("data", name);
            downloadedLeaderBoard.renameTo(newReport);
        }
        webdriver.close();
    }

    public static void main(String... args) throws Exception {
        fangraphsdownloader fgcli = new fangraphsdownloader();
        fgcli.downloadLeaderboards();
        int exitCode = new CommandLine(fgcli).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        return 0;
    }
}
