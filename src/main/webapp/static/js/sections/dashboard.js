/**
 * sections/dashboard.js - 대시보드
 */
var DashboardSection = (function () {

    function load() {
        loadKpi();
        loadRecentOrders();
        loadSalesByCountry();
        loadCountrySummary();
        loadBatchSummary();
    }

    // ── KPI ──────────────────────────────────────────────────
    function loadKpi() {
        API.dashboard.getKpi().then(function (d) {
            document.getElementById('k-orders').textContent   = d.todayOrders   || 0;
            document.getElementById('k-pending').textContent  = d.pendingCount || 0;
            document.getElementById('k-shipping').textContent = d.shippingCount || 0;
            document.getElementById('k-sales').textContent    = Utils.fmt(d.monthlySalesKrw);
            document.getElementById('k-aov').textContent = Utils.fmt(d.globalAov);


            // PENDING 강조
            var pending = d.pendingCount || 0;
            var pendingEl = document.getElementById('k-pending');
            pendingEl.textContent  = pending;
            pendingEl.style.color  = pending > 0 ? '#dc2626' : '#1a202c';

            // 국가별 비중
            var total   = d.todayOrders || 1;
            var country = d.countryOrders || {};
            var breakdown = Object.keys(country).map(function (code) {
                var cnt = (country[code].todayOrders || 0);
                var pct = total > 0 ? Math.round((cnt / total) * 100) : 0;
                return '<div style="display:flex;justify-content:space-between;'
                    +   'font-size:13px;margin-bottom:4px">'
                    +   '<span>' + code + '</span>'
                    +   '<span><b>' + cnt + '</b>건 (' + pct + '%)</span>'
                    + '</div>';
            }).join('');
            document.getElementById('k-country-breakdown').innerHTML =
                breakdown || '<span style="color:#94a3b8">데이터 없음</span>';

            document.getElementById('lastUpdate').textContent = Utils.nowTime();
        }).catch(function () {});
    }


    // ── 최근 주문 ────────────────────────────────────────────
    function loadRecentOrders() {
        API.dashboard.getRecentOrders().then(function (data) {
            var el = document.getElementById('recent-orders');
            // 응답이 { KR: [...], US: [...] } 구조인 경우 처리
            var list = [];

            if (Array.isArray(data)) {
                list = data;
            } else if (data && typeof data === 'object') {
                // KR/US 합쳐서 정렬 후 중복 제거
                var seen = {};
                Object.keys(data).forEach(function (code) {
                    (data[code] || []).forEach(function (o) {
                        // order_no + country로 중복 방지
                        var key = code + '-' + o.order_no;
                        if(!seen[key]) {
                            seen[key] =true;
                            o._country = code;
                            list.push(o);
                        }
                    });
                });
                //최신순 정력
                list.sort(function (a, b) {
                    return (b.created_at || b.order_date || '')
                        .localeCompare(a.created_at || a.order_date || '');
                });
                list = list.slice(0, 5); // 최근 5건
            }

            if (!list.length) {
                el.innerHTML = '<div class="empty">데이터 없음</div>';
                return;
            }

            var rows = list.map(function (o) {
                var country = o._country
                    ? '<span style="font-size:10px;background:#f1f5f9;color:#64748b;'
                    +   'padding:1px 5px;border-radius:3px;margin-right:4px">'
                    +   o._country + '</span>'
                    : '';
                return '<tr>'
                    + '<td>' + country + o.order_no + '</td>'
                    + '<td>' + o.customer_id + '</td>'
                    + '<td>' + Utils.fmt(o.total_amount) + '</td>'
                    + '<td>' + Utils.orderBadge(o.status) + '</td>'
                    + '<td style="font-size:11px;color:#94a3b8">'
                    +   (o.order_date || o.created_at || '-').substring(0, 10)
                    + '</td>'
                    + '</tr>';
            }).join('');

            el.innerHTML =
                '<table id="orders-data">'
                + '<thead><tr>'
                +   '<th>주문번호</th><th>고객사</th>'
                +   '<th>금액</th><th>상태</th><th>주문일</th>'
                + '</tr></thead>'
                + '<tbody>' + rows + '</tbody>'
                + '</table>';

        }).catch(function () {
            Utils.renderError('recent-orders', '주문 데이터 로드 실패');
        });
    }

    // ── 국가별 매출 ──────────────────────────────────────────
    function loadSalesByCountry() {
        API.dashboard.getSalesByCountry().then(function (list) {
            var el = document.getElementById('sales-by-country');
            if (!list || !list.length) { el.innerHTML = '<div class="empty">데이터 없음</div>'; return; }

            var rows = list.map(function (r) {
                return '<tr>'
                    + '<td>' + (r.country_name || r.country_code || '-') + '</td>'
                    + '<td>' + Utils.fmt(r.total_krw) + '</td>'
                    + '<td>' + Utils.nvl(r.total_qty) + '개</td>'
                    + '</tr>';
            }).join('');

            el.innerHTML = '<table><thead><tr><th>국가</th><th>매출(KRW)</th><th>수량</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';
        }).catch(function () {
            Utils.renderError('sales-by-country', '매출 데이터 로드 실패');
        });
    }

    // ── 국가 연결 현황 (요약) ────────────────────────────────
    function loadCountrySummary() {
        API.countries.getStatus().then(function (list) {
            var html = (list || []).map(function (c) {
                var color = c.dbStatus === 'OK' ? '#16a34a' : '#dc2626';
                return '<div class="country-row">'
                    + '<div class="country-flag">' + Components.flag(c.country_code) + '</div>'
                    + '<div class="country-info">'
                    +   '<div class="country-name">' + c.country_name + '</div>'
                    +   '<div class="country-meta">오늘주문: ' + Utils.nvl(c.todayOrders) + '건</div>'
                    + '</div>'
                    + '<div style="color:' + color + '">● ' + (c.dbStatus || '-') + '</div>'
                    + '</div>';
            }).join('');
            document.getElementById('dash-countries').innerHTML = html || '<div class="empty">-</div>';
        }).catch(function () {});
    }

    // ── 배치 현황 (요약) ─────────────────────────────────────
    function loadBatchSummary() {
        API.batch.getSchedules().then(function (list) {
            var html = (list || []).map(function (b) {
                return '<div class="batch-item">'
                    + '<div style="flex:1">'
                    +   '<div style="font-size:13px;font-weight:600">' + b.desc + '</div>'
                    +   '<div style="font-size:11px;color:#94a3b8">' + b.cron + '</div>'
                    + '</div>'
                    + Utils.batchBadge(b.lastStatus || '미실행')
                    + '</div>';
            }).join('');
            document.getElementById('dash-batch').innerHTML = html || '<div class="empty">-</div>';
        }).catch(function () {});
    }

    return { load: load };

})();
