<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HQ B2B Global Commerce Portal</title>

<!-- CSS -->
<link rel="stylesheet" href="/static/css/common.css">
<link rel="stylesheet" href="/static/css/layout.css">
<link rel="stylesheet" href="/static/css/components.css">

<!-- 외부 라이브러리 -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/3.9.1/chart.min.js"></script>
</head>
<body>

<!-- ── 사이드바 ─────────────────────────────────────────── -->
<nav class="sidebar">
    <div class="sidebar-logo">
        <div class="logo-lg">HQ</div>
        <div class="logo-sub">B2B Global Commerce Portal</div>
    </div>

    <div class="menu-group-title">메인</div>
    <div class="menu-item active" data-section="sec-dashboard">
        <span class="menu-icon">📊</span> 통합 대시보드
    </div>

    <div class="menu-group-title">운영</div>
    <div class="menu-item" data-section="sec-orders">
        <span class="menu-icon">📦</span> 주문 관리
    </div>
    <div class="menu-item" data-section="sec-countries">
        <span class="menu-icon">🌍</span> 국가 현황
    </div>
    <div class="menu-item" data-section="sec-exchange">
        <span class="menu-icon">💱</span> 환율 현황
    </div>

    <div class="menu-group-title">분석</div>
    <div class="menu-item" data-section="sec-chart">
        <span class="menu-icon">📈</span> 매출 분석
    </div>

    <div class="menu-group-title">시스템</div>
    <div class="menu-item" data-section="sec-batch">
        <span class="menu-icon">⚙️</span> 배치 모니터
    </div>
    <div class="menu-item" data-section="sec-monitor">
        <span class="menu-icon">🖥️</span> 시스템 모니터
    </div>
    <div class="menu-item" data-section="sec-audit">
        <span class="menu-icon">📋</span> 감사 로그
    </div>

    <div class="menu-group-title">인사</div>
    <div class="menu-item" data-section="sec-hr">
        <span class="menu-icon">👥</span> 인사 관리
    </div>
</nav>

