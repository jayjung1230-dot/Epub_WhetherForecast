import os
from PIL import Image, ImageDraw
from generate_mockup import (
    OUT_DIR,
    font,
    draw_weather_icon,
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


def build_page1_hourly_v2():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    # --- 5. Hourly Forecast (00:00 today ~ 06:00 tomorrow + Temp Graph + Current Hour Highlight) ---
    hr_top, hr_bot = 906, 1254
    draw_rounded_box(d, [M, hr_top, W - M, hr_bot], radius=22, fill=255, outline=0, width=3)
    d.text((M + 28, hr_top + 18), "시간별 날씨", font=font(32, True), fill=0)
    d.text((M + 215, hr_top + 24), "오늘 00시 ~ 내일 06시", font=font(24, False), fill=85)

    # Legend pill on top right
    badge_text = "현재 구간: 06시~09시"
    bb = d.textbbox((0, 0), badge_text, font=font(21, True))
    bw = (bb[2] - bb[0]) + 32
    draw_rounded_box(d, [W - M - 24 - bw, hr_top + 18, W - M - 24, hr_top + 58], radius=10, fill=24, outline=0, width=2)
    d.text((W - M - 24 - bw + 16, hr_top + 25), badge_text, font=font(21, True), fill=255)

    d.line([(M + 24, hr_top + 70), (W - M - 24, hr_top + 70)], fill=170, width=2)

    # 11 slots (3-hour intervals from today 00:00 to tomorrow 06:00)
    hourly = [
        ("00시", "cloud", 5, "10%", False, False),
        ("03시", "cloud", 4, "10%", False, False),
        ("06시", "partly", 4, "10%", True, False),   # Current slot (clock is 07:12)
        ("09시", "partly", 9, "10%", False, False),
        ("12시", "sun", 13, "0%", False, False),
        ("15시", "sun", 14, "0%", False, False),
        ("18시", "partly", 11, "10%", False, False),
        ("21시", "cloud", 8, "20%", False, False),
        ("00시", "cloud", 6, "20%", False, True),    # Tomorrow 00:00
        ("03시", "rain", 5, "60%", False, True),
        ("06시", "rain", 4, "70%", False, True),
    ]

    pad_x = 20
    inner_w = (W - 2 * M - 2 * pad_x) / len(hourly)
    col_top = hr_top + 82
    col_bot = hr_bot - 14

    # Draw column backgrounds & dividers first
    for i, (t, ic, tmp, pop, is_cur, is_tmr) in enumerate(hourly):
        x1 = int(M + pad_x + i * inner_w)
        x2 = int(M + pad_x + (i + 1) * inner_w)
        if is_cur:
            draw_rounded_box(
                d, [x1 + 3, col_top, x2 - 3, col_bot],
                radius=14, fill=236, outline=0, width=3
            )
        elif i > 0:
            if t == "00시" and is_tmr:
                # Tomorrow boundary dashed-style thick divider
                for dy in range(col_top + 6, col_bot - 6, 12):
                    d.line([(x1, dy), (x1, min(dy + 6, col_bot - 6))], fill=90, width=2)
            else:
                d.line([(x1, col_top + 8), (x1, col_bot - 8)], fill=224, width=2)

    # Temperature graph coordinate calculation
    temps = [item[2] for item in hourly]
    t_min, t_max = min(temps), max(temps)
    t_span = max(1, t_max - t_min)
    graph_top = hr_top + 216
    graph_bot = hr_top + 286

    pts = []
    for i, item in enumerate(hourly):
        cx = int(M + pad_x + i * inner_w + inner_w / 2)
        norm = (item[2] - t_min) / t_span
        py = int(graph_bot - norm * (graph_bot - graph_top))
        pts.append((cx, py))

    # Subtle horizontal reference line at average temp
    mid_y = (graph_top + graph_bot) // 2
    for dx in range(M + pad_x + 16, W - M - pad_x - 16, 14):
        d.line([(dx, mid_y), (min(dx + 6, W - M - pad_x - 16), mid_y)], fill=215, width=1)

    # Draw temperature polyline
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=40, width=4)

    # Draw per-column labels, icons, graph nodes, and precip
    for i, (t, ic, tmp, pop, is_cur, is_tmr) in enumerate(hourly):
        cx, py = pts[i]

        # Day tag for tomorrow 00시 or current pill
        if is_cur:
            draw_rounded_box(d, [cx - 42, col_top + 8, cx + 42, col_top + 42], radius=10, fill=20, outline=0, width=2)
            tb = d.textbbox((0, 0), t, font=font(22, True))
            d.text((cx - (tb[2] - tb[0]) / 2, col_top + 11), t, font=font(22, True), fill=255)
        else:
            if t == "00시" and is_tmr:
                nb = d.textbbox((0, 0), "내일", font=font(15, True))
                d.text((cx - (nb[2] - nb[0]) / 2, col_top + 1), "내일", font=font(15, True), fill=80)
                t_y = col_top + 18
            else:
                t_y = col_top + 12
            tb = d.textbbox((0, 0), t, font=font(22, True if t == "00시" else False))
            d.text((cx - (tb[2] - tb[0]) / 2, t_y), t, font=font(22, True if t == "00시" else False), fill=40)

        # Weather icon
        draw_weather_icon(d, ic, cx, hr_top + 150, scale=0.62)

        # Temperature label above node
        tmp_str = f"{tmp}°"
        tpb = d.textbbox((0, 0), tmp_str, font=font(26, True))
        d.text((cx - (tpb[2] - tpb[0]) / 2, py - 38), tmp_str, font=font(26, True), fill=0)

        # Graph node
        if is_cur:
            d.ellipse([cx - 9, py - 9, cx + 9, py + 9], fill=0, outline=255, width=2)
            d.ellipse([cx - 3, py - 3, cx + 3, py + 3], fill=255)
        else:
            d.ellipse([cx - 6, py - 6, cx + 6, py + 6], fill=255, outline=20, width=3)

        # Precipitation %
        pb = d.textbbox((0, 0), pop, font=font(19, True if is_cur else False))
        d.text((cx - (pb[2] - pb[0]) / 2, col_bot - 32), pop, font=font(19, True if is_cur else False), fill=20 if is_cur else 90)

    # --- 6. Weekly Forecast (7 Days) ---
    wk_top, wk_bot = 1278, 1776
    draw_rounded_box(d, [M, wk_top, W - M, wk_bot], radius=22, fill=255, outline=0, width=3)
    d.text((M + 28, wk_top + 18), "주간 날씨 (7일 예보)", font=font(32, True), fill=0)
    d.line([(M + 24, wk_top + 68), (W - M - 24, wk_top + 68)], fill=160, width=2)

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
    row_h = (wk_bot - (wk_top + 78) - 12) / len(weekly)
    bar_x1, bar_x2 = M + 740, W - M - 190

    for i, (day, dt, ic, desc, pop, lo, hi) in enumerate(weekly):
        ry = int(wk_top + 80 + i * row_h)
        if i > 0:
            d.line([(M + 28, ry), (W - M - 28, ry)], fill=225, width=2)
        cy = int(ry + row_h / 2)
        d.text((M + 36, cy - 19), f"{day} ({dt})", font=font(27, True if i == 0 else False), fill=0)
        draw_weather_icon(d, ic, M + 280, cy, scale=0.64)
        d.text((M + 340, cy - 17), desc, font=font(25, False), fill=30)
        d.text((M + 530, cy - 15), f"강수 {pop}", font=font(23, False), fill=80)

        d.text((bar_x1 - 76, cy - 17), f"{lo}°", font=font(27, False), fill=60)
        d.rounded_rectangle([bar_x1, cy - 8, bar_x2, cy + 8], radius=8, fill=230, outline=160, width=1)
        sx = bar_x1 + (lo - g_min) / (g_max - g_min) * (bar_x2 - bar_x1)
        ex = bar_x1 + (hi - g_min) / (g_max - g_min) * (bar_x2 - bar_x1)
        d.rounded_rectangle([sx, cy - 8, ex, cy + 8], radius=8, fill=30)
        d.text((bar_x2 + 24, cy - 19), f"{hi}°", font=font(27, True), fill=0)

    draw_footer(d, W, M, 1, "날씨 예보")
    out_path = os.path.join(OUT_DIR, "sample_page1_hourly_v2.png")
    img.save(out_path)
    print("Saved:", out_path)


