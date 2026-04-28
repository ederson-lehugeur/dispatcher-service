package com.invest.application;

import com.invest.domain.entities.ComparisonOperator;
import com.invest.domain.entities.RuleField;
import com.invest.domain.events.AlertCondition;
import com.invest.domain.events.AlertTriggeredEvent;
import com.invest.domain.events.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailContentBuilderHtmlTest {

    private EmailContentBuilder builder;
    private String individualHtml;
    private String groupHtml;

    @BeforeEach
    void setUp() {
        builder = new EmailContentBuilder();
        individualHtml = builder.buildHtmlBody(buildIndividualEvent());
        groupHtml = builder.buildHtmlBody(buildGroupEvent());
    }

    @Test
    void shouldContainViewportMetaTag() {
        assertThat(individualHtml).contains("<meta name=\"viewport\"");
        assertThat(groupHtml).contains("<meta name=\"viewport\"");
    }

    @Test
    void shouldContainOuterTableMaxWidth600px() {
        assertThat(individualHtml).contains("max-width:600px");
        assertThat(groupHtml).contains("max-width:600px");
    }

    @Test
    void shouldContainInnerCardTableWithFullWidth() {
        // The inner card table must have width="100%"
        assertThat(individualHtml).contains("<table width=\"100%\"");
        assertThat(groupHtml).contains("<table width=\"100%\"");
    }

    @Test
    void shouldNotContainFixedPixelWidthOnIndicatorGridCells() {
        // Cells must NOT use fixed pixel widths like width="600"
        assertThat(individualHtml).doesNotContain("width=\"600\"");
        assertThat(groupHtml).doesNotContain("width=\"600\"");
    }

    @Test
    void shouldContainPercentageWidthOnIndicatorGridCells() {
        // Each indicator cell must use width="33%"
        assertThat(individualHtml).contains("width=\"33%\"");
        assertThat(groupHtml).contains("width=\"33%\"");
    }

    @Test
    void shouldContainConditionBoxLightGreenBackground() {
        assertThat(individualHtml).contains("#f0fdf4");
        assertThat(groupHtml).contains("#f0fdf4");
    }

    @Test
    void shouldContainConditionBoxGreenBorderColor() {
        assertThat(individualHtml).contains("#16a34a");
        assertThat(groupHtml).contains("#16a34a");
    }

    @Test
    void shouldContainCtaButtonHrefHash() {
        assertThat(individualHtml).contains("href=\"#\"");
        assertThat(groupHtml).contains("href=\"#\"");
    }

    @Test
    void shouldContainCtaButtonBlueBackground() {
        assertThat(individualHtml).contains("#2563eb");
        assertThat(groupHtml).contains("#2563eb");
    }

    @Test
    void shouldContainAssetNameWithBoldFontWeight() {
        assertThat(individualHtml).contains("font-weight:bold");
        assertThat(groupHtml).contains("font-weight:bold");
    }

    @Test
    void shouldContainTickerMutedColor() {
        assertThat(individualHtml).contains("#6b7280");
        assertThat(groupHtml).contains("#6b7280");
    }

    private AlertTriggeredEvent buildIndividualEvent() {
        AlertCondition condition = new AlertCondition(
                RuleField.DIVIDEND_YIELD,
                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                new BigDecimal("10.00")
        );

        AlertTriggeredEvent.Data data = new AlertTriggeredEvent.Data(
                42L, 1L, "user@email.com",
                "FII CSHG Logistica", "HGLG11",
                new BigDecimal("162.50"), new BigDecimal("10.25"), new BigDecimal("0.98"),
                null, List.of(condition), "2026-04-25T11:59:30"
        );

        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED", "550e8400-e29b-41d4-a716-446655440000",
                "2026-04-25T12:00:00Z", NotificationChannel.EMAIL, data
        );
    }

    private AlertTriggeredEvent buildGroupEvent() {
        List<AlertCondition> conditions = List.of(
                new AlertCondition(RuleField.DIVIDEND_YIELD, ComparisonOperator.GREATER_THAN_OR_EQUAL, new BigDecimal("10.00")),
                new AlertCondition(RuleField.P_VP, ComparisonOperator.LESS_THAN_OR_EQUAL, new BigDecimal("1.00"))
        );

        AlertTriggeredEvent.Data data = new AlertTriggeredEvent.Data(
                43L, 1L, "user@email.com",
                "FII CSHG Logistica", "HGLG11",
                new BigDecimal("162.50"), new BigDecimal("10.25"), new BigDecimal("0.98"),
                "Grupo FIIs Baratos", conditions, "2026-04-25T11:59:30"
        );

        return new AlertTriggeredEvent(
                "ALERT_TRIGGERED", "660e8400-e29b-41d4-a716-446655440001",
                "2026-04-25T12:00:00Z", NotificationChannel.EMAIL, data
        );
    }
}
