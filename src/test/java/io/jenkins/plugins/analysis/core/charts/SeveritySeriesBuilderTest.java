package io.jenkins.plugins.analysis.core.charts;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import io.jenkins.plugins.analysis.core.charts.ChartModelConfiguration.AxisType;
import io.jenkins.plugins.analysis.core.util.AnalysisBuildResult;

import static edu.hm.hafner.analysis.Severity.*;
import static io.jenkins.plugins.analysis.core.charts.BuildResultStubs.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link SeveritySeriesBuilder}.
 *
 * @author Ullrich Hafner
 */
class SeveritySeriesBuilderTest {
    /** Verifies that an empty list of builds produces no data. */
    @Test
    void shouldHaveEmptyDataSetForEmptyIterator() {
        SeveritySeriesBuilder builder = new SeveritySeriesBuilder();

        LinesDataSet model = builder.createDataSet(createConfiguration(), Lists.newArrayList());

        assertThat(model.getXAxisSize()).isEqualTo(0);
        assertThat(model.getDataSetIds()).isEmpty();
    }

    private ChartModelConfiguration createConfiguration() {
        ChartModelConfiguration configuration = mock(ChartModelConfiguration.class);
        when(configuration.getAxisType()).thenReturn(AxisType.BUILD);
        return configuration;
    }

    /**
     * Verifies that a list with one build result produces one column with rows containing the correct number of issues
     * per priority.
     */
    @Test
    void shouldHaveThreeValuesForSingleBuild() {
        SeveritySeriesBuilder builder = new SeveritySeriesBuilder();

        AnalysisBuildResult singleResult = createResult(1, 0, 1, 2, 3);

        LinesDataSet dataSet = builder.createDataSet(createConfiguration(), Lists.newArrayList(singleResult));

        assertThat(dataSet.getXAxisSize()).isEqualTo(1);
        assertThat(dataSet.getXAxisLabels()).containsExactly("#1");

        assertThat(dataSet.getDataSetIds()).containsExactlyInAnyOrder(
                ERROR.getName(), WARNING_HIGH.getName(), WARNING_NORMAL.getName(), WARNING_LOW.getName());

        assertThat(dataSet.getSeries(WARNING_HIGH.getName())).containsExactly(1);
        assertThat(dataSet.getSeries(WARNING_NORMAL.getName())).containsExactly(2);
        assertThat(dataSet.getSeries(WARNING_LOW.getName())).containsExactly(3);
        assertThat(dataSet.getSeries(ERROR.getName())).containsExactly(0);
    }

    /**
     * Verifies that the number of builds in the chart is limited by the {@link ChartModelConfiguration} settings.
     */
    @Test
    void shouldHaveNotMoreValuesThatAllowed() {
        SeveritySeriesBuilder builder = new SeveritySeriesBuilder();

        ChartModelConfiguration configuration = createConfiguration();
        when(configuration.getBuildCount()).thenReturn(3);
        when(configuration.isBuildCountDefined()).thenReturn(true);

        LinesDataSet dataSet = builder.createDataSet(configuration, Lists.newArrayList(
                createResult(4, 4000, 400, 40, 4),
                createResult(3, 3000, 300, 30, 3),
                createResult(2, 2000, 200, 20, 2),
                createResult(1, 1000, 100, 10, 1)
        ));

        assertThat(dataSet.getXAxisSize()).isEqualTo(3);
        assertThat(dataSet.getXAxisLabels()).containsExactly("#2", "#3", "#4");

        assertThat(dataSet.getDataSetIds()).containsExactlyInAnyOrder(
                ERROR.getName(), WARNING_HIGH.getName(), WARNING_NORMAL.getName(), WARNING_LOW.getName());

        assertThat(dataSet.getSeries(ERROR.getName())).containsExactly(2000, 3000, 4000);
        assertThat(dataSet.getSeries(WARNING_HIGH.getName())).containsExactly(200, 300, 400);
        assertThat(dataSet.getSeries(WARNING_NORMAL.getName())).containsExactly(20, 30, 40);
        assertThat(dataSet.getSeries(WARNING_LOW.getName())).containsExactly(2, 3, 4);
    }
}
