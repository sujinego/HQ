/**
 * sections/orders.js - 주문 관리
 */
var OrdersSection = (function () {

    var currentPage = 1;
    var pageSize    = 10;


    function load(page) {
        currentPage = page || 1;
        var params = {
            page:      currentPage,
            size:      pageSize,
            country:    document.getElementById('order-country')
                        ? document.getElementById('order-country').value : '',
            keyword:   document.getElementById('order-keyword').value,
            status:    document.getElementById('order-status').value,
            startDate: document.getElementById('order-start').value,
            endDate:   document.getElementById('order-end').value
        };

        Utils.renderLoading('orders-table');

        API.orders.getList(params).then(function (d) {
            var list       = d.list || [];
            var total      = d.total || 0;
            var totalPages = d.totalPages || 1;

            if (!list.length) {
                document.getElementById('orders-table').innerHTML = '<div class="empty">검색 결과 없음</div>';
                document.getElementById('orders-paging').innerHTML = '';
                return;
            }
        console.log("list:" ,list);
            var rows = list.map(function (o) {
                var deleteBtn = App.Auth.canDelete()
                    ? '<button onclick="OrdersSection.deleteOrder(\'' + o.order_no + '\')" '
                    +   'class="btn btn-secondary btn-sm perm-delete" '
                    +   'style="color:#dc2626">삭제</button>'
                    : '';

                var changeBtn = App.Auth.canWrite()
                    ? '<button onclick="OrdersSection.changeStatus(\'' + o.order_no + '\',\'' + o.status + '\')" '
                    +   'class="perm-write" '
                    +   'style="font-size:11px;padding:3px 8px;border:1px solid #e2e8f0;border-radius:4px;cursor:pointer;background:#fff">'
                    +   '상태변경</button>'
                    : '';

                return '<tr style="cursor:pointer"'
                    + ' onclick="OrdersSection.openDetail(\'' + o.order_no + '\',\''
                    +   (o._country || 'KR') + '\')">'
                    + '<td>' + (o._country ? '[' + o._country + '] ' : '') + o.order_no + '</td>'
                    + '<td>' + (o.customer_id || '-') + '</td>'
                    + '<td>' + Utils.fmt(o.total_amount) + '</td>'
                    + '<td>' + Utils.orderBadge(o.status) + '</td>'
                    + '<td>' + (o.order_date || '-').substring(0, 10) + '</td>'
                    + '<td onclick="event.stopPropagation()">'  // ← 버튼 클릭 시 모달 안 열리게
                    +   changeBtn + ' ' + deleteBtn
                    + '</td>'
                    + '</tr>';
            }).join('');

            document.getElementById('orders-table').innerHTML =
                '<div style="font-size:12px;color:#64748b;margin-bottom:8px">총 ' + total + '건</div>'
                + '<table id="orders-data">'
                + '<thead><tr><th>주문번호</th><th>고객사</th><th>상품</th><th>금액</th><th>상태</th><th>주문일</th><th>관리</th></tr></thead>'
                + '<tbody>' + rows + '</tbody></table>';

            document.getElementById('orders-paging').innerHTML =
                Components.pagination(currentPage, totalPages, 'OrdersSection.load');

        }).catch(function () {
            Utils.renderError('orders-table', '주문 목록 로드 실패');
        });
    }

    // 상태 필터 적용 후 로드 (KPI 카드 클릭용)
    function loadByStatus(status) {
        App.show('sec-orders');
        document.getElementById('order-status').value = status;
        load(1);  // 기존 load() 재호출
    }

    function openDetail(orderNo, country) {
        API.orders.getDetail(orderNo, country).then(function (d) {
            // 헤더
            document.getElementById('detail-order-no').textContent =
                '주문 상세 - ' + d.order_no;
            document.getElementById('detail-header').innerHTML =
                '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">'
                + '<div>고객사: <b>' + (d.customer_id || '-') + '</b></div>'
                + '<div>상태: '    + Utils.orderBadge(d.status) + '</div>'
                + '<div>주문일: <b>' + (d.order_date || '-') + '</b></div>'
                + '<div>국가: <b>'  + (d._country || '-') + '</b></div>'
                + '</div>';

            // 상품 목록
            var items = d.items || [];
            if (!items.length) {
                document.getElementById('detail-items').innerHTML =
                    '<div class="empty">주문 상품 없음</div>';
            } else {
                var rows = items.map(function (i) {
                    return '<tr>'
                        + '<td>' + i.product_code + '</td>'
                        + '<td style="text-align:center">' + i.quantity + '개</td>'
                        + '<td style="text-align:right">' + Utils.fmt(i.unit_price) + '</td>'
                        + '<td style="text-align:right"><b>' + Utils.fmt(i.subtotal) + '</b></td>'
                        + '</tr>';
                }).join('');
                document.getElementById('detail-items').innerHTML =
                    '<table>'
                    + '<thead><tr><th>상품코드</th><th>수량</th>'
                    +   '<th>단가</th><th>소계</th></tr></thead>'
                    + '<tbody>' + rows + '</tbody>'
                    + '</table>';
            }

            // 합계
            document.getElementById('detail-total').textContent =
                '합계: ' + Utils.fmt(d.total_amount);

            // 모달 열기
            document.getElementById('orderDetailModal').classList.add('open');

        }).catch(function () {
            alert('주문 상세 조회 실패');
        });
    }

    function search() { load(1); }

    function reset() {
        document.getElementById('order-keyword').value = '';
        document.getElementById('order-country').value  = '';
        document.getElementById('order-status').value  = '';
        document.getElementById('order-start').value   = '';
        document.getElementById('order-end').value     = '';
        load(1);
    }

    function changeStatus(orderNo, currentStatus) {
        var statuses = ['PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED'];
        var newStatus = prompt(
            orderNo + ' 상태 변경\n현재: ' + currentStatus
            + '\n\n변경할 상태 입력:\nPENDING / CONFIRMED / SHIPPING / COMPLETED / CANCELLED'
        );
        if (!newStatus) return;
        newStatus = newStatus.trim().toUpperCase();
        if (!statuses.includes(newStatus)) { alert('올바른 상태를 입력하세요.'); return; }

        API.orders.changeStatus(orderNo, newStatus).then(function (res) {
            if (res.success) { alert('상태가 변경되었습니다.'); load(currentPage); }
            else             { alert('실패: ' + res.message); }
        });
    }

    // ── 주문 등록 모달 ───────────────────────────────────────
    function openModal() {
        document.getElementById('f-order-date').value = Utils.today();
        document.getElementById('f-order-no').value   = 'KR-ORD-' + new Date().getFullYear() + '-' + String(Date.now()).slice(-4);
        document.getElementById('orderModal').classList.add('open');
    }

    function closeModal() {
        document.getElementById('orderModal').classList.remove('open');
    }

    function submitOrder() {
        var orderNo    = document.getElementById('f-order-no').value;
        var customerId = document.getElementById('f-customer-id').value;
        var productCode= document.getElementById('f-product-code').value;
        var quantity   = Number(document.getElementById('f-quantity').value);
        var unitPrice  = Number(document.getElementById('f-unit-price').value);
        var orderDate  = document.getElementById('f-order-date').value;
        var status     = document.getElementById('f-status').value;

        if (!orderNo || !unitPrice) { alert('주문번호와 단가를 입력해주세요.'); return; }

        API.orders.create({
            orderNo, customerId, productCode,
            quantity, unitPrice,
            totalAmount: quantity * unitPrice,
            status, orderDate
        }).then(function (res) {
            if (res.success) { alert('주문이 등록되었습니다.'); closeModal(); load(1); }
            else             { alert('등록 실패: ' + res.message); }
        }).catch(function (e) { alert('오류: ' + e.message); });
    }



    return { load: load,  loadByStatus: loadByStatus,openDetail:openDetail , search:search, reset: reset, changeStatus: changeStatus, openModal: openModal, closeModal: closeModal, submitOrder: submitOrder };

})();
