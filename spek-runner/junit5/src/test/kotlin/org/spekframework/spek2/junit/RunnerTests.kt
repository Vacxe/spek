package org.spekframework.spek2.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.EngineFilter
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

class RunnerTests {
    private lateinit var launcher: Launcher

    @BeforeEach
    fun setup() {
        launcher = LauncherFactory.create()
    }

    @Test
    fun testSelectPackage() {
        val listener = SummaryGeneratingListener()
        launcher.execute(
            LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines("spek2"))
                .selectors(DiscoverySelectors.selectPackage("testData.package1"))
                .build(),
            listener
        )

        val summary = listener.summary

        assertThat(summary.testsSucceededCount, equalTo(3L))
    }

    @Test
    fun testSelectClass() {
        val listener = SummaryGeneratingListener()
        launcher.execute(
            LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines("spek2"))
                .selectors(DiscoverySelectors.selectClass("testData.package1.SpekTest1"))
                .build(),
            listener
        )

        val summary = listener.summary

        assertThat(summary.testsSucceededCount, equalTo(1L))
    }
}