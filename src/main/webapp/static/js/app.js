/**
 * app.js - 섹션 라우팅 + 앱 초기화
 * 메뉴 클릭 시 해당 섹션을 활성화하고 데이터를 로드합니다.
 */
var App = (function () {

    var currentUser = { username: '', roles: [] };  // 현재 사용자 정보

    // 권한 확인 유틸
    var Auth = {
        hasRole: function (role) {
            return currentUser.roles.includes('ROLE_' + role);
        },
        isAdmin: function () { return Auth.hasRole('ADMIN'); },
        isKrUser: function () {
            return Auth.hasRole('ADMIN') || Auth.hasRole('KR_USER');
        },
        isUsUser: function () { return Auth.hasRole('US_USER'); },
        canWrite: function () {
            // 주문 등록/수정 가능 여부
            return Auth.hasRole('ADMIN') || Auth.hasRole('KR_USER');
        },
        canDelete: function () {
            // 주문 삭제 가능 여부
            return Auth.hasRole('ADMIN');
        },
        canRunBatch: function () {
            // 배치 수동 실행 가능 여부
            return Auth.hasRole('ADMIN');
        }
    };

    // 권한에 따라 UI 요소 표시/숨김
    function applyPermissions() {
        // 주문 등록 버튼
        document.querySelectorAll('.perm-write').forEach(function (el) {
            el.style.display = Auth.canWrite() ? '' : 'none';
        });

        // 주문 삭제 버튼
        document.querySelectorAll('.perm-delete').forEach(function (el) {
            el.style.display = Auth.canDelete() ? '' : 'none';
        });

        // 배치 수동 실행 버튼
        document.querySelectorAll('.perm-batch').forEach(function (el) {
            el.style.display = Auth.canRunBatch() ? '' : 'none';
        });

        // 탑바에 사용자 정보 표시
        var userEl = document.getElementById('current-user');
        if (userEl) {
            var roleBadge = Auth.isAdmin()  ? '<span style="background:#fef3c7;color:#92400e;padding:1px 6px;border-radius:4px;font-size:11px;margin-left:4px">ADMIN</span>'
                : Auth.isKrUser() ? '<span style="background:#dbeafe;color:#1e40af;padding:1px 6px;border-radius:4px;font-size:11px;margin-left:4px">KR</span>'
                    : '<span style="background:#dcfce7;color:#166534;padding:1px 6px;border-radius:4px;font-size:11px;margin-left:4px">US</span>';
            userEl.innerHTML = currentUser.username + roleBadge;
        }
    }

    // ── 섹션 → 로더 매핑 ─────────────────────────────────────
    var SECTIONS = {
        'sec-dashboard': DashboardSection.load,
        'sec-orders':    function () { OrdersSection.load(1); },
        'sec-countries': CountriesSection.load,
        'sec-batch':     BatchSection.load,
        'sec-exchange':  ExchangeSection.load,
        'sec-chart':     ChartSection.load,
        'sec-monitor':   MonitorSection.load,
        'sec-audit':     AuditSection.load,
        'sec-hr': HrSection.load
    };

    // ── 섹션 → 페이지 타이틀 매핑 ───────────────────────────
    var TITLES = {
        'sec-dashboard': '통합 대시보드',
        'sec-orders':    '주문 관리',
        'sec-countries': '국가 현황',
        'sec-batch':     '배치 모니터',
        'sec-exchange':  '환율 현황',
        'sec-chart':     '매출 분석',
        'sec-monitor':   '시스템 모니터',
        'sec-audit':     '감사 로그',
        'sec-hr'    :    '인사 관리'
    };

    var currentSection = null;
    var autoRefreshTimer = null;

    // ── 섹션 전환 ────────────────────────────────────────────
    function show(sectionId) {
        if (currentSection === sectionId) return;
        currentSection = sectionId;

        // 모든 섹션 숨김
        document.querySelectorAll('.section').forEach(function (el) {
            el.classList.remove('active');
        });

        // 모든 메뉴 비활성화
        document.querySelectorAll('.menu-item').forEach(function (el) {
            el.classList.remove('active');
        });

        // 대상 섹션 활성화
        var sec = document.getElementById(sectionId);
        if (sec) sec.classList.add('active');

        // 대응 메뉴 활성화
        var menu = document.querySelector('[data-section="' + sectionId + '"]');
        if (menu) menu.classList.add('active');

        // 페이지 타이틀 업데이트
        var titleEl = document.getElementById('page-title');
        if (titleEl) titleEl.textContent = TITLES[sectionId] || '';

        // 데이터 로드
        var loader = SECTIONS[sectionId];
        if (loader) loader();
    }

    // ── 자동 갱신 (대시보드: 30초) ───────────────────────────
    function startAutoRefresh() {
        stopAutoRefresh();
        autoRefreshTimer = setInterval(function () {
            if (currentSection === 'sec-dashboard') {
                DashboardSection.load();
            }
        }, 30000);
    }

    function stopAutoRefresh() {
        if (autoRefreshTimer) {
            clearInterval(autoRefreshTimer);
            autoRefreshTimer = null;
        }
    }

    // ── 초기화 ───────────────────────────────────────────────
    function init() {

        // 현재 사용자 정보 먼저 로드 후 초기화
        fetch('/api/me').then(function (r) { return r.json(); })
            .then(function (user) {
                currentUser = user;
                applyPermissions();  // 권한에 따라 UI 적용

                // 메뉴 클릭 이벤트 등록
                document.querySelectorAll('.menu-item[data-section]').forEach(function (el) {
                    el.addEventListener('click', function () {
                        show(el.getAttribute('data-section'));
                    });
                });

                // 로그아웃
                var logoutBtn = document.getElementById('logout-btn');
                if (logoutBtn) {
                    logoutBtn.addEventListener('click', function () {
                        if (confirm('로그아웃 하시겠습니까?')) {
                            window.location.href = '/logout';
                        }
                    });
                }

                show('sec-dashboard');  // 기본 섹션 = 대시보드
                startAutoRefresh(); // 자동 갱신 시작

                setInterval(function () {// 시간 표시
                    var el = document.getElementById('current-time');
                    if (el) el.textContent = new Date().toLocaleTimeString('ko-KR');
                }, 1000);
            });
    }

    // 외부에서 Auth 접근 가능하도록 노출
    return { init: init, show: show, Auth: Auth, currentUser: currentUser };

})();
// DOM 로드 완료 후 앱 시작
document.addEventListener('DOMContentLoaded', App.init);

