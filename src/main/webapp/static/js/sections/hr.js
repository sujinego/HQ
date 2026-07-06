/**
 * sections/hr.js - 인사 관리
 */
var HrSection = (function () {

    function load() {
        loadEmployees();
        loadAttendance();
        loadPayroll();
    }

    // ── 직원 목록 ─────────────────────────────────────
    function loadEmployees() {
        Utils.renderLoading('hr-employees');
        API.hr.getEmployees().then(function (res) {
            // res가 배열이면 그대로, 아니면 data 꺼내기
            var raw  = Array.isArray(res) ? res : (res || []);
            // 소문자 키로 변환
            var list = raw.map(function (e) {
                var r = {};
                Object.keys(e).forEach(function (k) { r[k.toLowerCase()] = e[k]; });
                return r;
            });

            if (!list.length) {
                document.getElementById('hr-employees').innerHTML =
                    '<div class="empty">직원 없음</div>';
                return;
            }

            var rows = list.map(function (e) {
                var statusBadge = e.status === 'ACTIVE'
                    ? '<span class="status s-success">재직</span>'
                    : e.status === 'LEAVE'
                        ? '<span class="status s-warn">휴직</span>'
                        : '<span class="status s-fail">퇴직</span>';

                var salaryBtn = App.Auth.isAdmin()
                    ? '<button onclick="HrSection.openSalaryModal(\''
                    +     e.emp_id + '\',' + e.base_salary + ')" '
                    +   'style="font-size:11px;padding:2px 8px;border:1px solid #e2e8f0;'
                    +   'border-radius:4px;cursor:pointer;background:#fff">급여변경</button>'
                    : '';

                return '<tr>'
                    + '<td>' + e.emp_id + '</td>'
                    + '<td><b>' + e.emp_name + '</b></td>'
                    + '<td>' + (e.department || '-') + '</td>'
                    + '<td>' + (e.position || '-') + '</td>'
                    + '<td style="text-align:right">'
                    +   Utils.fmt(e.base_salary) + '</td>'
                    + '<td>' + statusBadge + '</td>'
                    + '<td>' + (e.hire_date || '-').substring(0, 10) + '</td>'
                    + '<td>' + salaryBtn + '</td>'
                    + '</tr>';
            }).join('');

            document.getElementById('hr-employees').innerHTML =
                '<table id="hr-emp-data">'
                + '<thead><tr>'
                +   '<th>사번</th><th>이름</th><th>부서</th><th>직급</th>'
                +   '<th>기본급</th><th>상태</th><th>입사일</th><th>관리</th>'
                + '</tr></thead>'
                + '<tbody>' + rows + '</tbody>'
                + '</table>';
        }).catch(function () {
            Utils.renderError('hr-employees', '직원 목록 로드 실패');
        });
    }

    // ── 근태 현황 ─────────────────────────────────────
    function loadAttendance(yearMonth) {
        Utils.renderLoading('hr-attendance');
        API.hr.getAttendance(yearMonth).then(function (res) {
            var d    = res || {};
            var list = (d.list || []).map(function (a) {
                var r = {};
                Object.keys(a).forEach(function (k) { r[k.toLowerCase()] = a[k]; });
                return r;
            });

            // 요약
            document.getElementById('hr-att-summary').innerHTML =
                '<div style="display:flex;gap:20px;font-size:13px;margin-bottom:12px">'
                + '<span>📅 <b>' + (d.yearmonth || d.yearMonth || '-') + '</b></span>'
                + '<span>총 <b>' + (d.totalrecords || d.totalRecords || 0) + '</b>건</span>'
                + '<span style="color:#dc2626">결근 <b>' + (d.absentcount || d.absentCount || 0) + '</b>명</span>'
                + '<span style="color:#eab308">지각 <b>' + (d.latecount || d.lateCount || 0) + '</b>명</span>'
                + '</div>';

            if (!list.length) {
                document.getElementById('hr-attendance').innerHTML =
                    '<div class="empty">근태 데이터 없음</div>';
                return;
            }

            var statusMap = {
                NORMAL: '<span class="status s-success">정상</span>',
                LATE:   '<span class="status s-warn">지각</span>',
                ABSENT: '<span class="status s-fail">결근</span>',
                HALF:   '<span class="status s-warn">반차</span>'
            };

            var rows = list.map(function (a) {
                return '<tr>'
                    + '<td>' + (a.work_date || '-').substring(0, 10) + '</td>'
                    + '<td>' + (a.emp_id    || '-') + '</td>'
                    + '<td><b>' + (a.emp_name  || '-') + '</b></td>'
                    + '<td>' + (a.department || '-') + '</td>'
                    + '<td>' + (a.check_in  || '-') + '</td>'
                    + '<td>' + (a.check_out || '-') + '</td>'
                    + '<td style="text-align:center">' + (a.work_hours || 0) + 'h</td>'
                    + '<td style="text-align:center;color:#e57300">'
                    +   (a.overtime_hours || 0) + 'h</td>'
                    + '<td>' + (statusMap[a.status] || a.status || '-') + '</td>'
                    + '</tr>';
            }).join('');

            document.getElementById('hr-attendance').innerHTML =
                '<table id="hr-att-data">'
                + '<thead><tr>'
                +   '<th>날짜</th><th>사번</th><th>이름</th><th>부서</th>'
                +   '<th>출근</th><th>퇴근</th><th>근무</th><th>초과</th><th>상태</th>'
                + '</tr></thead>'
                + '<tbody>' + rows + '</tbody>'
                + '</table>';
        }).catch(function () {
            Utils.renderError('hr-attendance', '근태 데이터 로드 실패');
        });
    }

    // ── 급여 현황 ─────────────────────────────────────
    function loadPayroll(yearMonth) {
        Utils.renderLoading('hr-payroll');
        API.hr.getPayroll(yearMonth).then(function (res) {
            var d       = res || {};
            var list    = (d.list || []).map(function (p) {
                var r = {};
                Object.keys(p).forEach(function (k) { r[k.toLowerCase()] = p[k]; });
                return r;
            });
            var summary = d.summary || {};
            // summary도 소문자로
            var sum = {};
            Object.keys(summary).forEach(function (k) { sum[k.toLowerCase()] = summary[k]; });


            // 급여 요약
            document.getElementById('hr-pay-summary').innerHTML =
                '<div style="display:flex;gap:20px;font-size:13px;'
                +   'background:#f8fafc;padding:12px;border-radius:8px;margin-bottom:12px">'
                + '<span>📅 <b>' + (d.yearmonth || d.yearMonth || '-') + '</b></span>'
                + '<span>대상 <b>' + (sum.total_employees || 0) + '</b>명</span>'
                + '<span>총지급 <b style="color:#1d4ed8">'
                +   Utils.fmt(sum.total_gross) + '</b></span>'
                + '<span>총공제 <b style="color:#dc2626">'
                +   Utils.fmt(sum.total_deduction) + '</b></span>'
                + '<span>실지급 <b style="color:#15803d">'
                +   Utils.fmt(sum.total_net) + '</b></span>'
                + (App.Auth.isAdmin()
                    ? '<button onclick="HrSection.markAsPaid(\'' + (d.yearmonth || d.yearMonth) + '\')" '
                    +   'class="btn btn-green btn-sm" style="margin-left:auto">'
                    +   '✓ 지급완료 처리</button>'
                    : '')
                + '</div>';

            if (!list.length) {
                document.getElementById('hr-payroll').innerHTML =
                    '<div class="empty">급여 데이터 없음 (배치 실행 필요)</div>';
                return;
            }

            var rows = list.map(function (p) {
                var paidBadge = p.status === 'PAID'
                    ? '<span class="status s-success">지급완료</span>'
                    : '<span class="status s-warn">계산완료</span>';

                return '<tr>'
                    + '<td>' + (p.emp_id || '-') + '</td>'
                    + '<td><b>' + (p.emp_name || '-') + '</b></td>'
                    + '<td>' + (p.department || '-') + '</td>'
                    + '<td style="text-align:right">' + Utils.fmt(p.base_salary) + '</td>'
                    + '<td style="text-align:right;color:#e57300">'
                    +   Utils.fmt(p.overtime_pay) + '</td>'
                    + '<td style="text-align:right">' + Utils.fmt(p.gross_pay) + '</td>'
                    + '<td style="text-align:right;color:#dc2626">'
                    +   Utils.fmt(p.total_deduction) + '</td>'
                    + '<td style="text-align:right;font-weight:700;color:#15803d">'
                    +   Utils.fmt(p.net_pay) + '</td>'
                    + '<td>' + paidBadge + '</td>'
                    + '<td>'
                    +   '<button onclick="HrSection.loadPayrollHistory(\''
                    +       p.emp_id + '\')" '
                    +   'style="font-size:11px;padding:2px 8px;border:1px solid #e2e8f0;'
                    +   'border-radius:4px;cursor:pointer;background:#fff">이력</button>'
                    + '</td>'
                    + '</tr>';
            }).join('');

            document.getElementById('hr-payroll').innerHTML =
                '<table id="hr-pay-data">'
                + '<thead><tr>'
                +   '<th>사번</th><th>이름</th><th>부서</th>'
                +   '<th>기본급</th><th>초과수당</th><th>총지급</th>'
                +   '<th>공제액</th><th>실지급</th><th>상태</th><th>이력</th>'
                + '</tr></thead>'
                + '<tbody>' + rows + '</tbody>'
                + '</table>';
        }).catch(function () {
            Utils.renderError('hr-payroll', '급여 데이터 로드 실패');
        });
    }

    // ── 급여 이력 모달 ────────────────────────────────
    function loadPayrollHistory(empId) {
        API.hr.getPayrollHistory(empId).then(function (res) {
            var list = res.data || [];
            if (!list.length) { alert('급여 이력 없음'); return; }

            var rows = list.map(function (p) {
                return '<tr>'
                    + '<td>' + p.pay_year_month + '</td>'
                    + '<td>' + Utils.fmt(p.base_salary) + '</td>'
                    + '<td>' + Utils.fmt(p.overtime_pay) + '</td>'
                    + '<td>' + Utils.fmt(p.gross_pay) + '</td>'
                    + '<td style="color:#dc2626">'
                    +   Utils.fmt(p.total_deduction) + '</td>'
                    + '<td style="font-weight:700;color:#15803d">'
                    +   Utils.fmt(p.net_pay) + '</td>'
                    + '<td>' + (p.status === 'PAID'
                        ? '<span class="status s-success">지급완료</span>'
                        : '<span class="status s-warn">계산완료</span>')
                    + '</td>'
                    + '</tr>';
            }).join('');

            document.getElementById('hr-history-name').textContent =
                list[0].emp_name + ' 급여 이력';
            document.getElementById('hr-history-body').innerHTML =
                '<table>'
                + '<thead><tr><th>년월</th><th>기본급</th><th>초과수당</th>'
                +   '<th>총지급</th><th>공제액</th><th>실지급</th><th>상태</th></tr></thead>'
                + '<tbody>' + rows + '</tbody>'
                + '</table>';

            document.getElementById('hrHistoryModal').classList.add('open');
        });
    }

    // ── 급여 지급 완료 ────────────────────────────────
    function markAsPaid(yearMonth) {
        if (!confirm(yearMonth + ' 급여를 지급완료 처리하시겠습니까?\n' +
            '이 작업은 되돌릴 수 없습니다.')) return;
        API.hr.markAsPaid(yearMonth).then(function (res) {
            alert(res.message);
            loadPayroll(yearMonth);
        });
    }

    // ── 급여 변경 모달 ────────────────────────────────
    function openSalaryModal(empId, currentSalary) {
        document.getElementById('salary-emp-id').value      = empId;
        document.getElementById('salary-current').textContent =
            Utils.fmt(currentSalary);
        document.getElementById('salary-new').value = currentSalary;
        document.getElementById('hrSalaryModal').classList.add('open');
    }

    function submitSalaryChange() {
        var empId  = document.getElementById('salary-emp-id').value;
        var salary = Number(document.getElementById('salary-new').value);
        if (!salary || salary <= 0) { alert('올바른 금액을 입력하세요.'); return; }

        API.hr.updateSalary(empId, salary).then(function (res) {
            alert(res.message);
            document.getElementById('hrSalaryModal').classList.remove('open');
            loadEmployees();
        });
    }

    // ── 직원 등록 모달 ────────────────────────────────
    function openEmployeeModal() {
        document.getElementById('hrEmployeeModal').classList.add('open');
    }

    function submitEmployee() {
        var body = {
            empId:      document.getElementById('hr-emp-id').value,
            empName:    document.getElementById('hr-emp-name').value,
            department: document.getElementById('hr-dept').value,
            position:   document.getElementById('hr-position').value,
            baseSalary: Number(document.getElementById('hr-salary').value),
            hireDate:   document.getElementById('hr-hire-date').value
        };
        if (!body.empId || !body.empName) {
            alert('사번과 이름은 필수입니다.');
            return;
        }
        API.hr.createEmployee(body).then(function (res) {
            alert(res.message);
            document.getElementById('hrEmployeeModal').classList.remove('open');
            loadEmployees();
        });
    }

    return {
        load:               load,
        loadEmployees:      loadEmployees,
        loadAttendance:     loadAttendance,
        loadPayroll:        loadPayroll,
        loadPayrollHistory: loadPayrollHistory,
        markAsPaid:         markAsPaid,
        openSalaryModal:    openSalaryModal,
        submitSalaryChange: submitSalaryChange,
        openEmployeeModal:  openEmployeeModal,
        submitEmployee:     submitEmployee
    };

})();