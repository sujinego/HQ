/**
 * sections/countries.js - 국가 현황
 */
var CountriesSection = (function () {

    function load() {
        loadStatus();
        loadDataQuality();
    }

    function loadStatus() {
        Utils.renderLoading('countries-status');
        API.countries.getStatus().then(function (list) {
            var html = (list || []).map(Components.countryStatusRow).join('');
            document.getElementById('countries-status').innerHTML =
                html || '<div class="empty">조회된 인프라가 없습니다.</div>';
        }).catch(function () {
            Utils.renderError('countries-status', '인프라 관제 데이터 로드 실패');
        });
    }

    function loadDataQuality() {
        Utils.renderLoading('data-quality');
        API.countries.getDataQuality().then(function (list) {
            var html = (list || []).map(Components.dataQualityRow).join('');
            document.getElementById('data-quality').innerHTML =
                html || '<div class="empty">품질 분석 데이터가 없습니다.</div>';
        }).catch(function () {
            Utils.renderError('data-quality', '데이터 품질 지표 로드 실패');
        });
    }

    return { load: load };

})();
