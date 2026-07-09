/**
 * sections/batch.js - 배치 모니터
 */
var BatchSection = (function () {

    function load() {
        loadSchedules();
        loadLogs();
    }

    // ── 배치 스케줄 ──────────────────────────────────────────
    function loadSchedules() {
        Utils.renderLoading('batch-schedules');
        API.batch.getSchedules().then(function (list) {
            var html = (list || []).map(function (b) {
                return '<div class="batch-item">'
                    + '<div style="flex:1">'
                    +   '<div style="font-size:13px;font-weight:600">' + b.desc + '</div>'
                    +   '<div style="font-size:11px;color:#94a3b8">'
                    +       b.cron + ' | 마지막: ' + (b.lastStarted || '미실행')
                    +   '</div>'
                    + '</div>'
                    + '<div style="margin-right:10px">' + Utils.batchBadge(b.lastStatus || '미실행') + '</div>'
                    + '<button class="run-btn perm-batch" onclick="BatchSection.run(\'' + b.jobKey + '\')">수동실행</button>'
                    + '</div>';
            }).join('');
            document.getElementById('batch-schedules').innerHTML = html || '<div class="empty">-</div>';
        }).catch(function () {
            Utils.renderError('batch-schedules', '배치 스케줄 로드 실패');
        });
    }

    // ── 배치 실행 이력 ───────────────────────────────────────
    function loadLogs() {
        Utils.renderLoading('batch-logs');
        API.batch.getLogs({ page: 0, size: 20 }).then(function (data) {
            var list = Utils.lowerKeysDeep(data.logs || []);
            if (!list.length) {
                document.getElementById('batch-logs').innerHTML = '<div class="empty">이력 없음</div>';
                return;
            }
            var rows = list.map(function (l) {
                return '<tr>'
                    + '<td>' + l.batch_name + '</td>'
                    + '<td>' + (l.country_code || '-') + '</td>'
                    + '<td>' + Utils.batchBadge(l.status) + '</td>'
                    + '<td>' + Utils.nvl(l.records_processed) + '건</td>'
                    + '<td>' + (l.started_at || '-') + '</td>'
                    + '<td style="color:#dc2626;font-size:11px">' + (l.error_message || '-') + '</td>'
                    + '</tr>';
            }).join('');
            document.getElementById('batch-logs').innerHTML =
                '<table id="batch-logs-data">'
                + '<thead><tr><th>배치명</th><th>국가</th><th>상태</th><th>처리건수</th><th>시작시간</th><th>오류</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';
        }).catch(function () {
            Utils.renderError('batch-logs', '배치 이력 로드 실패');
        });
    }

    // ── 수동 실행 ────────────────────────────────────────────
    function run(jobKey) {
        if (!confirm(jobKey + ' 배치를 실행하시겠습니까?')) return;
        API.batch.run(jobKey).then(function (msg) {
            alert(msg);
            load();
        }).catch(function (e) {
            alert('실행 실패: ' + e.message);
        });
    }

    return { load: load, run: run };

})();