<!-- ── 메인 영역 ─────────────────────────────────────────── -->
<div class="main-wrapper">

    <!-- 탑바 -->
    <div class="topbar">
        <div class="page-title" id="page-title">통합 대시보드</div>
        <div class="topbar-right">
            <span id="current-time"></span>
            <span>관리자: <b>${pageContext.request.userPrincipal.name}</b></span>
            <button id="logout-btn" class="btn btn-secondary btn-sm">로그아웃</button>
        </div>
    </div>

    <!-- 콘텐츠 -->
    <div class="content">

        <!-- ── 대시보드 ──────────────────────────────────── -->
        <div id="sec-dashboard" class="section active">
            <div class="kpi-grid">
                <div class="kpi-card">
                    <div class="kpi-label">오늘 주문 (글로벌)</div>
                    <div class="kpi-value" id="k-orders">-</div>
                </div>
                <div class="kpi-card" style="cursor: pointer" onclick="OrdersSection.loadByStatus('PENDING')">
                    <div class="kpi-label">견적 대기(글로벌)</div>
                    <div class="kpi-value" id="k-pending">-</div>
                </div>
                <div class="kpi-card" style="cursor: pointer" onclick="OrdersSection.loadByStatus('SHIPPING')">
                    <div class="kpi-label">배송 중</div>
                    <div class="kpi-value" id="k-shipping">-</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">이번 달 매출</div>
                    <div class="kpi-value" id="k-sales">-</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">평균 주문 금액 (AOV)</div>
                    <div class="kpi-value" id="k-aov">-</div>
                </div>
                <div class="kpi-card">
                    <div class="kpi-label">국가별 주문 비중</div>
                    <div style="font-size:13px;margin-top:8px" id="k-country-breakdown">-</div>
                </div>
            </div>

            <div class="card-row col2">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">최근 주문 (글로벌)</div>
                        <button class="excel-btn" onclick="Utils.exportExcel('orders-data','최근주문')">엑셀</button>
                    </div>
                    <div id="recent-orders"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">국가별 매출</div>
                    </div>
                    <div id="sales-by-country"><div class="loading">로딩 중...</div></div>
                </div>
            </div>

            <div class="card-row col2">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">국가 연결 현황</div>
                        <span class="card-action" onclick="App.show('sec-countries')">전체 보기 →</span>
                    </div>
                    <div id="dash-countries"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">배치 현황</div>
                        <span class="card-action" onclick="App.show('sec-batch')">전체 보기 →</span>
                    </div>
                    <div id="dash-batch"><div class="loading">로딩 중...</div></div>
                </div>
            </div>

            <div style="font-size:11px;color:#94a3b8;text-align:right;margin-top:4px">
                마지막 갱신: <span id="lastUpdate">-</span> (30초 자동갱신)
            </div>
        </div>

        <!-- ── 주문 관리 ──────────────────────────────────── -->
        <div id="sec-orders" class="section">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">주문 목록</div>
                    <button class="btn btn-primary btn-sm perm-write" onclick="OrdersSection.openModal()">+ 주문 등록</button>
                </div>
                <div class="search-bar">
                    <input id="order-keyword" placeholder="주문번호 / 고객사" style="width:200px">
                    <!-- 국가 필터 -->
                    <select id="order-country">
                        <option value="">전체 국가</option>
                        <option value="KR">🇰🇷 한국</option>
                        <option value="US">🇺🇸 미국</option>
                    </select>
                    <!-- 상태 필터 -->
                    <select id="order-status">
                        <option value="">전체 상태</option>
                        <option>PENDING</option>
                        <option>CONFIRMED</option>
                        <option>SHIPPING</option>
                        <option>COMPLETED</option>
                        <option>CANCELLED</option>
                    </select>
                    <input id="order-start" type="date">
                    <span style="color:#94a3b8">~</span>
                    <input id="order-end" type="date">
                    <button class="btn btn-primary btn-sm" onclick="OrdersSection.search()">검색</button>
                    <button class="btn btn-secondary btn-sm" onclick="OrdersSection.reset()">초기화</button>
                    <button class="excel-btn" onclick="Utils.exportExcel('orders-data','주문목록')">엑셀</button>
                </div>
                <div id="orders-table"><div class="loading">로딩 중...</div></div>
                <div id="orders-paging" style="display:flex;gap:4px;justify-content:center;margin-top:12px"></div>
            </div>
        </div>

        <!-- ── 국가 현황 ───────────────────────────────────── -->
        <div id="sec-countries" class="section">
            <div class="card-row col2">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">🌐 글로벌 인프라 관제</div>
                        <button class="btn btn-secondary btn-sm" onclick="CountriesSection.load()">새로고침</button>
                    </div>
                    <div id="countries-status"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">📊 데이터 품질 지수</div>
                    </div>
                    <div id="data-quality"><div class="loading">로딩 중...</div></div>
                </div>
            </div>
        </div>

        <!-- ── 환율 현황 ───────────────────────────────────── -->
        <div id="sec-exchange" class="section">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">💱 오늘의 환율 (한국수출입은행)</div>
                    <button class="excel-btn" onclick="Utils.exportExcel('exchange-data','환율현황')">엑셀</button>
                </div>
                <div id="exchange-table"><div class="loading">로딩 중...</div></div>
            </div>
        </div>

        <!-- ── 매출 분석 ───────────────────────────────────── -->
        <div id="sec-chart" class="section">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">📈 국가별 매출 분석</div>
                </div>
                <div id="chart-container" style="position:relative;height:350px">
                    <canvas id="salesChart"></canvas>
                </div>
            </div>
        </div>

        <!-- ── 배치 모니터 ────────────────────────────────── -->
        <div id="sec-batch" class="section">
            <div class="card-row col2">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">⚙️ 배치 스케줄 현황</div>
                    </div>
                    <div id="batch-schedules"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">📋 실행 이력</div>
                        <button class="excel-btn" onclick="Utils.exportExcel('batch-logs-data','배치이력')">엑셀</button>
                    </div>
                    <div id="batch-logs"><div class="loading">로딩 중...</div></div>
                </div>
            </div>
        </div>

        <!-- ── 시스템 모니터 ──────────────────────────────── -->
        <div id="sec-monitor" class="section">
            <div class="card-row col2">
                <div class="card">
                    <div class="card-header"><div class="card-title">🏥 헬스체크</div></div>
                    <div id="health-panel"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header"><div class="card-title">🖥️ JVM 리소스</div></div>
                    <div id="jvm-panel"><div class="loading">로딩 중...</div></div>
                </div>
            </div>
            <div class="card-row col2">
                <div class="card">
                    <div class="card-header"><div class="card-title">⚡ 캐시 통계</div></div>
                    <div id="cache-stats"><div class="loading">로딩 중...</div></div>
                </div>
                <div class="card">
                    <div class="card-header"><div class="card-title">📊 배치 성공률 (7일)</div></div>
                    <div id="batch-stats"><div class="loading">로딩 중...</div></div>
                </div>
            </div>
        </div>

        <!-- ── 감사 로그 ───────────────────────────────────── -->
        <div id="sec-audit" class="section">
            <div class="card">
                <div class="card-header">
                    <div class="card-title">📋 감사 로그</div>
                    <button class="excel-btn" onclick="Utils.exportExcel('audit-data','감사로그')">엑셀</button>
                </div>
                <div class="search-bar">
                    <select id="audit-action">
                        <option value="">전체 액션</option>
                        <option>LOGIN</option><option>LOGOUT</option>
                        <option>CREATE</option><option>UPDATE</option><option>DELETE</option>
                    </select>
                    <input id="audit-entity" placeholder="대상 (orders, product...)" style="width:180px">
                    <select id="audit-size">
                        <option value="20">20건</option>
                        <option value="50" selected>50건</option>
                        <option value="100">100건</option>
                    </select>
                    <button class="btn btn-primary btn-sm" onclick="AuditSection.load()">검색</button>
                </div>
                <div id="audit-logs"><div class="loading">로딩 중...</div></div>
            </div>
        </div>


        <!-- ── 인사 관리 ───────────────────────────────── -->
        <div id="sec-hr" class="section">

            <!-- 직원 목록 -->
            <div class="card" style="margin-bottom:16px">
                <div class="card-header">
                    <div class="card-title">👥 직원 현황</div>
                    <div style="display:flex;gap:8px">
                        <button class="excel-btn"
                                onclick="Utils.exportExcel('hr-emp-data','직원현황')">엑셀</button>
                        <button class="btn btn-primary btn-sm perm-admin"
                                onclick="HrSection.openEmployeeModal()">+ 직원 등록</button>
                    </div>
                </div>
                <div id="hr-employees"><div class="loading">로딩 중...</div></div>
            </div>

            <div class="card-row col2">
                <!-- 근태 현황 -->
                <div class="card">
                    <div class="card-header">
                        <div class="card-title"> 근태 현황</div>
                        <div style="display:flex;gap:8px;align-items:center">
                            <input type="month" id="hr-att-month"
                                   style="padding:4px 8px;border:1px solid #e2e8f0;
                                  border-radius:6px;font-size:12px"
                                   onchange="HrSection.loadAttendance(this.value)">
                            <button class="excel-btn"
                                    onclick="Utils.exportExcel('hr-att-data','근태현황')">엑셀</button>
                        </div>
                    </div>
                    <div id="hr-att-summary"></div>
                    <div id="hr-attendance"><div class="loading">로딩 중...</div></div>
                </div>

                <!-- 급여 현황 -->
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">💰 급여 현황</div>
                        <div style="display:flex;gap:8px;align-items:center">
                            <input type="month" id="hr-pay-month"
                                   style="padding:4px 8px;border:1px solid #e2e8f0;
                                  border-radius:6px;font-size:12px"
                                   onchange="HrSection.loadPayroll(this.value)">
                            <button class="excel-btn"
                                    onclick="Utils.exportExcel('hr-pay-data','급여현황')">엑셀</button>
                        </div>
                    </div>
                    <div id="hr-pay-summary"></div>
                    <div id="hr-payroll"><div class="loading">로딩 중...</div></div>
                </div>
            </div>
        </div>

    </div><!-- /content -->
