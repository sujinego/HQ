/**
 * sections/exchange.js - 환율 현황
 */
var ExchangeSection = (function () {

    function load() {
        Utils.renderLoading('exchange-table');
        API.batch.getExchangeRates().then(function (list) {
            if (!list || !list.length) {
                document.getElementById('exchange-table').innerHTML =
                    '<div class="empty">오늘 환율 데이터가 없습니다. (주말/공휴일 또는 배치 미실행)</div>';
                return;
            }
            var rows = list.map(function (r) {
                return '<tr>'
                    + '<td><b>' + r.currency_code + '</b></td>'
                    + '<td>' + Number(r.rate_to_krw).toLocaleString() + '원</td>'
                    + '<td>' + (r.rate_date || '-') + '</td>'
                    + '</tr>';
            }).join('');
            document.getElementById('exchange-table').innerHTML =
                '<table id="exchange-data">'
                + '<thead><tr><th>통화</th><th>환율(KRW)</th><th>기준일</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';
        }).catch(function () {
            Utils.renderError('exchange-table', '환율 데이터 로드 실패');
        });
    }

    return { load: load };

})();
