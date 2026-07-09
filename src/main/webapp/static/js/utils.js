/**
 * utils.js - 공통 유틸리티 함수
 * 포맷, 배지, 엑셀 등 여러 섹션에서 공통으로 쓰는 함수 모음
 */
var Utils = (function () {

    // ── 금액 포맷 ────────────────────────────────────────────
    function fmt(n) {
        if (n == null) return '-';
        n = Number(n);
        if (n >= 100000000) return '₩' + (n / 100000000).toFixed(1) + '억';
        if (n >= 10000)     return '₩' + Math.round(n / 10000) + '만';
        return '₩' + n.toLocaleString();
    }

    // ── 상태 배지 ────────────────────────────────────────────
    var STATUS_CLASS = {
        CONFIRMED: 's-confirmed', SHIPPING: 's-shipping',
        PENDING:   's-pending',   COMPLETED: 's-completed',
        CANCELLED: 's-cancelled', SUCCESS: 's-success',
        FAIL:      's-fail',      WARN: 's-warn',
        ACTIVE:    's-active',    INACTIVE: 's-inactive',
        OK:        's-success',   ERROR: 's-fail'
    };

    var ORDER_LABEL = {
        CONFIRMED: '확정', SHIPPING: '배송중', PENDING: '대기',
        COMPLETED: '완료', CANCELLED: '취소'
    };

    var BATCH_LABEL = {
        SUCCESS: '성공', FAIL: '실패', WARN: '경고', '미실행': '미실행'
    };

    var QUALITY_LABEL = { GOOD: '우수', WARN: '주의', BAD: '위험', ERROR: '오류' };

    function statusBadge(s, labelMap) {
        var label = (labelMap && labelMap[s]) || s || '-';
        var cls   = STATUS_CLASS[s] || '';
        return '<span class="status ' + cls + '">' + label + '</span>';
    }

    function orderBadge(s)   { return statusBadge(s, ORDER_LABEL); }
    function batchBadge(s)   { return statusBadge(s, BATCH_LABEL); }
    function qualityBadge(s) { return statusBadge(s, QUALITY_LABEL); }

    // ── 날짜 포맷 ────────────────────────────────────────────
    function today() {
        return new Date().toISOString().split('T')[0];
    }

    function nowTime() {
        return new Date().toLocaleTimeString('ko-KR');
    }

    // ── 엑셀 다운로드 ────────────────────────────────────────
    function exportExcel(tableId, fileName) {
        var table = document.getElementById(tableId);
        if (!table) { alert('데이터가 없습니다.'); return; }
        var wb = XLSX.utils.book_new();
        var ws = XLSX.utils.table_to_sheet(table);
        XLSX.utils.book_append_sheet(wb, ws, fileName);
        XLSX.writeFile(wb, fileName + '_' + today() + '.xlsx');
    }

    // ── null 방어 ────────────────────────────────────────────
    function nvl(v, def) {
        return v != null ? v : (def !== undefined ? def : 0);
    }

    // ── 에러 렌더링 ──────────────────────────────────────────
    function renderError(elId, msg) {
        var el = document.getElementById(elId);
        if (el) el.innerHTML = '<div class="empty">' + (msg || '로드 실패') + '</div>';
    }

    function renderLoading(elId) {
        var el = document.getElementById(elId);
        if (el) el.innerHTML = '<div class="loading">로딩 중...</div>';
    }
    //응답 객체 모든 키 소문자로 변환
    function lowerKeysDeep(value) {
        if (Array.isArray(value)) {
            return value.map(lowerKeysDeep);
        }
        if (value !== null && typeof value === 'object') {
            var result = {};
            Object.keys(value).forEach(function (key) {
                result[key.toLowerCase()] = lowerKeysDeep(value[key]);
            });
            return result;
        }
        return value;
    }


    return {
        fmt: fmt,
        statusBadge: statusBadge,
        orderBadge:  orderBadge,
        batchBadge:  batchBadge,
        qualityBadge: qualityBadge,
        today:       today,
        nowTime:     nowTime,
        exportExcel: exportExcel,
        nvl:         nvl,
        renderError: renderError,
        renderLoading: renderLoading,
        lowerKeysDeep:lowerKeysDeep
    };

})();
