/**
 * sections/chart.js - 매출 차트 (Chart.js)
 */
var ChartSection = (function () {

    var salesChart = null;

    function load() {
        API.dashboard.getSalesByCountry().then(function (list) {
            renderSalesChart(list || []);
        }).catch(function () {
            Utils.renderError('chart-container', '차트 데이터 로드 실패');
        });
    }

    function renderSalesChart(list) {
        var ctx = document.getElementById('salesChart');
        if (!ctx) return;

        if (salesChart) { salesChart.destroy(); }

        var labels = list.map(function (r) { return r.country_name || r.country_code; });
        var values = list.map(function (r) { return r.total_krw || 0; });

        salesChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: '매출액 (KRW)',
                    data: values,
                    backgroundColor: ['rgba(165,0,52,0.7)', 'rgba(59,130,246,0.7)',
                        'rgba(16,185,129,0.7)', 'rgba(245,158,11,0.7)'],
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return Utils.fmt(ctx.parsed.y);
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function (v) { return Utils.fmt(v); }
                        }
                    }
                }
            }
        });
    }

    return { load: load };

})();
