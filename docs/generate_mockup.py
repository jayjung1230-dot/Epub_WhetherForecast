import os
import math
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
FONT_REG = r"C:\Windows\Fonts\malgun.ttf"
FONT_BOLD = r"C:\Windows\Fonts\malgunbd.ttf"

def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)

def draw_sun(draw, cx, cy, r, color=0, width=4):
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=width)
    for i in range(8):
        ang = math.radians(i * 45)
        x1 = cx + (r + 6) * math.cos(ang)
        y1 = cy + (r + 6) * math.sin(ang)
        x2 = cx + (r + 15) * math.cos(ang)
        y2 = cy + (r + 15) * math.sin(ang)
        draw.line([(x1, y1), (x2, y2)], fill=color, width=width)

def draw_cloud(draw, cx, cy, scale=1.0, fill=255, outline=0, width=4):
    s = scale
    draw.ellipse([cx - 32*s, cy - 10*s, cx - 2*s, cy + 18*s], fill=fill, outline=outline, width=width)
    draw.ellipse([cx - 16*s, cy - 24*s, cx + 18*s, cy + 10*s], fill=fill, outline=outline, width=width)
    draw.ellipse([cx + 4*s, cy - 12*s, cx + 32*s, cy + 18*s], fill=fill, outline=outline, width=width)
    draw.rectangle([cx - 18*s, cy - 2*s, cx + 18*s, cy + 17*s], fill=fill)
    draw.line([(cx - 18*s, cy + 18*s), (cx + 18*s, cy + 18*s)], fill=outline, width=width)

def draw_partly_cloudy(draw, cx, cy, scale=1.0):
    draw_sun(draw, cx - 14*scale, cy - 12*scale, 13*scale, color=0, width=max(2, int(3*scale)))
    draw_cloud(draw, cx + 6*scale, cy + 8*scale, scale=scale*0.95, fill=255, outline=0, width=max(2, int(3*scale)))

def draw_rain(draw, cx, cy, scale=1.0):
    draw_cloud(draw, cx, cy - 6*scale, scale=scale*0.9, fill=255, outline=0, width=max(2, int(3*scale)))
    for dx in (-14, 0, 14):
        x = cx + dx * scale
        draw.line([(x, cy + 16*scale), (x - 5*scale, cy + 28*scale)], fill=0, width=max(2, int(3*scale)))

def draw_weather_icon(draw, kind, cx, cy, scale=1.0):
    if kind == "sun":
        draw_sun(draw, cx, cy, 18*scale, color=0, width=max(2, int(4*scale)))
    elif kind == "partly":
        draw_partly_cloudy(draw, cx, cy, scale)
    elif kind == "cloud":
        draw_cloud(draw, cx, cy, scale, fill=240, outline=0, width=max(2, int(3*scale)))
    elif kind == "rain":
        draw_rain(draw, cx, cy, scale)

def draw_rounded_box(draw, box, radius=18, fill=255, outline=0, width=3):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)