</div><!-- /main-wrapper -->

<!-- ── 주문 등록 모달 ─────────────────────────────────────── -->
<div id="orderModal" class="modal-overlay">
    <div class="modal">
        <div class="modal-header">
            <div class="modal-title">주문 등록</div>
            <button class="modal-close" onclick="OrdersSection.closeModal()">✕</button>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>주문번호</label>
                <input id="f-order-no" placeholder="자동 생성">
            </div>
            <div class="form-group">
                <label>고객사 ID</label>
                <input id="f-customer-id" placeholder="CUST-001">
            </div>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>상품 코드</label>
                <input id="f-product-code" placeholder="LG-GRAM-17">
            </div>
            <div class="form-group">
                <label>수량</label>
                <input id="f-quantity" type="number" value="1" min="1">
            </div>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>단가 (KRW)</label>
                <input id="f-unit-price" type="number" placeholder="1000000">
            </div>
            <div class="form-group">
                <label>주문일</label>
                <input id="f-order-date" type="date">
            </div>
        </div>
        <div class="form-group" style="margin-bottom:14px">
            <label>상태</label>
            <select id="f-status">
                <option value="PENDING">PENDING</option>
                <option value="CONFIRMED">CONFIRMED</option>
            </select>
        </div>
        <div class="form-actions">
            <button class="btn btn-secondary" onclick="OrdersSection.closeModal()">취소</button>
            <button class="btn btn-primary" onclick="OrdersSection.submitOrder()">등록</button>
        </div>
    </div>
</div>

