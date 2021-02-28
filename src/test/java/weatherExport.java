import com.google.common.io.Files;
import io.restassured.response.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.restassured.*;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class weatherExport {
    private WebDriver driver;
    WebDriverWait wait;
    static File jsonfile;
    HashMap<String, String> APIData = new HashMap<String, String>();
    HashMap<String, String> UIData = new HashMap<String, String>();
    ArrayList<String> Cities = new ArrayList<>();
    long variance = -1;

    @Before
    public void setUp() throws Exception {
        String projectPath = System.getProperty("user.dir");

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(projectPath+"\\Cities.json"));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray cityList = (JSONArray) jsonObject.get("City");
            variance = (Long) jsonObject.get("Variance");
            Iterator<JSONObject> iterator = cityList.iterator();
            while (iterator.hasNext()) {
                Cities.add(String.valueOf(iterator.next()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.setProperty("webdriver.chrome.driver", "C:\\Users\\Kamal Jeet\\IdeaProjects\\WeatherAssignment\\Drivers\\chromedriver.exe");
        driver=new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        wait = new WebDriverWait(driver,30);
        driver.manage().window().maximize();
        String baseUrl = "https://weather.com";
        driver.get(baseUrl + "/");
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

    public void comparison(double UI, double api) throws Exception{
        System.out.println(UI +"  ******  "+ api);
        double diff = (UI * (double) variance) / 100;
        try{
            if((UI + diff) >= api & (UI - diff) <= api){

            } else {
                throw new Exception("Matcher Exception");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void weatherExtraction() throws Exception {
        String projectPath = System.getProperty("user.dir");
        JSONParser parser = new JSONParser();

        for(int i = 0; i<Cities.size(); i++) {
            String currCity = Cities.get(i);
            try {
                WebElement searchField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("LocationSearch_input")));
                searchField.click();
                Thread.sleep(2000);
                searchField.clear();
                searchField.sendKeys(currCity);
                Thread.sleep(2000);
                searchField.sendKeys(Keys.ENTER);
            } catch (org.openqa.selenium.StaleElementReferenceException ex) {
                WebElement searchField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("LocationSearch_input")));
                searchField.click();
                searchField.clear();
                searchField.sendKeys(currCity);
                Thread.sleep(2000);
                searchField.sendKeys(Keys.ENTER);
            }
            String UItxtTemp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='todayDetails']/section/div[1]/div[1]/span[1]"))).getText();
            String UItxtHumidity = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='todayDetails']/section/div[2]/div[3]/div[2]/span"))).getText();
            String UItxtPressure = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='todayDetails']/section/div[2]/div[5]/div[2]/span"))).getText();
            String UItxtVisibility = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='todayDetails']/section/div[2]/div[7]/div[2]/span"))).getText();

            Double UITemp = Double.valueOf(UItxtTemp.replaceAll("°", ""));
            Double UIPressure = Double.valueOf(UItxtPressure.replaceAll(" mb",""));
            Double UIHumidity = Double.valueOf(UItxtHumidity.replaceAll("%",""));

            String api = "https://api.openweathermap.org/data/2.5/weather?q="+currCity+"&appid=950b8978d121678244eaa7f8b2639711";
            Response resp = RestAssured.get(api);
            String responseAsString = resp.asString();
            byte[] responseAsStringByte = responseAsString.getBytes();
            File targetFileForString = new File(projectPath + "\\testResult.json");
            Files.write(responseAsStringByte, targetFileForString);

            Object obj = parser.parse(new FileReader(projectPath + "\\testResult.json"));
            JSONObject jsonObject = (JSONObject) obj;
            JSONObject mainObj = (JSONObject) jsonObject.get("main");

            Double apiTemp = (Double) mainObj.get("temp");
            Long apiPressure = (Long) mainObj.get("pressure");
            Long apiHumidity = (Long) mainObj.get("humidity");

            comparison(UITemp + 273.15, apiTemp);
            comparison(UIPressure, (double) apiPressure);
            comparison(UIHumidity, (double) apiHumidity);

            Thread.sleep(2000);

        }
    }
}