def draw_common_top(d, W, M):
    # --- 1. Top Bar: Region (Auto) + Action Buttons ---
    d.text((M, 42), "서울특별시 강남구 (자동)", font=font(40, True), fill=0)
    d.text((M, 96), "최근 갱신: 07:10  ·  Wi-Fi 연결됨", font=font(24, False), fill=85)

    btn_h = 64
    btn_y = 44
    buttons = [
        ("지역", 96),
        ("설정", 96),
        ("새로고침", 136),
        ("닫기", 104),
    ]
    cur_x = W - M
    for label, bw in reversed(buttons):
        cur_x -= bw
        fill_c = 20 if label == "닫기" else 245
        txt_c = 255 if label == "닫기" else 0
        draw_rounded_box(d, [cur_x, btn_y, cur_x + bw, btn_y + btn_h], radius=14, fill=fill_c, outline=0, width=3)
        bbox = d.textbbox((0, 0), label, font=font(25, True))
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
        d.text((cur_x + (bw - tw) / 2, btn_y + (btn_h - th) / 2 - 4), label, font=font(25, True), fill=txt_c)
        cur_x -= 12

    d.line([(M, 140), (W - M, 140)], fill=0, width=3)

    # --- 2. Clock & Date Banner + Lunar Date + World Clocks (max 2) ---
    d.text((M, 166), "07:12", font=font(132, True), fill=0)
    d.text((M + 392, 178), "2026년 11월 14일 (토)", font=font(36, True), fill=0)
    d.text((M + 392, 228), "음력 10월 6일", font=font(28, True), fill=55)
    d.text((M + 392, 272), "일출 07:11  ·  일몰 17:22", font=font(26, False), fill=75)

    world_clocks = [
        ("뉴욕", "-14h", "17:12", "11.13 (금)"),
        ("런던", "-9h", "22:12", "11.13 (금)"),
    ]
    wc_w = 224
    wc_gap = 14
    wc_top, wc_bot = 166, 314
    wc_start_x = W - M - (wc_w * 2 + wc_gap)
    for i, (city, diff, wtime, wdate) in enumerate(world_clocks):
        wx1 = wc_start_x + i * (wc_w + wc_gap)
        wx2 = wx1 + wc_w
        draw_rounded_box(d, [wx1, wc_top, wx2, wc_bot], radius=16, fill=248, outline=0, width=3)
        d.text((wx1 + 16, wc_top + 14), f"{city} ({diff})", font=font(22, True), fill=60)
        d.text((wx1 + 16, wc_top + 48), wtime, font=font(46, True), fill=0)
        d.text((wx1 + 16, wc_top + 106), wdate, font=font(21, False), fill=75)

    # --- 3. Hero Weather Card ---
    hero_top, hero_bot = 340, 670
    draw_rounded_box(d, [M, hero_top, W - M, hero_bot], radius=24, fill=255, outline=0, width=4)
    draw_weather_icon(d, "partly", M + 150, hero_top + 130, scale=2.3)
    d.text((M + 75, hero_top + 235), "구름 조금", font=font(42, True), fill=0)
    d.text((M + 340, hero_top + 48), "9.4°", font=font(168, True), fill=0)

    rx = M + 740
    draw_rounded_box(d, [rx, hero_top + 52, W - M - 40, hero_top + 116], radius=14, fill=235, outline=0, width=2)
    d.text((rx + 24, hero_top + 66), "▼ 어제보다 2.1° 낮음", font=font(30, True), fill=0)
    d.text((rx, hero_top + 144), "최고  14°   /   최저  4°", font=font(36, True), fill=0)
    d.text((rx, hero_top + 206), "체감온도  7.2°", font=font(30, False), fill=60)
    d.text((rx, hero_top + 252), "강수확률  10%", font=font(30, False), fill=60)

    # --- 4. Environment Metrics Row ---
    met_top, met_bot = 696, 876
    cards = [
        ("미세먼지 (PM10)", "34 ㎍/㎥", "보통"),
        ("초미세 (PM2.5)", "16 ㎍/㎥", "보통"),
        ("자외선 지수", "3.8", "보통"),
        ("습도", "58 %", "쾌적"),
        ("풍속", "3.4 m/s", "북서풍"),
    ]
    gap = 16
    cw = (W - 2 * M - gap * 4) / 5
    for i, (title, val, sub) in enumerate(cards):
        x1 = int(M + i * (cw + gap))
        x2 = int(x1 + cw)
        draw_rounded_box(d, [x1, met_top, x2, met_bot], radius=18, fill=248, outline=0, width=3)
        d.text((x1 + 18, met_top + 20), title, font=font(22, True), fill=70)
        d.text((x1 + 18, met_top + 66), val, font=font(36, True), fill=0)
        draw_rounded_box(d, [x1 + 18, met_top + 124, x1 + 116, met_top + 162], radius=8, fill=255, outline=0, width=2)
        sb = d.textbbox((0, 0), sub, font=font(21, True))
        d.text((x1 + 18 + (98 - (sb[2] - sb[0])) / 2, met_top + 127), sub, font=font(21, True), fill=0)


def draw_footer(d, W, M, page_idx, page_name):
    d.text((M, 1804), "자동 갱신: 30분마다  ·  화면 유지: 1시간  ·  밝기: 40%", font=font(23, False), fill=80)
    ind = f"◀  {page_idx} / 3 · {page_name}  ▶"
    ib = d.textbbox((0, 0), ind, font=font(24, True))
    d.text((W - M - (ib[2] - ib[0]), 1802), ind, font=font(24, True), fill=0)


