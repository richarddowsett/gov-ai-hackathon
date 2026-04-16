import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}

object Main extends App {
  WebDriverManager.chromedriver().setup()

  val options = new ChromeOptions()
  // options.addArguments("--headless") // uncomment to run without a browser window

  val driver = new ChromeDriver(options)

  try {
    driver.get("https://www.gov.uk")
    println(s"Title: ${driver.getTitle}")
    println(s"URL:   ${driver.getCurrentUrl}")
  } finally {
    driver.quit()
  }
}
