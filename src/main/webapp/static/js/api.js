/**
 * api.js - API 호출 공통 모듈
 * 모든 fetch 호출을 여기서 관리합니다.
 * 엔드포인트가 바뀌면 이 파일만 수정합니다.
 */
var API = (function () {

    var BASE = '/api';

    // ── 공통 fetch 래퍼 ──────────────────────────────────────
    function get(url) {
        return fetch(BASE + url, {
          headers:   {
              'X-Requested-With': 'XMLHttpRequest'
          }
        }).then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        });
    }

    function post(url, body) {
        return fetch(BASE + url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body ? JSON.stringify(body) : undefined
        }).then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        });
    }

    function postText(url) {
        return fetch(BASE + url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
        }).then(function (r) {
            return r.text();
        });
    }

    function put(url, body) {
        return fetch(BASE + url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        }).then(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        });
    }
    // ── H2 대문자 컬럼명 → 소문자 변환 ──────────────────────

    function normalizeKeys(obj) {
        if (!obj || typeof obj !== 'object') return obj;
        var result = {};
        Object.keys(obj).forEach(function (key) {
            result[key.toLowerCase()] = obj[key];
        });
        return result;
    }

    function normalizeResponse(data) {
        if (Array.isArray(data)) {
            return data.map(function (item) {
                return normalizeResponse(item);
            });
        }
        if (data && typeof data === 'object') {
            var lowered = normalizeKeys(data);
            Object.keys(lowered).forEach(function (key) {
                var val = lowered[key];
                if (Array.isArray(val) || (val && typeof val === 'object')) {
                    lowered[key] = normalizeResponse(val);
                }
            });
            return lowered;
        }
        return data;
    }


    // ApiResponse 래퍼 처리
    function unwrap(res) {
        var data = (res && res.data !== undefined) ? res.data : res;
        return normalizeResponse(data);
    }

    // 응답 전체를 소문자 키로 변환
    function normalizeResponse(data) {
        if (Array.isArray(data)) {
            return data.map(function (item) {
                return typeof item === 'object' ? normalizeKeys(item) : item;
            });
        }
        if (data && typeof data === 'object') {
            var result = {};
            Object.keys(data).forEach(function (key) {
                var val = data[key];
                if (Array.isArray(val)) {
                    result[key] = normalizeResponse(val);
                } else if (val && typeof val === 'object') {
                    result[key] = normalizeKeys(val);
                } else {
                    result[key] = val;
                }
            });
            return result;
        }
        return data;
    }

    function normalizeKeys(obj) {
        if (!obj) return {};
        var result = {};
        Object.keys(obj).forEach(function (key) {
            result[key.toLowerCase()] = obj[key];
        });
        return result;
    }

        // ── Dashboard ────────────────────────────────────────────
    var dashboard = {
        getKpi:            function () { return get('/dashboard/kpi').then(unwrap); },
        getRecentOrders:   function () { return get('/dashboard/recent-orders').then(unwrap); },
        getSalesByCountry: function () { return get('/dashboard/sales-by-country').then(unwrap); }
    };

    // ── Orders ───────────────────────────────────────────────
    var orders = {
        getList: function (params) {
            var q = '/orders?page=' + (params.page || 1)
                + '&size=' + (params.size || 10)
                + '&country=' + (params.country || '')
                + '&keyword=' + encodeURIComponent(params.keyword || '')
                + '&status='  + (params.status || '')
                + '&startDate=' + (params.startDate || '')
                + '&endDate='   + (params.endDate || '');
            return get(q).then(unwrap);
        },
        getDetail: function (orderNo, country) {
            return get('/orders/' + orderNo + '?country=' + (country || 'KR')).then(unwrap);
        },
        create:       function (body)       { return post('/orders', body); },
        changeStatus: function (orderNo, status) {
            return put('/orders/' + orderNo + '/status', { status: status });
        }
    };

    // ── Countries ────────────────────────────────────────────
    var countries = {
        getStatus:     function () { return get('/countries/status').then(unwrap); },
        getDataQuality:function () { return get('/countries/data-quality').then(unwrap); },
        getSyncStatus: function () { return get('/countries/product-sync-status').then(unwrap); },
        toggle:        function (code) { return put('/countries/' + code + '/toggle').then(unwrap); }
    };

    // ── Batch ────────────────────────────────────────────────
    var batch = {
        getSchedules:   function ()        { return get('/batch/schedules'); },
        getLogs:        function (params)  {
            var q = '/batch/logs?page=' + (params.page || 0)
                + '&size=' + (params.size || 20)
                + (params.batchName ? '&batchName=' + params.batchName : '')
                + (params.status    ? '&status='    + params.status    : '');
            return get(q).then(unwrap);
        },
        getStats:        function ()       { return get('/batch/stats'); },
        getExchangeRates:function ()       { return get('/batch/exchange-rates').then(unwrap); },
        getAuditLogs:    function (params) {
            var q = '/batch/audit-logs?size=' + (params.size || 50)
                + (params.action ? '&action=' + params.action : '')
                + (params.entity ? '&entity=' + params.entity : '');
            return get(q);
        },
        getCacheStats:   function ()       { return get('/batch/cache-stats'); },
        run:             function (jobKey) { return postText('/batch/run/' + jobKey); }
    };

    // ── Actuator ─────────────────────────────────────────────
    var actuator = {
        getHealth:  function () { return fetch('/actuator/health').then(function (r) { return r.json(); }); },
        getInfo:    function () { return fetch('/actuator/info').then(function (r) { return r.json(); }); },
        getMetric:  function (name) {
            return fetch('/actuator/metrics/' + name).then(function (r) { return r.json(); });
        }
    };

    // ── HR ───────────────────────────────────────────────
    var hr = {
        // 직원
        getEmployees:   function () { return get('/hr/employees').then(unwrap); },
        getEmployee:    function (empId) { return get('/hr/employees/' + empId).then(unwrap); },
        createEmployee: function (body) { return post('/hr/employees', body); },
        updateStatus:   function (empId, status) {
            return put('/hr/employees/' + empId + '/status', { status: status });
        },
        updateSalary:   function (empId, salary) {
            return put('/hr/employees/' + empId + '/salary', { baseSalary: salary });
        },

        // 근태
        getAttendance: function (yearMonth) {
            return get('/hr/attendance' + (yearMonth ? '?yearMonth=' + yearMonth : '')).then(unwrap);
        },

        // 급여
        getPayroll: function (yearMonth) {
            return get('/hr/payroll' + (yearMonth ? '?yearMonth=' + yearMonth : '')).then(unwrap);
        },
        getPayrollHistory: function (empId) {
            return get('/hr/payroll/' + empId + '/history').then(unwrap);
        },
        markAsPaid: function (yearMonth) {
            return put('/hr/payroll/' + yearMonth + '/paid');
        },

        // 요약
        getSummary: function () { return get('/hr/summary').then(unwrap); }
    };

    return { dashboard: dashboard, orders: orders, countries: countries, batch: batch, actuator: actuator, hr:hr };

})();