def build_dashboard():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    # --- 5. Hourly Forecast ---
    hr_top, hr_bot = 906, 1186
    draw_rounded_box(d, [M, hr_top, W - M, hr_bot], radius=22, fill=255, outline=0, width=3)
    d.text((M + 28, hr_top + 20), "시간별 날씨", font=font(32, True), fill=0)
    d.line([(M + 24, hr_top + 72), (W - M - 24, hr_top + 72)], fill=160, width=2)

    hourly = [
        ("09시", "partly", "10°", "10%"),
        ("12시", "sun", "13°", "0%"),
        ("15시", "sun", "14°", "0%"),
        ("18시", "partly", "11°", "10%"),
        ("21시", "cloud", "8°", "20%"),
        ("00시", "cloud", "6°", "20%"),
        ("03시", "rain", "5°", "60%"),
        ("06시", "rain", "4°", "70%"),
    ]
    inner_w = (W - 2 * M - 40) / len(hourly)
    for i, (t, ic, tmp, pop) in enumerate(hourly):
        cx = int(M + 20 + i * inner_w + inner_w / 2)
        if i > 0:
            vx = int(M + 20 + i * inner_w)
            d.line([(vx, hr_top + 88), (vx, hr_bot - 22)], fill=215, width=2)
        tb = d.textbbox((0, 0), t, font=font(26, True))
        d.text((cx - (tb[2] - tb[0]) / 2, hr_top + 88), t, font=font(26, True), fill=50)
        draw_weather_icon(d, ic, cx, hr_top + 162, scale=0.80)
        tpb = d.textbbox((0, 0), tmp, font=font(34, True))
        d.text((cx - (tpb[2] - tpb[0]) / 2, hr_top + 202), tmp, font=font(34, True), fill=0)
        pb = d.textbbox((0, 0), f"강수 {pop}", font=font(20, False))
        d.text((cx - (pb[2] - pb[0]) / 2, hr_top + 246), f"강수 {pop}", font=font(20, False), fill=80)

    # --- 6. Weekly Forecast (7 Days) ---
    wk_top, wk_bot = 1216, 1776
    draw_rounded_box(d, [M, wk_top, W - M, wk_bot], radius=22, fill=255, outline=0, width=3)
    d.text((M + 28, wk_top + 20), "주간 날씨 (7일 예보)", font=font(32, True), fill=0)
    d.line([(M + 24, wk_top + 72), (W - M - 24, wk_top + 72)], fill=160, width=2)

    weekly = [
        ("오늘", "11.14", "partly", "구름 조금", "10%", 4, 14),
        ("일", "11.15", "rain", "비 후 갬", "70%", 3, 11),
        ("월", "11.16", "sun", "맑음", "0%", 1, 10),
        ("화", "11.17", "sun", "맑음", "0%", 2, 12),
        ("수", "11.18", "partly", "구름 많음", "20%", 4, 13),
        ("목", "11.19", "cloud", "흐림", "30%", 5, 12),
        ("금", "11.20", "sun", "맑음", "10%", 2, 11),
    ]
    g_min, g_max = 0, 16
    row_h = (wk_bot - (wk_top + 82) - 14) / len(weekly)
    bar_x1, bar_x2 = M + 740, W - M - 190

    for i, (day, dt, ic, desc, pop, lo, hi) in enumerate(weekly):
        ry = int(wk_top + 84 + i * row_h)
        if i > 0:
            d.line([(M + 28, ry), (W - M - 28, ry)], fill=225, width=2)
        cy = int(ry + row_h / 2)
        d.text((M + 36, cy - 20), f"{day} ({dt})", font=font(28, True if i == 0 else False), fill=0)
        draw_weather_icon(d, ic, M + 280, cy, scale=0.68)
        d.text((M + 340, cy - 18), desc, font=font(26, False), fill=30)
        d.text((M + 530, cy - 16), f"강수 {pop}", font=font(24, False), fill=80)

        d.text((bar_x1 - 76, cy - 18), f"{lo}°", font=font(28, False), fill=60)
        d.rounded_rectangle([bar_x1, cy - 8, bar_x2, cy + 8], radius=8, fill=230, outline=160, width=1)
        sx = bar_x1 + (lo - g_min) / (g_max - g_min) * (bar_x2 - bar_x1)
        ex = bar_x1 + (hi - g_min) / (g_max - g_min) * (bar_x2 - bar_x1)
        d.rounded_rectangle([sx, cy - 8, ex, cy + 8], radius=8, fill=30)
        d.text((bar_x2 + 24, cy - 20), f"{hi}°", font=font(28, True), fill=0)

    # --- 7. Footer Status ---
    draw_footer(d, W, M, 1, "날씨 예보")

    out_path = os.path.join(OUT_DIR, "weather_dashboard_sample.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_page2_quotes():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "오늘의 명언  ·  노력과 가능성", font=font(32, True), fill=0)
    # Tag pill on top right of quote card
    draw_rounded_box(d, [W - M - 230, box_top + 22, W - M - 32, box_top + 66], radius=12, fill=240, outline=0, width=2)
    d.text((W - M - 208, box_top + 30), "동기부여 · 실행", font=font(22, True), fill=40)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    # Main Featured Quote
    d.text((M + 56, box_top + 132), "“", font=font(110, True), fill=160)
    quote_lines = [
        "게으름은 쇠에 스며드는 녹과 같다.",
        "사용하지 않는 열쇠는 녹슬지만,",
        "끊임없이 쓰는 열쇠는 언제나 빛난다.",
    ]
    qy = box_top + 240
    for line in quote_lines:
        d.text((M + 72, qy), line, font=font(46, True), fill=0)
        qy += 76

    d.text((M + 72, qy + 24), "— 벤자민 프랭클린 (Benjamin Franklin)", font=font(30, False), fill=70)

    # Secondary sub-quote card at bottom of panel
    sub_top, sub_bot = box_top + 560, box_bot - 36
    draw_rounded_box(d, [M + 36, sub_top, W - M - 36, sub_bot], radius=18, fill=246, outline=0, width=2)
    d.text((M + 64, sub_top + 24), "오늘의 한 줄 점검", font=font(24, True), fill=60)
    d.text((M + 64, sub_top + 72), "“완벽한 준비를 기다리다 보면, 시작할 수 있는 날은 결코 오지 않는다.”", font=font(30, True), fill=0)
    d.text((M + 64, sub_top + 128), "— 이반 투르게네프 (Ivan Turgenev)", font=font(24, False), fill=80)
    d.text((M + 64, sub_top + 186), "· 탭하거나 물리버튼을 눌러 다른 명언 / 뉴스 페이지로 전환할 수 있습니다.", font=font(22, False), fill=100)

    draw_footer(d, W, M, 2, "오늘의 명언")
    out_path = os.path.join(OUT_DIR, "sample_page2_quotes.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_page3_news():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "주요 뉴스 헤드라인", font=font(32, True), fill=0)
    d.text((W - M - 320, box_top + 32), "Google 뉴스 KR · 방금 전", font=font(23, False), fill=85)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    news_items = [
        ("경제", "반도체·AI 수출 호조 지속… 11월 중순 무역수지 흑자 폭 확대", "연합뉴스 · 12분 전"),
        ("사회", "주말 전국 비 그친 뒤 밤부터 찬 공기 유입… 내일 아침 기온 뚝", "KBS 뉴스 · 25분 전"),
        ("IT/과학", "차세대 저전력 전자잉크 디스플레이 기술, 야외 시인성 30% 개선", "지디넷코리아 · 41분 전"),
        ("국제", "미 연준, 물가 둔화세 점검 속 금리 경로 신중론 유지", "한국경제 · 1시간 전"),
        ("생활/문화", "가을 끝자락 도서관·독서 모임 열기… 전자책 이용 시간 전년비 증가", "한겨레 · 1시간 전"),
        ("경제", "수도권 광역교통망 환승 할인 개편안 다음 달 시범 운영", "매일경제 · 2시간 전"),
    ]
    row_h = (box_bot - (box_top + 96) - 20) / len(news_items)
    for i, (cat, title, meta) in enumerate(news_items):
        ry = int(box_top + 94 + i * row_h)
        if i > 0:
            d.line([(M + 32, ry), (W - M - 32, ry)], fill=220, width=2)
        draw_rounded_box(d, [M + 36, ry + 20, M + 150, ry + 58], radius=10, fill=238, outline=0, width=2)
        cb = d.textbbox((0, 0), cat, font=font(21, True))
        d.text((M + 36 + (114 - (cb[2] - cb[0])) / 2, ry + 24), cat, font=font(21, True), fill=0)
        d.text((M + 172, ry + 20), title, font=font(28, True), fill=0)
        d.text((M + 172, ry + 64), meta, font=font(22, False), fill=90)

    draw_footer(d, W, M, 3, "주요 뉴스")
    out_path = os.path.join(OUT_DIR, "sample_page3_news.png")
    img.save(out_path)
    print("Saved:", out_path)


if __name__ == "__main__":
    build_dashboard()
    build_page2_quotes()
    build_page3_news()
