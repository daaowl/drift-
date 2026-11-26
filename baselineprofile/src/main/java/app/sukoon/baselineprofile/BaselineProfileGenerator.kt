package app.sukoon.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = "app.sukoon", includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.res("onboarding_skip")), 3_000)
        device.findObject(By.res("onboarding_skip"))?.click()

        device.wait(Until.hasObject(By.res("home_sections")), 5_000)
        val sections = device.findObject(By.res("home_sections"))
        sections?.fling(androidx.test.uiautomator.Direction.DOWN)
        sections?.fling(androidx.test.uiautomator.Direction.UP)

        device.findObject(By.res("section_card"))?.click()
        device.wait(Until.hasObject(By.res("section_duas")), 3_000)
        device.findObject(By.res("dua_row"))?.click()
        device.wait(Until.hasObject(By.res("reader_pager")), 3_000)
        device.findObject(By.res("reader_pager"))?.swipe(androidx.test.uiautomator.Direction.LEFT, 0.8f)
        device.pressBack()
        device.pressBack()
    }
}
