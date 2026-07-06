/**
 * sections/monitor.js - 시스템 모니터
 */
var MonitorSection = (function () {

    function load() {
        loadHealth();
        loadCacheStats();
        loadBatchStats();
    }

    // ── 헬스체크 ─────────────────────────────────────────────
    function loadHealth() {
        API.actuator.getHealth().then(function (d) {
            var color  = d.status === 'UP' ? '#16a34a' : '#dc2626';
            var detail = d.components || {};
            var html   = '<div style="font-size:24px;font-weight:800;color:' + color + ';margin-bottom:12px">● ' + d.status + '</div>';

            Object.keys(detail).forEach(function (k) {
                var c = detail[k];
                var cc = c.status === 'UP' ? '#16a34a' : '#dc2626';
                html += '<div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #f1f5f9">'
                    + '<span style="font-size:13px;color:#374151">' + k + '</span>'
                    + '<span style="color:' + cc + ';font-weight:600;font-size:13px">● ' + c.status + '</span>'
                    + '</div>';
            });

            document.getElementById('health-panel').innerHTML = html;
        }).catch(function () {
            Utils.renderError('health-panel', '헬스체크 실패');
        });

        // JVM 메트릭
        Promise.all([
            API.actuator.getMetric('jvm.memory.used'),
            API.actuator.getMetric('jvm.memory.max'),
            API.actuator.getMetric('system.cpu.usage')
        ]).then(function (results) {
            var used = results[0].measurements[0].value;
            var max  = results[1].measurements[0].value;
            var cpu  = results[2].measurements[0].value;
            var pct  = max > 0 ? Math.round((used / max) * 100) : 0;
            var memColor = pct > 80 ? '#dc2626' : pct > 60 ? '#eab308' : '#16a34a';
            var cpuColor = cpu > 0.8 ? '#dc2626' : cpu > 0.5 ? '#eab308' : '#16a34a';

            document.getElementById('jvm-panel').innerHTML =
                '<div style="margin-bottom:10px">'
                + '<div style="display:flex;justify-content:space-between;margin-bottom:4px">'
                +   '<span style="font-size:13px">JVM 메모리</span>'
                +   '<span style="font-size:13px;color:' + memColor + '">'
                +       Math.round(used/1024/1024) + 'MB / ' + Math.round(max/1024/1024) + 'MB (' + pct + '%)</span>'
                + '</div>'
                + '<div style="background:#e5e7eb;height:8px;border-radius:4px;overflow:hidden">'
                +   '<div style="background:' + memColor + ';width:' + pct + '%;height:100%;transition:width 0.3s"></div>'
                + '</div>'
                + '</div>'
                + '<div>'
                + '<div style="display:flex;justify-content:space-between;margin-bottom:4px">'
                +   '<span style="font-size:13px">CPU 사용률</span>'
                +   '<span style="font-size:13px;color:' + cpuColor + '">' + Math.round(cpu * 100) + '%</span>'
                + '</div>'
                + '<div style="background:#e5e7eb;height:8px;border-radius:4px;overflow:hidden">'
                +   '<div style="background:' + cpuColor + ';width:' + Math.round(cpu * 100) + '%;height:100%;transition:width 0.3s"></div>'
                + '</div>'
                + '</div>';
        }).catch(function () {});
    }

    // ── 캐시 통계 ────────────────────────────────────────────
    function loadCacheStats() {
        API.batch.getCacheStats().then(function (d) {
            var html = '';
            Object.keys(d).forEach(function (name) {
                var c = d[name];
                html += '<div style="padding:8px 0;border-bottom:1px solid #f1f5f9">'
                    + '<div style="display:flex;justify-content:space-between">'
                    +   '<span style="font-size:13px;font-weight:600">' + name + '</span>'
                    +   '<span style="font-size:12px;color:#16a34a">HIT: ' + c.hitRate + '</span>'
                    + '</div>'
                    + '<div style="font-size:12px;color:#94a3b8">'
                    +   '크기: ' + c.size + '건 | HIT: ' + c.hitCount + ' | MISS: ' + c.missCount
                    + '</div>'
                    + '</div>';
            });
            document.getElementById('cache-stats').innerHTML = html || '<div class="empty">캐시 없음</div>';
        }).catch(function () {
            Utils.renderError('cache-stats', '캐시 통계 로드 실패');
        });
    }

    // ── 배치 통계 (7일) ──────────────────────────────────────
    function loadBatchStats() {
        API.batch.getStats().then(function (list) {
            var rows = (list || []).map(function (r) {
                var rate = r.total_runs > 0
                    ? Math.round((r.success_cnt / r.total_runs) * 100) : 0;
                var rateColor = rate >= 90 ? '#16a34a' : rate >= 70 ? '#eab308' : '#dc2626';
                return '<tr>'
                    + '<td>' + r.batch_name + '</td>'
                    + '<td>' + r.total_runs + '</td>'
                    + '<td style="color:#16a34a">' + r.success_cnt + '</td>'
                    + '<td style="color:#dc2626">' + r.fail_cnt + '</td>'
                    + '<td style="color:' + rateColor + ';font-weight:600">' + rate + '%</td>'
                    + '<td>' + Utils.nvl(r.avg_sec) + '초</td>'
                    + '</tr>';
            }).join('');
            document.getElementById('batch-stats').innerHTML =
                '<table><thead><tr><th>배치명</th><th>총실행</th><th>성공</th><th>실패</th><th>성공률</th><th>평균소요</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';
        }).catch(function () {
            Utils.renderError('batch-stats', '배치 통계 로드 실패');
        });
    }

    return { load: load };

})();
