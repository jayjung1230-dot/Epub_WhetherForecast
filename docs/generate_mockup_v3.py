import os
import math
from PIL import Image, ImageDraw
from generate_mockup import (
    OUT_DIR,
    font,
    draw_rounded_box,
    draw_common_top,
    draw_footer,
)


def wrap_text(draw, text, fnt, max_w):
    lines = []
    cur = ""
    for ch in text:
        test = cur + ch
        bb = draw.textbbox((0, 0), test, font=fnt)
        if bb[2] - bb[0] > max_w and cur:
            lines.append(cur.rstrip())
            cur = ch.lstrip()
        else:
            cur = test
    if cur:
        lines.append(cur)
    return lines


def draw_refresh_btn(d, x2, y1, size=44):
    x1, y2 = x2 - size, y1 + size
    draw_rounded_box(d, [x1, y1, x2, y2], radius=12, fill=245, outline=0, width=2)
    cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
    r = 12
    d.arc([cx - r, cy - r, cx + r, cy + r], start=20, end=310, fill=0, width=3)
    ax = cx + r * math.cos(math.radians(-50))
    ay = cy + r * math.sin(math.radians(-50))
    d.line([(ax, ay), (ax - 7, ay - 2)], fill=0, width=3)
    d.line([(ax, ay), (ax + 1, ay - 7)], fill=0, width=3)
    return x1


