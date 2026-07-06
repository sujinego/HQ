/**
 * components.js - 재사용 UI 컴포넌트
 * 여러 섹션에서 공통으로 쓰는 HTML 생성 함수 모음
 */
var Components = (function () {

    // ── 페이지네이션 ─────────────────────────────────────────
    function pagination(current, total, onPageFn) {
        if (total <= 1) return '';
        var html = '';
        if (current > 1) {
            html += '<button class="btn btn-secondary btn-sm" onclick="' + onPageFn + '(' + (current - 1) + ')">이전</button>';
        }
        var from = Math.max(1, current - 2);
        var to   = Math.min(total, current + 2);
        for (var i = from; i <= to; i++) {
            var cls = i === current ? 'btn-primary' : 'btn-secondary';
            html += '<button class="btn ' + cls + ' btn-sm" onclick="' + onPageFn + '(' + i + ')">' + i + '</button>';
        }
        if (current < total) {
            html += '<button class="btn btn-secondary btn-sm" onclick="' + onPageFn + '(' + (current + 1) + ')">다음</button>';
        }
        return html;
    }

    // ── 국가 플래그 ──────────────────────────────────────────
    var FLAGS = { KR: '🇰🇷', US: '🇺🇸', DE: '🇩🇪', CN: '🇨🇳', JP: '🇯🇵' };

    function flag(code) {
        return FLAGS[code] || '🌐';
    }

    // ── 커넥션 풀 게이지 ─────────────────────────────────────
    function poolGauge(active, total) {
        var pct   = total > 0 ? Math.round((active / total) * 100) : 0;
        var color = pct > 80 ? '#dc2626' : pct > 50 ? '#eab308' : '#16a34a';
        return '<div style="width:130px;text-align:right">'
            + '<div style="font-size:11px;color:#4b5563;margin-bottom:3px;font-family:monospace">'
            + 'Pool: ' + active + '/' + total + ' (' + pct + '%)</div>'
            + '<div style="background:#e5e7eb;height:6px;border-radius:3px;overflow:hidden">'
            + '<div style="background:' + color + ';width:' + pct + '%;height:100%;transition:width 0.3s"></div>'
            + '</div></div>';
    }

    // ── 국가 현황 행 ─────────────────────────────────────────
    function countryStatusRow(c) {
        var statusColor = c.dbStatus === 'OK' ? '#16a34a' : '#dc2626';
        var pingColor   = c.statusLevel === 'ERROR' ? '#dc2626' : c.statusLevel === 'WARN' ? '#eab308' : '#16a34a';
        var pingText    = c.pingMs ? c.pingMs + 'ms' : '장애';
        var syncDate    = c.lastSync ? c.lastSync.substring(11, 16) : '-';
        var syncStatus  = c.lastSyncStatus || '미실행';
        var syncBg      = syncStatus === 'SUCCESS' ? '#dcfce7' : syncStatus === 'FAIL' ? '#fee2e2' : '#f3f4f6';
        var syncTxt     = syncStatus === 'SUCCESS' ? '#15803d' : syncStatus === 'FAIL' ? '#b91c1c' : '#4b5563';

        return '<div class="country-row" style="display:flex;align-items:center;padding:12px;border-bottom:1px solid #f3f4f6">'
            + '<div style="font-size:24px;margin-right:12px">' + flag(c.country_code) + '</div>'
            + '<div class="country-info" style="flex:1">'
            +   '<div class="country-name" style="font-weight:600;font-size:14px;color:#1f2937">'
            +       c.country_name + ' (' + c.currency + ')'
            +       '<span style="background:' + pingColor + ';color:#fff;padding:2px 6px;border-radius:4px;font-size:11px;margin-left:8px">' + pingText + '</span>'
            +   '</div>'
            +   '<div class="country-meta" style="font-size:12px;color:#6b7280;margin-top:4px">'
            +       '오늘주문: <strong>' + Utils.nvl(c.todayOrders) + '</strong>건 / '
            +       '전체고객: <strong>' + Utils.nvl(c.totalCustomers) + '</strong>사'
            +   '</div>'
            +   '<div style="margin-top:6px">'
            +       '<span style="background:' + syncBg + ';color:' + syncTxt + ';padding:1px 8px;border-radius:10px;font-size:11px">'
            +           '최근배치: ' + syncDate + ' (' + syncStatus + ')'
            +       '</span>'
            +   '</div>'
            + '</div>'
            + poolGauge(c.poolActiveConnections || 0, c.poolTotalConnections || 0)
            + '<div style="color:' + statusColor + ';font-weight:bold;font-size:14px;min-width:70px;text-align:right">'
            +   '● ' + (c.dbStatus || '-')
            + '</div>'
            + '</div>';
    }

    // ── 데이터 품질 행 ───────────────────────────────────────
    function dataQualityRow(c) {
        var badgeBg = '#f3f4f6', badgeTxt = '#4b5563';
        if (c.level === 'GOOD')  { badgeBg = '#dcfce7'; badgeTxt = '#15803d'; }
        if (c.level === 'WARN')  { badgeBg = '#fef9c3'; badgeTxt = '#a16207'; }
        if (c.level === 'BAD' || c.level === 'ERROR') { badgeBg = '#fee2e2'; badgeTxt = '#b91c1c'; }
        var levelMap = { GOOD: '우수', WARN: '주의', BAD: '위험', ERROR: '오류' };

        return '<div class="country-row" style="display:flex;align-items:center;justify-content:space-between;padding:12px;border-bottom:1px solid #f3f4f6">'
            + '<div class="country-info">'
            +   '<div class="country-name" style="font-weight:600;font-size:14px;color:#1f2937">'
            +       c.countryCode + ' 데이터 품질지수: <span style="color:#2563eb">'
            +       (c.qualityScore != null ? c.qualityScore + '점' : '계산불가') + '</span>'
            +   '</div>'
            +   '<div class="country-meta" style="font-size:12px;color:#6b7280;margin-top:4px;display:flex;gap:12px">'
            +       '<span>마이너스 결제액: <strong style="color:#dc2626">' + Utils.nvl(c.negativeAmountCnt) + '</strong>건</span>'
            +       '<span>이메일 누락: <strong style="color:#dc2626">' + Utils.nvl(c.missingEmailCnt) + '</strong>명</span>'
            +   '</div>'
            + '</div>'
            + '<div style="background:' + badgeBg + ';color:' + badgeTxt + ';padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600">'
            +   (levelMap[c.level] || c.level)
            + '</div>'
            + '</div>';
    }

    return {
        pagination:       pagination,
        flag:             flag,
        poolGauge:        poolGauge,
        countryStatusRow: countryStatusRow,
        dataQualityRow:   dataQualityRow
    };

})();
