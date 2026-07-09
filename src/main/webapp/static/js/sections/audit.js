/**
 * sections/audit.js - 감사 로그
 */
var AuditSection = (function () {

    function load() {
        var params = {
            size:   document.getElementById('audit-size')   ? document.getElementById('audit-size').value   : 50,
            action: document.getElementById('audit-action') ? document.getElementById('audit-action').value : '',
            entity: document.getElementById('audit-entity') ? document.getElementById('audit-entity').value : ''
        };

        Utils.renderLoading('audit-logs');
        API.batch.getAuditLogs(params).then(function (list) {
            var normalized = Utils.lowerKeysDeep(list || []);
            if (!normalized.length) {
                document.getElementById('audit-logs').innerHTML = '<div class="empty">감사 로그 없음</div>';
                return;
            }
            var rows = normalized.map(function (l) {
                return '<tr>'
                    + '<td style="color:#64748b;font-size:11px">' + (l.created_at || '-') + '</td>'
                    + '<td><b>' + (l.action || '-') + '</b></td>'
                    + '<td>' + (l.entity || '-') + '</td>'
                    + '<td style="font-family:monospace;font-size:11px">' + (l.entity_id || '-') + '</td>'
                    + '<td style="font-size:11px;color:#64748b">' + (l.user_id || 'system') + '</td>'
                    + '<td style="font-size:11px;max-width:200px;overflow:hidden;text-overflow:ellipsis">'
                    +   (l.detail || '-')
                    + '</td>'
                    + '</tr>';
            }).join('');
            document.getElementById('audit-logs').innerHTML =
                '<table id="audit-data">'
                + '<thead><tr><th>일시</th><th>액션</th><th>대상</th><th>ID</th><th>사용자</th><th>상세</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';
        }).catch(function () {
            Utils.renderError('audit-logs', '감사 로그 로드 실패');
        });
    }

    return { load: load };

})();
