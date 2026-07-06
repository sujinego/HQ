<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>LG B2B Global Portal - 로그인</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { background: #1a1a2e; display: flex; align-items: center;
            justify-content: center; min-height: 100vh;
            font-family: 'Malgun Gothic', sans-serif; }

        .login-box { background: #fff; border-radius: 16px; padding: 40px;
            width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }

        .logo { text-align: center; margin-bottom: 28px; }
        .logo-lg  { font-size: 36px; font-weight: 900; color: #a50034; }
        .logo-sub { font-size: 13px; color: #64748b; margin-top: 4px; }

        .form-group { margin-bottom: 16px; }
        .form-group label { display: block; font-size: 12px; font-weight: 600;
            color: #374151; margin-bottom: 6px; }
        .form-group input { width: 100%; padding: 11px 14px;
            border: 1px solid #e2e8f0; border-radius: 8px;
            font-size: 14px; outline: none; transition: border-color 0.2s; }
        .form-group input:focus { border-color: #a50034; }

        .btn-login { width: 100%; padding: 12px; background: #a50034;
            color: #fff; border: none; border-radius: 8px;
            font-size: 15px; font-weight: 700; cursor: pointer; margin-top: 8px; }
        .btn-login:hover { background: #8b0029; }

        .alert-error  { background: #fee2e2; color: #dc2626; padding: 10px 14px;
            border-radius: 8px; font-size: 13px; margin-bottom: 16px; text-align: center; }
        .alert-success{ background: #dcfce7; color: #16a34a; padding: 10px 14px;
            border-radius: 8px; font-size: 13px; margin-bottom: 16px; text-align: center; }

        /* 권한 안내 */
        .role-hint { margin-top: 20px; border-radius: 10px; overflow: hidden;
            border: 1px solid #e2e8f0; }
        .role-hint-title { background: #f8fafc; padding: 10px 14px;
            font-size: 11px; font-weight: 700; color: #64748b;
            text-transform: uppercase; letter-spacing: 0.5px; }
        .role-item { display: flex; align-items: flex-start; padding: 10px 14px;
            border-top: 1px solid #f1f5f9; gap: 10px; }
        .role-badge { font-size: 11px; font-weight: 700; padding: 2px 8px;
            border-radius: 20px; white-space: nowrap; margin-top: 1px; }
        .badge-admin { background: #fef3c7; color: #92400e; }
        .badge-kr    { background: #dbeafe; color: #1e40af; }
        .badge-us    { background: #dcfce7; color: #166534; }
        .role-info { flex: 1; }
        .role-id   { font-size: 13px; font-weight: 600; color: #1a202c; }
        .role-pw   { font-size: 11px; color: #94a3b8; margin-top: 1px; }
        .role-perms{ margin-top: 6px; display: flex; flex-wrap: wrap; gap: 4px; }
        .perm { font-size: 10px; padding: 1px 6px; border-radius: 4px; }
        .perm-allow { background: #dcfce7; color: #15803d; }
        .perm-deny  { background: #fee2e2; color: #dc2626;
            text-decoration: line-through; opacity: 0.7; }

        .divider { height: 1px; background: #f1f5f9; margin: 20px 0; }
    </style>
</head>
<body>
<div class="login-box">

    <div class="logo">
        <div class="logo-lg">LG</div>
        <div class="logo-sub">B2B Global Commerce Portal</div>
    </div>

    <!-- 에러/로그아웃 메시지 -->
    <% if (request.getParameter("error") != null) { %>
    <div class="alert-error">
        ⚠️ 아이디 또는 비밀번호가 올바르지 않습니다.
    </div>
    <% } %>
    <% if (request.getParameter("logout") != null) { %>
    <div class="alert-success">
        ✓ 로그아웃되었습니다.
    </div>
    <% } %>

    <!-- 로그인 폼 -->
    <form method="POST" action="/login">
        <div class="form-group">
            <label>아이디</label>
            <input type="text" name="username"
                   placeholder="아이디를 입력하세요" autofocus>
        </div>
        <div class="form-group">
            <label>비밀번호</label>
            <input type="password" name="password"
                   placeholder="비밀번호를 입력하세요">
        </div>
        <button type="submit" class="btn-login">로그인</button>
    </form>

    <div class="divider"></div>

    <!-- 권한별 계정 안내 -->
    <div class="role-hint">
        <div class="role-hint-title">🔑 테스트 계정 및 권한 안내</div>

        <!-- ADMIN -->
        <div class="role-item">
            <span class="role-badge badge-admin">ADMIN</span>
            <div class="role-info">
                <div class="role-id">admin
                    <span class="role-pw">/ admin123</span>
                </div>
                <div class="role-perms">
                    <span class="perm perm-allow">전체 조회</span>
                    <span class="perm perm-allow">주문 등록</span>
                    <span class="perm perm-allow">상태 변경</span>
                    <span class="perm perm-allow">주문 삭제</span>
                    <span class="perm perm-allow">배치 실행</span>
                </div>
            </div>
        </div>

        <!-- KR_USER -->
        <div class="role-item">
            <span class="role-badge badge-kr">🇰🇷 KR</span>
            <div class="role-info">
                <div class="role-id">kr_user
                    <span class="role-pw">/ admin123</span>
                </div>
                <div class="role-perms">
                    <span class="perm perm-allow">전체 조회</span>
                    <span class="perm perm-allow">주문 등록</span>
                    <span class="perm perm-allow">상태 변경</span>
                    <span class="perm perm-deny">주문 삭제</span>
                    <span class="perm perm-deny">배치 실행</span>
                </div>
            </div>
        </div>

        <!-- US_USER -->
        <div class="role-item">
            <span class="role-badge badge-us">🇺🇸 US</span>
            <div class="role-info">
                <div class="role-id">us_user
                    <span class="role-pw">/ admin123</span>
                </div>
                <div class="role-perms">
                    <span class="perm perm-allow">전체 조회</span>
                    <span class="perm perm-deny">주문 등록</span>
                    <span class="perm perm-deny">상태 변경</span>
                    <span class="perm perm-deny">주문 삭제</span>
                    <span class="perm perm-deny">배치 실행</span>
                </div>
            </div>
        </div>
    </div>

</div>
</body>
</html>