<%--// ── 주문 상세 모달 ───────────────────────────────────────--%>
<div id="orderDetailModal" class="modal-overlay">
    <div class="modal">
        <div class="modal-header">
            <div class="modal-title" id="detail-order-no">주문 상세</div>
            <button class="modal-close"
                    onclick="document.getElementById('orderDetailModal')
                             .classList.remove('open')">✕</button>
        </div>

        <!-- 주문 헤더 -->
        <div id="detail-header" style="background:#f8fafc;padding:14px;
             border-radius:8px;margin-bottom:16px;font-size:13px"></div>

        <!-- 주문 상품 목록 -->
        <div id="detail-items"></div>

        <!-- 합계 -->
        <div id="detail-total" style="text-align:right;font-size:15px;
             font-weight:700;margin-top:12px;padding-top:12px;
             border-top:2px solid #e2e8f0"></div>
    </div>
</div>


<!-- 급여 이력 모달 -->
<div id="hrHistoryModal" class="modal-overlay">
    <div class="modal" style="width:680px">
        <div class="modal-header">
            <div class="modal-title" id="hr-history-name">급여 이력</div>
            <button class="modal-close"
                    onclick="document.getElementById('hrHistoryModal')
                             .classList.remove('open')">✕</button>
        </div>
        <div id="hr-history-body"></div>
    </div>
</div>

<!-- 급여 변경 모달 -->
<div id="hrSalaryModal" class="modal-overlay">
    <div class="modal" style="width:400px">
        <div class="modal-header">
            <div class="modal-title">기본급 변경</div>
            <button class="modal-close"
                    onclick="document.getElementById('hrSalaryModal')
                             .classList.remove('open')">✕</button>
        </div>
        <input type="hidden" id="salary-emp-id">
        <div class="form-group" style="margin-bottom:14px">
            <label>현재 기본급</label>
            <div id="salary-current"
                 style="font-size:18px;font-weight:700;color:#a50034;
                        padding:8px 0"></div>
        </div>
        <div class="form-group" style="margin-bottom:20px">
            <label>변경할 기본급 (원)</label>
            <input id="salary-new" type="number" placeholder="예: 5000000">
        </div>
        <div class="form-actions">
            <button class="btn btn-secondary"
                    onclick="document.getElementById('hrSalaryModal')
                             .classList.remove('open')">취소</button>
            <button class="btn btn-primary"
                    onclick="HrSection.submitSalaryChange()">변경</button>
        </div>
    </div>
</div>

<!-- 직원 등록 모달 -->
<div id="hrEmployeeModal" class="modal-overlay">
    <div class="modal" style="width:480px">
        <div class="modal-header">
            <div class="modal-title">직원 등록</div>
            <button class="modal-close"
                    onclick="document.getElementById('hrEmployeeModal')
                             .classList.remove('open')">✕</button>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>사번 *</label>
                <input id="hr-emp-id" placeholder="EMP-006">
            </div>
            <div class="form-group">
                <label>이름 *</label>
                <input id="hr-emp-name" placeholder="홍길동">
            </div>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>부서</label>
                <select id="hr-dept">
                    <option value="영업팀">영업팀</option>
                    <option value="개발팀">개발팀</option>
                    <option value="마케팅팀">마케팅팀</option>
                    <option value="인사팀">인사팀</option>
                    <option value="재무팀">재무팀</option>
                </select>
            </div>
            <div class="form-group">
                <label>직급</label>
                <select id="hr-position">
                    <option value="사원">사원</option>
                    <option value="대리">대리</option>
                    <option value="과장">과장</option>
                    <option value="차장">차장</option>
                    <option value="부장">부장</option>
                </select>
            </div>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>기본급 (원)</label>
                <input id="hr-salary" type="number" placeholder="3500000">
            </div>
            <div class="form-group">
                <label>입사일</label>
                <input id="hr-hire-date" type="date">
            </div>
        </div>
        <div class="form-actions">
            <button class="btn btn-secondary"
                    onclick="document.getElementById('hrEmployeeModal')
                             .classList.remove('open')">취소</button>
            <button class="btn btn-primary"
                    onclick="HrSection.submitEmployee()">등록</button>
        </div>
    </div>
</div>


<!-- ── JS 로드 ────────────────────────────────────────────── -->
<script src="/static/js/api.js"></script>
<script src="/static/js/utils.js"></script>
<script src="/static/js/components.js"></script>
<script src="/static/js/sections/dashboard.js"></script>
<script src="/static/js/sections/orders.js"></script>
<script src="/static/js/sections/countries.js"></script>
<script src="/static/js/sections/batch.js"></script>
<script src="/static/js/sections/exchange.js"></script>
<script src="/static/js/sections/chart.js"></script>
<script src="/static/js/sections/monitor.js"></script>
<script src="/static/js/sections/audit.js"></script>
<script src="/static/js/sections/hr.js"></script>
<script src="/static/js/app.js"></script>

</body>
</html>