def build_page2_quotes_v2():
    W, H = 1404, 1872
    M = 48
    img = Image.new("L", (W, H), 255)
    d = ImageDraw.Draw(img)
    draw_common_top(d, W, M)

    box_top, box_bot = 906, 1776
    draw_rounded_box(d, [M, box_top, W - M, box_bot], radius=24, fill=255, outline=0, width=3)
    d.text((M + 36, box_top + 26), "오늘의 명언  ·  기억과 인연", font=font(32, True), fill=0)

    # Category pill on top right
    cat_label = "애니메이션 명대사"
    cb = d.textbbox((0, 0), cat_label, font=font(22, True))
    cw = (cb[2] - cb[0]) + 36
    draw_rounded_box(d, [W - M - 32 - cw, box_top + 22, W - M - 32, box_top + 66], radius=12, fill=24, outline=0, width=2)
    d.text((W - M - 32 - cw + 18, box_top + 30), cat_label, font=font(22, True), fill=255)
    d.line([(M + 28, box_top + 84), (W - M - 28, box_top + 84)], fill=160, width=2)

    # Main Featured Quote (Movie / Animation)
    d.text((M + 56, box_top + 114), "“", font=font(108, True), fill=160)
    quote_lines = [
        "한 번 만난 인연은 잊혀지지 않는 거야.",
        "다만 기억해내지 못할 뿐이지.",
    ]
    qy = box_top + 216
    for line in quote_lines:
        d.text((M + 72, qy), line, font=font(46, True), fill=0)
        qy += 78

    d.text(
        (M + 72, qy + 20),
        "— 제니바 · 영화 《센과 치히로의 행방불명》 (2001)",
        font=font(29, False),
        fill=70,
    )

    # Context / Source / Interpretation Box (replaces "오늘의 한 줄 점검")
    sub_top, sub_bot = box_top + 480, box_bot - 34
    draw_rounded_box(d, [M + 36, sub_top, W - M - 36, sub_bot], radius=18, fill=246, outline=0, width=2)

    # Section title bar inside box
    draw_rounded_box(d, [M + 62, sub_top + 24, M + 324, sub_top + 62], radius=8, fill=232, outline=0, width=1)
    d.text((M + 78, sub_top + 29), "작품 출처 · 배경 및 해석", font=font(22, True), fill=20)

    max_text_w = (W - M - 64) - (M + 64)
    f_label = font(24, True)
    f_body = font(25, False)

    cur_y = sub_top + 86
    sections = [
        (
            "작품 배경",
            "미야자키 하야오 감독의 스튜디오 지브리 장편 애니메이션 《센과 치히로의 행방불명》에서, "
            "치히로가 하쿠의 본명을 떠올리기 전 제니바가 건네는 따뜻한 조언입니다.",
        ),
        (
            "명대사 해석",
            "살아가며 스쳐 간 소중한 인연과 경험은 완전히 사라지는 것이 아니라 마음 깊은 곳에 남아 있으며, "
            "우리가 스스로를 다시 찾을 때 든든한 힘이 되어 준다는 의미를 담고 있습니다.",
        ),
    ]
    for title, body in sections:
        d.text((M + 64, cur_y), f"■ {title}", font=f_label, fill=20)
        cur_y += 38
        for line in wrap_text(d, body, f_body, max_text_w):
            d.text((M + 64, cur_y), line, font=f_body, fill=45)
            cur_y += 36
        cur_y += 14

    d.text(
        (M + 64, sub_bot - 42),
        "· 탭하거나 물리버튼을 눌러 날씨 / 명언 / 뉴스 페이지로 전환할 수 있습니다.",
        font=font(21, False),
        fill=100,
    )

    draw_footer(d, W, M, 2, "오늘의 명언")
    out_path = os.path.join(OUT_DIR, "sample_page2_quotes_v2.png")
    img.save(out_path)
    print("Saved:", out_path)


if __name__ == "__main__":
    build_page1_hourly_v2()
    build_page2_quotes_v2()
