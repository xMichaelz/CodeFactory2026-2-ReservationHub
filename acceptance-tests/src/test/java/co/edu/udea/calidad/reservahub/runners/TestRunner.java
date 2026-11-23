package co.edu.udea.calidad.reservahub.runners;

import io.cucumber.junit.CucumberOptions;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "co.edu.udea.calidad.reservahub.stepdefinitions",
        snippets = CucumberOptions.SnippetType.CAMELCASE
)
public class TestRunner {}