def build_page4_books():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "오늘의 책 추천", font=font(32, True), fill=0)

    ref_x1 = draw_refresh_btn(d, W - M - 32, box_top + 22, 44)
    cat = "소설 · 인문"
    cb = d.textbbox((0, 0), cat, font=font(21, True))
    cw = (cb[2] - cb[0]) + 32
    draw_rounded_box(d, [ref_x1 - 12 - cw, box_top + 22, ref_x1 - 12, box_top + 66], radius=12, fill=24, outline=0, width=2)
    d.text((ref_x1 - 12 - cw + 16, box_top + 30), cat, font=font(21, True), fill=255)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    # Book Cover Artwork (Left)
    cov_x1, cov_y1 = M + 48, box_top + 124
    cov_w, cov_h = 330, 480
    cov_x2, cov_y2 = cov_x1 + cov_w, cov_y1 + cov_h
    # Drop shadow for book
    draw_rounded_box(d, [cov_x1 + 10, cov_y1 + 10, cov_x2 + 10, cov_y2 + 10], radius=12, fill=210, outline=210, width=1)
    # Book cover frame
    draw_rounded_box(d, [cov_x1, cov_y1, cov_x2, cov_y2], radius=12, fill=244, outline=0, width=3)
    # Spine line
    d.line([(cov_x1 + 22, cov_y1 + 4), (cov_x1 + 22, cov_y2 - 4)], fill=120, width=2)
    # Stylized monochrome book cover art (moon + mountains + waves)
    art_box = [cov_x1 + 44, cov_y1 + 36, cov_x2 - 26, cov_y1 + 270]
    d.rectangle(art_box, fill=30, outline=0, width=2)
    d.ellipse([cov_x1 + 160, cov_y1 + 64, cov_x1 + 230, cov_y1 + 134], fill=245)
    d.polygon([(cov_x1 + 46, cov_y1 + 268), (cov_x1 + 135, cov_y1 + 150), (cov_x1 + 215, cov_y1 + 268)], fill=130)
    d.polygon([(cov_x1 + 145, cov_y1 + 268), (cov_x1 + 235, cov_y1 + 172), (cov_x2 - 28, cov_y1 + 268)], fill=185)
    # Title on cover
    d.text((cov_x1 + 46, cov_y1 + 300), "물고기는", font=font(34, True), fill=0)
    d.text((cov_x1 + 46, cov_y1 + 344), "존재하지 않는다", font=font(34, True), fill=0)
    d.line([(cov_x1 + 46, cov_y1 + 402), (cov_x2 - 34, cov_y1 + 402)], fill=140, width=2)
    d.text((cov_x1 + 46, cov_y1 + 422), "룰루 밀러 지음", font=font(22, False), fill=60)

    # Book Details (Right)
    rx = cov_x2 + 52
    max_rw = W - M - 44 - rx
    d.text((rx, cov_y1 + 4), "물고기는 존재하지 않는다", font=font(42, True), fill=0)
    d.text((rx, cov_y1 + 64), "룰루 밀러 (Lulu Miller) · 곰출판 · 2021", font=font(25, False), fill=75)

    # Rating / Tags row
    tags = ["★ 4.8 / 5.0", "과학 에세이", "300쪽 · 완독 4시간"]
    tx = rx
    for tg in tags:
        tb = d.textbbox((0, 0), tg, font=font(20, True))
        tw = (tb[2] - tb[0]) + 28
        draw_rounded_box(d, [tx, cov_y1 + 114, tx + tw, cov_y1 + 152], radius=8, fill=240, outline=0, width=2)
        d.text((tx + 14, cov_y1 + 121), tg, font=font(20, True), fill=20)
        tx += tw + 12

    d.text((rx, cov_y1 + 186), "■ 책 소개 & 추천 이유", font=font(24, True), fill=20)
    intro = (
        "과학 전문 기자 룰루 밀러가 집착에 가까운 열정으로 '질서'를 세우려 했던 "
        "분류학자 데이비드 스타 조던의 삶을 추적하며, 우리가 당연하게 믿어온 "
        "세상의 기준과 삶의 의미를 다시 묻는 매혹적인 논픽션입니다."
    )
    cy = cov_y1 + 226
    for line in wrap_text(d, intro, font(25, False), max_rw):
        d.text((rx, cy), line, font=font(25, False), fill=45)
        cy += 38

    # Highlight quote from the book
    q_top = cov_y1 + 392
    draw_rounded_box(d, [rx, q_top, W - M - 44, cov_y2], radius=14, fill=246, outline=0, width=2)
    d.text((rx + 22, q_top + 16), "책 속의 한 문장", font=font(20, True), fill=80)
    d.text((rx + 22, q_top + 48), "“혼돈은 우리가 생각하는 것보다 훨씬 자비롭다.”", font=font(26, True), fill=0)

    # Bottom bar inside card
    d.line([(M + 48, box_bot - 104), (W - M - 48, box_bot - 104)], fill=210, width=2)
    d.text((M + 48, box_bot - 72), "· 추천 키워드: #질서와혼돈  #자기이해  #주말독서", font=font(23, False), fill=70)

    draw_footer(d, W, M, 4, "책 추천")
    out_path = os.path.join(OUT_DIR, "sample_page4_books_v3.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_page5_movies():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "오늘의 영화 소개", font=font(32, True), fill=0)

    ref_x1 = draw_refresh_btn(d, W - M - 32, box_top + 22, 44)
    cat = "드라마 · 모험"
    cb = d.textbbox((0, 0), cat, font=font(21, True))
    cw = (cb[2] - cb[0]) + 32
    draw_rounded_box(d, [ref_x1 - 12 - cw, box_top + 22, ref_x1 - 12, box_top + 66], radius=12, fill=24, outline=0, width=2)
    d.text((ref_x1 - 12 - cw + 16, box_top + 30), cat, font=font(21, True), fill=255)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    # Movie Poster (Left)
    px1, py1 = M + 48, box_top + 124
    pw, ph = 330, 480
    px2, py2 = px1 + pw, py1 + ph
    draw_rounded_box(d, [px1, py1, px2, py2], radius=14, fill=22, outline=0, width=3)
    # Film perforation accents on poster top/bottom
    for fx in range(px1 + 22, px2 - 22, 34):
        d.rectangle([fx, py1 + 12, fx + 18, py1 + 22], fill=235)
        d.rectangle([fx, py2 - 22, fx + 18, py2 - 12], fill=235)
    # Stylized mountain & traveler silhouette inside poster
    d.ellipse([px1 + 105, py1 + 78, px1 + 225, py1 + 198], fill=235)
    d.polygon([(px1 + 16, py1 + 310), (px1 + 125, py1 + 185), (px1 + 240, py1 + 310)], fill=120)
    d.polygon([(px1 + 120, py1 + 310), (px1 + 245, py1 + 205), (px2 - 16, py1 + 310)], fill=165)
    d.text((px1 + 30, py1 + 332), "WALTER", font=font(40, True), fill=255)
    d.text((px1 + 30, py1 + 378), "MITTY", font=font(40, True), fill=255)
    d.text((px1 + 30, py1 + 428), "BEN STILLER · 2013", font=font(19, False), fill=195)

    # Movie Details (Right)
    rx = px2 + 52
    max_rw = W - M - 44 - rx
    d.text((rx, py1 + 4), "월터의 상상은 현실이 된다", font=font(40, True), fill=0)
    d.text((rx, py1 + 62), "The Secret Life of Walter Mitty (2013)", font=font(24, False), fill=80)

    chips = ["★ IMDb 7.3", "감독: 벤 스틸러", "114분 · 전체관람가"]
    cx = rx
    for ch in chips:
        tb = d.textbbox((0, 0), ch, font=font(20, True))
        tw = (tb[2] - tb[0]) + 28
        draw_rounded_box(d, [cx, py1 + 110, cx + tw, py1 + 148], radius=8, fill=240, outline=0, width=2)
        d.text((cx + 14, py1 + 117), ch, font=font(20, True), fill=20)
        cx += tw + 12

    d.text((rx, py1 + 182), "■ 작품 줄거리 & 감상 포인트", font=font(24, True), fill=20)
    synopsis = (
        "잡지사 '라이프'에서 16년째 필름을 관리하며 소심한 일상을 보내던 월터가, "
        "폐간호 표지용 마지막 필름을 찾기 위해 그린란드와 아이슬란드, 히말라야로 "
        "직접 떠나며 진짜 삶을 마주하는 이야기입니다."
    )
    cy = py1 + 222
    for line in wrap_text(d, synopsis, font(25, False), max_rw):
        d.text((rx, cy), line, font=font(25, False), fill=45)
        cy += 38

    q_top = py1 + 392
    draw_rounded_box(d, [rx, q_top, W - M - 44, py2], radius=14, fill=246, outline=0, width=2)
    d.text((rx + 22, q_top + 16), "추천 관람 포인트", font=font(20, True), fill=80)
    d.text((rx + 22, q_top + 48), "광활한 아이슬란드 풍경과 가슴 뛰는 OST, 용기의 메시지", font=font(24, True), fill=0)

    d.line([(M + 48, box_bot - 104), (W - M - 48, box_bot - 104)], fill=210, width=2)
    d.text((M + 48, box_bot - 72), "· 출연: 벤 스틸러, 크리스틴 위그, 숀 펜  |  OTT 스트리밍 추천", font=font(23, False), fill=70)

    draw_footer(d, W, M, 5, "영화 소개")
    out_path = os.path.join(OUT_DIR, "sample_page5_movies_v3.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_page6_economy():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "주요 경제지표", font=font(32, True), fill=0)

    ref_x1 = draw_refresh_btn(d, W - M - 32, box_top + 22, 44)
    d.text((ref_x1 - 245, box_top + 32), "최근 7일 추이 · 기준", font=font(22, False), fill=85)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    indicators = [
        ("코스피 (KOSPI)", "2,486.35", "▲ +18.42 (+0.75%)", [2440, 2455, 2448, 2462, 2470, 2468, 2486]),
        ("코스닥 (KOSDAQ)", "712.80", "▼ -3.15 (-0.44%)", [726, 722, 719, 721, 715, 716, 713]),
        ("원/달러 환율", "1,394.50", "▲ +4.20 (+0.30%)", [1382, 1385, 1388, 1386, 1391, 1390, 1395]),
        ("S&P 500", "5,948.71", "▲ +24.10 (+0.41%)", [5870, 5895, 5910, 5902, 5925, 5924, 5949]),
        ("나스닥 (NASDAQ)", "19,107.65", "▲ +112.4 (+0.59%)", [18820, 18900, 18875, 18960, 19010, 18995, 19108]),
        ("국제 금 (Gold)", "$2,572.4", "▼ -8.60 (-0.33%)", [2615, 2602, 2595, 2588, 2590, 2581, 2572]),
    ]

    cols, rows = 2, 3
    gap_x, gap_y = 24, 22
    pad_x, pad_top, pad_bot = 36, 108, 28
    card_w = (W - 2 * M - 2 * pad_x - gap_x) / cols
    card_h = (box_bot - box_top - pad_top - pad_bot - gap_y * (rows - 1)) / rows

    for idx, (name, val, diff, series) in enumerate(indicators):
        r = idx // cols
        c = idx % cols
        x1 = int(M + pad_x + c * (card_w + gap_x))
        y1 = int(box_top + pad_top + r * (card_h + gap_y))
        x2 = int(x1 + card_w)
        y2 = int(y1 + card_h)

        draw_rounded_box(d, [x1, y1, x2, y2], radius=18, fill=249, outline=0, width=2)
        d.text((x1 + 24, y1 + 18), name, font=font(24, True), fill=55)
        d.text((x1 + 24, y1 + 56), val, font=font(42, True), fill=0)

        is_up = diff.startswith("▲")
        pill_fill = 24 if is_up else 232
        pill_txt = 255 if is_up else 20
        db = d.textbbox((0, 0), diff, font=font(20, True))
        dw = (db[2] - db[0]) + 24
        draw_rounded_box(d, [x1 + 24, y1 + 114, x1 + 24 + dw, y1 + 148], radius=8, fill=pill_fill, outline=0, width=2)
        d.text((x1 + 36, y1 + 120), diff, font=font(20, True), fill=pill_txt)

        # Sparkline Graph on right side of card
        gx1, gy1 = x1 + 310, y1 + 28
        gx2, gy2 = x2 - 26, y2 - 28
        draw_rounded_box(d, [gx1 - 12, gy1 - 10, gx2 + 12, gy2 + 10], radius=12, fill=255, outline=190, width=1)

        s_min, s_max = min(series), max(series)
        s_span = max(1e-6, s_max - s_min)
        pts = []
        for i, v in enumerate(series):
            px = gx1 + i * (gx2 - gx1) / (len(series) - 1)
            py = gy2 - ((v - s_min) / s_span) * (gy2 - gy1)
            pts.append((px, py))

        # Baseline
        mid_y = (gy1 + gy2) / 2
        for dx in range(int(gx1), int(gx2), 10):
            d.line([(dx, mid_y), (min(dx + 4, int(gx2)), mid_y)], fill=220, width=1)

        for i in range(len(pts) - 1):
            d.line([pts[i], pts[i + 1]], fill=20, width=4)
        for i, (px, py) in enumerate(pts):
            if i == len(pts) - 1:
                d.ellipse([px - 6, py - 6, px + 6, py + 6], fill=0)
            else:
                d.ellipse([px - 3, py - 3, px + 3, py + 3], fill=255, outline=40, width=2)

    draw_footer(d, W, M, 6, "주요 경제지표")
    out_path = os.path.join(OUT_DIR, "sample_page6_economy_v3.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_settings_toggle_sample():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    # Dim background slightly to highlight Settings dialog
    overlay = Image.new("L", (W, H), 235)
    img = Image.blend(img, overlay, 0.35)
    d = ImageDraw.Draw(img)

    # 80% width Settings Dialog
    dw = int(W * 0.80)
    dx1 = (W - dw) // 2
    dx2 = dx1 + dw
    dy1, dy2 = 340, 1490
    draw_rounded_box(d, [dx1, dy1, dx2, dy2], radius=24, fill=255, outline=0, width=4)
    d.text((dx1 + 40, dy1 + 32), "대시보드 설정", font=font(34, True), fill=0)
    d.line([(dx1 + 32, dy1 + 88), (dx2 - 32, dy1 + 88)], fill=170, width=2)

    # Compact 2-column top settings (existing)
    col_w = (dw - 104) // 2
    fields = [
        ("화면 유지 시간", "1시간"),
        ("자동 새로고침", "2시간마다"),
        ("페이지 자동 전환", "사용 안 함"),
        ("화면 밝기", "20%"),
        ("표시 언어", "한국어"),
        ("지역 설정", "수동 (동작구)"),
    ]
    fy = dy1 + 110
    for i, (lbl, val) in enumerate(fields):
        c = i % 2
        r = i // 2
        fx = dx1 + 40 + c * (col_w + 24)
        fy_row = fy + r * 92
        d.text((fx, fy_row), lbl, font=font(22, True), fill=60)
        draw_rounded_box(d, [fx, fy_row + 30, fx + col_w, fy_row + 76], radius=10, fill=248, outline=0, width=2)
        d.text((fx + 16, fy_row + 40), val, font=font(24, True), fill=0)
        d.text((fx + col_w - 36, fy_row + 40), "▼", font=font(20, False), fill=80)

    # New Section: Lower Page Toggles
    sec_y = dy1 + 406
    d.line([(dx1 + 32, sec_y), (dx2 - 32, sec_y)], fill=170, width=2)
    d.text((dx1 + 40, sec_y + 20), "하단 페이지 표시 설정 (날씨 고정 · 선택 토글)", font=font(26, True), fill=0)

    toggles = [
        ("시간별 · 주간 날씨", "기본 고정 (항상 표시)", True, True),
        ("오늘의 명언 (영화·애니 포함)", "표시 중", True, False),
        ("주요 뉴스 헤드라인", "표시 중", True, False),
        ("오늘의 책 추천", "표시 중", True, False),
        ("오늘의 영화 소개", "표시 중", True, False),
        ("주요 경제지표 (그래프)", "표시 안 함", False, False),
    ]

    ty = sec_y + 72
    row_h = 84
    for i, (title, sub, is_on, is_locked) in enumerate(toggles):
        ry = ty + i * row_h
        if i > 0:
            d.line([(dx1 + 40, ry), (dx2 - 40, ry)], fill=215, width=2)
        d.text((dx1 + 44, ry + 14), title, font=font(25, True), fill=0)
        d.text((dx1 + 44, ry + 48), sub, font=font(20, False), fill=90)

        # Toggle switch pill on right
        sw_w, sw_h = 92, 46
        sx1, sy1 = dx2 - 44 - sw_w, ry + 19
        sx2, sy2 = sx1 + sw_w, sy1 + sw_h
        if is_locked:
            draw_rounded_box(d, [sx1 - 56, sy1 + 4, sx2, sy2 - 4], radius=12, fill=232, outline=120, width=2)
            d.text((sx1 - 36, sy1 + 10), "고정 ON", font=font(21, True), fill=60)
        else:
            bg = 24 if is_on else 235
            draw_rounded_box(d, [sx1, sy1, sx2, sy2], radius=23, fill=bg, outline=0, width=2)
            knob_x = sx2 - 39 if is_on else sx1 + 5
            d.ellipse([knob_x, sy1 + 5, knob_x + 34, sy1 + 39], fill=255 if is_on else 80)

    # Bottom Buttons
    btn_y = dy2 - 86
    draw_rounded_box(d, [dx2 - 330, btn_y, dx2 - 190, btn_y + 56], radius=12, fill=245, outline=0, width=2)
    d.text((dx2 - 285, btn_y + 14), "취소", font=font(24, True), fill=0)
    draw_rounded_box(d, [dx2 - 170, btn_y, dx2 - 40, btn_y + 56], radius=12, fill=20, outline=0, width=2)
    d.text((dx2 - 130, btn_y + 14), "저장", font=font(24, True), fill=255)

    out_path = os.path.join(OUT_DIR, "sample_settings_pages_v3.png")
    img.save(out_path)
    print("Saved:", out_path)


if __name__ == "__main__":
    build_page4_books()
    build_page5_movies()
    build_page6_economy()
    build_settings_toggle_sample()
