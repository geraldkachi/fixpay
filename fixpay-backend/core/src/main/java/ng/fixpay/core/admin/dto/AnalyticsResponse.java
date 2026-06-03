package ng.fixpay.core.admin.dto;

import java.util.List;

public record AnalyticsResponse(
        List<DailyVolumeDto>      dailyVolume,
        List<RevenueTrendDto>     revenueTrend,
        List<ProcessorBreakdownDto> processorBreakdown,
        KpiDto                    kpis
) {

    public record DailyVolumeDto(String date, long transactions, double volume) {}

    public record RevenueTrendDto(String month, double fixed, double percentage) {}

    public record ProcessorBreakdownDto(String name, long value, double percentage) {}

    public record KpiDto(
            long   totalTransactions,
            double totalVolume,
            double platformRevenue,
            double averageTransactionValue,
            double successRate
    ) {}
}
